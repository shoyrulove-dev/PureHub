from __future__ import annotations

import re
from typing import Any
from urllib.parse import urlencode

import requests

try:
    from .database import get_config_value, update_config
except ImportError:
    from database import get_config_value, update_config


AUTH_URL = "https://www.reddit.com/api/v1/authorize"
TOKEN_URL = "https://www.reddit.com/api/v1/access_token"
REVOKE_URL = "https://www.reddit.com/api/v1/revoke_token"
API_BASE = "https://oauth.reddit.com"
SCOPES = "identity submit"
SUBREDDIT_PATTERN = re.compile(r"^[A-Za-z0-9_]{3,21}$")


def _user_agent() -> str:
    return get_config_value(
        "reddit_user_agent",
        "web:PureHub.CommandCenter:v1.0 (by /u/PureHubAAA)",
    )


def reddit_connection_state() -> dict[str, Any]:
    return {
        "client_configured": bool(get_config_value("reddit_client_id") and get_config_value("reddit_client_secret")),
        "connected": bool(get_config_value("reddit_refresh_token")),
        "username": get_config_value("reddit_username"),
        "default_subreddit": get_config_value("reddit_default_subreddit", "droidappshowcase"),
    }


def build_authorization_url(*, state: str, redirect_uri: str) -> str:
    client_id = get_config_value("reddit_client_id")
    if not client_id or not get_config_value("reddit_client_secret"):
        raise ValueError("Configure the Reddit client ID and secret first.")
    params = {
        "client_id": client_id,
        "response_type": "code",
        "state": state,
        "redirect_uri": redirect_uri,
        "duration": "permanent",
        "scope": SCOPES,
    }
    return f"{AUTH_URL}?{urlencode(params)}"


def _token_request(data: dict[str, str]) -> dict[str, Any]:
    response = requests.post(
        TOKEN_URL,
        auth=(get_config_value("reddit_client_id"), get_config_value("reddit_client_secret")),
        data=data,
        headers={"User-Agent": _user_agent()},
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    if payload.get("error"):
        raise ValueError(f"Reddit OAuth failed: {payload['error']}")
    return payload


def _access_token() -> str:
    refresh_token = get_config_value("reddit_refresh_token")
    if not refresh_token:
        raise ValueError("Reddit is not connected.")
    payload = _token_request({"grant_type": "refresh_token", "refresh_token": refresh_token})
    access_token = str(payload.get("access_token") or "")
    if not access_token:
        raise ValueError("Reddit did not return an access token.")
    return access_token


def _api_headers(access_token: str) -> dict[str, str]:
    return {"Authorization": f"bearer {access_token}", "User-Agent": _user_agent()}


def fetch_reddit_profile(access_token: str | None = None) -> dict[str, Any]:
    response = requests.get(
        f"{API_BASE}/api/v1/me",
        headers=_api_headers(access_token or _access_token()),
        timeout=30,
    )
    response.raise_for_status()
    return response.json()


def connect_reddit(*, code: str, redirect_uri: str) -> dict[str, Any]:
    payload = _token_request(
        {
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": redirect_uri,
        }
    )
    refresh_token = str(payload.get("refresh_token") or "")
    if not refresh_token:
        raise ValueError("Reddit did not return a permanent refresh token. Reconnect and allow access.")
    profile = fetch_reddit_profile(str(payload.get("access_token") or ""))
    update_config(
        {
            "reddit_refresh_token": refresh_token,
            "reddit_username": str(profile.get("name") or ""),
        }
    )
    return reddit_connection_state()


def disconnect_reddit() -> None:
    refresh_token = get_config_value("reddit_refresh_token")
    if refresh_token:
        try:
            requests.post(
                REVOKE_URL,
                auth=(get_config_value("reddit_client_id"), get_config_value("reddit_client_secret")),
                data={"token": refresh_token, "token_type_hint": "refresh_token"},
                headers={"User-Agent": _user_agent()},
                timeout=20,
            )
        except Exception:
            pass
    update_config({"reddit_refresh_token": "", "reddit_username": ""})


def normalize_subreddit(value: str) -> str:
    subreddit = (value or "").strip()
    subreddit = re.sub(r"^https?://(?:www\.)?reddit\.com/r/", "", subreddit, flags=re.IGNORECASE)
    subreddit = re.sub(r"^/?r/", "", subreddit, flags=re.IGNORECASE).strip("/ ")
    if not SUBREDDIT_PATTERN.fullmatch(subreddit):
        raise ValueError("Enter one valid subreddit name, for example droidappshowcase (without r/).")
    return subreddit


def submit_reddit_post(*, subreddit: str, title: str, body: str) -> tuple[str, str]:
    target = normalize_subreddit(subreddit)
    clean_title = (title or "").strip()
    clean_body = (body or "").strip()
    if not clean_title or len(clean_title) > 300:
        raise ValueError("Reddit title must contain 1-300 characters.")
    if not clean_body:
        raise ValueError("Reddit post body cannot be empty.")
    response = requests.post(
        f"{API_BASE}/api/submit",
        headers=_api_headers(_access_token()),
        data={
            "api_type": "json",
            "kind": "self",
            "sr": target,
            "title": clean_title,
            "text": clean_body,
            "resubmit": "true",
            "sendreplies": "true",
        },
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    reddit_json = payload.get("json") or {}
    errors = reddit_json.get("errors") or []
    if errors:
        details = "; ".join(": ".join(str(part) for part in error[:2]) for error in errors)
        raise ValueError(f"Reddit rejected the post: {details}")
    data = reddit_json.get("data") or {}
    external_id = str(data.get("name") or data.get("id") or "")
    external_url = str(data.get("url") or "")
    if external_url.startswith("/"):
        external_url = f"https://www.reddit.com{external_url}"
    if not external_id or not external_url:
        raise ValueError("Reddit accepted the request but did not return a post URL.")
    return external_id, external_url
