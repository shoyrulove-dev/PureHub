from __future__ import annotations

import re
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlencode

import requests

try:
    from .database import get_config_value, get_growth_post, list_growth_posts, update_config, update_growth_post
except ImportError:
    from database import get_config_value, get_growth_post, list_growth_posts, update_config, update_growth_post


AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_URL = "https://oauth2.googleapis.com/token"
API_BASE = "https://www.googleapis.com/youtube/v3"
UPLOAD_BASE = "https://www.googleapis.com/upload/youtube/v3/videos"
SCOPES = "https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube.readonly"


def youtube_connection_state() -> dict[str, Any]:
    return {
        "client_configured": bool(get_config_value("youtube_client_id") and get_config_value("youtube_client_secret")),
        "connected": bool(get_config_value("youtube_refresh_token")),
        "channel_id": get_config_value("youtube_channel_id"),
        "channel_title": get_config_value("youtube_channel_title"),
        "privacy": get_config_value("youtube_default_privacy", "unlisted"),
    }


def build_authorization_url(*, state: str, redirect_uri: str) -> str:
    client_id = get_config_value("youtube_client_id")
    if not client_id or not get_config_value("youtube_client_secret"):
        raise ValueError("Configure the Google OAuth client ID and secret first.")
    return f"{AUTH_URL}?{urlencode({'client_id': client_id, 'redirect_uri': redirect_uri, 'response_type': 'code', 'scope': SCOPES, 'access_type': 'offline', 'prompt': 'consent', 'include_granted_scopes': 'true', 'state': state})}"


def _exchange(payload: dict[str, str]) -> dict[str, Any]:
    response = requests.post(TOKEN_URL, data=payload, timeout=30)
    response.raise_for_status()
    return response.json()


def _access_token() -> str:
    refresh_token = get_config_value("youtube_refresh_token")
    if not refresh_token:
        raise ValueError("YouTube is not connected.")
    payload = _exchange(
        {
            "client_id": get_config_value("youtube_client_id"),
            "client_secret": get_config_value("youtube_client_secret"),
            "refresh_token": refresh_token,
            "grant_type": "refresh_token",
        }
    )
    return str(payload["access_token"])


def connect_youtube(*, code: str, redirect_uri: str) -> dict[str, Any]:
    payload = _exchange(
        {
            "client_id": get_config_value("youtube_client_id"),
            "client_secret": get_config_value("youtube_client_secret"),
            "code": code,
            "redirect_uri": redirect_uri,
            "grant_type": "authorization_code",
        }
    )
    refresh_token = str(payload.get("refresh_token") or "")
    if not refresh_token:
        raise ValueError("Google did not return a refresh token. Reconnect with consent enabled.")
    update_config({"youtube_refresh_token": refresh_token})
    profile = fetch_youtube_channel()
    update_config(
        {
            "youtube_channel_id": str(profile.get("id", "")),
            "youtube_channel_title": str((profile.get("snippet") or {}).get("title", "")),
        }
    )
    return youtube_connection_state()


def disconnect_youtube() -> None:
    token = get_config_value("youtube_refresh_token")
    if token:
        try:
            requests.post("https://oauth2.googleapis.com/revoke", params={"token": token}, timeout=20)
        except Exception:
            pass
    update_config({"youtube_refresh_token": "", "youtube_channel_id": "", "youtube_channel_title": ""})


def fetch_youtube_channel() -> dict[str, Any]:
    response = requests.get(
        f"{API_BASE}/channels",
        headers={"Authorization": f"Bearer {_access_token()}"},
        params={"part": "snippet,statistics", "mine": "true"},
        timeout=30,
    )
    response.raise_for_status()
    items = response.json().get("items") or []
    if not items:
        raise ValueError("No YouTube channel belongs to this Google account.")
    return items[0]


def _parse_upload_copy(content: str) -> tuple[str, str]:
    title_match = re.search(r"(?im)^Title:\s*(.+)$", content)
    title = (title_match.group(1).strip() if title_match else "PureHub mini-app demo")[:100]
    description_match = re.search(r"(?ims)^Description:\s*(.+?)(?:\n\s*Short script:|\Z)", content)
    description = description_match.group(1).strip() if description_match else content.strip()
    return title, description[:5000]


def _scheduled_publish_at(row: dict[str, Any]) -> datetime | None:
    raw = row.get("scheduled_at")
    if not raw:
        return None
    try:
        value = datetime.fromisoformat(str(raw).replace("Z", "+00:00"))
    except ValueError:
        return None
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)
    value = value.astimezone(timezone.utc)
    return value if value > datetime.now(timezone.utc) else None


def create_upload_session(post_id: str, *, content_type: str, content_length: int) -> dict[str, str]:
    row = get_growth_post(post_id)
    if not row or row.get("channel") != "youtube":
        raise ValueError("YouTube queue item not found.")
    if content_type not in {"video/mp4", "video/quicktime", "video/webm"}:
        raise ValueError("Use an MP4, MOV, or WebM video.")
    if content_length <= 0 or content_length > 1024 * 1024 * 1024:
        raise ValueError("Video size must be between 1 byte and 1 GB.")
    title, description = _parse_upload_copy(str(row.get("content", "")))
    publish_at = _scheduled_publish_at(row)
    privacy = "private" if publish_at else get_config_value("youtube_default_privacy", "unlisted")
    if privacy not in {"private", "unlisted", "public"}:
        privacy = "unlisted"
    status: dict[str, Any] = {"privacyStatus": privacy, "selfDeclaredMadeForKids": False}
    if publish_at:
        status["publishAt"] = publish_at.isoformat().replace("+00:00", "Z")
    response = requests.post(
        UPLOAD_BASE,
        headers={
            "Authorization": f"Bearer {_access_token()}",
            "Content-Type": "application/json; charset=UTF-8",
            "X-Upload-Content-Length": str(content_length),
            "X-Upload-Content-Type": content_type,
        },
        params={"uploadType": "resumable", "part": "snippet,status"},
        json={
            "snippet": {
                "title": title,
                "description": description,
                "categoryId": "28",
                "defaultLanguage": "en",
                "tags": ["PureHub", "OpenSource", "AndroidApps", "NoAds", "PrivacyTools"],
            },
            "status": status,
        },
        timeout=30,
    )
    response.raise_for_status()
    upload_url = response.headers.get("Location", "")
    if not upload_url:
        raise ValueError("YouTube did not return a resumable upload URL.")
    metadata = dict(row.get("metadata") or {})
    if publish_at:
        metadata["youtube_publish_at"] = publish_at.isoformat().replace("+00:00", "Z")
    update_growth_post(post_id, {"status": "uploading", "metadata": metadata, "error_message": ""})
    return {
        "upload_url": upload_url,
        "title": title,
        "privacy": privacy,
        "publish_at": status.get("publishAt", ""),
    }


def complete_upload(post_id: str, payload: dict[str, Any]) -> dict[str, Any]:
    video_id = str(payload.get("id") or "").strip()
    if not video_id:
        raise ValueError("YouTube upload response is missing the video ID.")
    metadata = dict((get_growth_post(post_id) or {}).get("metadata") or {})
    publish_at = str(metadata.get("youtube_publish_at") or "")
    metadata["youtube"] = {
        "privacy": str((payload.get("status") or {}).get("privacyStatus", "")),
        "uploaded_at": datetime.now(timezone.utc).isoformat(),
        "publish_at": publish_at,
    }
    update_growth_post(
        post_id,
        {
            "status": "scheduled" if publish_at else "published",
            "external_id": video_id,
            "external_url": f"https://youtu.be/{video_id}",
            "published_at": None if publish_at else datetime.now(timezone.utc),
            "metadata": metadata,
            "error_message": "",
        },
    )
    return get_growth_post(post_id) or {}


def sync_youtube_metrics() -> dict[str, Any]:
    rows = [
        row
        for status in ("published", "scheduled")
        for row in list_growth_posts(50, status=status, channel="youtube")
        if row.get("external_id")
    ]
    if not rows or not get_config_value("youtube_refresh_token"):
        return {}
    ids = [str(row["external_id"]) for row in rows]
    response = requests.get(
        f"{API_BASE}/videos",
        headers={"Authorization": f"Bearer {_access_token()}"},
        params={"part": "statistics,status", "id": ",".join(ids)},
        timeout=30,
    )
    response.raise_for_status()
    by_id = {str(item["id"]): item for item in response.json().get("items", [])}
    result: dict[str, Any] = {}
    for row in rows:
        item = by_id.get(str(row["external_id"]))
        if not item:
            continue
        stats = item.get("statistics") or {}
        metrics = {
            "views": int(stats.get("viewCount") or 0),
            "likes": int(stats.get("likeCount") or 0),
            "comments": int(stats.get("commentCount") or 0),
        }
        metadata = dict(row.get("metadata") or {})
        metadata.update({"metrics": metrics, "metrics_fetched_at": datetime.now(timezone.utc).isoformat()})
        values: dict[str, Any] = {"metadata": metadata}
        if str((item.get("status") or {}).get("privacyStatus", "")) == "public" and row.get("status") == "scheduled":
            values.update({"status": "published", "published_at": datetime.now(timezone.utc)})
        update_growth_post(str(row["id"]), values)
        result[str(row["id"])] = metrics
    return result
