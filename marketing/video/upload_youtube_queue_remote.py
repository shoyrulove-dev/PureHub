from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Any

import requests
from dotenv import dotenv_values


REPO_ROOT = Path(__file__).resolve().parents[2]
COMMAND_CENTER_ROOT = Path(os.environ.get("PUREHUB_COMMAND_CENTER_ROOT", REPO_ROOT.parent / "PureHub-Command-Center"))
for source_root in (REPO_ROOT, COMMAND_CENTER_ROOT):
    if str(source_root) not in sys.path:
        sys.path.insert(0, str(source_root))

from command_center.database import get_growth_post, list_growth_posts, upsert_growth_post
from marketing.video.upload_youtube_queue import queue_content, validate_manifest


class CsrfParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.token = ""

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        if tag == "input" and values.get("name") == "_csrf_token":
            self.token = str(values.get("value") or "")
        elif tag == "meta" and values.get("name") == "csrf-token":
            self.token = str(values.get("content") or "")


def csrf_from(response: requests.Response) -> str:
    parser = CsrfParser()
    parser.feed(response.text)
    if not parser.token:
        raise RuntimeError("Command Center did not return a CSRF token.")
    return parser.token


def admin_session(base_url: str, credentials_file: Path) -> tuple[requests.Session, str]:
    values = dotenv_values(credentials_file)
    username = str(values.get("ADMIN_USERNAME") or "").strip()
    password = str(values.get("ADMIN_PASSWORD") or "")
    if not username or not password:
        raise RuntimeError(f"ADMIN_USERNAME and ADMIN_PASSWORD are required in {credentials_file}")
    session = requests.Session()
    login_url = f"{base_url.rstrip('/')}/admin/login"
    login_page = session.get(login_url, timeout=30)
    login_page.raise_for_status()
    response = session.post(
        login_url,
        data={"_csrf_token": csrf_from(login_page), "username": username, "password": password},
        timeout=30,
    )
    response.raise_for_status()
    if response.url.rstrip("/").endswith("/admin/login"):
        raise RuntimeError("Command Center rejected the configured admin credentials.")
    return session, csrf_from(response)


def upload_item(
    session: requests.Session,
    csrf_token: str,
    base_url: str,
    row: dict[str, Any],
    path: Path,
) -> dict[str, Any]:
    payload = path.read_bytes()
    headers = {"x-csrf-token": csrf_token}
    api = f"{base_url.rstrip('/')}/admin/api/youtube"
    start = session.post(
        f"{api}/upload-session",
        headers=headers,
        json={"post_id": row["id"], "content_type": "video/mp4", "content_length": len(payload)},
        timeout=60,
    )
    if not start.ok:
        raise RuntimeError(f"Could not create YouTube upload session: {start.status_code} {start.text[:500]}")
    upload = requests.put(
        start.json()["upload_url"],
        headers={"Content-Type": "video/mp4", "Content-Length": str(len(payload))},
        data=payload,
        timeout=900,
    )
    if not upload.ok:
        raise RuntimeError(f"YouTube upload failed: {upload.status_code} {upload.text[:500]}")
    complete = session.post(
        f"{api}/complete",
        headers=headers,
        json={"post_id": row["id"], "youtube": upload.json()},
        timeout=60,
    )
    if not complete.ok:
        raise RuntimeError(f"Could not finalize YouTube upload: {complete.status_code} {complete.text[:500]}")
    return dict(complete.json().get("item") or {})


def main() -> None:
    parser = argparse.ArgumentParser(description="Upload a local queue through the production Command Center OAuth connection.")
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--campaign-id", required=True)
    parser.add_argument("--base-url", default="https://hub.blissbiovn.com")
    parser.add_argument("--credentials-file", type=Path, default=COMMAND_CENTER_ROOT / "command_center" / ".env")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    items = json.loads(args.manifest.read_text(encoding="utf-8"))
    if not isinstance(items, list) or not items:
        raise ValueError("YouTube queue manifest must contain at least one item.")
    validate_manifest(items)
    requested_slots = {
        datetime.fromisoformat(str(item["publish_at"])).astimezone(timezone.utc)
        for item in items
    }
    collisions = []
    for row in list_growth_posts(500, channel="youtube"):
        scheduled_at = row.get("scheduled_at")
        if not isinstance(scheduled_at, datetime):
            continue
        if scheduled_at.astimezone(timezone.utc) not in requested_slots:
            continue
        if row.get("campaign_id") == args.campaign_id:
            continue
        collisions.append(row)
    if collisions:
        details = ", ".join(
            f"{row.get('scheduled_at')} ({row.get('status')}: {row.get('topic')})"
            for row in collisions
        )
        raise RuntimeError(f"YouTube schedule collision: {details}")
    if args.dry_run:
        print(f"Validated {len(items)} collision-free queue item(s); no upload performed.")
        return

    session, csrf_token = admin_session(args.base_url, args.credentials_file)
    for index, item in enumerate(items, start=1):
        scheduled_at = datetime.fromisoformat(str(item["publish_at"])).astimezone(timezone.utc)
        row = upsert_growth_post(
            campaign_id=args.campaign_id,
            day_number=index,
            channel="youtube",
            topic=str(item["hook"]),
            content=queue_content(item),
            status="ready_upload",
            scheduled_at=scheduled_at,
            metadata={"device_demo": True, "source_file": Path(str(item["file"])).name},
        )
        current = get_growth_post(str(row["id"])) or row
        if current.get("status") in {"scheduled", "published"} and current.get("external_id"):
            print(f"{index:02d}/{len(items)} already uploaded: {current.get('external_url')}")
            continue
        uploaded = upload_item(session, csrf_token, args.base_url, current, Path(str(item["file"])))
        print(f"{index:02d}/{len(items)} scheduled {scheduled_at.isoformat()}: {uploaded.get('external_url')}")


if __name__ == "__main__":
    main()
