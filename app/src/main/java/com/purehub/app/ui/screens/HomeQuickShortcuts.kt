package com.purehub.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.catalog.MiniAppId

@Composable
fun HomeQuickShortcuts(
    visibleTools: Set<MiniAppId>,
    favoriteTools: Set<MiniAppId>,
    onOpenTool: (MiniAppId) -> Unit,
) {
    val pinnedVisible = favoriteTools.filter { it in visibleTools }
    val fallbackVisible = visibleTools.filterNot { it in favoriteTools }
    val quickTools = (pinnedVisible + fallbackVisible).take(6)

    if (quickTools.isEmpty()) {
        LocalizedText(
            text = "No visible mini-apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        quickTools.forEach { tool ->
            Surface(
                modifier = Modifier
                    .widthIn(min = 132.dp)
                    .clickable { onOpenTool(tool) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    LocalizedText(
                        text = tool.title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
