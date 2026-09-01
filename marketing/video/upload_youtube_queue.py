from __future__ import annotations

import json
import argparse
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests

REPO_ROOT = Path(__file__).resolve().parents[2]
COMMAND_CENTER_ROOT = Path(os.environ.get("PUREHUB_COMMAND_CENTER_ROOT", REPO_ROOT.parent / "PureHub-Command-Center"))
for source_root in (REPO_ROOT, COMMAND_CENTER_ROOT):
    if str(source_root) not in sys.path:
        sys.path.insert(0, str(source_root))

from command_center.database import get_growth_post, init_database, upsert_growth_post
from command_center.youtube_connector import API_BASE, _access_token, complete_upload, create_upload_session


ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / "output" / "miniapp-shorts" / "youtube-queue.json"
CAMPAIGN_ID = "youtube-all-flagships-2026-08-v2"


def queue_content(item: dict[str, str]) -> str:
    return f"Title: {item['title']}\n\nDescription:\n{item['description']}"


def upload_item(row: dict[str, Any], path: Path) -> dict[str, Any]:
    payload = path.read_bytes()
    last_error: Exception | None = None
    for _attempt in range(3):
        try:
            session = create_upload_session(
                str(row["id"]),
                content_type="video/mp4",
                content_length=len(payload),
            )
            response = requests.put(
                session["upload_url"],
                headers={"Content-Type": "video/mp4", "Content-Length": str(len(payload))},
                data=payload,
                timeout=900,
            )
            response.raise_for_status()
            return complete_upload(str(row["id"]), response.json())
        except requests.RequestException as exc:
            last_error = exc
    raise RuntimeError(f"YouTube upload failed after three resumable sessions: {last_error}")


def verify_schedule(video_id: str) -> dict[str, str]:
    last_error: Exception | None = None
    for _attempt in range(3):
        try:
            response = requests.get(
                f"{API_BASE}/videos",
                headers={"Authorization": f"Bearer {_access_token()}"},
                params={"part": "status", "id": video_id},
                timeout=30,
            )
            response.raise_for_status()
            items = response.json().get("items") or []
            if not items:
                raise ValueError("YouTube did not return the uploaded video for schedule verification.")
            status = items[0].get("status") or {}
            return {"privacy": str(status.get("privacyStatus", "")), "publish_at": str(status.get("publishAt", ""))}
        except requests.RequestException as exc:
            last_error = exc
    raise RuntimeError(f"Could not verify the YouTube schedule after three attempts: {last_error}")


def validate_manifest(items: list[dict[str, Any]]) -> None:
    """Reject an ambiguous queue before creating any external upload."""
    seen: dict[datetime, int] = {}
    for index, item in enumerate(items, start=1):
        try:
            scheduled_at = datetime.fromisoformat(str(item["publish_at"])).astimezone(timezone.utc)
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError(f"Queue item {index} has an invalid publish_at value.") from exc
        previous = seen.get(scheduled_at)
        if previous is not None:
            raise ValueError(f"Duplicate YouTube schedule at {scheduled_at.isoformat()} for items {previous} and {index}.")
        seen[scheduled_at] = index
        source = Path(str(item.get("file", "")))
        if not source.is_file():
            raise FileNotFoundError(f"Queue item {index} source file does not exist: {source}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--start", type=int, default=1)
    parser.add_argument("--limit", type=int, default=22)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--campaign-id", default=CAMPAIGN_ID)
    parser.add_argument("--dry-run", action="store_true", help="Validate the queue without touching the database or YouTube")
    args = parser.parse_args()
    items = json.loads(args.manifest.read_text(encoding="utf-8"))
    if not isinstance(items, list) or not items:
        raise ValueError("YouTube queue manifest must contain at least one item.")
    validate_manifest(items)
    if args.dry_run:
        print(f"Validated {len(items)} queue item(s); no upload performed.")
        return
    init_database()
    for index, item in enumerate(items, start=1):
        if index < args.start or index >= args.start + args.limit:
            continue
        scheduled_at = datetime.fromisoformat(item["publish_at"]).astimezone(timezone.utc)
        row = upsert_growth_post(
            campaign_id=args.campaign_id,
            day_number=index,
            channel="youtube",
            topic=item["hook"],
            content=queue_content(item),
            status="ready_upload",
            scheduled_at=scheduled_at,
            metadata={
                "miniapp_short": True,
                "benefit": item["benefit"],
                "source_file": Path(item["file"]).name,
            },
        )
        current = get_growth_post(str(row["id"])) or row
        if current.get("status") in {"scheduled", "published"} and current.get("external_id"):
            print(f"{index:02d}/{len(items)} already uploaded: {current.get('external_url')}")
            continue
        uploaded = upload_item(current, Path(item["file"]))
        verified = verify_schedule(str(uploaded.get("external_id", "")))
        expected_publish_at = scheduled_at.isoformat().replace("+00:00", "Z")
        if verified["privacy"] != "private" or verified["publish_at"] != expected_publish_at:
            raise ValueError(f"YouTube did not preserve the schedule for item {index}: {verified}")
        print(f"{index:02d}/{len(items)} scheduled {verified['publish_at']}: {uploaded.get('external_url')}")


if __name__ == "__main__":
    main()
