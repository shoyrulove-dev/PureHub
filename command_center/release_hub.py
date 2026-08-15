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


def validate_social_content(content: str) -> str:
    """Reject text that was damaged while crossing a non-UTF-8 boundary."""
    text = (content or "").strip()
    mojibake_markers = ("\ufffd", "\u00c2", "\u00c3", "\u00e2\u20ac", "\u00f0\u0178")
    has_standalone_replacement = bool(re.search(r"(?m)(^|\s)\?(?:\s|$)", text))
    if not text:
        raise ValueError("Social content cannot be empty.")
    if any(marker in text for marker in mojibake_markers) or "??" in text or has_standalone_replacement:
        raise ValueError("Social content appears to contain mojibake or lost Unicode characters.")
    return text


def format_reddit_draft(title: str, body: str, suggested_communities: str = "") -> str:
    parts = [f"Title: {title.strip()}"]
    if suggested_communities.strip():
        parts.append(f"Suggested communities: {suggested_communities.strip()}")
    parts.append(body.strip())
    return "\n\n".join(part for part in parts if part)


def parse_reddit_draft(content: str) -> dict[str, str]:
    title = ""
    communities = ""
    body_lines: list[str] = []
    for line in (content or "").splitlines():
        stripped = line.strip()
        if not title and stripped.lower().startswith("title:"):
            title = stripped.split(":", 1)[1].strip()
        elif not communities and stripped.lower().startswith("suggested communities:"):
            communities = stripped.split(":", 1)[1].strip()
        else:
            body_lines.append(line)
    body = "\n".join(body_lines).strip()
    if not title:
        lines = [line.strip() for line in body.splitlines() if line.strip()]
        title = lines[0] if lines else "PureHub: free, ad-free, open-source utility tools"
        body = "\n".join(lines[1:]).strip() if len(lines) > 1 else body
    return {"title": title, "communities": communities, "body": body}


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
        "hackernews_en": f"Show HN: PureHub {version} - 25 free, no-ads utility tools\n{url}",
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
            "product": "PureHub - 25 free, no-ads, privacy-first, open-source utility tools",
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


def generate_reddit_draft(release_id: str) -> dict[str, Any]:
    release = get_release(release_id)
    if not release:
        raise ValueError("Release not found.")
    fallback_title = f"I built PureHub {release['version']}: free, ad-free, open-source everyday tools"
    fallback_body = (
        f"Hi everyone — I’m building PureHub, a community-driven collection of 25 privacy-first utility tools.\n\n"
        f"{release.get('summary') or 'The project is free to use, contains no ads, and is open source.'}\n\n"
        "I’d value honest feedback on the mobile experience, which tools feel genuinely useful, and what should be simplified.\n\n"
        f"Source and release: {release.get('github_url') or f'https://github.com/{get_config_value("github_repo")}'}"
    )
    result = {
        "title": fallback_title,
        "body": fallback_body,
        "suggested_communities": "r/droidappshowcase (moderator-designated Android showcase)",
    }
    try:
        client, model = _ai_client()
        response = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You write transparent, non-spammy Reddit drafts for an open-source project. Return JSON only with "
                        "title and body. Do not invent metrics, testimonials, audits, or shipped features. "
                        "Write as the maker, disclose the project connection, ask for specific feedback, avoid marketing hype and "
                        "emoji. Do not recommend subreddits or destinations."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "product": "PureHub — 25 free, no-ads, privacy-first, open-source utility tools",
                            "release": release,
                            "requirements": {
                                "title_max_characters": 150,
                                "body_max_words": 260,
                                "include_links_only_when_present": True,
                            },
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
            temperature=0.45,
        )
        raw = (response.choices[0].message.content or "").strip().removeprefix("```json").removesuffix("```").strip()
        generated = json.loads(raw)
        for key in ("title", "body"):
            if isinstance(generated.get(key), str) and generated[key].strip():
                result[key] = generated[key].strip()
    except Exception:
        pass
    return upsert_release_publication(
        release_id=release_id,
        channel="reddit",
        language="en",
        content=format_reddit_draft(result["title"], result["body"], result["suggested_communities"]),
        status="ready_manual",
    )


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
    content = validate_social_content(content)
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
    content = validate_social_content(content)
    article = {
        "title": _markdown_title(content, release["title"]),
        "body_markdown": content,
        "published": get_config_value("devto_publish_as_draft", "true").lower() != "true",
        "tags": ["android", "opensource", "productivity", "privacy"],
    }
    # Campaign articles are original DEV posts, so they must not all reuse the
    # same canonical URL. DEV rejects duplicate canonical URLs with HTTP 422.
    canonical_url = str(release.get("canonical_url") or "").strip()
    if canonical_url:
        article["canonical_url"] = canonical_url
    response = requests.post(
        "https://dev.to/api/articles",
        headers={
            "api-key": get_config_value("devto_api_key"),
            "accept": "application/vnd.forem.api-v1+json",
            "content-type": "application/json",
            "user-agent": "PureHub-Release-Hub/1.0",
        },
        json={"article": article},
        timeout=45,
    )
    response.raise_for_status()
    payload = response.json()
    return str(payload.get("id", "")), payload.get("url", "")


def _publish_bluesky(content: str) -> tuple[str, str]:
    content = validate_social_content(content)
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
    content = validate_social_content(content)
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
