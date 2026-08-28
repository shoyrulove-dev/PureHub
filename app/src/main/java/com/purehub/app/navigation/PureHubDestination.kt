package com.purehub.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.appText

sealed class PureHubDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : PureHubDestination("home", "Home", Icons.Rounded.Home)
    data object AllTools : PureHubDestination("all_tools", "Tools", Icons.Rounded.Widgets)
    data object Community : PureHubDestination("community", "Community", Icons.Rounded.Groups)
    data object Settings : PureHubDestination("settings", "Settings", Icons.Rounded.Tune)

    data object ZenTime : PureHubDestination("zen_time", "Zen", Icons.Rounded.AutoAwesome)
    data object MeasureTools : PureHubDestination("measure_tools", "Measure", Icons.Rounded.Tune)
    data object Vision : PureHubDestination("vision", "Vision", Icons.Rounded.CameraAlt)
    data object SystemSecurity : PureHubDestination("system_security", "Security", Icons.Rounded.Security)
    data object FinanceFun : PureHubDestination("finance_fun", "Finance", Icons.Rounded.CreditCard)
    data object Help : PureHubDestination("help", "Help", Icons.Rounded.AutoAwesome)
}

val bottomNavDestinations = listOf(
    PureHubDestination.Home,
    PureHubDestination.AllTools,
    PureHubDestination.Community,
    PureHubDestination.Settings,
)

fun PureHubDestination.labelFor(language: AppLanguage): String = when (this) {
    PureHubDestination.Home -> appText(language, "Home", "Trang chủ", "主页")
    PureHubDestination.AllTools -> appText(language, "Tools", "Công cụ", "工具")
    PureHubDestination.Community -> appText(language, "Community", "Cộng đồng", "社区")
    PureHubDestination.Settings -> appText(language, "Settings", "Cài đặt", "设置")
    PureHubDestination.ZenTime -> appText(language, "Zen", "Thư giãn", "禅意")
    PureHubDestination.MeasureTools -> appText(language, "Measure", "Đo lường", "测量")
    PureHubDestination.Vision -> appText(language, "Vision", "Quét", "扫描")
    PureHubDestination.SystemSecurity -> appText(language, "Security", "Bảo mật", "安全")
    PureHubDestination.FinanceFun -> appText(language, "Finance", "Tài chính", "财务")
    PureHubDestination.Help -> appText(language, "Help", "Trợ giúp", "帮助")
}
