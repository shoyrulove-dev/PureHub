from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Any
from zoneinfo import ZoneInfo


AUGUST_2026_GOALS = {
    "audience": 100,
    "views": 1500,
    "engagement": 30,
    "feedback": 20,
    "videos": 8,
}


def _metrics(platforms: dict[str, dict[str, Any]], platform: str) -> dict[str, int]:
    values = (platforms.get(platform) or {}).get("metrics") or {}
    return {key: max(0, int(value or 0)) for key, value in values.items()}


def _parse_datetime(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    except ValueError:
        return None


def _progress(value: int, target: int) -> int:
    return min(100, round((max(0, value) / target) * 100)) if target else 0


def build_august_growth_goal(
    *,
    config: dict[str, Any],
    community_metrics: list[dict[str, Any]],
    growth_posts: list[dict[str, Any]],
    support_messages: list[dict[str, Any]],
    support_metrics: dict[str, Any],
    reddit_connected: bool,
    now: datetime | None = None,
) -> dict[str, Any]:
    timezone_name = str(config.get("growth_timezone") or "Asia/Bangkok")
    try:
        local_zone = ZoneInfo(timezone_name)
    except Exception:
        local_zone = timezone.utc
    local_now = (now or datetime.now(timezone.utc)).astimezone(local_zone)
    period_start, period_end = date(2026, 8, 1), date(2026, 8, 31)
    elapsed_days = min(31, max(0, (local_now.date() - period_start).days + 1))
    timeline_percent = _progress(elapsed_days, 31)

    platforms = {str(item.get("platform")): item for item in community_metrics}
    telegram = _metrics(platforms, "telegram")
    devto = _metrics(platforms, "devto")
    bluesky = _metrics(platforms, "bluesky")
    mastodon = _metrics(platforms, "mastodon")

    audience = telegram.get("members", 0) + bluesky.get("followers", 0) + mastodon.get("followers", 0)
    views = devto.get("views", 0)
    engagement = (
        devto.get("reactions", 0)
        + devto.get("comments", 0)
        + bluesky.get("likes", 0)
        + bluesky.get("replies", 0)
        + bluesky.get("reposts", 0)
        + bluesky.get("quotes", 0)
        + mastodon.get("favourites", 0)
        + mastodon.get("boosts", 0)
        + mastodon.get("replies", 0)
        + int(support_metrics.get("replied", 0) or 0)
    )

    actionable_categories = {"question", "bug", "feature_request", "privacy", "installation"}
    feedback = 0
    for item in support_messages:
        received_at = _parse_datetime(item.get("received_at"))
        if (
            received_at
            and period_start <= received_at.astimezone(local_zone).date() <= period_end
            and item.get("category") in actionable_categories
        ):
            feedback += 1

    videos = 0
    published_content = 0
    ready_video_uploads = 0
    next_youtube: tuple[datetime, str] | None = None
    for item in growth_posts:
        scheduled_at = _parse_datetime(item.get("scheduled_at"))
        metadata = item.get("metadata") or {}
        post_metrics = metadata.get("metrics") or {}
        if item.get("channel") in {"youtube", "telegram"}:
            views += max(0, int(post_metrics.get("views") or 0))
        if item.get("status") in {"published", "scheduled"}:
            published_content += 1
        if item.get("channel") == "youtube" and item.get("status") in {"published", "scheduled"}:
            if scheduled_at and period_start <= scheduled_at.astimezone(local_zone).date() <= period_end:
                videos += 1
            if scheduled_at and scheduled_at > local_now.astimezone(timezone.utc):
                candidate = (scheduled_at, str(item.get("topic") or "PureHub Short"))
                if next_youtube is None or candidate[0] < next_youtube[0]:
                    next_youtube = candidate
        if item.get("channel") == "youtube" and item.get("status") == "ready_upload" and metadata.get("purpose") != "oauth-upload-test":
            ready_video_uploads += 1

    values = {
        "audience": audience,
        "views": views,
        "engagement": engagement,
        "feedback": feedback,
        "videos": videos,
    }
    labels = {
        "audience": "Community",
        "views": "Qualified views",
        "engagement": "Interactions",
        "feedback": "Useful feedback",
        "videos": "Short demos",
    }
    notes = {
        "audience": "Telegram members + Bluesky and Mastodon followers",
        "views": "DEV, Telegram and YouTube views returned by connected APIs",
        "engagement": "Likes, reactions, comments, reposts and handled conversations",
        "feedback": "Questions, bugs, installation, privacy and feature requests in August",
        "videos": "YouTube Shorts published or scheduled during August",
    }
    milestones = [
        {
            "key": key,
            "label": labels[key],
            "value": values[key],
            "target": target,
            "percent": _progress(values[key], target),
            "note": notes[key],
        }
        for key, target in AUGUST_2026_GOALS.items()
    ]
    weights = {"audience": 0.30, "views": 0.25, "engagement": 0.20, "feedback": 0.15, "videos": 0.10}
    outcome_percent = round(sum(item["percent"] * weights[item["key"]] for item in milestones))

    campaign_start_raw = str(config.get("growth_campaign_start_date") or "2026-08-02")
    try:
        campaign_start = date.fromisoformat(campaign_start_raw)
    except ValueError:
        campaign_start = date(2026, 8, 2)
    campaign_day = min(30, max(0, (local_now.date() - campaign_start).days + 1))

    actions: list[dict[str, str]] = []
    open_support = int(support_metrics.get("open", 0) or 0)
    if open_support:
        actions.append({"tone": "urgent", "label": f"Review {open_support} community repl{'y' if open_support == 1 else 'ies'}", "href": "#support"})
    else:
        actions.append({"tone": "done", "label": "Support inbox is clear", "href": "#support"})
    if ready_video_uploads:
        actions.append({"tone": "attention", "label": f"Upload {ready_video_uploads} prepared YouTube demo(s)", "href": "#growth-autopilot"})
    elif next_youtube:
        publish_time = next_youtube[0].astimezone(local_zone).strftime("%d %b at %H:%M")
        actions.append({"tone": "done", "label": f"Next Short scheduled {publish_time}", "href": "#growth-autopilot"})
    if not reddit_connected:
        actions.append({"tone": "muted", "label": "Reddit API approval pending - drafts stay manual", "href": "#reddit-review"})
    actions.append({"tone": "info", "label": f"{published_content} campaign item(s) published or scheduled", "href": "#growth-autopilot"})

    weeks = [
        {"label": "Week 1", "range": "1-7 Aug", "focus": "Awareness & first testers", "end": date(2026, 8, 7)},
        {"label": "Week 2", "range": "8-14 Aug", "focus": "Useful demos & proof", "end": date(2026, 8, 14)},
        {"label": "Week 3", "range": "15-21 Aug", "focus": "Feedback & roadmap vote", "end": date(2026, 8, 21)},
        {"label": "Week 4", "range": "22-31 Aug", "focus": "Contributors & recap", "end": date(2026, 8, 31)},
    ]
    for index, week in enumerate(weeks):
        week_start = period_start if index == 0 else weeks[index - 1]["end"].replace(day=weeks[index - 1]["end"].day + 1)
        if local_now.date() > week["end"]:
            week["status"] = "done"
        elif week_start <= local_now.date() <= week["end"]:
            week["status"] = "active"
        else:
            week["status"] = "upcoming"

    return {
        "label": "August 2026",
        "headline": "Build trust, reach 100 real community members, and turn feedback into one shipped improvement.",
        "timeline_percent": timeline_percent,
        "elapsed_days": elapsed_days,
        "days_total": 31,
        "outcome_percent": outcome_percent,
        "campaign_day": campaign_day,
        "campaign_total": 30,
        "values": values,
        "milestones": milestones,
        "actions": actions,
        "weeks": weeks,
        "automation": [
            {"time": "07:00", "label": "Discover questions"},
            {"time": "19:00", "label": "Generate & publish"},
            {"time": "21:00", "label": "Refresh metrics"},
        ],
    }
