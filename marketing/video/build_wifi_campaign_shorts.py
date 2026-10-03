from __future__ import annotations

import json
import subprocess
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
RAW_DIR = ROOT / "raw" / "wifi-october"
BUILD_DIR = ROOT / "build" / "wifi-october"
OUTPUT_DIR = ROOT / "output" / "wifi-october"
LANDING_VIDEO = ROOT.parents[1] / "pwa" / "public" / "media" / "landing" / "wifi-analyzer-android.mp4"
WIDTH, HEIGHT = 720, 1280

SHORTS = (
    ("wifi-overview.mp4", "wifi-signal-dbm", "Read Wi-Fi strength in dBm", "Live Android signal data, kept on your device", "2026-10-03T19:30:00+07:00", 0.0),
    ("wifi-scan.mp4", "wifi-channel-scan", "See which Wi-Fi channels are crowded", "Nearby scan with channel and security details", "2026-10-05T19:30:00+07:00", 1.5),
    ("wifi-diagnose.mp4", "wifi-network-diagnostics", "Test latency, DNS and throughput", "A test runs only when you request it", "2026-10-07T19:30:00+07:00", 1.0),
    ("wifi-scan.mp4", "wifi-sort-networks", "Sort nearby networks by real signal", "Compare SSID, BSSID, channel and dBm", "2026-10-08T19:30:00+07:00", 4.0),
    ("wifi-overview.mp4", "wifi-health-overview", "Get a clear Wi-Fi health overview", "Signal, link and local recommendations in one place", "2026-10-11T19:30:00+07:00", 3.0),
    ("wifi-scan.mp4", "wifi-security-check", "Spot open or weak Wi-Fi security", "Review nearby network security before connecting", "2026-10-14T19:30:00+07:00", 5.0),
    ("wifi-diagnose.mp4", "wifi-data-saver-test", "Choose how much data a speed test uses", "Data saver, balanced and accurate profiles", "2026-10-15T19:30:00+07:00", 2.5),
    ("wifi-overview.mp4", "wifi-private-survey", "Map weak rooms without an account", "Coverage measurements stay on this device", "2026-10-18T19:30:00+07:00", 5.5),
    ("wifi-scan.mp4", "wifi-vendor-detection", "Recognize nearby Wi-Fi hardware", "Offline vendor lookup with honest unknown states", "2026-10-21T19:30:00+07:00", 6.0),
    ("wifi-diagnose.mp4", "wifi-test-history", "Keep a local history of network tests", "Compare recent runs without a cloud account", "2026-10-22T19:30:00+07:00", 7.0),
    ("wifi-overview.mp4", "wifi-floor-survey", "Organize coverage by project and floor", "Build a repeatable home or office survey", "2026-10-25T19:30:00+07:00", 7.0),
    ("wifi-scan.mp4", "wifi-roaming-check", "Compare access points while you walk", "Track BSSID and signal changes during roaming", "2026-10-28T19:30:00+07:00", 2.5),
    ("wifi-diagnose.mp4", "wifi-complete-health-check", "Run a complete Wi-Fi health check", "Signal evidence plus on-demand network diagnostics", "2026-10-29T19:30:00+07:00", 4.5),
)


def font(size: int, *, bold: bool = False) -> ImageFont.FreeTypeFont:
    name = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / name), size=size)


def wrap(draw: ImageDraw.ImageDraw, text: str, max_width: int, face: ImageFont.FreeTypeFont) -> list[str]:
    lines: list[str] = []
    current = ""
    for word in text.split():
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
    image = Image.new("RGB", (WIDTH, HEIGHT), "#06131f")
    draw = ImageDraw.Draw(image)
    draw.ellipse((-190, -220, 500, 470), fill="#083e42")
    draw.ellipse((390, 850, 950, 1410), fill="#122f55")
    draw.rounded_rectangle((42, 54, 678, 1226), radius=44, fill="#0b1e2d", outline="#27d3a2", width=2)
    draw.rounded_rectangle((78, 90, 174, 186), radius=28, fill="#11b981")
    draw.text((108, 98), "P", font=font(61, bold=True), fill="white")
    draw.text((200, 99), "PureHub WiFi", font=font(39, bold=True), fill="white")
    draw.text((202, 151), "ANDROID BETA 52", font=font(18, bold=True), fill="#5eeac2")
    if outro:
        lines = ("Measure clearly.", "Fix with evidence.", "Keep data private.")
        for index, line in enumerate(lines):
            box = draw.textbbox((0, 0), line, font=font(43, bold=True))
            draw.text(((WIDTH - box[2]) / 2, 390 + index * 80), line, font=font(43, bold=True), fill="white")
        draw.rounded_rectangle((94, 820, 626, 920), radius=30, fill="#11b981")
        label = "hub.blissbiovn.com/en/wifi-analyzer"
        box = draw.textbbox((0, 0), label, font=font(20, bold=True))
        draw.text(((WIDTH - box[2]) / 2, 854), label, font=font(20, bold=True), fill="#041511")
        note = "Free · open source · privacy-first"
        box = draw.textbbox((0, 0), note, font=font(21))
        draw.text(((WIDTH - box[2]) / 2, 995), note, font=font(21), fill="#b7cad7")
    else:
        face = font(43, bold=True)
        lines = wrap(draw, hook, 560, face)
        y = 380
        for line in lines:
            box = draw.textbbox((0, 0), line, font=face)
            draw.text(((WIDTH - box[2]) / 2, y), line, font=face, fill="white")
            y += 62
        subface = font(24)
        y += 35
        for line in wrap(draw, benefit, 540, subface):
            box = draw.textbbox((0, 0), line, font=subface)
            draw.text(((WIDTH - box[2]) / 2, y), line, font=subface, fill="#b7cad7")
            y += 38
        draw.rounded_rectangle((198, 850, 522, 946), radius=28, fill="#11b981")
        label = "REAL DEVICE DEMO"
        box = draw.textbbox((0, 0), label, font=font(24, bold=True))
        draw.text(((WIDTH - box[2]) / 2, 881), label, font=font(24, bold=True), fill="#041511")
    image.save(path, quality=94)


def render(index: int, item: tuple[str, str, str, str, str, float]) -> Path:
    source_name, slug, hook, benefit, _, start = item
    source = RAW_DIR / source_name
    intro = BUILD_DIR / f"{slug}-intro.png"
    outro = BUILD_DIR / f"{slug}-outro.png"
    output = OUTPUT_DIR / f"{index:02d}-{slug}-short.mp4"
    card(intro, hook, benefit)
    card(outro, hook, benefit, outro=True)
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    demo_duration = 10.5
    total = 14.1
    privacy_masks = "drawbox=x=88:y=125:w=335:h=58:color=0x0b1e2d:t=fill"
    if source_name == "wifi-scan.mp4":
        privacy_masks += ",drawbox=x=80:y=680:w=370:h=455:color=0x06131f:t=fill"
    filters = (
        f"[0:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30[intro];"
        f"[1:v]crop=iw:ih-100:0:100,scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=decrease,"
        f"pad={WIDTH}:{HEIGHT}:(ow-iw)/2:(oh-ih)/2:color=0x06131f,setsar=1,fps=30,{privacy_masks},"
        f"trim=duration={demo_duration},setpts=N/(30*TB)[demo];"
        f"[2:v]scale={WIDTH}:{HEIGHT},setsar=1,fps=30[outro];"
        "[intro][demo][outro]concat=n=3:v=1:a=0,format=yuv420p[outv]"
    )
    command = [
        ffmpeg, "-y", "-loop", "1", "-t", "1.8", "-i", str(intro),
        "-ss", str(start), "-i", str(source),
        "-loop", "1", "-t", "1.8", "-i", str(outro),
        "-f", "lavfi", "-t", str(total), "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-filter_complex", filters, "-map", "[outv]", "-map", "3:a", "-t", str(total),
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "26", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "64k", "-movflags", "+faststart", str(output),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-5000:])
    return output


def update_landing_video() -> None:
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    source = RAW_DIR / "wifi-scan.mp4"
    command = [
        ffmpeg, "-y", "-ss", "1.5", "-i", str(source), "-t", "10",
        "-vf", "crop=iw:ih-100:0:100,scale=540:-2,fps=24,format=yuv420p", "-an",
        "-c:v", "libx264", "-preset", "medium", "-crf", "28", "-movflags", "+faststart", str(LANDING_VIDEO),
    ]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-5000:])


def main() -> None:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    manifest = []
    for index, item in enumerate(SHORTS, start=1):
        source_name, slug, hook, benefit, publish_at, _ = item
        print(f"Rendering WiFi Short {index}/{len(SHORTS)}: {hook}")
        output = render(index, item)
        scheduled = datetime.fromisoformat(publish_at).astimezone(ZoneInfo("Asia/Bangkok"))
        manifest.append({
            "file": str(output),
            "title": f"{hook} | PureHub WiFi Analyzer #Shorts",
            "description": (
                f"{benefit}. Recorded on a real Android device running PureHub beta 52.\n\n"
                "PureHub is free and open source. Wi-Fi scans and survey history stay on your device; "
                "network diagnostics contact the selected test endpoints only when you start a test.\n\n"
                "https://hub.blissbiovn.com/en/wifi-analyzer?utm_source=youtube&utm_campaign=wifi-analyzer-october-2026-v1\n\n"
                "#PureHub #WiFiAnalyzer #AndroidApps #OpenSource #Privacy #Shorts"
            ),
            "hook": hook,
            "benefit": benefit,
            "source": source_name,
            "publish_at": scheduled.isoformat(),
        })
    (OUTPUT_DIR / "youtube-queue.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    update_landing_video()
    print(OUTPUT_DIR / "youtube-queue.json")
    print(f"Updated landing video: {LANDING_VIDEO}")


if __name__ == "__main__":
    main()
