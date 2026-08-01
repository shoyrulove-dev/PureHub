# PureHub short-video kit

`build_short.py` creates a 22.5-second English-first vertical teaser at 1080×1920 from current PureHub mobile screenshots.

`capture_device_demos.ps1` records real Android miniapp footage through USB. `build_miniapp_shorts.py` turns those captures into ten branded 1080×1920 Shorts and creates `output/miniapp-shorts/youtube-queue.json`. `upload_youtube_queue.py` uploads each Short as private and sets YouTube's native `publishAt` schedule.

Run:

```powershell
python marketing/video/build_short.py
```

The MP4 is written to `marketing/video/output/purehub-community-short-en.mp4`. It intentionally uses silent audio so the owner can add a licensed or platform-native track separately in YouTube Studio or TikTok.

Suggested caption:

> 22 useful tools. Free, private, open source, and no ads. PureHub is being built with the community — tell us what we should improve next. 🌱

Suggested hashtags: `#PureHub #OpenSource #AndroidApps #Privacy #NoAds #Productivity`
