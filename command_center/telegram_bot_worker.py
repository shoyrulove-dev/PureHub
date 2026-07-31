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
GITHUB_URL = "https://github.com/shoyrulove-dev/PureHub"
PUREHUB_URL = "https://hub.blissbiovn.com"

RELEASE_ANNOUNCEMENT = (
    "<b>PureHub vừa được cập nhật toàn diện ✨</b>\n\n"
    "• Giao diện mới hiện đại, nhẹ và thân thiện hơn\n"
    "• Điều hướng Trang chủ · Công cụ · Cộng đồng · Cài đặt\n"
    "• 22 mini app được đồng bộ và cải thiện trải nghiệm\n"
    "• Thêm tìm kiếm, yêu thích, lịch sử dùng và theme sáng/tối\n"
    "• Tiếp tục cam kết: miễn phí, không quảng cáo, mã nguồn mở\n\n"
    f"Trải nghiệm PureHub: {PUREHUB_URL}\n"
    f"Đóng góp trên GitHub: {GITHUB_URL}\n\n"
    "Cảm ơn mọi người đã đồng hành và góp ý để PureHub ngày càng hữu ích hơn 💚"
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

    def publish_release_update(self) -> dict[str, str]:
        token = get_config_value("telegram_bot_token")
        chat_id = get_config_value("telegram_notify_chat_id")
        if not token:
            raise RuntimeError("Missing Telegram bot token in config table.")
        if not chat_id:
            raise RuntimeError("Missing Telegram notify chat ID in config table.")

        bot = telebot.TeleBot(token, parse_mode="HTML")
        bot.set_my_commands(
            [
                telebot.types.BotCommand("start", "Tham gia cộng đồng PureHub"),
                telebot.types.BotCommand("about", "Giới thiệu PureHub"),
                telebot.types.BotCommand("github", "Xem mã nguồn và đóng góp"),
            ]
        )
        bot.set_my_short_description("PureHub — công cụ miễn phí, không quảng cáo, mã nguồn mở.")
        bot.set_my_description(
            "Cộng đồng PureHub: 22 tiện ích thân thiện, miễn phí cho mọi người, "
            "không quảng cáo và phát triển minh bạch trên GitHub."
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
                "<b>PureHub</b> là bộ công cụ miễn phí, không quảng cáo và mã nguồn mở.\n\n"
                f"Trải nghiệm: {PUREHUB_URL}\n"
                f"GitHub: {GITHUB_URL}",
                disable_web_page_preview=True,
            )

        @bot.message_handler(commands=["github"])
        def handle_github(message: telebot.types.Message) -> None:  # pragma: no cover - runtime integration
            bot.send_message(
                message.chat.id,
                f"Xem mã nguồn, báo lỗi hoặc đóng góp cho PureHub tại:\n{GITHUB_URL}",
                disable_web_page_preview=True,
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
