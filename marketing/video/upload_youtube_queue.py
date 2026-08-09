from __future__ import annotations

import json
import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from command_center.database import get_growth_post, init_database, upsert_growth_post
from command_center.youtube_connector import API_BASE, _access_token, complete_upload, create_upload_session


ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / "output" / "miniapp-shorts" / "youtube-queue.json"
CAMPAIGN_ID = "youtube-miniapp-shorts-2026-08-v1"


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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--start", type=int, default=1)
    parser.add_argument("--limit", type=int, default=10)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--campaign-id", default=CAMPAIGN_ID)
    args = parser.parse_args()
    init_database()
    items = json.loads(args.manifest.read_text(encoding="utf-8"))
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
        if verified["privacy"] != "private" or not verified["publish_at"]:
            raise ValueError(f"YouTube did not preserve the schedule for item {index}: {verified}")
        print(f"{index:02d}/{len(items)} scheduled {verified['publish_at']}: {uploaded.get('external_url')}")


if __name__ == "__main__":
    main()
