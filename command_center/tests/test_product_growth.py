from __future__ import annotations

import unittest
from unittest.mock import MagicMock, patch

from command_center.database import get_product_growth_snapshot, record_growth_funnel_event, record_miniapp_event


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

    def test_snapshot_includes_anonymous_funnel_and_flagships(self) -> None:
        miniapp_collection = MagicMock()
        miniapp_collection.find.return_value = [
            {"day": "2026-08-02", "miniapp_id": "zen-pomodoro", "event": "open", "count": 6},
            {"day": "2026-08-02", "miniapp_id": "qr-studio", "event": "helpful", "count": 2},
        ]
        funnel_collection = MagicMock()
        funnel_collection.find.return_value = [
            {"day": "2026-08-02", "stage": "visit", "source": "telegram", "campaign": "august", "count": 10},
            {"day": "2026-08-02", "stage": "download", "source": "telegram", "campaign": "august", "count": 2},
            {"day": "2026-08-02", "stage": "device_report", "source": "early-testers", "campaign": "zen-pomodoro", "count": 1},
        ]
        with patch("command_center.database.collection", side_effect=lambda name: funnel_collection if name == "growth_funnel_daily" else miniapp_collection), patch(
            "command_center.database.list_roadmap_options", return_value=[]
        ):
            snapshot = get_product_growth_snapshot()

        self.assertEqual(snapshot["funnel"]["visit"], 10)
        self.assertEqual(snapshot["funnel"]["device_report"], 1)
        self.assertEqual(snapshot["funnel_rates"]["download"], 20.0)
        self.assertEqual(snapshot["top_sources"], [{"source": "telegram", "visits": 10}])
        self.assertEqual(snapshot["flagship"][0]["miniapp_id"], "zen-pomodoro")

    def test_funnel_dimensions_are_sanitized_and_never_store_identity(self) -> None:
        with patch("command_center.database.collection") as collection_mock:
            record_growth_funnel_event("visit", " Telegram / user@example.com ", "Launch August!!!")
        query = collection_mock.return_value.update_one.call_args.args[0]
        self.assertEqual(query["source"], "other")
        self.assertEqual(query["campaign"], "none")
        self.assertNotIn("user_id", query)
        self.assertNotIn("ip", query)


if __name__ == "__main__":
    unittest.main()
