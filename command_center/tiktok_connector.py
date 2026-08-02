from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlencode

import requests

try:
    from .database import (
        get_config_value,
        get_growth_post,
        update_config,
        update_growth_post,
        upsert_growth_post,
    )
except ImportError:
    from database import (
        get_config_value,
        get_growth_post,
        update_config,
        update_growth_post,
        upsert_growth_post,
    )


AUTH_URL = "https://www.tiktok.com/v2/auth/authorize/"
TOKEN_URL = "https://open.tiktokapis.com/v2/oauth/token/"
REVOKE_URL = "https://open.tiktokapis.com/v2/oauth/revoke/"
API_BASE = "https://open.tiktokapis.com/v2"
SCOPES = "user.info.basic,video.upload,video.publish"
MAX_SINGLE_CHUNK_BYTES = 64 * 1024 * 1024


def tiktok_connection_state() -> dict[str, Any]:
    return {
        "client_configured": bool(get_config_value("tiktok_client_key") and get_config_value("tiktok_client_secret")),
        "connected": bool(get_config_value("tiktok_refresh_token")),
        "display_name": get_config_value("tiktok_display_name"),
        "open_id": get_config_value("tiktok_open_id"),
        "environment": get_config_value("tiktok_environment", "sandbox"),
    }


def build_authorization_url(*, state: str, redirect_uri: str) -> str:
    client_key = get_config_value("tiktok_client_key")
    if not client_key or not get_config_value("tiktok_client_secret"):
        raise ValueError("Configure the TikTok Client key and Client secret first.")
    return f"{AUTH_URL}?{urlencode({'client_key': client_key, 'redirect_uri': redirect_uri, 'response_type': 'code', 'scope': SCOPES, 'state': state})}"


def _exchange(payload: dict[str, str]) -> dict[str, Any]:
    response = requests.post(TOKEN_URL, data=payload, timeout=30)
    if not response.ok:
        detail = response.text[:500]
        raise ValueError(f"TikTok token request failed ({response.status_code}): {detail}")
    data = response.json()
    if data.get("error"):
        raise ValueError(f"TikTok token request failed: {data.get('error_description') or data.get('error')}")
    return data


def _save_tokens(payload: dict[str, Any]) -> None:
    refresh_token = str(payload.get("refresh_token") or "")
    if not refresh_token:
        raise ValueError("TikTok did not return a refresh token.")
    update_config(
        {
            "tiktok_refresh_token": refresh_token,
            "tiktok_open_id": str(payload.get("open_id") or get_config_value("tiktok_open_id")),
        }
    )


def _access_token() -> str:
    refresh_token = get_config_value("tiktok_refresh_token")
    if not refresh_token:
        raise ValueError("TikTok is not connected.")
    payload = _exchange(
        {
            "client_key": get_config_value("tiktok_client_key"),
            "client_secret": get_config_value("tiktok_client_secret"),
            "grant_type": "refresh_token",
            "refresh_token": refresh_token,
        }
    )
    _save_tokens(payload)
    access_token = str(payload.get("access_token") or "")
    if not access_token:
        raise ValueError("TikTok did not return an access token.")
    return access_token


def connect_tiktok(*, code: str, redirect_uri: str) -> dict[str, Any]:
    payload = _exchange(
        {
            "client_key": get_config_value("tiktok_client_key"),
            "client_secret": get_config_value("tiktok_client_secret"),
            "code": code,
            "grant_type": "authorization_code",
            "redirect_uri": redirect_uri,
        }
    )
    _save_tokens(payload)
    profile = fetch_tiktok_profile(str(payload.get("access_token") or ""))
    update_config(
        {
            "tiktok_open_id": str(profile.get("open_id") or payload.get("open_id") or ""),
            "tiktok_display_name": str(profile.get("display_name") or "TikTok creator"),
        }
    )
    return tiktok_connection_state()


def disconnect_tiktok() -> None:
    refresh_token = get_config_value("tiktok_refresh_token")
    if refresh_token:
        try:
            requests.post(
                REVOKE_URL,
                data={
                    "client_key": get_config_value("tiktok_client_key"),
                    "client_secret": get_config_value("tiktok_client_secret"),
                    "token": refresh_token,
                },
                timeout=20,
            )
        except Exception:
            pass
    update_config({"tiktok_refresh_token": "", "tiktok_open_id": "", "tiktok_display_name": ""})


def _authorized_headers(access_token: str = "") -> dict[str, str]:
    return {"Authorization": f"Bearer {access_token or _access_token()}", "Content-Type": "application/json; charset=UTF-8"}


def _api_data(response: requests.Response, label: str) -> dict[str, Any]:
    if not response.ok:
        try:
            error = dict(response.json().get("error") or {})
        except (ValueError, AttributeError, TypeError):
            error = {}
        if str(error.get("code") or "") == "unaudited_client_can_only_post_to_private_accounts":
            raise ValueError(
                "TikTok has not audited this client yet. Sandbox Direct Post requires the connected "
                "TikTok account itself to be Private and the post visibility to be Only me. "
                "Setting a post to Only me is not enough while the account is Public. "
                "Use a private account for the review demo, or complete TikTok Production review "
                "before posting to a public account."
            )
        raise ValueError(f"{label} failed ({response.status_code}): {response.text[:500]}")
    payload = response.json()
    error = dict(payload.get("error") or {})
    if error and str(error.get("code") or "").lower() not in {"", "ok"}:
        raise ValueError(f"{label} failed: {error.get('message') or error.get('code')}")
    return dict(payload.get("data") or {})


def fetch_tiktok_profile(access_token: str = "") -> dict[str, Any]:
    response = requests.get(
        f"{API_BASE}/user/info/",
        headers=_authorized_headers(access_token),
        params={"fields": "open_id,avatar_url,display_name"},
        timeout=30,
    )
    return dict(_api_data(response, "TikTok profile").get("user") or {})


def fetch_creator_info() -> dict[str, Any]:
    response = requests.post(
        f"{API_BASE}/post/publish/creator_info/query/",
        headers=_authorized_headers(),
        json={},
        timeout=30,
    )
    return _api_data(response, "TikTok creator info")


def _validate_video(*, content_type: str, content_length: int) -> None:
    if content_type not in {"video/mp4", "video/quicktime"}:
        raise ValueError("Use an MP4 or MOV video.")
    if content_length <= 0 or content_length > MAX_SINGLE_CHUNK_BYTES:
        raise ValueError("For this browser uploader, video size must be between 1 byte and 64 MB.")


def create_upload_session(
    *,
    caption: str,
    mode: str,
    privacy_level: str,
    disable_comment: bool,
    disable_duet: bool,
    disable_stitch: bool,
    content_type: str,
    content_length: int,
) -> dict[str, Any]:
    _validate_video(content_type=content_type, content_length=content_length)
    mode = "direct" if mode == "direct" else "draft"
    caption = caption.strip()[:2200]
    creator_info: dict[str, Any] = {}
    if mode == "direct":
        creator_info = fetch_creator_info()
        options = [str(item) for item in creator_info.get("privacy_level_options") or []]
        if not options:
            options = ["SELF_ONLY"]
        if get_config_value("tiktok_environment", "sandbox") == "sandbox":
            options = ["SELF_ONLY"]
        if privacy_level not in options:
            privacy_level = "SELF_ONLY" if "SELF_ONLY" in options else options[0]
        payload: dict[str, Any] = {
            "post_info": {
                "title": caption,
                "privacy_level": privacy_level,
                "disable_comment": bool(disable_comment or creator_info.get("comment_disabled")),
                "disable_duet": bool(disable_duet or creator_info.get("duet_disabled")),
                "disable_stitch": bool(disable_stitch or creator_info.get("stitch_disabled")),
            },
            "source_info": {
                "source": "FILE_UPLOAD",
                "video_size": content_length,
                "chunk_size": content_length,
                "total_chunk_count": 1,
            },
        }
        endpoint = f"{API_BASE}/post/publish/video/init/"
    else:
        payload = {
            "source_info": {
                "source": "FILE_UPLOAD",
                "video_size": content_length,
                "chunk_size": content_length,
                "total_chunk_count": 1,
            }
        }
        endpoint = f"{API_BASE}/post/publish/inbox/video/init/"

    response = requests.post(endpoint, headers=_authorized_headers(), json=payload, timeout=30)
    data = _api_data(response, "TikTok upload initialization")
    publish_id = str(data.get("publish_id") or "")
    upload_url = str(data.get("upload_url") or "")
    if not publish_id or not upload_url:
        raise ValueError("TikTok did not return a publish ID and upload URL.")

    now = datetime.now(timezone.utc)
    row = upsert_growth_post(
        campaign_id=f"tiktok-admin-{now:%Y-%m}",
        day_number=int(now.timestamp()),
        channel="tiktok",
        topic="TikTok creator upload",
        content=caption,
        status="uploading",
        scheduled_at=now,
        metadata={
            "tiktok_publish_id": publish_id,
            "tiktok_mode": mode,
            "privacy_level": privacy_level if mode == "direct" else "DRAFT",
            "creator_info": creator_info,
        },
    )
    return {
        "post_id": str(row.get("id") or ""),
        "publish_id": publish_id,
        "upload_url": upload_url,
        "mode": mode,
        "privacy_level": privacy_level if mode == "direct" else "DRAFT",
    }


def complete_upload(*, post_id: str, publish_id: str) -> dict[str, Any]:
    row = get_growth_post(post_id)
    if not row or row.get("channel") != "tiktok":
        raise ValueError("TikTok queue item not found.")
    metadata = dict(row.get("metadata") or {})
    metadata["tiktok_publish_id"] = publish_id
    metadata["uploaded_at"] = datetime.now(timezone.utc).isoformat()
    update_growth_post(post_id, {"status": "processing", "metadata": metadata, "error_message": ""})
    return get_growth_post(post_id) or {}


def fetch_publish_status(publish_id: str) -> dict[str, Any]:
    response = requests.post(
        f"{API_BASE}/post/publish/status/fetch/",
        headers=_authorized_headers(),
        json={"publish_id": publish_id},
        timeout=30,
    )
    return _api_data(response, "TikTok status request")
