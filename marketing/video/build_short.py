from __future__ import annotations

import subprocess
from pathlib import Path

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parent
SOURCE_DIR = ROOT / "source"
BUILD_DIR = ROOT / "build"
OUTPUT_DIR = ROOT / "output"
WIDTH, HEIGHT = 1080, 1920

SCENES = [
    ("01-home.png", "22 everyday tools", "One calm, private hub"),
    ("02-tools.png", "Free · No ads · Open source", "Useful tools without tracking walls"),
    ("03-pomodoro.png", "Focus fully offline", "Timers and everyday tools stay on your device"),
    ("04-password.png", "Built with the community", "Friendly, privacy-first, and open to everyone"),
]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    filename = "seguisb.ttf" if bold else "segoeui.ttf"
    path = Path("C:/Windows/Fonts") / filename
    return ImageFont.truetype(str(path), size=size)


def cover(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    target_w, target_h = size
    scale = max(target_w / image.width, target_h / image.height)
    resized = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
    left = (resized.width - target_w) // 2
    top = (resized.height - target_h) // 2
    return resized.crop((left, top, left + target_w, top + target_h))


def fit(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    scale = min(size[0] / image.width, size[1] / image.height)
    return image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)


def centered(draw: ImageDraw.ImageDraw, text: str, y: int, text_font: ImageFont.FreeTypeFont, fill: str) -> None:
    box = draw.textbbox((0, 0), text, font=text_font)
    draw.text(((WIDTH - (box[2] - box[0])) / 2, y), text, font=text_font, fill=fill)


def make_scene(source_name: str, title: str, subtitle: str, index: int) -> Path:
    source = Image.open(SOURCE_DIR / source_name).convert("RGB")
    background = cover(source, (WIDTH, HEIGHT)).filter(ImageFilter.GaussianBlur(34))
    wash = Image.new("RGBA", (WIDTH, HEIGHT), (236, 253, 245, 220))
    canvas = Image.alpha_composite(background.convert("RGBA"), wash)
    draw = ImageDraw.Draw(canvas)

    draw.rounded_rectangle((66, 62, 1014, 284), radius=46, fill=(255, 255, 255, 242), outline=(209, 250, 229, 255), width=3)
    draw.rounded_rectangle((98, 96, 194, 192), radius=28, fill="#00bd84")
    draw.text((129, 108), "P", font=font(54, True), fill="white")
    draw.text((226, 92), "PureHub", font=font(48, True), fill="#071126")
    draw.text((226, 158), "FREE · PRIVATE · NO ADS", font=font(25, True), fill="#047857")

    phone = fit(source, (870, 1320)).convert("RGBA")
    mask = Image.new("L", phone.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, phone.width, phone.height), radius=44, fill=255)
    phone.putalpha(mask)
    phone_x = (WIDTH - phone.width) // 2
    phone_y = 330
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (phone_x - 14, phone_y + 12, phone_x + phone.width + 14, phone_y + phone.height + 32),
        radius=54,
        fill=(15, 23, 42, 45),
    )
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(18)))
    canvas.alpha_composite(phone, (phone_x, phone_y))

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((64, 1690, 1016, 1860), radius=42, fill=(7, 17, 38, 238))
    centered(draw, title, 1718, font(45, True), "#ffffff")
    centered(draw, subtitle, 1786, font(25), "#cbd5e1")
    path = BUILD_DIR / f"scene-{index:02d}.png"
    canvas.convert("RGB").save(path, quality=95)
    return path


def make_outro(index: int) -> Path:
    canvas = Image.new("RGB", (WIDTH, HEIGHT), "#ecfdf5")
    draw = ImageDraw.Draw(canvas)
    for radius, color in ((720, "#dbeafe"), (540, "#ccfbf1"), (360, "#a7f3d0")):
        draw.ellipse((WIDTH // 2 - radius, 320 - radius, WIDTH // 2 + radius, 320 + radius), fill=color)
    draw.rounded_rectangle((390, 490, 690, 790), radius=88, fill="#00bd84")
    centered(draw, "P", 518, font(180, True), "#ffffff")
    centered(draw, "PureHub", 870, font(88, True), "#071126")
    centered(draw, "Useful tools. Zero ads.", 1000, font(43), "#334155")
    centered(draw, "Free for everyone.", 1066, font(43), "#334155")
    draw.rounded_rectangle((150, 1240, 930, 1375), radius=40, fill="#071126")
    centered(draw, "hub.blissbiovn.com", 1270, font(43, True), "#ffffff")
    centered(draw, "Join the open-source community", 1480, font(31, True), "#047857")
    centered(draw, "GitHub · Telegram · PureHub", 1540, font(27), "#475569")
    path = BUILD_DIR / f"scene-{index:02d}.png"
    canvas.save(path, quality=95)
    return path


def render() -> Path:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    scenes = [make_scene(*scene, index) for index, scene in enumerate(SCENES, start=1)]
    scenes.append(make_outro(len(scenes) + 1))
    output = OUTPUT_DIR / "purehub-community-short-en.mp4"
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    command = [ffmpeg, "-y"]
    for scene in scenes:
        command.extend(["-loop", "1", "-t", "5", "-i", str(scene)])
    command.extend(["-f", "lavfi", "-t", "23", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100"])

    filters = []
    for index in range(len(scenes)):
        direction = "iw/2-(iw/zoom/2)" if index % 2 == 0 else "iw/2-(iw/zoom/2)+12*sin(on/35)"
        filters.append(
            f"[{index}:v]zoompan=z='min(zoom+0.00035,1.035)':x='{direction}':"
            f"y='ih/2-(ih/zoom/2)':d=150:s={WIDTH}x{HEIGHT}:fps=30,format=yuv420p[v{index}]"
        )
    filters.extend(
        [
            "[v0][v1]xfade=transition=fade:duration=0.5:offset=4.5[x1]",
            "[x1][v2]xfade=transition=fade:duration=0.5:offset=9.0[x2]",
            "[x2][v3]xfade=transition=fade:duration=0.5:offset=13.5[x3]",
            "[x3][v4]xfade=transition=fade:duration=0.5:offset=18.0,format=yuv420p[outv]",
        ]
    )
    command.extend(
        [
            "-filter_complex",
            ";".join(filters),
            "-map",
            "[outv]",
            "-map",
            f"{len(scenes)}:a",
            "-t",
            "22.5",
            "-c:v",
            "libx264",
            "-preset",
            "medium",
            "-crf",
            "20",
            "-pix_fmt",
            "yuv420p",
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            "-movflags",
            "+faststart",
            str(output),
        ]
    )
    subprocess.run(command, check=True)
    return output


if __name__ == "__main__":
    print(render())
