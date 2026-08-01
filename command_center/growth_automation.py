from __future__ import annotations

import json
import re
from datetime import date, datetime, timezone
from typing import Any
from zoneinfo import ZoneInfo

import requests

try:
    from .community_support import sync_support_channels
    from .database import (
        get_config_value,
        get_growth_post,
        list_growth_posts,
        record_audit_log,
        update_growth_post,
        upsert_growth_post,
    )
    from .release_hub import (
        _ai_client,
        _publish_bluesky,
        _publish_devto,
        _publish_mastodon,
        _publish_telegram,
    )
except ImportError:
    from community_support import sync_support_channels
    from database import (
        get_config_value,
        get_growth_post,
        list_growth_posts,
        record_audit_log,
        update_growth_post,
        upsert_growth_post,
    )
    from release_hub import _ai_client, _publish_bluesky, _publish_devto, _publish_mastodon, _publish_telegram


CAMPAIGN_ID = "community-foundation-30d-v1"
AUTO_CHANNELS = {"telegram", "devto", "bluesky", "mastodon"}
TOPICS = (
    "Meet PureHub: 22 useful tools with no ads",
    "How local-first utility tools protect everyday privacy",
    "OCR: extract text from an image in English, Vietnamese, or Chinese",
    "A clean Pomodoro flow without subscriptions or popups",
    "Engineering lesson: load OCR language data only when requested",
    "Community poll: which mini-app should be improved next?",
    "Week one build-in-public report and an invitation for testers",
    "Password generation and safer Vault defaults",
    "Why automatic locking and timed password reveals matter",
    "Android beta challenge across different phone models",
    "Bubble Level: turn a phone sensor into a useful everyday check",
    "Testing navigation and security contracts across all 22 Android tools",
    "Behind the scenes of building a community-owned utility hub",
    "Two-week feedback report and roadmap checkpoint",
    "Start a seven-day PureHub mini-tool challenge",
    "QR scanning with a privacy-first product mindset",
    "Thank the first testers and explain how feedback becomes work",
    "Open community Q&A: ask about the app, roadmap, or source code",
    "What no-ads software changes about product and interface design",
    "Before-and-after: a calmer shared interface for 22 tools",
    "Three-week transparency report: shipped, learned, and still open",
    "Wi-Fi information made easier to understand",
    "Community roadmap vote for the next deep mini-app upgrade",
    "Build log: bugs, UX findings, and fixes in progress",
    "How route-level code splitting keeps a multi-tool web app responsive",
    "Community spotlight and contributor recognition",
    "Preview the tool selected by the community roadmap vote",
    "Open-source mini AMA and invitation to GitHub Discussions",
    "What the first month of community feedback taught us",
    "Thirty-day transparency report and next-month roadmap",
)


def _channels_for_day(day_number: int) -> list[str]:
    cycle_day = ((day_number - 1) % 30) + 1
    channels = ["bluesky", "mastodon"]
    if ((cycle_day - 1) % 7) + 1 in {1, 3, 6}:
        channels.append("telegram")
    if cycle_day in {5, 12, 19, 25}:
        channels.append("devto")
    if ((cycle_day - 1) % 7) + 1 in {1, 4, 7}:
        channels.append("youtube")
    if cycle_day in {14, 28}:
        channels.append("reddit")
    return channels


def _fallback_content(channel: str, topic: str, day_number: int) -> str:
    site = get_config_value("site_url", "https://hub.blissbiovn.com").rstrip("/")
    tools_url = f"{site}/en/tools"
    if channel == "telegram":
        return (
            f"🛠️ PureHub community build — day {day_number}\n\n{topic}.\n\n"
            "PureHub is free, ad-free, open source, and shaped by user feedback. "
            f"Tell us what would make this tool more useful.\n\nExplore: {tools_url}"
        )
    if channel == "bluesky":
        return (
            f"Building PureHub in public, day {day_number}: {topic}. "
            f"Free, no ads, open source. What should we improve? {tools_url} #OpenSource #Android"
        )[:300]
    if channel == "mastodon":
        return (
            f"PureHub build-in-public day {day_number} 🛠️\n\n{topic}.\n\n"
            f"Free, ad-free, privacy-first, and open source. Feedback is welcome: {tools_url}\n\n"
            "#OpenSource #FOSS #Android"
        )[:500]
    if channel == "devto":
        return (
            f"# {topic}\n\n"
            "PureHub is a free, ad-free, open-source collection of 22 utility tools. "
            "This article shares a practical engineering lesson from the project rather than a product pitch.\n\n"
            "## What we changed\n\nWe focused on clearer mobile interaction, local-first behavior, and measurable loading or testing improvements.\n\n"
            "## What we learned\n\nSmall utility features become easier to trust when their limits and data behavior are visible.\n\n"
            f"Explore the implementation and share feedback: {tools_url}"
        )
    if channel == "youtube":
        return (
            f"Title: {topic} | PureHub\n\n"
            "Description:\nPureHub brings 22 free, ad-free mini tools into one privacy-first, open-source project. "
            "Find the project through the link on this channel profile.\n\n#PureHub #OpenSource #AndroidApps\n\n"
            "Short script:\n0–1s: Show the result.\n1–4s: State the problem.\n4–14s: Demonstrate the tool.\n"
            "14–19s: Free • No ads • Open source.\n19–22s: Ask which tool should improve next."
        )
    return (
        f"Title: I am building PureHub in public: {topic}\n\n"
        "Maker disclosure: I build PureHub, a free, ad-free, open-source collection of utility tools. "
        "I am looking for specific product feedback, not drive-by promotion."
    )


def _generate_bundle(topic: str, day_number: int, channels: list[str]) -> dict[str, str]:
    fallback = {channel: _fallback_content(channel, topic, day_number) for channel in channels}
    try:
        client, model = _ai_client()
        prompt = {
            "task": "Create channel-specific community content for one day of PureHub's build-in-public campaign.",
            "day": day_number,
            "topic": topic,
            "channels": channels,
            "verified_facts": [
                "PureHub contains 22 mini-apps and is free, ad-free, open source, and community-built.",
                "The shared mobile UI has received a clarity, contrast, and one-handed-use pass.",
                "OCR supports English, Vietnamese, and Simplified Chinese packs loaded on demand.",
                "OCR and Password Vault are route-level lazy chunks on the web app.",
                "The Android instrumentation suite passed 4 of 4 tests on a physical RMX3941 phone.",
                "Password Vault received internal hardening but has not had an independent security audit.",
            ],
            "rules": [
                "Return a JSON object whose keys exactly match the requested channels.",
                "English first. Do not invent users, metrics, reviews, features, audits, or release dates.",
                "Write genuinely different content for each channel.",
                "Bluesky must be at most 300 characters; Mastodon at most 500 characters.",
                "Telegram should be compact and scannable; DEV should teach a useful engineering lesson in Markdown.",
                "YouTube must contain Title, Description without an external URL, and a 12–22 second shot script.",
                "Reddit is a manual-review draft with maker disclosure and no subreddit recommendation.",
                "Use at most a few relevant emoji and hashtags. Avoid hype, urgency, engagement bait, and repeated calls to action.",
            ],
        }
        response = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": "You are the careful community editor for an open-source project. Return valid JSON only."},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            temperature=0.45,
        )
        raw = (response.choices[0].message.content or "").strip().removeprefix("```json").removesuffix("```").strip()
        generated = json.loads(raw)
        for channel in channels:
            value = generated.get(channel)
            if isinstance(value, str) and value.strip():
                fallback[channel] = value.strip()
    except Exception:
        pass
    fallback["bluesky"] = fallback.get("bluesky", "")[:300]
    fallback["mastodon"] = fallback.get("mastodon", "")[:500]
    return fallback


def _publish_post(row: dict[str, Any]) -> dict[str, Any]:
    channel = str(row["channel"])
    if channel not in AUTO_CHANNELS:
        return row
    if row.get("status") == "published":
        return row
    content = str(row.get("content", "")).strip()
    try:
        if channel == "telegram":
            external_id, external_url = _publish_telegram(content)
        elif channel == "bluesky":
            external_id, external_url = _publish_bluesky(content)
        elif channel == "mastodon":
            external_id, external_url = _publish_mastodon(content)
        else:
            release = {
                "title": str(row.get("topic") or "PureHub community engineering note"),
                "github_url": f"{get_config_value('site_url', 'https://hub.blissbiovn.com').rstrip('/')}/en/tools",
            }
            external_id, external_url = _publish_devto(content, release)
        update_growth_post(
            str(row["id"]),
            {
                "status": "published",
                "external_id": external_id,
                "external_url": external_url,
                "error_message": "",
                "published_at": datetime.now(timezone.utc),
                "increment_attempts": True,
            },
        )
    except Exception as exc:
        update_growth_post(
            str(row["id"]),
            {"status": "failed", "error_message": str(exc)[:500], "increment_attempts": True},
        )
    return get_growth_post(str(row["id"])) or row


def run_growth_automation(
    *,
    force: bool = False,
    actor: str = "growth-cron",
    sync_support: bool = True,
) -> dict[str, Any]:
    enabled = get_config_value("growth_automation_enabled", "false").lower() == "true"
    if not enabled and not force:
        return {
            "enabled": False,
            "created": 0,
            "published": 0,
            "support": sync_support_channels(generate_drafts=True) if sync_support else {"skipped": True},
        }

    timezone_name = get_config_value("growth_timezone", "Asia/Bangkok") or "Asia/Bangkok"
    try:
        local_today = datetime.now(ZoneInfo(timezone_name)).date()
    except Exception:
        local_today = datetime.now(timezone.utc).date()
    start_raw = get_config_value("growth_campaign_start_date")
    try:
        start_date = date.fromisoformat(start_raw)
    except ValueError:
        start_date = local_today
    if local_today < start_date:
        return {
            "enabled": enabled,
            "starts_on": start_date.isoformat(),
            "created": 0,
            "published": 0,
            "support": sync_support_channels(generate_drafts=True) if sync_support else {"skipped": True},
        }
    day_number = max(1, (local_today - start_date).days + 1)
    cycle_day = ((day_number - 1) % 30) + 1
    topic = TOPICS[cycle_day - 1]
    channels = _channels_for_day(cycle_day)
    bundle = _generate_bundle(topic, cycle_day, channels)

    rows: list[dict[str, Any]] = []
    created = 0
    existing_keys = {
        (int(item.get("day_number", 0)), str(item.get("channel", "")))
        for item in list_growth_posts(500)
        if item.get("campaign_id") == CAMPAIGN_ID
    }
    for channel in channels:
        initial_status = "ready_upload" if channel == "youtube" else "ready_manual" if channel == "reddit" else "ready"
        row = upsert_growth_post(
            campaign_id=CAMPAIGN_ID,
            day_number=day_number,
            channel=channel,
            topic=topic,
            content=bundle[channel],
            status=initial_status,
            metadata={"cycle_day": cycle_day, "generated_by": get_config_value("ai_provider", "fallback")},
        )
        created += int((day_number, channel) not in existing_keys)
        rows.append(row)

    auto_publish = get_config_value("growth_auto_publish", "true").lower() == "true"
    if auto_publish:
        rows = [_publish_post(row) if row.get("channel") in AUTO_CHANNELS else row for row in rows]
    support = sync_support_channels(generate_drafts=True) if sync_support else {"skipped": True}
    published = sum(1 for row in rows if row.get("status") == "published")
    record_audit_log(
        actor=actor,
        action="run_growth_automation",
        target_type="growth_campaign",
        target_id=CAMPAIGN_ID,
        details={"day_number": day_number, "cycle_day": cycle_day, "channels": channels, "published": published},
    )
    return {
        "enabled": enabled,
        "day_number": day_number,
        "cycle_day": cycle_day,
        "topic": topic,
        "created": created,
        "published": published,
        "posts": rows,
        "support": support,
    }


def retry_growth_post(post_id: str) -> dict[str, Any]:
    row = get_growth_post(post_id)
    if not row:
        raise ValueError("Growth post not found.")
    if row.get("channel") not in AUTO_CHANNELS:
        raise ValueError("This channel requires a manual action.")
    return _publish_post(row)


def sync_growth_post_metrics() -> dict[str, Any]:
    result: dict[str, Any] = {}
    for row in list_growth_posts(100, status="published"):
        channel = str(row.get("channel", ""))
        external_id = str(row.get("external_id", ""))
        metrics: dict[str, int] = {}
        try:
            if channel == "telegram" and row.get("external_url"):
                response = requests.get(f"{row['external_url']}?embed=1&mode=tme", timeout=30)
                response.raise_for_status()
                match = re.search(r"tgme_widget_message_views[^>]*>([^<]+)", response.text)
                raw = match.group(1).strip().upper() if match else "0"
                multiplier = 1000 if raw.endswith("K") else 1
                metrics = {"views": int(float(raw.rstrip("K")) * multiplier)}
            elif channel == "devto" and external_id:
                response = requests.get(
                    f"https://dev.to/api/articles/{external_id}",
                    headers={"api-key": get_config_value("devto_api_key"), "user-agent": "PureHub-Growth-Metrics/1.0"},
                    timeout=30,
                )
                response.raise_for_status()
                payload = response.json()
                metrics = {
                    "views": int(payload.get("page_views_count") or 0),
                    "reactions": int(payload.get("public_reactions_count") or 0),
                    "comments": int(payload.get("comments_count") or 0),
                }
            elif channel == "bluesky" and external_id:
                response = requests.get(
                    "https://public.api.bsky.app/xrpc/app.bsky.feed.getPosts",
                    params={"uris": external_id},
                    timeout=30,
                )
                response.raise_for_status()
                post = (response.json().get("posts") or [{}])[0]
                metrics = {
                    "likes": int(post.get("likeCount") or 0),
                    "replies": int(post.get("replyCount") or 0),
                    "reposts": int(post.get("repostCount") or 0),
                }
            elif channel == "mastodon" and external_id:
                base = get_config_value("mastodon_base_url").rstrip("/")
                response = requests.get(f"{base}/api/v1/statuses/{external_id}", timeout=30)
                response.raise_for_status()
                post = response.json()
                metrics = {
                    "favourites": int(post.get("favourites_count") or 0),
                    "replies": int(post.get("replies_count") or 0),
                    "boosts": int(post.get("reblogs_count") or 0),
                }
            if metrics:
                metadata = dict(row.get("metadata") or {})
                metadata.update({"metrics": metrics, "metrics_fetched_at": datetime.now(timezone.utc).isoformat()})
                update_growth_post(str(row["id"]), {"metadata": metadata})
                result[str(row["id"])] = {"ok": True, "metrics": metrics}
        except Exception as exc:
            result[str(row["id"])] = {"ok": False, "error": str(exc)[:300]}
    return result
