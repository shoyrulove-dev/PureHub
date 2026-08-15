from __future__ import annotations

import unittest
from datetime import datetime, timezone

from command_center.community_goal import build_august_growth_goal


class CommunityGoalTests(unittest.TestCase):
    def test_august_goal_aggregates_connected_platform_signals(self) -> None:
        goal = build_august_growth_goal(
            config={"growth_timezone": "Asia/Bangkok", "growth_campaign_start_date": "2026-08-02"},
            community_metrics=[
                {"platform": "telegram", "metrics": {"members": 2}},
                {"platform": "devto", "metrics": {"views": 20, "reactions": 1, "comments": 2}},
                {"platform": "bluesky", "metrics": {"followers": 3, "likes": 4, "replies": 1, "reposts": 1}},
                {"platform": "mastodon", "metrics": {"followers": 2, "favourites": 1, "boosts": 1, "replies": 1}},
            ],
            growth_posts=[
                {
                    "channel": "youtube",
                    "status": "scheduled",
                    "scheduled_at": "2026-08-04T12:30:00+00:00",
                    "metadata": {"metrics": {"views": 30}},
                },
                {
                    "channel": "youtube",
                    "status": "ready_upload",
                    "scheduled_at": "2026-08-02T03:00:00+00:00",
                    "metadata": {},
                },
            ],
            support_messages=[
                {"category": "bug", "received_at": "2026-08-02T01:00:00+00:00"},
                {"category": "spam", "received_at": "2026-08-02T02:00:00+00:00"},
            ],
            support_metrics={"open": 0, "replied": 2},
            product_growth={"totals": {"complete": 9}},
            reddit_connected=False,
            now=datetime(2026, 8, 2, 4, 0, tzinfo=timezone.utc),
        )

        milestones = {item["key"]: item for item in goal["milestones"]}
        self.assertEqual(milestones["audience"]["value"], 7)
        self.assertEqual(milestones["views"]["value"], 50)
        self.assertEqual(milestones["feedback"]["value"], 1)
        self.assertEqual(milestones["videos"]["value"], 1)
        self.assertEqual(milestones["completions"]["value"], 9)
        self.assertEqual(goal["campaign_day"], 1)
        self.assertEqual(goal["weeks"][0]["status"], "active")
        self.assertIn("Reddit API approval pending", " ".join(item["label"] for item in goal["actions"]))
        action_labels = " ".join(item["label"] for item in goal["actions"])
        self.assertIn("Next Short scheduled", action_labels)
        self.assertNotIn("Upload 1 prepared", action_labels)


if __name__ == "__main__":
    unittest.main()
