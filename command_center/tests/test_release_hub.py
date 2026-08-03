from __future__ import annotations

import unittest

from command_center.release_hub import format_reddit_draft, parse_reddit_draft, validate_social_content


class ReleaseHubTests(unittest.TestCase):
    def test_social_content_accepts_icons_and_real_questions(self) -> None:
        content = "\U0001f4f1 PureHub is ready. What should we improve?"
        self.assertEqual(validate_social_content(content), content)

    def test_social_content_rejects_lost_unicode_placeholders(self) -> None:
        with self.assertRaisesRegex(ValueError, "mojibake"):
            validate_social_content("?? PureHub is ready\n? QR Studio ? scan and share")

    def test_reddit_draft_round_trip(self) -> None:
        content = format_reddit_draft(
            "PureHub needs honest Android feedback",
            "I am the maker. What should be simpler?",
            "r/droidappshowcase",
        )
        self.assertEqual(
            parse_reddit_draft(content),
            {
                "title": "PureHub needs honest Android feedback",
                "communities": "r/droidappshowcase",
                "body": "I am the maker. What should be simpler?",
            },
        )


if __name__ == "__main__":
    unittest.main()
