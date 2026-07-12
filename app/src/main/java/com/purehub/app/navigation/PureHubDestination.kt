package com.purehub.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class PureHubDestination(
    val route: String,
    val icon: ImageVector,
) {
    data object ZenTime : PureHubDestination("zen_time", Icons.Rounded.AutoAwesome)
    data object MeasureTools : PureHubDestination("measure_tools", Icons.Rounded.Tune)
    data object Vision : PureHubDestination("vision", Icons.Rounded.CameraAlt)
    data object SystemSecurity : PureHubDestination("system_security", Icons.Rounded.Security)
    data object FinanceFun : PureHubDestination("finance_fun", Icons.Rounded.CreditCard)
    data object Settings : PureHubDestination("settings", Icons.Rounded.Tune)
    data object Help : PureHubDestination("help", Icons.Rounded.AutoAwesome)
}

val bottomNavDestinations = listOf(
    PureHubDestination.ZenTime,
    PureHubDestination.MeasureTools,
    PureHubDestination.Vision,
    PureHubDestination.SystemSecurity,
    PureHubDestination.FinanceFun,
)
