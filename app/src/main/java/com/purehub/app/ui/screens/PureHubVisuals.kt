package com.purehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.purehub.app.feature.catalog.MiniAppTab
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.appText

/** Shared quiet violet canvas used by every non-camera mini app and utility page. */
@Composable
fun Modifier.pureHubCanvas(): Modifier = background(
    Brush.verticalGradient(
        listOf(Color(0xFF231240), Color(0xFF130D22), Color(0xFF10182B), Color(0xFF21143A)),
    ),
)

val PureHubHeroBrush: Brush
    @Composable get() = Brush.linearGradient(
        listOf(Color(0xFF4C1D95), Color(0xFF7C3AED), Color(0xFF2563EB)),
    )

@Composable
fun MiniAppTab.uiAccent(): Color = when (this) {
    MiniAppTab.ZEN_TIME -> Color(0xFF2DD4BF)
    MiniAppTab.MEASURE_TOOLS -> Color(0xFF60A5FA)
    MiniAppTab.VISION -> Color(0xFFC084FC)
    MiniAppTab.SYSTEM_SECURITY -> Color(0xFF5EEAD4)
    MiniAppTab.FINANCE_FUN -> Color(0xFFFBBF24)
}

@Composable
fun MiniAppTab.uiAccentContainer(): Color = when (this) {
    MiniAppTab.ZEN_TIME -> Color(0xFF143D3A)
    MiniAppTab.MEASURE_TOOLS -> Color(0xFF172E52)
    MiniAppTab.VISION -> Color(0xFF35205C)
    MiniAppTab.SYSTEM_SECURITY -> Color(0xFF143D3A)
    MiniAppTab.FINANCE_FUN -> Color(0xFF493612)
}

fun compactToolTitle(tool: MiniAppId, language: AppLanguage): String = when (tool) {
    MiniAppId.LUNAR_CALENDAR -> appText(language, "Lunar", "Lịch âm", "农历", "Calendario lunar")
    MiniAppId.ZEN_HABIT -> appText(language, "Habits", "Thói quen", "习惯", "Hábitos")
    MiniAppId.ZEN_POMODORO -> appText(language, "Focus", "Tập trung", "专注", "Enfoque")
    MiniAppId.ZEN_BREATH -> appText(language, "Breathe", "Hít thở", "呼吸", "Respirar")
    MiniAppId.COMPASS -> appText(language, "Compass", "La bàn", "指南针", "Brújula")
    MiniAppId.BUBBLE_LEVEL -> appText(language, "Level", "Thước cân", "水平仪", "Nivel")
    MiniAppId.DECIBEL_METER -> appText(language, "Sound Meter", "Đo độ ồn", "分贝仪", "Sonómetro")
    MiniAppId.SMART_FLASHLIGHT -> appText(language, "Flashlight", "Đèn pin", "手电筒", "Linterna")
    MiniAppId.UNIT_CONVERTER -> appText(language, "Converter", "Đổi đơn vị", "单位换算", "Conversor")
    MiniAppId.QR_STUDIO -> appText(language, "QR Scanner", "Quét QR", "扫码", "Escáner QR")
    MiniAppId.DOC_TO_PDF -> appText(language, "PDF Scan", "Quét PDF", "PDF 扫描", "Escáner PDF")
    MiniAppId.OCR_TEXT -> "OCR"
    MiniAppId.COLOR_GRABBER -> appText(language, "Color Picker", "Lấy màu", "取色器", "Selector de color")
    MiniAppId.PHOTO_PRIVACY -> appText(language, "Photo Clean", "Xóa metadata", "照片清理", "Limpiar foto")
    MiniAppId.DEEP_CLEANER -> appText(language, "Cleaner", "Dọn bộ nhớ", "清理", "Limpieza")
    MiniAppId.SPEAKER_CLEANER -> appText(language, "Speaker", "Làm sạch loa", "扬声器", "Altavoz")
    MiniAppId.WIFI_ANALYZER -> "Wi-Fi"
    MiniAppId.PASSWORD_VAULT -> appText(language, "Passwords", "Mật khẩu", "密码", "Contraseñas")
    MiniAppId.AUTHENTICATOR_VAULT -> "2FA"
    MiniAppId.FILE_STUDIO -> appText(language, "Files", "Tệp", "文件", "Archivos")
    MiniAppId.WALLPAPER_CHANGER -> appText(language, "Wallpaper", "Hình nền", "壁纸", "Fondo")
    MiniAppId.BILL_SPLITTER -> appText(language, "Split Bill", "Chia hóa đơn", "分账", "Dividir cuenta")
    MiniAppId.EXPENSE_TRACKER -> appText(language, "Expenses", "Chi tiêu", "支出", "Gastos")
    MiniAppId.DECISION_WHEEL -> appText(language, "Wheel", "Vòng quay", "转盘", "Ruleta")
    MiniAppId.COMMUNITY_UNLOCK -> appText(language, "Community", "Cộng đồng", "社区", "Comunidad")
    MiniAppId.SCREEN_RECORDER -> appText(language, "Recorder", "Quay màn hình", "录屏", "Grabador")
}

