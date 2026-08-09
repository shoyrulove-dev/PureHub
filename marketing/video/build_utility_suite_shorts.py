from __future__ import annotations

import json
import subprocess
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "raw"
BUILD = ROOT / "build" / "utility-suite-shorts"
OUTPUT = ROOT / "output" / "utility-suite-shorts"
WIDTH, HEIGHT = 1080, 1920

SHORTS = (
    ("14-speaker-cleaner-flagship.png", "Water in your speaker?", "Run a careful timed cleaning tone", "Speaker Cleaner: Timed Water Eject Tones With No Ads #Shorts", "Timed 150–185 Hz presets, local playback and clear safety guidance."),
    ("15-document-suite-flagship.png", "Documents stay on your phone", "Scan, reorder, OCR and export locally", "PureHub Document Suite: OCR and PDF Without the Cloud #Shorts", "Capture, crop and export PDFs, then extract editable text with OCR Studio."),
    ("16-finance-suite-flagship.png", "Private money tools", "Track spending and split bills without sign-up", "PureHub Finance Suite: Expense Tracker and Bill Splitter #Shorts", "A local expense ledger, category trends, CSV export and transparent bill settlement."),
    ("17-sensor-suite-flagship.png", "Your phone is a sensor toolkit", "Compass, level and sound estimates in one suite", "PureHub Sensor Suite: Compass, Level and Sound Meter #Shorts", "Smooth local sensor readings with clear calibration and safety guidance."),
)
PUBLISH_DAYS = (22, 24, 26, 28)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / ("seguisb.ttf" if bold else "segoeui.ttf")), size=size)


def center(draw: ImageDraw.ImageDraw, text: str, y: int, face: ImageFont.FreeTypeFont, fill: str) -> None:
    box = draw.textbbox((0, 0), text, font=face)
    draw.text(((WIDTH - box[2] + box[0]) / 2, y), text, font=face, fill=fill)


def card(path: Path, hook: str, benefit: str, outro: bool = False) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f8fafc")
    draw = ImageDraw.Draw(image)
    draw.ellipse((-330, -350, 850, 780), fill="#ccfbf1")
    draw.ellipse((570, 1100, 1420, 2020), fill="#dbeafe")
    draw.rounded_rectangle((78, 100, 1002, 1820), radius=70, fill="#ffffff", outline="#d9e5e2", width=4)
    draw.rounded_rectangle((135, 165, 295, 325), radius=45, fill="#047857")
    center(draw, "P", 184, font(88, True), "#ffffff")
    draw.text((335, 180), "PureHub", font=font(66, True), fill="#0f172a")
    draw.text((335, 275), "FREE · NO ADS · OPEN SOURCE", font=font(25, True), fill="#047857")
    if outro:
        center(draw, "Useful tools.", 610, font(78, True), "#0f172a")
        center(draw, "Zero ads.", 720, font(78, True), "#0f172a")
        center(draw, "Built with the community.", 880, font(38), "#475569")
        draw.rounded_rectangle((160, 1125, 920, 1275), radius=46, fill="#0f172a")
        center(draw, "hub.blissbiovn.com", 1162, font(41, True), "#ffffff")
        center(draw, "What should we improve next?", 1460, font(34, True), "#047857")
    else:
        center(draw, hook, 640, font(60, True), "#0f172a")
        center(draw, benefit, 790, font(34), "#475569")
        draw.rounded_rectangle((245, 1080, 835, 1225), radius=44, fill="#047857")
        center(draw, "See it in action", 1116, font(40, True), "#ffffff")
        center(draw, "Private by design · Works offline", 1450, font(30, True), "#047857")
    image.save(path, quality=95)


def render(spec: tuple[str, str, str, str, str], publish_at: str) -> dict[str, str]:
    raw_name, hook, benefit, title, detail = spec
    source = RAW / raw_name
    slug = source.stem
    intro, outro = BUILD / f"{slug}-intro.png", BUILD / f"{slug}-outro.png"
    target = OUTPUT / f"{slug}-short.mp4"
    card(intro, hook, benefit); card(outro, hook, benefit, True)
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    filters = (
        f"[0:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,settb=AVTB,setpts=N/(30*TB)[intro];"
        f"[1:v]scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=increase,crop={WIDTH}:{HEIGHT},setsar=1,fps=30,tpad=stop_mode=clone:stop_duration=8,trim=duration=8,settb=AVTB,setpts=N/(30*TB)[demo];"
        f"[2:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,settb=AVTB,setpts=N/(30*TB)[outro];"
        "[intro][demo][outro]concat=n=3:v=1:a=0,format=yuv420p[outv]"
    )
    command = [ffmpeg, "-y", "-loop", "1", "-t", "2.7", "-i", str(intro), "-loop", "1", "-t", "8", "-i", str(source), "-loop", "1", "-t", "2.7", "-i", str(outro), "-f", "lavfi", "-t", "13.4", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100", "-filter_complex", filters, "-map", "[outv]", "-map", "3:a", "-t", "13.4", "-c:v", "libx264", "-preset", "ultrafast", "-crf", "21", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", str(target)]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-5000:])
    description = f"{detail}\n\nPureHub is free, ad-free, privacy-first and open source. Try it and tell us what to improve next.\n\nhttps://hub.blissbiovn.com/en\n\n#PureHub #OpenSource #AndroidApps #NoAds #Privacy #Shorts"
    return {"file": str(target), "title": title, "description": description, "hook": hook, "benefit": benefit, "publish_at": publish_at}


def main() -> None:
    BUILD.mkdir(parents=True, exist_ok=True); OUTPUT.mkdir(parents=True, exist_ok=True)
    zone = ZoneInfo("Asia/Bangkok")
    rows = [render(spec, datetime(2026, 8, day, 19, 30, tzinfo=zone).isoformat()) for spec, day in zip(SHORTS, PUBLISH_DAYS)]
    manifest = OUTPUT / "youtube-queue.json"
    manifest.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    print(manifest)


if __name__ == "__main__":
    main()
