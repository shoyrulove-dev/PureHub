from __future__ import annotations

import json
import subprocess
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
RAW_DIR = ROOT / "raw" / "ocr-campaign"
BUILD_DIR = ROOT / "build" / "ocr-campaign"
OUTPUT_DIR = ROOT / "output" / "ocr-campaign"
LANDING_VIDEO = ROOT.parents[1] / "pwa" / "public" / "media" / "landing" / "private-ocr.mp4"
WIDTH, HEIGHT = 1080, 1920

SHORTS = (
    {
        "source": "06-ocr-android-success-beta44.mp4",
        "slug": "private-ocr-vietnamese-receipt-android",
        "hook": "Turn a Vietnamese receipt into editable text",
        "benefit": "Android beta.44 · local OCR · review before sharing",
        "title": "Private OCR for Vietnamese Receipts on Android #Shorts",
        "description": "A real PureHub beta.44 device run: import a printed Vietnamese receipt, review the page frame, then extract editable text locally.",
        "speed": 1.0,
        "duration": 24.0,
        "publish_at": "2026-09-08T19:30:00+07:00",
    },
    {
        "source": "02-ocr-batch-en-vi.webm",
        "slug": "private-ocr-two-page-batch",
        "hook": "OCR two printed pages in one private batch",
        "benefit": "Browser workflow · English + Vietnamese · editable result",
        "title": "Batch OCR: Two Pages, Editable Text, No Account #Shorts",
        "description": "PureHub browser OCR processes two selected printed pages in one batch and returns editable text. The first language-pack use may require a download.",
        "speed": 1.0,
        "duration": 15.0,
        "publish_at": "2026-09-16T19:30:00+07:00",
    },
    {
        "source": "03-ocr-chinese.webm",
        "slug": "private-ocr-simplified-chinese",
        "hook": "Recognize Simplified Chinese printed text",
        "benefit": "Language-aware OCR · editable result · private workflow",
        "title": "Simplified Chinese OCR in PureHub #Shorts",
        "description": "PureHub OCR recognizes a synthetic Simplified Chinese printed-text sample and returns editable text. Handwriting is not claimed or shown.",
        "speed": 1.0,
        "duration": 10.0,
        "publish_at": "2026-09-24T19:30:00+07:00",
    },
)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    filename = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / filename), size=size)


def fit_lines(draw: ImageDraw.ImageDraw, text: str, max_width: int, face: ImageFont.FreeTypeFont) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = f"{current} {word}".strip()
        if draw.textbbox((0, 0), candidate, font=face)[2] <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def card(path: Path, hook: str, benefit: str, *, outro: bool = False) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f8fafc")
    draw = ImageDraw.Draw(image)
    draw.ellipse((-300, -350, 800, 750), fill="#d1fae5")
    draw.ellipse((560, 1180, 1450, 2050), fill="#dbeafe")
    draw.rounded_rectangle((72, 90, 1008, 1830), radius=68, fill="#ffffff", outline="#d7e2df", width=3)
    draw.rounded_rectangle((132, 160, 292, 320), radius=45, fill="#047857")
    draw.text((180, 170), "P", font=font(96, True), fill="#ffffff")
    draw.text((330, 170), "PureHub OCR", font=font(62, True), fill="#0f172a")
    draw.text((330, 258), "FREE · NO ADS · PRIVACY-FIRST", font=font(25, True), fill="#047857")
    if outro:
        for index, line in enumerate(("Scan locally.", "Review the result.", "Export when ready.")):
            bounds = draw.textbbox((0, 0), line, font=font(64, True))
            draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, 570 + index * 120), line, font=font(64, True), fill="#0f172a")
        draw.rounded_rectangle((165, 1160, 915, 1310), radius=44, fill="#0f172a")
        label = "hub.blissbiovn.com/en/ocr-text"
        bounds = draw.textbbox((0, 0), label, font=font(33, True))
        draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, 1205), label, font=font(33, True), fill="#ffffff")
        note = "Printed-text OCR · review before sharing"
        bounds = draw.textbbox((0, 0), note, font=font(27))
        draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, 1460), note, font=font(27), fill="#475569")
    else:
        face = font(58, True)
        lines = fit_lines(draw, hook, 820, face)
        y = 610 - max(0, len(lines) - 2) * 42
        for line in lines:
            bounds = draw.textbbox((0, 0), line, font=face)
            draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, y), line, font=face, fill="#0f172a")
            y += 86
        benefit_face = font(31)
        for line in fit_lines(draw, benefit, 780, benefit_face):
            bounds = draw.textbbox((0, 0), line, font=benefit_face)
            draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, y + 55), line, font=benefit_face, fill="#475569")
            y += 48
        draw.rounded_rectangle((265, 1180, 815, 1325), radius=42, fill="#047857")
        label = "REAL WORKFLOW"
        bounds = draw.textbbox((0, 0), label, font=font(37, True))
        draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, 1220), label, font=font(37, True), fill="#ffffff")
    image.save(path, quality=95)


def render(item: dict[str, object]) -> Path:
    source = RAW_DIR / str(item["source"])
    if not source.is_file():
        raise FileNotFoundError(source)
    intro = BUILD_DIR / f"{item['slug']}-intro.png"
    outro = BUILD_DIR / f"{item['slug']}-outro.png"
    output = OUTPUT_DIR / f"{item['slug']}-short.mp4"
    card(intro, str(item["hook"]), str(item["benefit"]))
    card(outro, str(item["hook"]), str(item["benefit"]), outro=True)
    demo_duration = float(item["duration"])
    speed = float(item["speed"])
    total = 2.6 + demo_duration + 2.6
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    filters = (
        f"[0:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,setpts=N/(30*TB)[intro];"
        f"[1:v]setpts={speed}*PTS,scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=decrease,"
        f"pad={WIDTH}:{HEIGHT}:(ow-iw)/2:(oh-ih)/2:color=0x07111e,setsar=1,"
        f"fps=30,tpad=stop_mode=clone:stop_duration={demo_duration},trim=duration={demo_duration},setpts=N/(30*TB)[demo];"
        f"[2:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30,setpts=N/(30*TB)[outro];"
        "[intro][demo][outro]concat=n=3:v=1:a=0,format=yuv420p[outv]"
    )
    command = [
        ffmpeg, "-y",
        "-loop", "1", "-t", "2.6", "-i", str(intro),
        "-i", str(source),
        "-loop", "1", "-t", "2.6", "-i", str(outro),
        "-f", "lavfi", "-t", str(total), "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-filter_complex", filters,
        "-map", "[outv]", "-map", "3:a", "-t", str(total),
        "-c:v", "libx264", "-preset", "ultrafast", "-crf", "21", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", str(output),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-6000:])
    return output


def update_landing_video() -> None:
    source = RAW_DIR / "01-ocr-receipt-vi.webm"
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    command = [
        ffmpeg, "-y", "-i", str(source), "-vf", "fps=24,format=yuv420p",
        "-an", "-c:v", "libx264", "-preset", "medium", "-crf", "23", "-movflags", "+faststart", str(LANDING_VIDEO),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-6000:])


def main() -> None:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    manifest = []
    for index, item in enumerate(SHORTS, start=1):
        print(f"Rendering OCR Short {index}/{len(SHORTS)}: {item['hook']}")
        output = render(item)
        publish_at = datetime.fromisoformat(str(item["publish_at"])).astimezone(ZoneInfo("Asia/Bangkok"))
        manifest.append({
            "file": str(output),
            "title": item["title"],
            "description": (
                f"{item['description']}\n\n"
                "PureHub is free, ad-free and open source. Try OCR Studio and report the device, language and result quality.\n\n"
                "https://hub.blissbiovn.com/en/ocr-text?utm_source=youtube&utm_campaign=private-ocr-30d-v1\n\n"
                "#PureHub #OCR #AndroidApps #Privacy #OpenSource #Shorts"
            ),
            "hook": item["hook"],
            "benefit": item["benefit"],
            "publish_at": publish_at.isoformat(),
        })
    (OUTPUT_DIR / "youtube-queue.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    update_landing_video()
    print(OUTPUT_DIR / "youtube-queue.json")
    print(f"Updated landing video: {LANDING_VIDEO}")


if __name__ == "__main__":
    main()
