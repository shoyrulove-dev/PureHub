from __future__ import annotations

import json
import re
from typing import Any

import requests
from openai import OpenAI

try:
    from .database import (
        get_config_value,
        get_release,
        list_release_publications,
        update_release,
        update_release_publication,
        upsert_release_publication,
    )
except ImportError:
    from database import (
        get_config_value,
        get_release,
        list_release_publications,
        update_release,
        update_release_publication,
        upsert_release_publication,
    )

CHANNELS = ("telegram", "devto", "bluesky", "mastodon", "reddit", "hackernews", "producthunt", "linkedin", "facebook")
AUTO_CHANNELS = {"telegram", "devto", "bluesky", "mastodon"}


def _markdown_title(content: str, fallback: str) -> str:
    for line in content.splitlines():
        if line.startswith("# "):
            return line[2:].strip() or fallback
    return fallback


def _bluesky_facets(content: str) -> list[dict[str, Any]]:
    facets: list[dict[str, Any]] = []
    matches = [
        (match, {"$type": "app.bsky.richtext.facet#link", "uri": match.group(0)})
        for match in re.finditer(r"https?://[^\s]+", content)
    ]
    matches.extend(
        (match, {"$type": "app.bsky.richtext.facet#tag", "tag": match.group(1)})
        for match in re.finditer(r"(?<!\w)#([\w]+)", content)
    )
    for match, feature in sorted(matches, key=lambda item: item[0].start()):
        facets.append(
            {
                "index": {
                    "byteStart": len(content[: match.start()].encode("utf-8")),
                    "byteEnd": len(content[: match.end()].encode("utf-8")),
                },
                "features": [feature],
            }
        )
    return facets


def _ai_client() -> tuple[OpenAI, str]:
    provider = get_config_value("ai_provider", "deepseek").lower()
    if provider == "groq":
        api_key = get_config_value("groq_api_key")
        if api_key:
            return OpenAI(api_key=api_key, base_url="https://api.groq.com/openai/v1"), get_config_value(
                "groq_model", "llama-3.3-70b-versatile"
            )
    api_key = get_config_value("deepseek_api_key")
    if api_key:
        return OpenAI(api_key=api_key, base_url="https://api.deepseek.com"), get_config_value(
            "deepseek_model", "deepseek-chat"
        )
    raise RuntimeError("Configure a valid DeepSeek or Groq API key.")


def _fallback_bundle(release: dict[str, Any]) -> dict[str, str]:
    version = release["version"]
    summary = release.get("summary") or "A cleaner, faster PureHub release with useful privacy-first tools."
    url = release.get("github_url") or f"https://github.com/{get_config_value('github_repo')}/releases/tag/v{version}"
    short = f"PureHub {version} is available. {summary}\n\nDownload: {url}"
    return {
        "telegram_en": short,
        "telegram_vi": f"PureHub {version} da san sang. {summary}\n\nTai xuong: {url}",
        "telegram_zh": f"PureHub {version} is now available.\n\nDownload: {url}",
        "devto_en": f"# PureHub {version}\n\n{summary}\n\n{release.get('changelog', '')}\n\n[Download the signed APK]({url}).",
        "bluesky_en": short[:300],
        "mastodon_en": short[:500],
        "reddit_en": f"PureHub {version}: free, no-ads, open-source Android utilities\n\n{summary}\n\n{url}",
        "hackernews_en": f"Show HN: PureHub {version} - 22 free, no-ads utility tools\n{url}",
        "producthunt_en": f"PureHub {version} - 22 useful tools, free forever, no ads.",
        "linkedin_en": short,
        "facebook_en": short,
    }


def generate_release_bundle(release_id: str) -> list[dict[str, Any]]:
    release = get_release(release_id)
    if not release:
        raise ValueError("Release not found.")
    bundle = _fallback_bundle(release)
    try:
        client, model = _ai_client()
        prompt = {
            "task": "Create an English-first launch content bundle for PureHub. Vietnamese and Chinese are secondary.",
            "product": "PureHub - 22 free, no-ads, privacy-first, open-source utility tools",
            "release": release,
            "requirements": {
                "json_only": True,
                "keys": list(bundle.keys()),
                "rules": [
                    "Do not invent metrics, testimonials, security audits, or features.",
                    "Telegram, Bluesky, and Mastodon must link to the GitHub release.",
                    "Write genuinely different copy for each channel instead of shortening one shared post.",
                    "Use a few relevant Unicode icons to improve scanning, but keep the tone professional and avoid emoji spam.",
                    "For DEV, write a useful standalone Markdown launch article with a clear title, short sections, bullet points, and a call for community feedback.",
                    "For Telegram, make the announcement warm, compact, and easy to scan on a phone.",
                    "For Bluesky, use a conversational open-source community tone.",
                    "For Mastodon, emphasize open source, privacy, and community participation without marketing hype.",
                    "Reddit, Hacker News, Product Hunt, LinkedIn, and Facebook are review drafts, not spam.",
                    "Keep Bluesky under 300 characters and Mastodon under 500 characters.",
                ],
            },
        }
        response = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": "You are PureHub's concise open-source release editor. Return valid JSON only."},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            temperature=0.3,
        )
        raw = (response.choices[0].message.content or "").strip().removeprefix("```json").removesuffix("```").strip()
        generated = json.loads(raw)
        for key in bundle:
            if isinstance(generated.get(key), str) and generated[key].strip():
                bundle[key] = generated[key].strip()
    except Exception:
        pass

    existing = {
        (item["channel"], item["language"]): item
        for item in list_release_publications(release_id)
    }
    rows = []
    for channel in CHANNELS:
        languages = ("en", "vi", "zh") if channel == "telegram" else ("en",)
        for language in languages:
            previous = existing.get((channel, language))
            if previous and previous.get("status") == "published":
                rows.append(previous)
                continue
            content = bundle.get(f"{channel}_{language}", bundle.get(f"{channel}_en", ""))
            status = "draft" if channel in AUTO_CHANNELS else "ready_manual"
            rows.append(
                upsert_release_publication(
                    release_id=release_id,
                    channel=channel,
                    language=language,
                    content=content,
                    status=status,
                )
            )
    update_release(release_id, {"status": "content_ready"})
    return rows


def generate_reply_draft(message: str, context: str = "") -> str:
    client, model = _ai_client()
    response = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "system",
                "content": (
                    "Draft a short, friendly PureHub community reply in English. Never claim a fix is shipped unless the "
                    "context proves it. Ask for device/version details when needed. Do not request secrets."
                ),
            },
            {"role": "user", "content": f"Context:\n{context}\n\nCommunity message:\n{message}"},
        ],
        temperature=0.2,
    )
    return (response.choices[0].message.content or "").strip()


def _publish_telegram(content: str) -> tuple[str, str]:
    token = get_config_value("telegram_bot_token")
    chat_id = get_config_value("telegram_notify_chat_id")
    response = requests.post(
        f"https://api.telegram.org/bot{token}/sendMessage",
        json={"chat_id": chat_id, "text": content, "disable_web_page_preview": False},
        timeout=30,
    )
    response.raise_for_status()
    result = response.json()["result"]
    return str(result["message_id"]), f"https://t.me/purehubaaa/{result['message_id']}"


def _publish_devto(content: str, release: dict[str, Any]) -> tuple[str, str]:
    response = requests.post(
        "https://dev.to/api/articles",
        headers={
            "api-key": get_config_value("devto_api_key"),
            "accept": "application/vnd.forem.api-v1+json",
            "content-type": "application/json",
            "user-agent": "PureHub-Release-Hub/1.0",
        },
        json={
            "article": {
                "title": _markdown_title(content, release["title"]),
                "body_markdown": content,
                "published": get_config_value("devto_publish_as_draft", "true").lower() != "true",
                "tags": ["android", "opensource", "productivity", "privacy"],
                "canonical_url": release.get("github_url") or get_config_value("site_url"),
            }
        },
        timeout=45,
    )
    response.raise_for_status()
    payload = response.json()
    return str(payload.get("id", "")), payload.get("url", "")


def _publish_bluesky(content: str) -> tuple[str, str]:
    handle = get_config_value("bluesky_handle")
    password = get_config_value("bluesky_app_password")
    session = requests.post(
        "https://bsky.social/xrpc/com.atproto.server.createSession",
        json={"identifier": handle, "password": password},
        timeout=30,
    )
    session.raise_for_status()
    auth = session.json()
    text = content[:300]
    post = requests.post(
        "https://bsky.social/xrpc/com.atproto.repo.createRecord",
        headers={"Authorization": f"Bearer {auth['accessJwt']}"},
        json={
            "repo": auth["did"],
            "collection": "app.bsky.feed.post",
            "record": {
                "$type": "app.bsky.feed.post",
                "text": text,
                "facets": _bluesky_facets(text),
                "langs": ["en"],
                "createdAt": __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat().replace("+00:00", "Z"),
            },
        },
        timeout=30,
    )
    post.raise_for_status()
    payload = post.json()
    record_key = payload["uri"].rsplit("/", 1)[-1]
    return payload["uri"], f"https://bsky.app/profile/{handle}/post/{record_key}"


def _publish_mastodon(content: str) -> tuple[str, str]:
    base_url = get_config_value("mastodon_base_url").rstrip("/")
    response = requests.post(
        f"{base_url}/api/v1/statuses",
        headers={"Authorization": f"Bearer {get_config_value('mastodon_access_token')}"},
        data={"status": content[:500], "visibility": "public"},
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    return str(payload["id"]), payload.get("url", "")


def publish_release(release_id: str, channels: list[str] | None = None) -> list[dict[str, Any]]:
    release = get_release(release_id)
    if not release:
        raise ValueError("Release not found.")
    enabled = channels or [item.strip() for item in get_config_value("release_auto_channels", "telegram").split(",") if item.strip()]
    publications = {(item["channel"], item["language"]): item for item in list_release_publications(release_id)}
    results = []
    publishers = {
        "telegram": _publish_telegram,
        "devto": lambda content: _publish_devto(content, release),
        "bluesky": _publish_bluesky,
        "mastodon": _publish_mastodon,
    }
    for channel in enabled:
        row = publications.get((channel, "en"))
        if (
            not row
            or row.get("status") not in {"approved", "waiting_credentials"}
            or channel not in publishers
        ):
            continue
        required = {
            "telegram": "telegram_bot_token",
            "devto": "devto_api_key",
            "bluesky": "bluesky_app_password",
            "mastodon": "mastodon_access_token",
        }[channel]
        if not get_config_value(required):
            update_release_publication(release_id, channel, "en", {"status": "waiting_credentials"})
            continue
        try:
            external_id, external_url = publishers[channel](row["content"])
            update_release_publication(
                release_id,
                channel,
                "en",
                {
                    "status": "published",
                    "external_id": external_id,
                    "external_url": external_url,
                    "error_message": "",
                    "increment_attempts": True,
                },
            )
        except Exception as exc:
            update_release_publication(
                release_id,
                channel,
                "en",
                {"status": "failed", "error_message": str(exc)[:500], "increment_attempts": True},
            )
        results.append({"channel": channel})
    update_release(release_id, {"status": "published"})
    return results
