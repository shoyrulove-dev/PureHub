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
BUILD_DIR = ROOT / "build" / "miniapp-shorts"
OUTPUT_DIR = ROOT / "output" / "miniapp-shorts"
WIDTH, HEIGHT = 1080, 1920

SHORTS = (
    ("01-lunar-calendar.mp4", "Plan with lunar context", "Solar and lunar dates, always available offline", "A Private Lunar Calendar for Everyday Planning #Shorts"),
    ("02-zen-habit.mp4", "Build habits without guilt", "Private streaks and gentle weekly progress", "Build Better Habits Without Ads or Accounts #Shorts"),
    ("03-zen-pomodoro.mp4", "Focus without popups", "A calm offline Pomodoro timer", "PureHub Pomodoro: Focus Without Ads #Shorts"),
    ("04-zen-breath.mp4", "Take one quiet minute", "Guided breathing, completely offline", "One-Minute Offline Breathing Guide | PureHub #Shorts"),
    ("05-compass.mp4", "A compass with no tracking", "Live phone sensors, clear and simple", "A Private Offline Compass for Android #Shorts"),
    ("06-bubble-level.mp4", "Level it with your phone", "Bubble level and ruler in one tool", "Turn Your Phone Into a Bubble Level #Shorts"),
    ("07-decibel-meter.mp4", "Understand nearby sound", "Private on-device sound estimates and history", "Measure Sound Privately on Android #Shorts"),
    ("08-unit-converter.mp4", "Convert units instantly", "Fast, offline, and completely ad-free", "An Offline Unit Converter With Zero Ads #Shorts"),
    ("09-smart-flashlight.mp4", "More than a flashlight", "Torch, SOS and Morse tools in one place", "A Smart Flashlight With SOS and Morse #Shorts"),
    ("10-qr-studio.mp4", "Create QR codes offline", "Generate and scan without cloud uploads", "Create QR Codes Offline With PureHub #Shorts"),
    ("11-doc-to-pdf.mp4", "Turn photos into a PDF", "Capture, reorder and export documents privately", "Create PDFs From Photos Without Uploading #Shorts"),
    ("12-ocr-studio.mp4", "Extract text on-device", "Scan, edit and save a private OCR library", "Private OCR That Works on Your Phone #Shorts"),
    ("13-color-grabber.mp4", "Capture the exact color", "Live HEX and RGB sampling without cloud frames", "Pick Colors Privately With Your Camera #Shorts"),
    ("14-speaker-cleaner.mp4", "Clear water from speakers", "Guided tones, presets and safe listening controls", "Try This Speaker Cleaning Tone Safely #Shorts"),
    ("15-deep-cleaner.mp4", "Review storage safely", "Find large and duplicate media before deleting", "A Storage Cleaner That Never Deletes Silently #Shorts"),
    ("16-wifi-analyzer.mp4", "Understand your Wi-Fi", "Signal details and nearby networks with clear permissions", "See What Your Wi-Fi Signal Is Doing #Shorts"),
    ("17-password-vault.mp4", "Keep passwords on-device", "An encrypted local vault with no ads", "A Local Password Vault Inside PureHub #Shorts"),
    ("18-wallpaper-changer.mp4", "Refresh your home screen", "Private local wallpaper sets and rotation", "Rotate Local Wallpapers Automatically #Shorts"),
    ("19-bill-splitter.mp4", "Split a bill in seconds", "Tax, tip, people, and item assignments", "Split Bills Without Ads or Sign-Up #Shorts"),
    ("20-expense-tracker.mp4", "Track spending privately", "Local-first tracking with no account", "Track Expenses Without an Account #Shorts"),
    ("21-decision-wheel.mp4", "Let the wheel decide", "A playful offline decision tool", "Let This Offline Wheel Decide #Shorts"),
    ("22-community.mp4", "Help shape free tools", "Open source feedback, support and roadmap", "Join the PureHub Open-Source Community #Shorts"),
)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    filename = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / filename), size=size)


def centered(draw: ImageDraw.ImageDraw, text: str, y: int, face: ImageFont.FreeTypeFont, fill: str) -> None:
    bounds = draw.textbbox((0, 0), text, font=face)
    draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, y), text, font=face, fill=fill)


def card(path: Path, hook: str, benefit: str, *, outro: bool = False) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f7f8ff")
    draw = ImageDraw.Draw(image)
    draw.ellipse((-420, -500, 980, 900), fill="#e0e7ff")
    draw.ellipse((420, 980, 1420, 1980), fill="#d1fae5")
    draw.rounded_rectangle((90, 110, 990, 1810), radius=72, fill="#ffffff", outline="#dbe4f0", width=3)
    draw.rounded_rectangle((140, 180, 310, 350), radius=48, fill="#0b5bd3")
    centered(draw, "P", 197, font(105, True), "#ffffff")
    draw.text((350, 188), "PureHub", font=font(70, True), fill="#101322")
    draw.text((350, 286), "FREE · NO ADS · OPEN SOURCE", font=font(27, True), fill="#0b5bd3")
    if outro:
        centered(draw, "Useful tools.", 650, font(76, True), "#101322")
        centered(draw, "Zero ads.", 755, font(76, True), "#101322")
        centered(draw, "Built with the community.", 900, font(38), "#475569")
        draw.rounded_rectangle((175, 1160, 905, 1305), radius=45, fill="#101322")
        centered(draw, "hub.blissbiovn.com", 1194, font(42, True), "#ffffff")
        centered(draw, "Which tool should improve next?", 1470, font(34, True), "#047857")
    else:
        centered(draw, hook, 660, font(70, True), "#101322")
        centered(draw, benefit, 800, font(36), "#475569")
        draw.rounded_rectangle((250, 1110, 830, 1250), radius=44, fill="#0b5bd3")
        centered(draw, "Watch the demo", 1144, font(42, True), "#ffffff")
        centered(draw, "Private by design · Works offline", 1450, font(31, True), "#047857")
    image.save(path, quality=95)


def render_one(raw_name: str, hook: str, benefit: str, title: str) -> dict[str, str]:
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
        f"[0:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30[intro];"
        f"[1:v]scale={WIDTH}:2400:force_original_aspect_ratio=increase,"
        f"crop={WIDTH}:{HEIGHT}:0:120,setsar=1,fps=30[demo];"
        f"[2:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30[outro];"
        "[intro][demo]xfade=transition=fade:duration=0.45:offset=2.55[x1];"
        "[x1][outro]xfade=transition=fade:duration=0.45:offset=14.1,format=yuv420p[outv]"
    )
    command = [
        ffmpeg, "-y",
        "-loop", "1", "-t", "3", "-i", str(intro),
        "-stream_loop", "-1", "-t", "12", "-i", str(raw),
        "-loop", "1", "-t", "3", "-i", str(outro),
        "-f", "lavfi", "-t", "17.1", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-filter_complex", filters,
        "-map", "[outv]", "-map", "3:a", "-t", "17.1",
        "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", str(output),
    ]
    subprocess.run(command, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    description = (
        f"{benefit}. PureHub is a free, ad-free, open-source collection of useful Android tools.\n\n"
        "Try PureHub from the link on this channel profile and tell us what to improve next.\n\n"
        "#PureHub #OpenSource #AndroidApps #NoAds #Privacy #Shorts"
    )
    return {"file": str(output), "title": title, "description": description, "hook": hook, "benefit": benefit}


def next_schedule(count: int) -> list[str]:
    local = datetime.now(ZoneInfo("Asia/Bangkok"))
    first = (local + timedelta(days=1)).replace(hour=19, minute=30, second=0, microsecond=0)
    return [(first + timedelta(days=index * 2)).isoformat() for index in range(count)]


def main() -> None:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    schedule = next_schedule(len(SHORTS))
    manifest = []
    for index, spec in enumerate(SHORTS):
        print(f"Rendering {index + 1}/{len(SHORTS)}: {spec[1]}")
        item = render_one(*spec)
        item["publish_at"] = schedule[index]
        manifest.append(item)
    manifest_path = OUTPUT_DIR / "youtube-queue.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(manifest_path)


if __name__ == "__main__":
    main()
