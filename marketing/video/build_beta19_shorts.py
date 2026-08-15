from __future__ import annotations

import json
import subprocess
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
RAW_DIR = ROOT / "raw"
BUILD_DIR = ROOT / "build" / "beta19-shorts"
OUTPUT_DIR = ROOT / "output" / "beta19-shorts"
WIDTH, HEIGHT = 1080, 1920

SHORTS = (
    (
        "beta19-bubble-level.mp4",
        "Know when the surface is truly level",
        "Flat + edge modes · adjustable tolerance · steady confirmation",
        "Bubble Level With Stable Confirmation | PureHub #Shorts",
        "PureHub Bubble Level now supports flat and edge measurements, adjustable tolerance, calibration guidance, and a steady-reading confirmation.",
    ),
    (
        "13-qr-studio-flagship.mp4",
        "Inspect a QR link before opening it",
        "Scan + create + local history · zero ads",
        "Inspect QR Links Before Opening Them | PureHub #Shorts",
        "QR Studio scans and creates codes locally, checks risky links before opening, and keeps private history on your device.",
    ),
    (
        "12-ocr-studio-flagship.mp4",
        "Turn a photo into searchable text",
        "Private OCR → edit → searchable PDF",
        "Private OCR to Searchable PDF | PureHub #Shorts",
        "OCR Studio recognizes text on-device and hands cleaned pages directly to Doc to PDF for a private searchable document workflow.",
    ),
)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    filename = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / filename), size=size)


def centered(draw: ImageDraw.ImageDraw, text: str, y: int, face: ImageFont.FreeTypeFont, fill: str) -> None:
    bounds = draw.textbbox((0, 0), text, font=face)
    draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, y), text, font=face, fill=fill)


def card(path: Path, hook: str, benefit: str, *, outro: bool = False) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f8fafc")
    draw = ImageDraw.Draw(image)
    draw.ellipse((-360, -420, 900, 800), fill="#cffafe")
    draw.ellipse((500, 1050, 1420, 2020), fill="#ede9fe")
    draw.rounded_rectangle((82, 100, 998, 1820), radius=72, fill="#ffffff", outline="#dbe4ee", width=3)
    draw.rounded_rectangle((140, 175, 305, 340), radius=46, fill="#0f766e")
    centered(draw, "P", 191, font(100, True), "#ffffff")
    draw.text((345, 185), "PureHub", font=font(68, True), fill="#0f172a")
    draw.text((345, 278), "FREE · NO ADS · OPEN SOURCE", font=font(26, True), fill="#0f766e")
    if outro:
        centered(draw, "Useful tools.", 610, font(78, True), "#0f172a")
        centered(draw, "Zero ads.", 720, font(78, True), "#0f172a")
        centered(draw, "Private by design.", 875, font(44), "#475569")
        draw.rounded_rectangle((170, 1125, 910, 1275), radius=46, fill="#0f172a")
        centered(draw, "hub.blissbiovn.com", 1160, font(42, True), "#ffffff")
        centered(draw, "What should improve next?", 1460, font(35, True), "#0f766e")
    else:
        centered(draw, hook, 650, font(52, True), "#0f172a")
        centered(draw, benefit, 790, font(31), "#475569")
        draw.rounded_rectangle((245, 1080, 835, 1225), radius=44, fill="#0f766e")
        centered(draw, "See the workflow", 1114, font(41, True), "#ffffff")
        centered(draw, "Runs locally · Works offline", 1450, font(31, True), "#0f766e")
    image.save(path, quality=95)


def render_one(spec: tuple[str, str, str, str, str]) -> dict[str, str]:
    raw_name, hook, benefit, title, description = spec
    raw = RAW_DIR / raw_name
    if not raw.exists():
        raise FileNotFoundError(raw)
    slug = Path(raw_name).stem
    intro, outro = BUILD_DIR / f"{slug}-intro.png", BUILD_DIR / f"{slug}-outro.png"
    output = OUTPUT_DIR / f"{slug}-short.mp4"
    card(intro, hook, benefit)
    card(outro, hook, benefit, outro=True)
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    filters = (
        f"[0:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,settb=AVTB,setpts=N/(30*TB)[intro];"
        f"[1:v]scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=increase,crop={WIDTH}:{HEIGHT},"
        "setsar=1,fps=30,tpad=stop_mode=clone:stop_duration=8,trim=duration=8,settb=AVTB,setpts=N/(30*TB)[demo];"
        f"[2:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,settb=AVTB,setpts=N/(30*TB)[outro];"
        "[intro][demo][outro]concat=n=3:v=1:a=0,format=yuv420p[outv]"
    )
    command = [
        ffmpeg, "-y", "-loop", "1", "-t", "2.7", "-i", str(intro), "-i", str(raw),
        "-loop", "1", "-t", "2.7", "-i", str(outro),
        "-f", "lavfi", "-t", "13.4", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-filter_complex", filters, "-map", "[outv]", "-map", "3:a", "-t", "13.4",
        "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", str(output),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-6000:])
    copy = (
        f"{description}\n\nPureHub is free, ad-free, open source, and local-first. "
        "Try the workflow and tell us which result or limitation we should test next.\n\n"
        "#PureHub #OpenSource #AndroidApps #NoAds #Privacy #Shorts"
    )
    return {"file": str(output), "title": title, "description": copy, "hook": hook, "benefit": benefit}


def schedule(count: int) -> list[str]:
    local = datetime.now(ZoneInfo("Asia/Bangkok"))
    first = local.replace(hour=19, minute=30, second=0, microsecond=0)
    while first <= local or first.date().isoformat() <= "2026-09-22":
        first += timedelta(days=1)
    return [(first + timedelta(days=index * 2)).isoformat() for index in range(count)]


def main() -> None:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    publish_times = schedule(len(SHORTS))
    manifest = []
    for index, spec in enumerate(SHORTS):
        print(f"Rendering beta.19 Short {index + 1}/{len(SHORTS)}: {spec[1]}")
        item = render_one(spec)
        item["publish_at"] = publish_times[index]
        manifest.append(item)
    path = OUTPUT_DIR / "youtube-queue.json"
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(path)


if __name__ == "__main__":
    main()
