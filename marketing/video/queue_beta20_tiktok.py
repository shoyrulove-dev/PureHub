from __future__ import annotations

import json
import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
COMMAND_CENTER_ROOT = Path(os.environ.get("PUREHUB_COMMAND_CENTER_ROOT", REPO_ROOT.parent / "PureHub-Command-Center"))
for source_root in (REPO_ROOT, COMMAND_CENTER_ROOT):
    if str(source_root) not in sys.path:
        sys.path.insert(0, str(source_root))

from command_center.database import init_database, upsert_growth_post


MANIFEST = Path(__file__).resolve().parent / "output" / "beta20-shorts" / "youtube-queue.json"
CAMPAIGN_ID = "tiktok-beta20-private-utilities-2026-08"
PUBLIC_BASE = "https://hub.blissbiovn.com/media/beta20"
YOUTUBE_URLS = (
    "https://youtu.be/du--kFuEWtg",
    "https://youtu.be/wvI3FohrTa0",
    "https://youtu.be/IuffOvkiE-U",
    "https://youtu.be/3dK7qmkgLRA",
)


def tiktok_caption(item: dict[str, str]) -> str:
    summary = item["description"].split("\n\n", 1)[0]
    return f"{summary}\n\n#PureHub #NoAds #OpenSource #PrivacyTools #AndroidApps"


def main() -> None:
    init_database()
    items = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if len(items) != len(YOUTUBE_URLS):
        raise ValueError(f"TikTok queue expects {len(YOUTUBE_URLS)} items, found {len(items)}.")
    for index, (item, youtube_url) in enumerate(zip(items, YOUTUBE_URLS, strict=True), start=1):
        source_name = Path(item["file"]).name
        scheduled_at = datetime.fromisoformat(item["publish_at"]).astimezone(timezone.utc) + timedelta(days=1)
        row = upsert_growth_post(
            campaign_id=CAMPAIGN_ID,
            day_number=index,
            channel="tiktok",
            topic=item["hook"],
            content=tiktok_caption(item),
            status="ready_manual",
            scheduled_at=scheduled_at,
            metadata={
                "manual_reminder": True,
                "prepared_video_url": f"{PUBLIC_BASE}/{source_name}",
                "source_file": source_name,
                "youtube_url": youtube_url,
            },
        )
        print(f"{index:02d}/{len(items)} TikTok reminder: {row['scheduled_at']} · {row['topic']}")


if __name__ == "__main__":
    main()
