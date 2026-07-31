from __future__ import annotations

import html
import json
from datetime import datetime, timezone
from html.parser import HTMLParser
from typing import Any

import requests

try:
    from .database import (
        get_config_value,
        get_support_message,
        list_support_messages,
        update_support_message,
        update_support_sync_state,
        upsert_community_metrics,
        upsert_support_message,
    )
    from .release_hub import _ai_client, _bluesky_facets
except ImportError:
    from database import (
        get_config_value,
        get_support_message,
        list_support_messages,
        update_support_message,
        update_support_sync_state,
        upsert_community_metrics,
        upsert_support_message,
    )
    from release_hub import _ai_client, _bluesky_facets


class _TextExtractor(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.parts: list[str] = []

    def handle_data(self, data: str) -> None:
        if data.strip():
            self.parts.append(data.strip())


def _plain_text(value: str) -> str:
    parser = _TextExtractor()
    parser.feed(value or "")
    return html.unescape(" ".join(parser.parts)).strip()


def _iso_datetime(value: str | None) -> datetime:
    if not value:
        return datetime.now(timezone.utc)
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return datetime.now(timezone.utc)


def ingest_telegram_update(payload: dict[str, Any]) -> dict[str, Any] | None:
    message = payload.get("message") or payload.get("edited_message")
    if not isinstance(message, dict):
        return None
    sender = message.get("from") or {}
    if sender.get("is_bot"):
        return None
    chat = message.get("chat") or {}
    chat_id = str(chat.get("id", ""))
    support_chat_id = get_config_value("telegram_support_chat_id", "-1003762178712")
    if chat.get("type") != "private" and chat_id != support_chat_id:
        return None
    content = str(message.get("text") or message.get("caption") or "").strip()
    if not content or content.startswith(("/start", "/help", "/profile", "/github", "/release", "/ask")):
        return None
    message_id = str(message.get("message_id", ""))
    username = str(chat.get("username", ""))
    source_url = f"https://t.me/{username}/{message_id}" if username else ""
    reply_to = message.get("reply_to_message") or {}
    row, _ = upsert_support_message(
        {
            "source_key": f"telegram:{chat_id}:{message_id}",
            "platform": "telegram",
            "external_id": message_id,
            "thread_id": chat_id,
            "parent_external_id": str(reply_to.get("message_id", "")),
            "author_id": str(sender.get("id", "")),
            "author_name": " ".join(
                part for part in (str(sender.get("first_name", "")), str(sender.get("last_name", ""))) if part
            ),
            "author_handle": str(sender.get("username", "")),
            "content": content,
            "source_url": source_url,
            "received_at": datetime.fromtimestamp(int(message.get("date", 0)), timezone.utc),
            "reply_context": {"chat_id": chat_id, "message_id": message_id},
        }
    )
    return row


def _analyze_message(row: dict[str, Any]) -> dict[str, Any]:
    fallback = {
        "category": "question" if "?" in row.get("content", "") else "feedback",
        "priority": "normal",
        "language": "en",
        "requires_reply": True,
        "draft": "Thanks for reaching out. Could you share your PureHub version, device model, and the steps that led to this issue?",
    }
    try:
        client, model = _ai_client()
        response = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You triage support for PureHub, a free, no-ads, privacy-first, open-source collection of 22 tools. "
                        "Return JSON only with category, priority, language, requires_reply, and draft. Categories: question, "
                        "bug, feature_request, privacy, installation, praise, spam, other. Priorities: low, normal, high, urgent. "
                        "Reply in the user's language, keep it friendly and concise, never invent a shipped fix, never request "
                        "passwords or API keys, and request app version/device details for bugs. Use no more than two relevant emoji."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "platform": row.get("platform"),
                            "author": row.get("author_handle") or row.get("author_name"),
                            "message": row.get("content"),
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
            temperature=0.2,
        )
        raw = (response.choices[0].message.content or "").strip().removeprefix("```json").removesuffix("```").strip()
        data = json.loads(raw)
        category = str(data.get("category", fallback["category"]))
        priority = str(data.get("priority", "normal"))
        if category not in {"question", "bug", "feature_request", "privacy", "installation", "praise", "spam", "other"}:
            category = "other"
        if priority not in {"low", "normal", "high", "urgent"}:
            priority = "normal"
        return {
            "category": category,
            "priority": priority,
            "language": str(data.get("language", "en"))[:12],
            "requires_reply": bool(data.get("requires_reply", True)),
            "draft": str(data.get("draft", fallback["draft"])).strip(),
        }
    except Exception:
        return fallback


def generate_support_drafts(limit: int = 20) -> dict[str, int]:
    generated = 0
    ignored = 0
    for row in list_support_messages(status="new", limit=limit):
        analysis = _analyze_message(row)
        requires_reply = analysis["requires_reply"] and analysis["category"] not in {"praise", "spam"}
        status = "draft_ready" if requires_reply else "ignored"
        update_support_message(
            row["id"],
            {
                "category": analysis["category"],
                "priority": analysis["priority"],
                "language": analysis["language"],
                "requires_reply": requires_reply,
                "ai_draft": analysis["draft"] if requires_reply else "",
                "reply_text": analysis["draft"] if requires_reply else "",
                "status": status,
                "error_message": "",
            },
        )
        generated += int(requires_reply)
        ignored += int(not requires_reply)
    return {"generated": generated, "ignored": ignored}


def generate_support_draft(message_id: str) -> dict[str, Any]:
    row = get_support_message(message_id)
    if not row:
        raise ValueError("Support message not found.")
    analysis = _analyze_message(row)
    requires_reply = analysis["requires_reply"] and analysis["category"] not in {"praise", "spam"}
    update_support_message(
        message_id,
        {
            "category": analysis["category"],
            "priority": analysis["priority"],
            "language": analysis["language"],
            "requires_reply": requires_reply,
            "ai_draft": analysis["draft"] if requires_reply else "",
            "reply_text": analysis["draft"] if requires_reply else "",
            "status": "draft_ready" if requires_reply else "ignored",
            "error_message": "",
        },
    )
    return get_support_message(message_id) or row


def _sync_devto() -> int:
    headers = {
        "api-key": get_config_value("devto_api_key"),
        "accept": "application/vnd.forem.api-v1+json",
        "user-agent": "PureHub-Support-Monitor/1.0",
    }
    response = requests.get("https://dev.to/api/articles/me", headers=headers, params={"per_page": 20}, timeout=30)
    response.raise_for_status()
    created = 0
    for article in response.json():
        article_id = str(article.get("id", ""))
        comments = requests.get(
            "https://dev.to/api/comments",
            headers=headers,
            params={"a_id": article_id},
            timeout=30,
        )
        comments.raise_for_status()
        own_names = {
            str((article.get("user") or {}).get("username", "")).lower(),
            str((article.get("organization") or {}).get("username", "")).lower(),
        }

        def visit(items: list[dict[str, Any]], parent_id: str = "") -> None:
            nonlocal created
            for comment in items:
                user = comment.get("user") or {}
                username = str(user.get("username", ""))
                comment_id = str(comment.get("id_code", ""))
                if username.lower() not in own_names:
                    _, inserted = upsert_support_message(
                        {
                            "source_key": f"devto:{article_id}:{comment_id}",
                            "platform": "devto",
                            "external_id": comment_id,
                            "thread_id": article_id,
                            "parent_external_id": parent_id,
                            "author_id": str(user.get("user_id", "")),
                            "author_name": str(user.get("name", username)),
                            "author_handle": username,
                            "content": _plain_text(str(comment.get("body_html", ""))),
                            "source_url": f"{article.get('url', '')}#comment-{comment_id}",
                            "received_at": _iso_datetime(comment.get("created_at")),
                            "reply_context": {"article_id": article_id, "comment_id": comment_id},
                        }
                    )
                    created += int(inserted)
                visit(comment.get("children") or [], comment_id)

        visit(comments.json())
    return created


def _bluesky_session() -> dict[str, Any]:
    response = requests.post(
        "https://bsky.social/xrpc/com.atproto.server.createSession",
        json={
            "identifier": get_config_value("bluesky_handle"),
            "password": get_config_value("bluesky_app_password"),
        },
        timeout=30,
    )
    response.raise_for_status()
    return response.json()


def _sync_bluesky() -> int:
    session = _bluesky_session()
    response = requests.get(
        "https://bsky.social/xrpc/app.bsky.notification.listNotifications",
        headers={"Authorization": f"Bearer {session['accessJwt']}"},
        params={"limit": 100},
        timeout=30,
    )
    response.raise_for_status()
    created = 0
    for notification in response.json().get("notifications", []):
        if notification.get("reason") not in {"reply", "mention", "quote"}:
            continue
        record = notification.get("record") or {}
        author = notification.get("author") or {}
        uri = str(notification.get("uri", ""))
        cid = str(notification.get("cid", ""))
        handle = str(author.get("handle", ""))
        record_key = uri.rsplit("/", 1)[-1]
        reply = record.get("reply") or {}
        root = reply.get("root") or {"uri": uri, "cid": cid}
        _, inserted = upsert_support_message(
            {
                "source_key": f"bluesky:{uri}",
                "platform": "bluesky",
                "external_id": uri,
                "thread_id": str((root or {}).get("uri", uri)),
                "parent_external_id": str((reply.get("parent") or {}).get("uri", "")),
                "author_id": str(author.get("did", "")),
                "author_name": str(author.get("displayName", handle)),
                "author_handle": handle,
                "content": str(record.get("text", "")).strip(),
                "source_url": f"https://bsky.app/profile/{handle}/post/{record_key}",
                "received_at": _iso_datetime(notification.get("indexedAt")),
                "reply_context": {"uri": uri, "cid": cid, "root": root},
            }
        )
        created += int(inserted)
    return created


def _sync_mastodon() -> int:
    base = get_config_value("mastodon_base_url").rstrip("/")
    response = requests.get(
        f"{base}/api/v1/notifications",
        headers={
            "Authorization": f"Bearer {get_config_value('mastodon_access_token')}",
            "user-agent": "PureHub-Support-Monitor/1.0",
        },
        params=[("limit", "80"), ("types[]", "mention")],
        timeout=30,
    )
    response.raise_for_status()
    created = 0
    for notification in response.json():
        status = notification.get("status") or {}
        account = notification.get("account") or {}
        status_id = str(status.get("id", ""))
        if not status_id:
            continue
        _, inserted = upsert_support_message(
            {
                "source_key": f"mastodon:{status_id}",
                "platform": "mastodon",
                "external_id": status_id,
                "thread_id": str(status.get("in_reply_to_id") or status_id),
                "parent_external_id": str(status.get("in_reply_to_id") or ""),
                "author_id": str(account.get("id", "")),
                "author_name": str(account.get("display_name") or account.get("username", "")),
                "author_handle": str(account.get("acct", "")),
                "content": _plain_text(str(status.get("content", ""))),
                "source_url": str(status.get("url", "")),
                "received_at": _iso_datetime(status.get("created_at")),
                "reply_context": {"status_id": status_id, "account": str(account.get("acct", ""))},
            }
        )
        created += int(inserted)
    return created


def _sync_telegram_metrics() -> dict[str, int]:
    response = requests.get(
        f"https://api.telegram.org/bot{get_config_value('telegram_bot_token')}/getChatMemberCount",
        params={"chat_id": get_config_value("telegram_notify_chat_id")},
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    if not payload.get("ok"):
        raise ValueError(str(payload.get("description") or "Telegram metrics request failed."))
    return {"members": int(payload.get("result") or 0)}


def _sync_devto_metrics() -> dict[str, int]:
    headers = {
        "api-key": get_config_value("devto_api_key"),
        "accept": "application/vnd.forem.api-v1+json",
        "user-agent": "PureHub-Community-Metrics/1.0",
    }
    response = requests.get("https://dev.to/api/articles/me", headers=headers, params={"per_page": 100}, timeout=30)
    response.raise_for_status()
    articles = response.json()
    return {
        "articles": len(articles),
        "views": sum(int(item.get("page_views_count") or 0) for item in articles),
        "reactions": sum(int(item.get("public_reactions_count") or 0) for item in articles),
        "comments": sum(int(item.get("comments_count") or 0) for item in articles),
    }


def _sync_bluesky_metrics() -> dict[str, int]:
    handle = get_config_value("bluesky_handle")
    profile = requests.get(
        "https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile",
        params={"actor": handle},
        timeout=30,
    )
    profile.raise_for_status()
    feed = requests.get(
        "https://public.api.bsky.app/xrpc/app.bsky.feed.getAuthorFeed",
        params={"actor": handle, "limit": 100, "filter": "posts_no_replies"},
        timeout=30,
    )
    feed.raise_for_status()
    posts = [item.get("post") or {} for item in feed.json().get("feed", [])]
    return {
        "followers": int(profile.json().get("followersCount") or 0),
        "posts": int(profile.json().get("postsCount") or len(posts)),
        "likes": sum(int(post.get("likeCount") or 0) for post in posts),
        "replies": sum(int(post.get("replyCount") or 0) for post in posts),
        "reposts": sum(int(post.get("repostCount") or 0) for post in posts),
        "quotes": sum(int(post.get("quoteCount") or 0) for post in posts),
    }


def _sync_mastodon_metrics() -> dict[str, int]:
    base = get_config_value("mastodon_base_url").rstrip("/")
    headers = {
        "Authorization": f"Bearer {get_config_value('mastodon_access_token')}",
        "user-agent": "PureHub-Community-Metrics/1.0",
    }
    account_response = requests.get(f"{base}/api/v1/accounts/verify_credentials", headers=headers, timeout=30)
    account_response.raise_for_status()
    account = account_response.json()
    statuses_response = requests.get(
        f"{base}/api/v1/accounts/{account['id']}/statuses",
        headers=headers,
        params={"limit": 40, "exclude_replies": "true", "exclude_reblogs": "true"},
        timeout=30,
    )
    statuses_response.raise_for_status()
    statuses = statuses_response.json()
    return {
        "followers": int(account.get("followers_count") or 0),
        "posts": int(account.get("statuses_count") or 0),
        "favourites": sum(int(status.get("favourites_count") or 0) for status in statuses),
        "boosts": sum(int(status.get("reblogs_count") or 0) for status in statuses),
        "replies": sum(int(status.get("replies_count") or 0) for status in statuses),
        "quotes": sum(int(status.get("quotes_count") or 0) for status in statuses),
    }


def sync_engagement_metrics() -> dict[str, Any]:
    functions = {
        "telegram": _sync_telegram_metrics,
        "devto": _sync_devto_metrics,
        "bluesky": _sync_bluesky_metrics,
        "mastodon": _sync_mastodon_metrics,
    }
    result: dict[str, Any] = {}
    for platform, sync in functions.items():
        try:
            metrics = sync()
            upsert_community_metrics(platform, metrics, "")
            result[platform] = {"ok": True, "metrics": metrics}
        except Exception as exc:
            error = str(exc)[:500]
            upsert_community_metrics(platform, None, error)
            result[platform] = {"ok": False, "error": error}
    return result


def sync_support_channels(generate_drafts: bool = True) -> dict[str, Any]:
    if get_config_value("support_monitor_enabled", "true").lower() != "true":
        return {"enabled": False, "channels": {}, "drafts": {}, "engagement": sync_engagement_metrics()}
    functions = {"devto": _sync_devto, "bluesky": _sync_bluesky, "mastodon": _sync_mastodon}
    result: dict[str, Any] = {"enabled": True, "channels": {}}
    for platform, sync in functions.items():
        try:
            created = sync()
            result["channels"][platform] = {"ok": True, "created": created}
            update_support_sync_state(platform, {"last_synced_at": datetime.now(timezone.utc), "error_message": ""})
        except Exception as exc:
            error = str(exc)[:500]
            result["channels"][platform] = {"ok": False, "created": 0, "error": error}
            update_support_sync_state(platform, {"last_synced_at": datetime.now(timezone.utc), "error_message": error})
    result["drafts"] = generate_support_drafts(limit=5) if generate_drafts else {}
    result["engagement"] = sync_engagement_metrics()
    return result


def _send_telegram(row: dict[str, Any], text: str) -> tuple[str, str]:
    response = requests.post(
        f"https://api.telegram.org/bot{get_config_value('telegram_bot_token')}/sendMessage",
        json={
            "chat_id": row["reply_context"]["chat_id"],
            "text": text,
            "reply_parameters": {"message_id": int(row["reply_context"]["message_id"])},
        },
        timeout=30,
    )
    response.raise_for_status()
    message = response.json()["result"]
    username = str((message.get("chat") or {}).get("username", ""))
    url = f"https://t.me/{username}/{message['message_id']}" if username else ""
    return str(message["message_id"]), url


def _send_bluesky(row: dict[str, Any], text: str) -> tuple[str, str]:
    session = _bluesky_session()
    context = row.get("reply_context") or {}
    parent = {"uri": context["uri"], "cid": context["cid"]}
    root = context.get("root") or parent
    response = requests.post(
        "https://bsky.social/xrpc/com.atproto.repo.createRecord",
        headers={"Authorization": f"Bearer {session['accessJwt']}"},
        json={
            "repo": session["did"],
            "collection": "app.bsky.feed.post",
            "record": {
                "$type": "app.bsky.feed.post",
                "text": text[:300],
                "facets": _bluesky_facets(text[:300]),
                "reply": {"root": root, "parent": parent},
                "langs": [row.get("language") or "en"],
                "createdAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
            },
        },
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    key = payload["uri"].rsplit("/", 1)[-1]
    return payload["uri"], f"https://bsky.app/profile/{get_config_value('bluesky_handle')}/post/{key}"


def _send_mastodon(row: dict[str, Any], text: str) -> tuple[str, str]:
    base = get_config_value("mastodon_base_url").rstrip("/")
    handle = str((row.get("reply_context") or {}).get("account", ""))
    body = text if not handle or f"@{handle}".lower() in text.lower() else f"@{handle} {text}"
    response = requests.post(
        f"{base}/api/v1/statuses",
        headers={
            "Authorization": f"Bearer {get_config_value('mastodon_access_token')}",
            "Idempotency-Key": f"purehub-support-{row['id']}",
        },
        data={
            "status": body[:500],
            "in_reply_to_id": (row.get("reply_context") or {}).get("status_id", row["external_id"]),
            "visibility": "public",
        },
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    return str(payload["id"]), str(payload.get("url", ""))


def send_support_reply(message_id: str) -> dict[str, Any]:
    row = get_support_message(message_id)
    if not row:
        raise ValueError("Support message not found.")
    if row.get("status") != "approved":
        raise ValueError("Approve the reply before sending it.")
    text = str(row.get("reply_text") or row.get("ai_draft") or "").strip()
    if not text:
        raise ValueError("Reply text is empty.")
    platform = row["platform"]
    if platform == "devto":
        update_support_message(message_id, {"status": "manual_required", "error_message": ""})
        return get_support_message(message_id) or row
    senders = {"telegram": _send_telegram, "bluesky": _send_bluesky, "mastodon": _send_mastodon}
    if platform not in senders:
        raise ValueError(f"Direct reply is not supported for {platform}.")
    try:
        external_id, external_url = senders[platform](row, text)
        update_support_message(
            message_id,
            {
                "status": "replied",
                "external_reply_id": external_id,
                "external_reply_url": external_url,
                "replied_at": datetime.now(timezone.utc),
                "error_message": "",
            },
        )
    except Exception as exc:
        update_support_message(message_id, {"status": "failed", "error_message": str(exc)[:500]})
        raise
    return get_support_message(message_id) or row
