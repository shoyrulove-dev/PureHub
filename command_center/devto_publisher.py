from __future__ import annotations

from pathlib import Path

import requests

try:
    from .database import OUTPUT_DIR, get_config_value, list_article_jobs, update_article_job
except ImportError:
    from database import OUTPUT_DIR, get_config_value, list_article_jobs, update_article_job

DEVTO_ENDPOINT = "https://dev.to/api/articles"


def extract_title(markdown: str, fallback: str) -> str:
    for line in markdown.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            return stripped.lstrip("#").strip()
    return fallback


def publish_articles() -> list[dict[str, str]]:
    api_key = get_config_value("devto_api_key")
    if not api_key:
        raise RuntimeError("Missing Dev.to API key in config table.")

    publish_as_draft = get_config_value("devto_publish_as_draft", "true").lower() == "true"
    site_url = get_config_value("site_url", "https://hub.blissbiovn.com")
    jobs = [job for job in list_article_jobs(50) if job["status"] == "generated"]

    if not jobs:
        return []

    results: list[dict[str, str]] = []
    headers = {
        "api-key": api_key,
        "content-type": "application/json",
    }

    for job in jobs:
        file_path = OUTPUT_DIR / str(job["source_filename"])
        if not file_path.exists():
            update_article_job(str(job["id"]), status="failed", error_message="Source markdown file not found.")
            continue

        body_markdown = file_path.read_text(encoding="utf-8")
        title = extract_title(body_markdown, str(job["title"]))

        payload = {
            "article": {
                "title": title,
                "published": not publish_as_draft,
                "body_markdown": body_markdown,
                "tags": ["pwa", "productivity", "privacy", "webdev"],
                "canonical_url": site_url,
            }
        }

        response = requests.post(DEVTO_ENDPOINT, json=payload, headers=headers, timeout=45)

        if response.ok:
            remote_url = response.json().get("url", "")
            update_article_job(str(job["id"]), status="published", remote_url=remote_url, error_message=None)
            results.append(
                {
                    "filename": str(job["source_filename"]),
                    "title": title,
                    "url": remote_url,
                }
            )
        else:
            update_article_job(
                str(job["id"]),
                status="failed",
                remote_url=None,
                error_message=f"{response.status_code}: {response.text[:300]}",
            )

    return results
