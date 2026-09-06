from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
OUTPUT_DIR = ROOT / "raw" / "ocr-fixtures"
WIDTH, HEIGHT = 1080, 1600


def font(size: int, *, bold: bool = False, chinese: bool = False) -> ImageFont.FreeTypeFont:
    windows_fonts = Path("C:/Windows/Fonts")
    if chinese:
        filename = "msyhbd.ttc" if bold else "msyh.ttc"
    else:
        filename = "seguisb.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(windows_fonts / filename), size=size)


def base_page(title: str, subtitle: str, *, chinese: bool = False) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f8fafc")
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((70, 70, WIDTH - 70, HEIGHT - 70), radius=30, fill="#ffffff", outline="#cbd5e1", width=4)
    draw.text((125, 125), title, font=font(54, bold=True, chinese=chinese), fill="#0f172a")
    draw.text((125, 205), subtitle, font=font(30, chinese=chinese), fill="#475569")
    draw.line((125, 275, WIDTH - 125, 275), fill="#10b981", width=6)
    return image, draw


def receipt() -> None:
    image, draw = base_page("HÓA ĐƠN MẪU PUREHUB", "Bản in dùng để kiểm thử OCR ngoại tuyến")
    rows = [
        ("Ngày: 06/09/2026", ""),
        ("Cà phê sữa", "35.000 đ"),
        ("Bánh mì", "25.000 đ"),
        ("Nước suối", "10.000 đ"),
        ("Tạm tính", "70.000 đ"),
        ("Thuế", "7.000 đ"),
        ("TỔNG CỘNG", "77.000 đ"),
    ]
    y = 350
    for left, right in rows:
        is_total = left == "TỔNG CỘNG"
        face = font(40 if is_total else 34, bold=is_total)
        draw.text((125, y), left, font=face, fill="#0f172a")
        if right:
            bounds = draw.textbbox((0, 0), right, font=face)
            draw.text((WIDTH - 125 - (bounds[2] - bounds[0]), y), right, font=face, fill="#0f172a")
        y += 115
    draw.line((125, y + 10, WIDTH - 125, y + 10), fill="#cbd5e1", width=3)
    draw.text((125, y + 70), "Không chứa dữ liệu cá nhân thật.", font=font(30), fill="#64748b")
    image.save(OUTPUT_DIR / "purehub-ocr-receipt-vi.png", quality=96)


def study_note() -> None:
    image, draw = base_page("PRIVATE OCR STUDY NOTE", "Printed-text sample · English + Vietnamese")
    lines = [
        "1. Scan a printed page on the phone.",
        "2. Review and correct recognized text.",
        "3. Export TXT or a searchable PDF.",
        "4. No account and no document upload.",
        "",
        "Ghi chú: kiểm tra nội dung trước khi chia sẻ.",
        "Mã kiểm thử: PUREHUB 24680",
    ]
    y = 365
    for line in lines:
        draw.text((125, y), line, font=font(34, bold="PUREHUB" in line), fill="#0f172a")
        y += 115
    image.save(OUTPUT_DIR / "purehub-ocr-note-en-vi.png", quality=96)


def chinese_note() -> None:
    image, draw = base_page("PUREHUB 中文 OCR", "简体中文印刷文字测试", chinese=True)
    lines = [
        "在手机上识别印刷文字。",
        "扫描前请保持光线均匀。",
        "识别后请检查文字内容。",
        "文档保存在设备本地。",
        "测试编号：PUREHUB 13579",
    ]
    y = 390
    for line in lines:
        draw.text((125, y), line, font=font(42, chinese=True), fill="#0f172a")
        y += 145
    image.save(OUTPUT_DIR / "purehub-ocr-note-zh.png", quality=96)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    receipt()
    study_note()
    chinese_note()
    print(f"Created OCR demo fixtures in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
