from __future__ import annotations

import unittest
from unittest.mock import patch

from command_center.community_support import _plain_text, ingest_telegram_update


class CommunitySupportTests(unittest.TestCase):
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
