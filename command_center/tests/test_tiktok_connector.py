from __future__ import annotations

from unittest import TestCase
from unittest.mock import patch
from urllib.parse import parse_qs, urlparse

from command_center import tiktok_connector


class TikTokConnectorTest(TestCase):
    def test_authorization_url_uses_web_oauth_v2_and_required_scopes(self) -> None:
        values = {"tiktok_client_key": "client-key", "tiktok_client_secret": "client-secret"}
        with patch.object(tiktok_connector, "get_config_value", side_effect=lambda key, default="": values.get(key, default)):
            url = tiktok_connector.build_authorization_url(
                state="safe-state",
                redirect_uri="https://hub.blissbiovn.com/admin/tiktok/callback",
            )

        parsed = urlparse(url)
        query = parse_qs(parsed.query)
        self.assertEqual(f"{parsed.scheme}://{parsed.netloc}{parsed.path}", tiktok_connector.AUTH_URL)
        self.assertEqual(query["client_key"], ["client-key"])
        self.assertEqual(query["state"], ["safe-state"])
        self.assertEqual(query["scope"], ["user.info.basic,video.upload,video.publish"])

    def test_browser_upload_rejects_oversized_video(self) -> None:
        with self.assertRaisesRegex(ValueError, "64 MB"):
            tiktok_connector._validate_video(
                content_type="video/mp4",
                content_length=tiktok_connector.MAX_SINGLE_CHUNK_BYTES + 1,
            )

    def test_browser_upload_rejects_unsupported_media_type(self) -> None:
        with self.assertRaisesRegex(ValueError, "MP4 or MOV"):
            tiktok_connector._validate_video(content_type="video/webm", content_length=1024)

    def test_unaudited_client_error_explains_private_account_requirement(self) -> None:
        response = type(
            "Response",
            (),
            {
                "ok": False,
                "status_code": 403,
                "text": '{"error":{"code":"unaudited_client_can_only_post_to_private_accounts"}}',
                "json": lambda self: {
                    "error": {"code": "unaudited_client_can_only_post_to_private_accounts"}
                },
            },
        )()

        with self.assertRaisesRegex(ValueError, "account itself to be Private"):
            tiktok_connector._api_data(response, "TikTok upload initialization")
