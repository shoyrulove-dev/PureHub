package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.feature.catalog.MiniAppTab

@Composable
fun ToolsScreen(
    innerPadding: PaddingValues,
    onOpenMiniApp: (MiniAppId) -> Unit,
) {
    val visibleTools = rememberVisibleTools()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniAppIconStrip(
            tab = MiniAppTab.MEASURE_TOOLS,
            visibleTools = visibleTools,
            onToolClick = onOpenMiniApp,
        )
    }
}
