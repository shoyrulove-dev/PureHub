from __future__ import annotations

import unittest
from unittest.mock import patch

from command_center.database import get_product_growth_snapshot, record_miniapp_event


class ProductGrowthTests(unittest.TestCase):
    def test_snapshot_aggregates_only_anonymous_daily_counters(self) -> None:
        rows = [
            {"day": "2026-08-02", "miniapp_id": "ocr-text", "event": "open", "count": 8},
            {"day": "2026-08-02", "miniapp_id": "ocr-text", "event": "helpful", "count": 4},
            {"day": "2026-08-02", "miniapp_id": "qr-studio", "event": "open", "count": 3},
            {"day": "2026-08-02", "miniapp_id": "qr-studio", "event": "share", "count": 2},
        ]
        roadmap = [{"option_id": "ocr-workflow", "title": "Improve OCR workflow", "votes": 5}]
        with patch("command_center.database.collection") as collection_mock, patch(
            "command_center.database.list_roadmap_options", return_value=roadmap
        ):
            collection_mock.return_value.find.return_value = rows
            snapshot = get_product_growth_snapshot()

        self.assertEqual(snapshot["totals"]["open"], 11)
        self.assertEqual(snapshot["totals"]["share"], 2)
        self.assertEqual(snapshot["helpful_rate"], 36.4)
        self.assertEqual(snapshot["top_tools"][0]["miniapp_id"], "ocr-text")
        self.assertEqual(snapshot["roadmap_votes"], 5)

    def test_product_event_rejects_unknown_tools_before_database_write(self) -> None:
        with patch("command_center.database.collection") as collection_mock:
            with self.assertRaises(ValueError):
                record_miniapp_event("not-a-purehub-tool", "open")
            collection_mock.assert_not_called()


if __name__ == "__main__":
    unittest.main()
