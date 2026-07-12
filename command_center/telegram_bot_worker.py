from __future__ import annotations

import threading
from dataclasses import dataclass

import telebot

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

        bot = telebot.TeleBot(token, parse_mode="HTML")
        self._register_handlers(bot, username)
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

    def stop(self) -> BotRuntimeState:
        if self._bot:
            self._bot.stop_polling()
        self._state.running = False
        return self._state

    def _polling_loop(self, bot: telebot.TeleBot) -> None:
        try:
            bot.infinity_polling(skip_pending=True, timeout=30, long_polling_timeout=30)
        except Exception as exc:  # pragma: no cover - runtime integration
            self._state.last_error = str(exc)
            self._state.running = False

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
            pro_code = get_config_value("pro_unlock_code", "PUREHUB-PRO-2026")
            reply = (
                "Welcome to <b>PureHub Command Center</b>.\n\n"
                f"Your invite link:\n{deep_link}\n\n"
                f"Invite <b>{INVITE_GOAL}</b> friends to unlock your Pro code automatically.\n"
                f"Current reward code: <code>{pro_code}</code>"
            )
            bot.send_message(message.chat.id, reply)

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
            pro_code = get_config_value("pro_unlock_code", "PUREHUB-PRO-2026")
            try:
                bot.send_message(
                    referrer_id,
                    f"You reached {INVITE_GOAL} invites.\nYour Pro Unlock Code: <code>{pro_code}</code>",
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
