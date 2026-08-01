from __future__ import annotations

import unittest
from unittest.mock import MagicMock, patch

from command_center.reddit_connector import normalize_subreddit, submit_reddit_post


class RedditConnectorTests(unittest.TestCase):
    def test_normalize_subreddit_accepts_common_forms(self) -> None:
        self.assertEqual(normalize_subreddit("r/droidappshowcase"), "droidappshowcase")
        self.assertEqual(
            normalize_subreddit("https://www.reddit.com/r/droidappshowcase/"),
            "droidappshowcase",
        )

    def test_normalize_subreddit_rejects_multiple_or_unsafe_destinations(self) -> None:
        for value in ("", "r/a", "r/androidapps, r/fossdroid", "../androidapps"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                normalize_subreddit(value)

    @patch("command_center.reddit_connector._user_agent", return_value="PureHub-Test/1.0")
    @patch("command_center.reddit_connector._access_token", return_value="access-token")
    @patch("command_center.reddit_connector.requests.post")
    def test_submit_reddit_post_returns_permalink(self, post: MagicMock, _token: MagicMock, _agent: MagicMock) -> None:
        response = MagicMock()
        response.json.return_value = {
            "json": {
                "errors": [],
                "data": {"name": "t3_abc123", "url": "/r/droidappshowcase/comments/abc123/purehub/"},
            }
        }
        post.return_value = response

        external_id, external_url = submit_reddit_post(
            subreddit="droidappshowcase",
            title="PureHub needs Android feedback",
            body="Maker disclosure: I build PureHub.",
        )

        self.assertEqual(external_id, "t3_abc123")
        self.assertEqual(external_url, "https://www.reddit.com/r/droidappshowcase/comments/abc123/purehub/")
        submitted = post.call_args.kwargs["data"]
        self.assertEqual(submitted["kind"], "self")
        self.assertEqual(submitted["sr"], "droidappshowcase")

    @patch("command_center.reddit_connector._user_agent", return_value="PureHub-Test/1.0")
    @patch("command_center.reddit_connector._access_token", return_value="access-token")
    @patch("command_center.reddit_connector.requests.post")
    def test_submit_reddit_post_surfaces_api_errors(self, post: MagicMock, _token: MagicMock, _agent: MagicMock) -> None:
        response = MagicMock()
        response.json.return_value = {"json": {"errors": [["SUBREDDIT_NOTALLOWED", "posting is restricted", "sr"]]}}
        post.return_value = response

        with self.assertRaisesRegex(ValueError, "SUBREDDIT_NOTALLOWED"):
            submit_reddit_post(
                subreddit="droidappshowcase",
                title="PureHub update",
                body="A reviewed maker update.",
            )


if __name__ == "__main__":
    unittest.main()
