from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock
from unittest.mock import patch

from command_center.community_support import _looks_like_question, _plain_text, ingest_telegram_update
from command_center.database import delete_support_message, upsert_support_message


class CommunitySupportTests(unittest.TestCase):
    def test_opportunity_filter_prefers_real_questions(self) -> None:
        self.assertTrue(_looks_like_question("Any app that works offline without ads?"))
        self.assertTrue(_looks_like_question("Looking for a simple QR code app"))
        self.assertFalse(_looks_like_question("Download the best Android app now"))

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

    def test_plain_text_removes_platform_html(self) -> None:
        self.assertEqual(_plain_text("<p>Hello <strong>PureHub</strong>!</p>"), "Hello PureHub !")

    @patch("command_center.community_support.upsert_support_message")
    @patch("command_center.community_support.get_config_value", return_value="-1003762178712")
    def test_telegram_group_message_is_ingested(self, _config, upsert) -> None:
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
    @patch("command_center.community_support.get_config_value", return_value="-1003762178712")
    def test_telegram_bot_commands_are_not_duplicated_in_support(self, _config, upsert) -> None:
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


if __name__ == "__main__":
    unittest.main()
