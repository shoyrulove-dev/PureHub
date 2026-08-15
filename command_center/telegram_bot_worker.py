from __future__ import annotations

import json
import threading
import time
from dataclasses import dataclass
from html import escape

import telebot

try:
    from .release_hub import generate_reply_draft
except ImportError:
    from release_hub import generate_reply_draft

try:
    from .database import (
        get_config_value,
        get_user,
        increment_invites,
        mark_reward_sent,
        upsert_user,
    )
except ImportError:
    from database import (
        get_config_value,
        get_user,
        increment_invites,
        mark_reward_sent,
        upsert_user,
    )

INVITE_GOAL = 3
GITHUB_URL = "https://github.com/shoyrulove-dev/PureHub"
PUREHUB_URL = "https://hub.blissbiovn.com"

RELEASE_ANNOUNCEMENT = (
    "<b>PureHub has a new major update</b>\n\n"
    "- A cleaner, faster, and more friendly interface\n"
    "- Home, Tools, Community, and Settings navigation\n"
    "- 25 mini apps with improved usability\n"
    "- Search, favorites, recent tools, and light/dark themes\n"
    "- Free for everyone, no ads, and open source\n\n"
    f"Try PureHub: {PUREHUB_URL}\n"
    f"Contribute: {GITHUB_URL}\n\n"
    "Thank you for helping us build useful tools for everyone."
)


@dataclass
class BotRuntimeState:
    running: bool = False
    thread_name: str | None = None
    last_error: str = ""


class TelegramBotManager:
    def __init__(self) -> None:
        self._bot: telebot.TeleBot | None = None
        self._thread: threading.Thread | None = None
        self._state = BotRuntimeState()
        self._ai_last_reply_at: dict[int, float] = {}
        self._bot_lock = threading.Lock()

    @property
    def state(self) -> BotRuntimeState:
        return self._state

    def start(self) -> BotRuntimeState:
        token = get_config_value("telegram_bot_token")
        username = get_config_value("telegram_bot_username")
        if not token:
            raise RuntimeError("Missing Telegram bot token in config table.")
        if not username:
            raise RuntimeError("Missing Telegram bot username in config table.")

        if self._thread and self._thread.is_alive():
            self._state.running = True
            return self._state

        bot = self._get_or_create_bot(token, username)
        thread = threading.Thread(
            target=self._polling_loop,
            args=(bot,),
            daemon=True,
            name="purehub-telegram-bot",
        )
        thread.start()

        self._bot = bot
        self._thread = thread
        self._state.running = True
        self._state.thread_name = thread.name
        self._state.last_error = ""
        return self._state

    def process_webhook(self, payload: dict[str, object]) -> None:
        token = get_config_value("telegram_bot_token")
        username = get_config_value("telegram_bot_username")
        if not token or not username:
            raise RuntimeError("Telegram bot credentials are not configured.")
        bot = self._get_or_create_bot(token, username)
        update = telebot.types.Update.de_json(json.dumps(payload))
        bot.process_new_updates([update])

    def stop(self) -> BotRuntimeState:
        if self._bot:
            self._bot.stop_polling()
        self._state.running = False
        return self._state

    def publish_release_update(self) -> dict[str, str]:
        token = get_config_value("telegram_bot_token")
        chat_id = get_config_value("telegram_notify_chat_id").strip()
        if not token:
            raise RuntimeError("Missing Telegram bot token in config table.")
        if not chat_id:
            raise RuntimeError("Missing Telegram notify chat ID in config table.")

        bot = telebot.TeleBot(token, parse_mode="HTML")
        bot.set_my_commands(
            [
                telebot.types.BotCommand("start", "Join the PureHub community"),
                telebot.types.BotCommand("about", "Learn about PureHub"),
                telebot.types.BotCommand("github", "View source code and contribute"),
                telebot.types.BotCommand("ask", "Ask the PureHub helper"),
            ]
        )
        bot.set_my_short_description("Free, no-ads, open-source everyday tools.")
        bot.set_my_description(
            "PureHub is a community-built collection of 25 useful tools. Free for everyone, "
            "with no ads, no surprise paywalls, and open-source development on GitHub."
        )
        message = bot.send_message(chat_id, RELEASE_ANNOUNCEMENT, disable_web_page_preview=True)
        return {
            "chat_id": str(message.chat.id),
            "message_id": str(message.message_id),
        }

    def _polling_loop(self, bot: telebot.TeleBot) -> None:
        try:
            bot.infinity_polling(skip_pending=True, timeout=30, long_polling_timeout=30)
        except Exception as exc:  # pragma: no cover - runtime integration
            self._state.last_error = str(exc)
            self._state.running = False

    def _get_or_create_bot(self, token: str, username: str) -> telebot.TeleBot:
        with self._bot_lock:
            if self._bot is None:
                # Webhook handlers must finish before a serverless invocation returns.
                self._bot = telebot.TeleBot(token, parse_mode="HTML", threaded=False)
                self._register_handlers(self._bot, username)
            return self._bot

    def _register_handlers(self, bot: telebot.TeleBot, username: str) -> None:
        @bot.message_handler(commands=["start"])
        def handle_start(message: telebot.types.Message) -> None:  # pragma: no cover - runtime integration
            text = message.text or "/start"
            parts = text.split(maxsplit=1)
            referral_arg = parts[1].strip() if len(parts) > 1 else ""
            user_id = int(message.from_user.id)
            referral_code = f"ref_{user_id}"
            referrer_id = self._extract_referrer_id(referral_arg)

            current_user = get_user(user_id)
            if not current_user:
                upsert_user(
                    user_id=user_id,
                    referral_code=referral_code,
                    referred_by=referrer_id if referrer_id and referrer_id != user_id else None,
                )

                if referrer_id and referrer_id != user_id:
                    self._reward_referrer_if_needed(bot, referrer_id, user_id)

            deep_link = f"https://t.me/{username}?start=ref_{user_id}"
            reply = (
                "Welcome to the <b>PureHub Community</b>.\n\n"
                "PureHub is free, open source, and has no ads. Every tool is available without a code.\n\n"
                f"Your invite link:\n{deep_link}\n\n"
                f"Invite <b>{INVITE_GOAL}</b> friends to earn a Community Supporter thank-you."
            )
            bot.send_message(message.chat.id, reply)

        @bot.message_handler(commands=["about"])
        def handle_about(message: telebot.types.Message) -> None:  # pragma: no cover - runtime integration
            bot.send_message(
                message.chat.id,
                "<b>PureHub</b> is a free, no-ads, open-source collection of everyday tools.\n\n"
                f"Try it: {PUREHUB_URL}\n"
                f"GitHub: {GITHUB_URL}",
                disable_web_page_preview=True,
            )

        @bot.message_handler(commands=["github"])
        def handle_github(message: telebot.types.Message) -> None:  # pragma: no cover - runtime integration
            bot.send_message(
                message.chat.id,
                f"View the source, report an issue, or contribute to PureHub:\n{GITHUB_URL}",
                disable_web_page_preview=True,
            )

        @bot.message_handler(commands=["ask"])
        def handle_ask(message: telebot.types.Message) -> None:  # pragma: no cover - runtime integration
            question = (message.text or "").partition(" ")[2].strip()
            if not question:
                bot.send_message(message.chat.id, "Try <code>/ask How do I export a PDF?</code>")
                return
            self._send_ai_reply(bot, message, question)

        @bot.message_handler(
            func=lambda message: (
                get_config_value("community_reply_mode", "draft") == "auto"
                and message.chat.type == "private"
                and bool(message.text)
                and not str(message.text).startswith("/")
            ),
            content_types=["text"],
        )
        def handle_private_auto_reply(message: telebot.types.Message) -> None:  # pragma: no cover - runtime integration
            self._send_ai_reply(bot, message, str(message.text))

    def _send_ai_reply(
        self,
        bot: telebot.TeleBot,
        message: telebot.types.Message,
        question: str,
    ) -> None:
        user_id = int(message.from_user.id)
        now = time.monotonic()
        if now - self._ai_last_reply_at.get(user_id, 0.0) < 15:
            bot.send_message(message.chat.id, "Please wait a few seconds before asking again.")
            return
        self._ai_last_reply_at[user_id] = now
        try:
            reply = generate_reply_draft(
                question[:4000],
                "PureHub is free, no-ads, privacy-first, and open source. Give support guidance only; do not invent shipped fixes.",
            )
            bot.send_message(message.chat.id, escape(reply[:3800]))
        except Exception:
            bot.send_message(
                message.chat.id,
                "The AI helper is temporarily unavailable. Please report the issue on GitHub or try again later.",
            )

    def _reward_referrer_if_needed(
        self,
        bot: telebot.TeleBot,
        referrer_id: int,
        invited_user_id: int,
    ) -> None:
        referrer = increment_invites(referrer_id)
        if not referrer:
            return

        notify_message = (
            f"You got a new referral from <code>{invited_user_id}</code>.\n"
            f"Current invites: <b>{referrer['invites_count']}</b>"
        )
        try:
            bot.send_message(referrer_id, notify_message)
        except Exception:
            pass

        should_reward = int(referrer["invites_count"]) >= INVITE_GOAL and not referrer.get("reward_sent_at")
        if should_reward:
            try:
                bot.send_message(
                    referrer_id,
                    f"You reached {INVITE_GOAL} invites. Thank you for helping a free, no-ads project grow.\n"
                    "You are now recognized as a <b>PureHub Community Supporter</b>. No features are locked behind this badge.",
                )
                mark_reward_sent(referrer_id)
            except Exception:
                pass

    @staticmethod
    def _extract_referrer_id(referral_arg: str) -> int | None:
        if not referral_arg.startswith("ref_"):
            return None
        try:
            return int(referral_arg.replace("ref_", "", 1))
        except ValueError:
            return None


telegram_bot_manager = TelegramBotManager()
