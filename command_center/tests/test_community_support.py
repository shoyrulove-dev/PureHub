from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock
from unittest.mock import patch

from command_center.community_support import (
    _analyze_message,
    _looks_like_question,
    _looks_like_relevant_opportunity,
    _plain_text,
    discover_opportunities,
    generate_support_draft,
    ingest_telegram_update,
)
from command_center.database import delete_support_message, infer_support_inbox_type, list_support_messages, upsert_support_message
from command_center.main import support_bulk_approve_action, support_bulk_send_action, support_complete_manual_action


class CommunitySupportTests(unittest.TestCase):
    @patch("command_center.community_support._ai_client")
    def test_empty_ai_reply_uses_nonempty_fallback(self, ai_client) -> None:
        client = MagicMock()
        client.chat.completions.create.return_value = SimpleNamespace(
            choices=[SimpleNamespace(message=SimpleNamespace(content='{"category":"opportunity","priority":"normal","language":"en","requires_reply":true,"draft":""}'))]
        )
        ai_client.return_value = (client, "test-model")

        result = _analyze_message(
            {
                "platform": "mastodon",
                "content": "Does anyone want this without Android?",
                "reply_context": {"source_kind": "discovery"},
            }
        )

        self.assertTrue(result["requires_reply"])
        self.assertTrue(result["draft"].strip())
        self.assertIn("I build PureHub", result["draft"])

    @patch("command_center.community_support._ai_client")
    def test_question_heuristic_has_draft_when_ai_says_no_reply(self, ai_client) -> None:
        client = MagicMock()
        client.chat.completions.create.return_value = SimpleNamespace(
            choices=[SimpleNamespace(message=SimpleNamespace(content='{"category":"other","priority":"low","language":"en","requires_reply":false,"draft":""}'))]
        )
        ai_client.return_value = (client, "test-model")

        result = _analyze_message(
            {
                "platform": "mastodon",
                "content": "Does anyone want this without Android?",
                "reply_context": {"source_kind": "discovery"},
            }
        )

        self.assertFalse(result["requires_reply"])
        self.assertTrue(result["draft"].strip())

    def test_support_inbox_source_classification(self) -> None:
        self.assertEqual(infer_support_inbox_type({"platform": "pwa"}), "product_feedback")
        self.assertEqual(infer_support_inbox_type({"platform": "devto"}), "purehub_post")
        self.assertEqual(
            infer_support_inbox_type({"platform": "mastodon", "parent_external_id": "parent-status"}),
            "purehub_post",
        )
        self.assertEqual(
            infer_support_inbox_type({"platform": "bluesky", "reply_context": {"source_kind": "discovery"}}),
            "social_opportunity",
        )
        self.assertEqual(infer_support_inbox_type({"platform": "telegram"}), "direct_support")

    def test_opportunity_filter_prefers_real_questions(self) -> None:
        self.assertTrue(_looks_like_question("Any app that works offline without ads?"))
        self.assertTrue(_looks_like_question("Looking for a simple QR code app"))
        self.assertFalse(_looks_like_question("Download the best Android app now"))

    def test_opportunity_relevance_rejects_broad_android_chatter(self) -> None:
        self.assertFalse(_looks_like_relevant_opportunity("Does anyone want a TV stick without Android?", "#android"))
        self.assertTrue(_looks_like_relevant_opportunity("Any offline QR scanner without ads?", "#android"))

    @patch("command_center.community_support.update_support_sync_state")
    @patch("command_center.community_support._discover_devto", side_effect=lambda _keywords, limit: limit)
    @patch("command_center.community_support._discover_mastodon", side_effect=lambda _keywords, limit: limit)
    @patch("command_center.community_support._discover_bluesky", side_effect=lambda _keywords, limit: limit)
    @patch("command_center.community_support._opportunity_keywords", return_value=["offline app"])
    @patch("command_center.community_support.get_support_sync_state", return_value={})
    @patch("command_center.community_support.get_config_value")
    def test_daily_discovery_prioritizes_direct_reply_platforms(
        self, config, _state, _keywords, bluesky, mastodon, devto, update_state
    ) -> None:
        config.side_effect = lambda key, default="": {
            "opportunity_monitor_enabled": "true",
            "opportunity_daily_limit": "27",
        }.get(key, default)

        result = discover_opportunities()

        self.assertEqual(result["target"], 27)
        self.assertEqual([result["channels"][name]["target"] for name in ("bluesky", "mastodon", "devto")], [12, 12, 3])
        self.assertEqual(sum(item["created"] for item in result["channels"].values()), 27)
        bluesky.assert_called_once_with(["offline app"], 12)
        mastodon.assert_called_once_with(["offline app"], 12)
        devto.assert_called_once_with(["offline app"], 3)
        update_state.assert_called_once()

    @patch("command_center.database.collection")
    def test_support_upsert_does_not_write_source_url_with_conflicting_operators(self, collection) -> None:
        support_messages = MagicMock()
        support_messages.update_one.return_value = SimpleNamespace(upserted_id="new-id")
        support_messages.find_one.return_value = {"source_key": "telegram:1:2", "source_url": "https://t.me/x/2"}
        collection.return_value = support_messages

        _, created = upsert_support_message(
            {
                "source_key": "telegram:1:2",
                "platform": "telegram",
                "content": "Hello",
                "source_url": "https://t.me/x/2",
            }
        )

        update = support_messages.update_one.call_args.args[1]
        self.assertNotIn("source_url", update["$setOnInsert"])
        self.assertEqual(update["$set"]["source_url"], "https://t.me/x/2")
        self.assertTrue(created)

    @patch("command_center.database.collection")
    def test_delete_support_message_uses_exact_object_id(self, collection) -> None:
        support_messages = MagicMock()
        support_messages.delete_one.return_value = SimpleNamespace(deleted_count=1)
        collection.return_value = support_messages

        self.assertTrue(delete_support_message("507f1f77bcf86cd799439011"))
        query = support_messages.delete_one.call_args.args[0]
        self.assertEqual(str(query["_id"]), "507f1f77bcf86cd799439011")

    @patch("command_center.database.collection")
    def test_delete_support_message_rejects_invalid_id(self, collection) -> None:
        self.assertFalse(delete_support_message("not-an-object-id"))
        collection.assert_not_called()

    @patch("command_center.database.collection")
    def test_support_pagination_filters_active_statuses_and_skips_previous_pages(self, collection) -> None:
        cursor = MagicMock()
        cursor.sort.return_value = cursor
        cursor.skip.return_value = cursor
        cursor.limit.return_value = [{"status": "draft_ready", "content": "Question"}]
        support_messages = MagicMock()
        support_messages.find.return_value = cursor
        collection.return_value = support_messages

        rows = list_support_messages(statuses=("new", "draft_ready"), limit=20, skip=20)

        support_messages.find.assert_called_once_with({"status": {"$in": ["new", "draft_ready"]}})
        cursor.skip.assert_called_once_with(20)
        cursor.limit.assert_called_once_with(20)
        self.assertEqual(rows[0]["content"], "Question")

    @patch("command_center.database.collection")
    def test_support_filter_combines_status_and_source_type(self, collection) -> None:
        cursor = MagicMock()
        cursor.sort.return_value = cursor
        cursor.skip.return_value = cursor
        cursor.limit.return_value = []
        support_messages = MagicMock()
        support_messages.find.return_value = cursor
        collection.return_value = support_messages

        list_support_messages(statuses=("new", "draft_ready"), inbox_filter="purehub_post", limit=20)

        support_messages.find.assert_called_once_with(
            {"status": {"$in": ["new", "draft_ready"]}, "inbox_type": "purehub_post"}
        )

    @patch("command_center.main.record_audit_log")
    @patch("command_center.main.update_support_message")
    @patch("command_center.main.get_support_message")
    @patch("command_center.main.require_admin_role", return_value={"username": "admin"})
    def test_bulk_approve_only_updates_eligible_drafts(self, _role, get_message, update, audit) -> None:
        get_message.side_effect = [
            {"id": "eligible", "status": "draft_ready", "ai_draft": "A useful answer."},
            {"id": "already-approved", "status": "approved", "reply_text": "Already approved."},
        ]

        response = support_bulk_approve_action(
            MagicMock(),
            message_ids=["eligible", "already-approved"],
            return_page=2,
        )

        update.assert_called_once_with(
            "eligible",
            {"reply_text": "A useful answer.", "status": "approved", "error_message": ""},
        )
        audit.assert_called_once()
        self.assertEqual(response.status_code, 303)
        self.assertIn("support_page=2", response.headers["location"])

    @patch("command_center.main.record_audit_log")
    @patch("command_center.main.send_support_reply")
    @patch("command_center.main.get_support_message")
    @patch("command_center.main.require_admin_role", return_value={"username": "admin"})
    def test_bulk_send_processes_only_approved_replies(self, _role, get_message, send, audit) -> None:
        get_message.side_effect = [
            {"id": "telegram", "status": "approved", "platform": "telegram"},
            {"id": "devto", "status": "approved", "platform": "devto"},
            {"id": "draft", "status": "draft_ready", "platform": "mastodon"},
        ]
        send.side_effect = [
            {"id": "telegram", "status": "replied", "platform": "telegram"},
            {"id": "devto", "status": "manual_required", "platform": "devto"},
        ]

        response = support_bulk_send_action(
            MagicMock(),
            message_ids=["telegram", "devto", "draft"],
            return_page=3,
        )

        self.assertEqual([call.args[0] for call in send.call_args_list], ["telegram", "devto"])
        audit.assert_called_once()
        self.assertEqual(response.status_code, 303)
        self.assertIn("support_page=3", response.headers["location"])

    @patch("command_center.main.record_audit_log")
    @patch("command_center.main.update_support_message")
    @patch("command_center.main.get_support_message")
    @patch("command_center.main.require_admin_role", return_value={"username": "admin"})
    def test_manual_reply_can_be_marked_completed(self, _role, get_message, update, audit) -> None:
        get_message.return_value = {
            "id": "devto-message",
            "status": "manual_required",
            "platform": "devto",
            "source_url": "https://dev.to/example/comment",
        }

        response = support_complete_manual_action(
            MagicMock(),
            "devto-message",
            return_page=2,
            return_filter="purehub_post",
        )

        values = update.call_args.args[1]
        self.assertEqual(values["status"], "replied")
        self.assertEqual(values["external_reply_url"], "https://dev.to/example/comment")
        self.assertIn("manual_completed_at", values)
        audit.assert_called_once()
        self.assertEqual(response.status_code, 303)
        self.assertIn("support_page=2", response.headers["location"])
        self.assertIn("support_filter=purehub_post", response.headers["location"])

    def test_plain_text_removes_platform_html(self) -> None:
        self.assertEqual(_plain_text("<p>Hello <strong>PureHub</strong>!</p>"), "Hello PureHub !")

    @patch("command_center.community_support.update_support_message")
    @patch("command_center.community_support._analyze_message")
    @patch("command_center.community_support.get_support_message")
    def test_regenerate_support_draft_uses_operator_correction(self, get_message, analyze, update) -> None:
        row = {"id": "message-id", "content": "Does this work offline?", "status": "draft_ready"}
        get_message.side_effect = [row, {**row, "reply_text": "New accurate answer"}]
        analyze.return_value = {
            "category": "question",
            "priority": "normal",
            "language": "en",
            "requires_reply": True,
            "draft": "New accurate answer",
        }

        result = generate_support_draft(
            "message-id",
            previous_draft="Old vague answer",
            guidance="Explain that only the Android app is fully offline.",
        )

        analyze.assert_called_once_with(
            row,
            previous_draft="Old vague answer",
            guidance="Explain that only the Android app is fully offline.",
        )
        self.assertEqual(update.call_args.args[1]["status"], "draft_ready")
        self.assertEqual(update.call_args.args[1]["reply_text"], "New accurate answer")
        self.assertEqual(result["reply_text"], "New accurate answer")

    @patch("command_center.community_support.update_support_message")
    @patch("command_center.community_support._analyze_message")
    @patch("command_center.community_support.get_support_message")
    def test_praise_with_a_question_still_creates_review_draft(self, get_message, analyze, update) -> None:
        row = {"id": "message-id", "content": "Well done! Did you develop all the tools?", "status": "ignored"}
        get_message.side_effect = [row, {**row, "status": "draft_ready", "category": "question"}]
        analyze.return_value = {
            "category": "praise",
            "priority": "normal",
            "language": "en",
            "requires_reply": False,
            "draft": "Thank you. Yes, PureHub's tools are developed as part of this open-source project.",
        }

        result = generate_support_draft("message-id")

        values = update.call_args.args[1]
        self.assertTrue(values["requires_reply"])
        self.assertEqual(values["category"], "question")
        self.assertEqual(values["status"], "draft_ready")
        self.assertTrue(values["reply_text"])
        self.assertEqual(result["status"], "draft_ready")

    @patch("command_center.community_support.upsert_support_message")
    @patch("command_center.community_support.get_config_value")
    def test_telegram_group_message_is_ingested(self, config, upsert) -> None:
        config.side_effect = lambda key, default="": {
            "telegram_support_chat_id": "-1003762178712",
            "telegram_notify_chat_id": "-1004332046536",
            "telegram_bot_username": "aaa_letan_vip_bot",
        }.get(key, default)
        upsert.return_value = ({"id": "message-id", "status": "new"}, True)
        result = ingest_telegram_update(
            {
                "message": {
                    "message_id": 42,
                    "date": 1_700_000_000,
                    "from": {"id": 8, "first_name": "Tester", "username": "tester", "is_bot": False},
                    "chat": {"id": -1003762178712, "type": "supergroup", "username": "purehubgroup"},
                    "text": "How do I use OCR?",
                }
            }
        )
        self.assertEqual(result, {"id": "message-id", "status": "new"})
        payload = upsert.call_args.args[0]
        self.assertEqual(payload["source_key"], "telegram:-1003762178712:42")
        self.assertEqual(payload["content"], "How do I use OCR?")

    @patch("command_center.community_support.upsert_support_message")
    @patch("command_center.community_support.get_config_value")
    def test_telegram_bot_commands_are_not_duplicated_in_support(self, config, upsert) -> None:
        config.side_effect = lambda key, default="": {
            "telegram_support_chat_id": "-1003762178712",
            "telegram_notify_chat_id": "-1004332046536",
        }.get(key, default)
        result = ingest_telegram_update(
            {
                "message": {
                    "message_id": 43,
                    "date": 1_700_000_000,
                    "from": {"id": 8, "first_name": "Tester", "is_bot": False},
                    "chat": {"id": -1003762178712, "type": "supergroup"},
                    "text": "/ask How do I use OCR?",
                }
            }
        )
        self.assertIsNone(result)
        upsert.assert_not_called()

    @patch("command_center.community_support.upsert_support_message")
    @patch("command_center.community_support.get_config_value")
    def test_telegram_channel_auto_forward_is_not_ingested(self, config, upsert) -> None:
        config.side_effect = lambda key, default="": {
            "telegram_support_chat_id": "-1003762178712",
            "telegram_notify_chat_id": "-1004332046536",
        }.get(key, default)
        result = ingest_telegram_update(
            {
                "message": {
                    "message_id": 44,
                    "date": 1_700_000_000,
                    "from": {"id": 777000, "first_name": "Telegram", "is_bot": False},
                    "sender_chat": {"id": -1004332046536, "type": "channel", "title": "PureHub"},
                    "chat": {"id": -1003762178712, "type": "supergroup"},
                    "is_automatic_forward": True,
                    "text": "PureHub community build — day 3",
                }
            }
        )
        self.assertIsNone(result)
        upsert.assert_not_called()

    @patch("command_center.community_support.upsert_support_message")
    @patch("command_center.community_support.get_config_value")
    def test_telegram_private_auto_reply_is_not_duplicated_in_support(self, config, upsert) -> None:
        config.side_effect = lambda key, default="": {
            "telegram_support_chat_id": "-1003762178712",
            "community_reply_mode": "auto",
        }.get(key, default)
        result = ingest_telegram_update(
            {
                "message": {
                    "message_id": 45,
                    "date": 1_700_000_000,
                    "from": {"id": 8, "first_name": "Tester", "is_bot": False},
                    "chat": {"id": 8, "type": "private"},
                    "text": "How do I use OCR?",
                }
            }
        )
        self.assertIsNone(result)
        upsert.assert_not_called()


if __name__ == "__main__":
    unittest.main()
