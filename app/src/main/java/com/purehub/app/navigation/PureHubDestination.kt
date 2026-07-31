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
