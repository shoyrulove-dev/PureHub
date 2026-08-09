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
BUILD_DIR = ROOT / "build" / "flagship-shorts"
OUTPUT_DIR = ROOT / "output" / "flagship-shorts"
WIDTH, HEIGHT = 1080, 1920

SHORTS = (
    (
        "11-zen-habit-flagship.png",
        "Build habits, not streak anxiety",
        "Private check-ins and calm weekly insights",
        "Zen Habit: A Private Habit Tracker With No Ads #Shorts",
        "Zen Habit keeps daily check-ins, weekly goals and progress insights on your device.",
    ),
    (
        "12-ocr-studio-flagship.mp4",
        "Scan documents without the cloud",
        "Clean, edit and export text on-device",
        "OCR Studio: Scan Documents Privately on Android #Shorts",
        "OCR Studio captures pages, cleans scans, recognizes text and exports private documents.",
    ),
    (
        "13-qr-studio-flagship.mp4",
        "QR tools without tracking",
        "Scan, create and keep local history",
        "QR Studio: Scan and Create Codes Without Ads #Shorts",
        "QR Studio scans and creates useful codes while keeping history private and local.",
    ),
)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    filename = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / filename), size=size)


def centered(draw: ImageDraw.ImageDraw, text: str, y: int, face: ImageFont.FreeTypeFont, fill: str) -> None:
    bounds = draw.textbbox((0, 0), text, font=face)
    draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, y), text, font=face, fill=fill)


def card(path: Path, hook: str, benefit: str, *, outro: bool = False) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f7faf9")
    draw = ImageDraw.Draw(image)
    draw.ellipse((-360, -420, 900, 800), fill="#d1fae5")
    draw.ellipse((500, 1050, 1420, 2020), fill="#dbeafe")
    draw.rounded_rectangle((82, 100, 998, 1820), radius=72, fill="#ffffff", outline="#dce8e4", width=3)
    draw.rounded_rectangle((140, 175, 305, 340), radius=46, fill="#047857")
    centered(draw, "P", 191, font(100, True), "#ffffff")
    draw.text((345, 185), "PureHub", font=font(68, True), fill="#0f172a")
    draw.text((345, 278), "FREE · NO ADS · OPEN SOURCE", font=font(26, True), fill="#047857")
    if outro:
        centered(draw, "Useful tools.", 610, font(78, True), "#0f172a")
        centered(draw, "Zero ads.", 720, font(78, True), "#0f172a")
        centered(draw, "Built with the community.", 875, font(39), "#475569")
        draw.rounded_rectangle((170, 1125, 910, 1275), radius=46, fill="#0f172a")
        centered(draw, "hub.blissbiovn.com", 1160, font(42, True), "#ffffff")
        centered(draw, "What should we improve next?", 1460, font(34, True), "#047857")
    else:
        centered(draw, hook, 650, font(62, True), "#0f172a")
        centered(draw, benefit, 790, font(35), "#475569")
        draw.rounded_rectangle((245, 1080, 835, 1225), radius=44, fill="#047857")
        centered(draw, "See it in action", 1114, font(41, True), "#ffffff")
        centered(draw, "Private by design · Works offline", 1450, font(31, True), "#047857")
    image.save(path, quality=95)


def render_one(raw_name: str, hook: str, benefit: str, title: str, description: str) -> dict[str, str]:
    slug = Path(raw_name).stem
    raw = RAW_DIR / raw_name
    if not raw.exists():
        raise FileNotFoundError(raw)
    intro = BUILD_DIR / f"{slug}-intro.png"
    outro = BUILD_DIR / f"{slug}-outro.png"
    output = OUTPUT_DIR / f"{slug}-short.mp4"
    card(intro, hook, benefit)
    card(outro, hook, benefit, outro=True)

    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    filters = (
        f"[0:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,settb=AVTB,setpts=N/(30*TB)[intro];"
        f"[1:v]scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=increase,"
        f"crop={WIDTH}:{HEIGHT},setsar=1,fps=30,"
        "tpad=stop_mode=clone:stop_duration=8,trim=duration=8,"
        "settb=AVTB,setpts=N/(30*TB)[demo];"
        f"[2:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,settb=AVTB,setpts=N/(30*TB)[outro];"
        "[intro][demo][outro]concat=n=3:v=1:a=0,format=yuv420p[outv]"
    )
    demo_input = ["-loop", "1", "-t", "8", "-i", str(raw)] if raw.suffix.lower() in {".png", ".jpg", ".jpeg"} else ["-i", str(raw)]
    command = [
        ffmpeg, "-y",
        "-loop", "1", "-t", "2.7", "-i", str(intro),
        *demo_input,
        "-loop", "1", "-t", "2.7", "-i", str(outro),
        "-f", "lavfi", "-t", "13.4", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-filter_complex", filters,
        "-map", "[outv]", "-map", "3:a", "-t", "13.4",
        "-c:v", "libx264", "-preset", "ultrafast", "-crf", "21", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", str(output),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-6000:])
    copy = (
        f"{description}\n\n"
        "PureHub is free, ad-free, privacy-first and open source. Try it and tell us what to improve next.\n\n"
        "https://hub.blissbiovn.com/en\n\n"
        "#PureHub #OpenSource #AndroidApps #NoAds #Privacy #Shorts"
    )
    return {"file": str(output), "title": title, "description": copy, "hook": hook, "benefit": benefit}


def schedule(count: int) -> list[str]:
    local = datetime.now(ZoneInfo("Asia/Bangkok"))
    first = (local + timedelta(days=1)).replace(hour=19, minute=30, second=0, microsecond=0)
    return [(first + timedelta(days=index * 2)).isoformat() for index in range(count)]


def main() -> None:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    publish_times = schedule(len(SHORTS))
    manifest = []
    for index, spec in enumerate(SHORTS):
        print(f"Rendering flagship {index + 1}/{len(SHORTS)}: {spec[1]}")
        item = render_one(*spec)
        item["publish_at"] = publish_times[index]
        manifest.append(item)
    manifest_path = OUTPUT_DIR / "youtube-queue.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(manifest_path)


if __name__ == "__main__":
    main()
