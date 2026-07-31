from __future__ import annotations

import unittest

from command_center.release_hub import format_reddit_draft, parse_reddit_draft


class ReleaseHubTests(unittest.TestCase):
    def test_reddit_draft_round_trip(self) -> None:
        content = format_reddit_draft(
            "PureHub needs honest Android feedback",
            "I am the maker. What should be simpler?",
            "r/androidapps, r/opensource",
        )
        self.assertEqual(
            parse_reddit_draft(content),
            {
                "title": "PureHub needs honest Android feedback",
                "communities": "r/androidapps, r/opensource",
                "body": "I am the maker. What should be simpler?",
            },
        )


if __name__ == "__main__":
    unittest.main()
