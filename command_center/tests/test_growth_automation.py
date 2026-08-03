import unittest
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

from command_center.growth_automation import TOPICS, _channels_for_day, _fallback_content, _has_upcoming_youtube_queue
from command_center.youtube_connector import _parse_upload_copy, _scheduled_publish_at


class GrowthAutomationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.config_patch = patch("command_center.growth_automation.get_config_value", side_effect=lambda _key, default="": default)
        self.config_patch.start()

    def tearDown(self) -> None:
        self.config_patch.stop()

    def test_future_youtube_schedule_is_accepted(self) -> None:
        future = (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()
        self.assertIsNotNone(_scheduled_publish_at({"scheduled_at": future}))

    def test_scheduled_queue_suppresses_duplicate_youtube_packages(self) -> None:
        future = (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()
        past = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
        self.assertTrue(_has_upcoming_youtube_queue([{"channel": "youtube", "status": "scheduled", "scheduled_at": future}]))
        self.assertFalse(_has_upcoming_youtube_queue([{"channel": "youtube", "status": "published", "scheduled_at": future}]))
        self.assertFalse(_has_upcoming_youtube_queue([{"channel": "youtube", "status": "scheduled", "scheduled_at": past}]))

    def test_campaign_has_thirty_distinct_topics(self) -> None:
        self.assertEqual(len(TOPICS), 30)
        self.assertEqual(len(set(TOPICS)), 30)

    def test_safe_platform_cadence(self) -> None:
        self.assertEqual(_channels_for_day(1), ["bluesky", "mastodon", "telegram", "youtube"])
        self.assertIn("devto", _channels_for_day(5))
        self.assertNotIn("telegram", _channels_for_day(5))
        self.assertIn("reddit", _channels_for_day(14))
        self.assertIn("reddit", _channels_for_day(28))

    def test_character_limited_fallbacks(self) -> None:
        topic = TOPICS[0]
        self.assertLessEqual(len(_fallback_content("bluesky", topic, 1)), 300)
        self.assertLessEqual(len(_fallback_content("mastodon", topic, 1)), 500)
        self.assertIn("utm_source=bluesky", _fallback_content("bluesky", topic, 1))
        self.assertIn("utm_campaign=community-foundation-30d-v1", _fallback_content("telegram", topic, 1))

    def test_youtube_copy_parser_excludes_script(self) -> None:
        title, description = _parse_upload_copy(
            "Title: A useful PureHub demo\n\nDescription:\nA clean description.\n\nShort script:\nShow the result."
        )
        self.assertEqual(title, "A useful PureHub demo")
        self.assertEqual(description, "A clean description.")


if __name__ == "__main__":
    unittest.main()
