package com.purehub.app.feature.catalog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color

enum class MiniAppTab(
    val title: String,
    val accent: Color,
    val accentContainer: Color,
) {
    ZEN_TIME("Zen & Time", Color(0xFF14B8A6), Color(0xFFDCF7F3)),
    MEASURE_TOOLS("Measure & Tools", Color(0xFF3B82F6), Color(0xFFDFECFF)),
    VISION("Vision", Color(0xFF8B5CF6), Color(0xFFEEE6FF)),
    SYSTEM_SECURITY("System & Security", Color(0xFF0F766E), Color(0xFFD9F4F0)),
    FINANCE_FUN("Finance & Fun", Color(0xFFF59E0B), Color(0xFFFFF0CC)),
}

enum class MiniAppId(
    val title: String,
    val tab: MiniAppTab,
    val icon: ImageVector,
) {
    LUNAR_CALENDAR("Lunar Calendar", MiniAppTab.ZEN_TIME, Icons.Rounded.Today),
    ZEN_HABIT("Zen Habit", MiniAppTab.ZEN_TIME, Icons.Rounded.AutoAwesome),
    ZEN_POMODORO("Zen Pomodoro", MiniAppTab.ZEN_TIME, Icons.Rounded.Timer),
    ZEN_BREATH("Zen Breath", MiniAppTab.ZEN_TIME, Icons.Rounded.SelfImprovement),

    COMPASS("Compass", MiniAppTab.MEASURE_TOOLS, Icons.Rounded.Explore),
    BUBBLE_LEVEL("Bubble Level & Ruler", MiniAppTab.MEASURE_TOOLS, Icons.Rounded.SquareFoot),
    DECIBEL_METER("Decibel Meter", MiniAppTab.MEASURE_TOOLS, Icons.Rounded.GraphicEq),
    SMART_FLASHLIGHT("Smart Flashlight", MiniAppTab.MEASURE_TOOLS, Icons.Rounded.FlashlightOn),
    UNIT_CONVERTER("Unit Converter", MiniAppTab.MEASURE_TOOLS, Icons.Rounded.SwapHoriz),

    QR_STUDIO("QR Studio", MiniAppTab.VISION, Icons.Rounded.QrCode2),
    DOC_TO_PDF("Doc to PDF", MiniAppTab.VISION, Icons.Rounded.PictureAsPdf),
    OCR_TEXT("OCR Text Extractor", MiniAppTab.VISION, Icons.Rounded.DocumentScanner),
    COLOR_GRABBER("Color Grabber", MiniAppTab.VISION, Icons.Rounded.Colorize),

    DEEP_CLEANER("Deep Cleaner", MiniAppTab.SYSTEM_SECURITY, Icons.Rounded.DeleteSweep),
    SPEAKER_CLEANER("Speaker Cleaner", MiniAppTab.SYSTEM_SECURITY, Icons.AutoMirrored.Rounded.VolumeUp),
    WIFI_ANALYZER("WiFi Analyzer", MiniAppTab.SYSTEM_SECURITY, Icons.Rounded.SignalWifi4Bar),
    PASSWORD_VAULT("Password Vault", MiniAppTab.SYSTEM_SECURITY, Icons.Rounded.Lock),
    WALLPAPER_CHANGER("Wallpaper Changer", MiniAppTab.SYSTEM_SECURITY, Icons.Rounded.Wallpaper),

    BILL_SPLITTER("Bill Splitter", MiniAppTab.FINANCE_FUN, Icons.Rounded.Payments),
    EXPENSE_TRACKER("Expense Tracker", MiniAppTab.FINANCE_FUN, Icons.Rounded.AccountBalanceWallet),
    DECISION_WHEEL("Decision Wheel", MiniAppTab.FINANCE_FUN, Icons.Rounded.Casino),
    COMMUNITY_UNLOCK("Community Unlock", MiniAppTab.FINANCE_FUN, Icons.Rounded.Groups),
}

val miniAppsByTab: Map<MiniAppTab, List<MiniAppId>> = MiniAppId.entries.groupBy { it.tab }
