from __future__ import annotations

import html
import json
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from html.parser import HTMLParser
from typing import Any

import requests

try:
    from .database import (
        get_config_value,
        get_support_sync_state,
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
        get_support_sync_state,
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
    # Private messages are handled immediately by TelegramBotWorker when auto
    # reply is enabled. Keeping a second Support Inbox copy would create a
    # duplicate draft after the user has already received an answer.
    if chat.get("type") == "private" and get_config_value("community_reply_mode", "draft") == "auto":
        return None

    # Telegram automatically forwards channel posts into a linked discussion
    # group. Those posts are PureHub campaign content, not support requests.
    # Also ignore posts explicitly authored or forwarded by our own channel.
    notify_chat_id = get_config_value("telegram_notify_chat_id", "").strip()
    sender_chat = message.get("sender_chat") or {}
    forward_from_chat = message.get("forward_from_chat") or {}
    forward_origin = message.get("forward_origin") or {}
    origin_chat = forward_origin.get("chat") if isinstance(forward_origin, dict) else {}
    source_chats = [item for item in (sender_chat, forward_from_chat, origin_chat) if isinstance(item, dict)]
    if message.get("is_automatic_forward"):
        return None
    if any(str(item.get("id", "")) == notify_chat_id for item in source_chats if notify_chat_id):
        return None
    if str(sender_chat.get("type", "")).lower() == "channel":
        return None

    bot_username = get_config_value("telegram_bot_username", "").strip().lstrip("@").lower()
    sender_username = str(sender.get("username", "")).strip().lstrip("@").lower()
    if bot_username and sender_username == bot_username:
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
            "inbox_type": "direct_support",
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


def _analyze_message(
    row: dict[str, Any],
    previous_draft: str = "",
    guidance: str = "",
) -> dict[str, Any]:
    is_opportunity = str((row.get("reply_context") or {}).get("source_kind", "")) == "discovery"
    fallback_draft = (
        "That depends on the use case. What matters most to you here: privacy, compatibility, or ease of use? "
        "I build PureHub, so I am interested in the trade-off rather than pushing a one-size-fits-all answer."
        if is_opportunity
        else "Thanks for reaching out. Could you share your PureHub version, device model, and the steps that led to this issue?"
    )
    fallback = {
        "category": "opportunity" if is_opportunity else "question" if "?" in row.get("content", "") else "feedback",
        "priority": "normal",
        "language": "en",
        "requires_reply": True,
        "draft": fallback_draft,
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
                        "bug, feature_request, privacy, installation, opportunity, praise, spam, other. Priorities: low, normal, high, urgent. "
                        "Reply in the user's language, keep it friendly and concise, never invent a shipped fix, never request "
                        "passwords or API keys, and request app version/device details for bugs. For discovery opportunities, answer "
                        "the person's actual question first, disclose 'I build PureHub' before mentioning it, mention PureHub only when "
                        "it genuinely fits, include no link unless the person asked for recommendations, and never sound like an ad. "
                        "Use no more than two relevant emoji. Answer the actual question before discussing PureHub. Base the reply "
                        "only on the supplied message and context; do not assume a feature, fix, release status, or platform capability. "
                        "If the facts are insufficient, say what is uncertain and ask one focused follow-up question. When a previous "
                        "draft is supplied, create a materially different and more accurate alternative rather than merely paraphrasing it, "
                        "and follow the operator's requested correction."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "platform": row.get("platform"),
                            "author": row.get("author_handle") or row.get("author_name"),
                            "message": row.get("content"),
                            "task": "write_alternative_reply" if previous_draft else "write_first_reply",
                            "previous_draft": previous_draft,
                            "operator_correction": guidance,
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
            temperature=0.55 if previous_draft else 0.2,
        )
        raw = (response.choices[0].message.content or "").strip().removeprefix("```json").removesuffix("```").strip()
        data = json.loads(raw)
        category = str(data.get("category", fallback["category"]))
        priority = str(data.get("priority", "normal"))
        if category not in {"question", "bug", "feature_request", "privacy", "installation", "opportunity", "praise", "spam", "other"}:
            category = "other"
        if priority not in {"low", "normal", "high", "urgent"}:
            priority = "normal"
        requires_reply = bool(data.get("requires_reply", True))
        draft = str(data.get("draft") or "").strip()
        if not draft:
            if previous_draft:
                raise ValueError("AI returned an empty alternative support draft.")
            draft = fallback["draft"]
        return {
            "category": category,
            "priority": priority,
            "language": str(data.get("language", "en"))[:12],
            "requires_reply": requires_reply,
            "draft": draft,
        }
    except Exception:
        if previous_draft:
            raise
        return fallback


def generate_support_drafts(limit: int = 20) -> dict[str, int]:
    generated = 0
    ignored = 0
    rows = list_support_messages(status="new", limit=limit)
    with ThreadPoolExecutor(max_workers=min(8, max(1, len(rows)))) as executor:
        analyses = list(executor.map(_analyze_message, rows))
    for row, analysis in zip(rows, analyses):
        asks_question = _looks_like_question(str(row.get("content") or ""))
        requires_reply = analysis["category"] != "spam" and (analysis["requires_reply"] or asks_question)
        category = "question" if asks_question and analysis["category"] == "praise" else analysis["category"]
        status = "draft_ready" if requires_reply else "ignored"
        update_support_message(
            row["id"],
            {
                "category": category,
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


def generate_support_draft(
    message_id: str,
    previous_draft: str = "",
    guidance: str = "",
) -> dict[str, Any]:
    row = get_support_message(message_id)
    if not row:
        raise ValueError("Support message not found.")
    analysis = _analyze_message(
        row,
        previous_draft=previous_draft.strip()[:2000],
        guidance=guidance.strip()[:500],
    )
    asks_question = _looks_like_question(str(row.get("content") or ""))
    requires_reply = analysis["category"] != "spam" and (analysis["requires_reply"] or asks_question)
    category = "question" if asks_question and analysis["category"] == "praise" else analysis["category"]
    update_support_message(
        message_id,
        {
            "category": category,
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
                            "inbox_type": "purehub_post",
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
                "inbox_type": "purehub_post" if notification.get("reason") in {"reply", "quote"} else "social_mention",
                "external_id": uri,
                "thread_id": str((root or {}).get("uri", uri)),
                "parent_external_id": str((reply.get("parent") or {}).get("uri", "")),
                "author_id": str(author.get("did", "")),
                "author_name": str(author.get("displayName", handle)),
                "author_handle": handle,
                "content": str(record.get("text", "")).strip(),
                "source_url": f"https://bsky.app/profile/{handle}/post/{record_key}",
                "received_at": _iso_datetime(notification.get("indexedAt")),
                "reply_context": {"uri": uri, "cid": cid, "root": root, "interaction_kind": notification.get("reason", "mention")},
            }
        )
        created += int(inserted)
    return created


def _sync_mastodon() -> int:
    base = get_config_value("mastodon_base_url").rstrip("/")
    headers = {
        "Authorization": f"Bearer {get_config_value('mastodon_access_token')}",
        "user-agent": "PureHub-Support-Monitor/1.0",
    }
    own_account_response = requests.get(f"{base}/api/v1/accounts/verify_credentials", headers=headers, timeout=30)
    own_account_response.raise_for_status()
    own_account_id = str(own_account_response.json().get("id", ""))
    response = requests.get(
        f"{base}/api/v1/notifications",
        headers=headers,
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
        is_purehub_reply = bool(own_account_id and str(status.get("in_reply_to_account_id", "")) == own_account_id)
        _, inserted = upsert_support_message(
            {
                "source_key": f"mastodon:{status_id}",
                "platform": "mastodon",
                "inbox_type": "purehub_post" if is_purehub_reply else "social_mention",
                "external_id": status_id,
                "thread_id": str(status.get("in_reply_to_id") or status_id),
                "parent_external_id": str(status.get("in_reply_to_id") or ""),
                "author_id": str(account.get("id", "")),
                "author_name": str(account.get("display_name") or account.get("username", "")),
                "author_handle": str(account.get("acct", "")),
                "content": _plain_text(str(status.get("content", ""))),
                "source_url": str(status.get("url", "")),
                "received_at": _iso_datetime(status.get("created_at")),
                "reply_context": {
                    "status_id": status_id,
                    "account": str(account.get("acct", "")),
                    "interaction_kind": "purehub_post" if is_purehub_reply else "mention",
                },
            }
        )
        created += int(inserted)
    return created


def _opportunity_keywords() -> list[str]:
    raw = get_config_value(
        "opportunity_keywords",
        "best offline app,app without ads,privacy first app,open source Android app,offline OCR scanner,QR scanner no ads,simple Pomodoro app,password manager offline,expense tracker offline,unit converter app,habit tracker no ads,note app offline",
    )
    values = [item.strip() for item in raw.replace("\n", ",").split(",") if item.strip()]
    return values[:16]


def _looks_like_question(text: str) -> bool:
    value = text.lower()
    signals = ("?", "how do", "how can", "which app", "what app", "any app", "recommend", "looking for", "alternative")
    return any(signal in value for signal in signals)


def _looks_like_relevant_opportunity(text: str, keyword: str = "") -> bool:
    if not _looks_like_question(text):
        return False
    value = f" {text.lower()} "
    intent_signals = (
        "any app", "which app", "what app", "recommend", "looking for", "alternative",
        "without ads", "no ads", "offline", "privacy", "open source",
    )
    utility_signals = (
        " app", " tool", "utility", "scanner", "ocr", "qr code", "pomodoro", "password",
        "expense", "converter", "habit", "notes", "timer", "calculator",
    )
    if any(signal in value for signal in intent_signals):
        return True
    if keyword.startswith("#"):
        return any(signal in value for signal in utility_signals)
    keyword_tokens = [token.lower() for token in keyword.split() if len(token) >= 4]
    return any(signal in value for signal in utility_signals) and any(token in value for token in keyword_tokens)


def _discover_bluesky(keywords: list[str], limit: int) -> int:
    own_handle = get_config_value("bluesky_handle").lower().lstrip("@")
    session = _bluesky_session()
    created = 0
    for keyword in keywords:
        response = requests.get(
            "https://bsky.social/xrpc/app.bsky.feed.searchPosts",
            headers={"Authorization": f"Bearer {session['accessJwt']}"},
            params={"q": keyword, "limit": 10, "sort": "latest"},
            timeout=30,
        )
        response.raise_for_status()
        for post in response.json().get("posts", []):
            record = post.get("record") or {}
            text = str(record.get("text", "")).strip()
            author = post.get("author") or {}
            handle = str(author.get("handle", ""))
            if not text or handle.lower() == own_handle or not _looks_like_relevant_opportunity(text, keyword):
                continue
            uri, cid = str(post.get("uri", "")), str(post.get("cid", ""))
            if not uri or not cid:
                continue
            key = uri.rsplit("/", 1)[-1]
            _, inserted = upsert_support_message(
                {
                    "source_key": f"opportunity:bluesky:{uri}",
                    "platform": "bluesky",
                    "inbox_type": "social_opportunity",
                    "external_id": uri,
                    "thread_id": uri,
                    "author_id": str(author.get("did", "")),
                    "author_name": str(author.get("displayName") or handle),
                    "author_handle": handle,
                    "content": text,
                    "source_url": f"https://bsky.app/profile/{handle}/post/{key}",
                    "received_at": _iso_datetime(record.get("createdAt") or post.get("indexedAt")),
                    "reply_context": {"uri": uri, "cid": cid, "root": {"uri": uri, "cid": cid}, "source_kind": "discovery", "keyword": keyword},
                }
            )
            created += int(inserted)
            if created >= limit:
                return created
    return created


def _discover_mastodon(keywords: list[str], limit: int) -> int:
    base = get_config_value("mastodon_base_url").rstrip("/")
    headers = {"Authorization": f"Bearer {get_config_value('mastodon_access_token')}", "user-agent": "PureHub-Opportunity-Monitor/1.0"}
    created = 0
    fallback_tags = ("android", "opensource", "foss", "productivity")
    sources: list[tuple[str, list[dict[str, Any]]]] = []
    search_denied = False
    for keyword in keywords:
        if search_denied:
            break
        response = requests.get(
            f"{base}/api/v2/search",
            headers=headers,
            params={"q": keyword, "type": "statuses", "limit": 10, "resolve": "false"},
            timeout=30,
        )
        if response.status_code == 403:
            search_denied = True
            break
        response.raise_for_status()
        sources.append((keyword, response.json().get("statuses", [])))
    if search_denied:
        for tag in fallback_tags:
            response = requests.get(
                f"{base}/api/v1/timelines/tag/{tag}",
                headers={"user-agent": "PureHub-Opportunity-Monitor/1.0"},
                params={"limit": 20},
                timeout=30,
            )
            response.raise_for_status()
            sources.append((f"#{tag}", response.json()))
    for keyword, statuses in sources:
        for status in statuses:
            account = status.get("account") or {}
            text = _plain_text(str(status.get("content", "")))
            status_id = str(status.get("id", ""))
            if not status_id or not text or not _looks_like_relevant_opportunity(text, keyword):
                continue
            _, inserted = upsert_support_message(
                {
                    "source_key": f"opportunity:mastodon:{status_id}",
                    "platform": "mastodon",
                    "inbox_type": "social_opportunity",
                    "external_id": status_id,
                    "thread_id": status_id,
                    "author_id": str(account.get("id", "")),
                    "author_name": str(account.get("display_name") or account.get("username", "")),
                    "author_handle": str(account.get("acct", "")),
                    "content": text,
                    "source_url": str(status.get("url", "")),
                    "received_at": _iso_datetime(status.get("created_at")),
                    "reply_context": {"status_id": status_id, "account": str(account.get("acct", "")), "source_kind": "discovery", "keyword": keyword},
                }
            )
            created += int(inserted)
            if created >= limit:
                return created
    return created


def _discover_devto(keywords: list[str], limit: int) -> int:
    tokens = {part.lower() for keyword in keywords for part in keyword.split() if len(part) >= 4}
    created = 0
    for tag in ("android", "opensource", "productivity", "discuss"):
        response = requests.get(
            "https://dev.to/api/articles",
            headers={"accept": "application/vnd.forem.api-v1+json", "user-agent": "PureHub-Opportunity-Monitor/1.0"},
            params={"tag": tag, "state": "fresh", "per_page": 20},
            timeout=30,
        )
        response.raise_for_status()
        for article in response.json():
            title = str(article.get("title", ""))
            description = str(article.get("description", ""))
            combined = f"{title}\n{description}".strip()
            if not _looks_like_relevant_opportunity(combined, tag) or not any(token in combined.lower() for token in tokens):
                continue
            article_id = str(article.get("id", ""))
            user = article.get("user") or {}
            _, inserted = upsert_support_message(
                {
                    "source_key": f"opportunity:devto:{article_id}",
                    "platform": "devto",
                    "inbox_type": "social_opportunity",
                    "external_id": article_id,
                    "thread_id": article_id,
                    "author_id": str(user.get("user_id", "")),
                    "author_name": str(user.get("name") or user.get("username", "")),
                    "author_handle": str(user.get("username", "")),
                    "content": combined,
                    "source_url": str(article.get("url", "")),
                    "received_at": _iso_datetime(article.get("published_timestamp") or article.get("published_at")),
                    "reply_context": {"article_id": article_id, "source_kind": "discovery", "keyword": tag},
                }
            )
            created += int(inserted)
            if created >= limit:
                return created
    return created


def discover_opportunities() -> dict[str, Any]:
    if get_config_value("opportunity_monitor_enabled", "true").lower() != "true":
        return {"enabled": False, "channels": {}}
    state = get_support_sync_state("opportunities")
    if state.get("last_synced_at") and _iso_datetime(str(state["last_synced_at"])).date() == datetime.now(timezone.utc).date():
        return {"enabled": True, "skipped": True, "reason": "Daily discovery already completed.", "channels": {}}
    keywords = _opportunity_keywords()
    total_limit = max(1, min(int(get_config_value("opportunity_daily_limit", "27") or 27), 30))
    functions = {"bluesky": _discover_bluesky, "mastodon": _discover_mastodon, "devto": _discover_devto}
    devto_limit = max(1, round(total_limit * 0.1))
    social_limit = total_limit - devto_limit
    platform_limits = {
        "bluesky": (social_limit + 1) // 2,
        "mastodon": social_limit // 2,
        "devto": devto_limit,
    }
    result: dict[str, Any] = {"enabled": True, "target": total_limit, "channels": {}}

    def run(platform: str, discover: Any) -> tuple[str, dict[str, Any]]:
        try:
            target = platform_limits[platform]
            return platform, {"ok": True, "target": target, "created": discover(keywords, target)}
        except Exception as exc:
            return platform, {"ok": False, "target": platform_limits[platform], "created": 0, "error": str(exc)[:500]}

    with ThreadPoolExecutor(max_workers=len(functions)) as executor:
        for platform, outcome in executor.map(lambda item: run(*item), functions.items()):
            result["channels"][platform] = outcome
    remaining = max(0, total_limit - sum(int(item.get("created") or 0) for item in result["channels"].values()))
    for platform in ("bluesky", "mastodon"):
        if not remaining:
            break
        outcome = result["channels"][platform]
        if not outcome.get("ok"):
            continue
        try:
            extra = functions[platform](keywords, remaining)
            outcome["created"] = int(outcome.get("created") or 0) + extra
            outcome["backfilled"] = extra
            remaining -= extra
        except Exception as exc:
            outcome["backfill_error"] = str(exc)[:500]
    result["created"] = sum(int(item.get("created") or 0) for item in result["channels"].values())
    result["shortfall"] = max(0, total_limit - result["created"])
    update_support_sync_state("opportunities", {"last_synced_at": datetime.now(timezone.utc), "error_message": ""})
    return result


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

    def run(platform: str, sync: Any) -> tuple[str, dict[str, Any]]:
        try:
            metrics = sync()
            upsert_community_metrics(platform, metrics, "")
            return platform, {"ok": True, "metrics": metrics}
        except Exception as exc:
            error = str(exc)[:500]
            upsert_community_metrics(platform, None, error)
            return platform, {"ok": False, "error": error}

    with ThreadPoolExecutor(max_workers=len(functions)) as executor:
        for platform, outcome in executor.map(lambda item: run(*item), functions.items()):
            result[platform] = outcome
    return result


def sync_support_channels(generate_drafts: bool = True) -> dict[str, Any]:
    if get_config_value("support_monitor_enabled", "true").lower() != "true":
        return {"enabled": False, "channels": {}, "drafts": {}, "engagement": sync_engagement_metrics()}
    functions = {"devto": _sync_devto, "bluesky": _sync_bluesky, "mastodon": _sync_mastodon}
    result: dict[str, Any] = {"enabled": True, "channels": {}}

    def run(platform: str, sync: Any) -> tuple[str, dict[str, Any]]:
        try:
            created = sync()
            update_support_sync_state(platform, {"last_synced_at": datetime.now(timezone.utc), "error_message": ""})
            return platform, {"ok": True, "created": created}
        except Exception as exc:
            error = str(exc)[:500]
            update_support_sync_state(platform, {"last_synced_at": datetime.now(timezone.utc), "error_message": error})
            return platform, {"ok": False, "created": 0, "error": error}

    with ThreadPoolExecutor(max_workers=len(functions)) as executor:
        for platform, outcome in executor.map(lambda item: run(*item), functions.items()):
            result["channels"][platform] = outcome
    result["opportunities"] = discover_opportunities()
    draft_limit = max(8, min(int(get_config_value("opportunity_daily_limit", "27") or 27) + 4, 34))
    result["drafts"] = generate_support_drafts(limit=draft_limit) if generate_drafts else {}
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
