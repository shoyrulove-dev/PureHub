package com.purehub.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.translateUiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.purehub.app.feature.catalog.MiniAppId

@Composable
fun HomeQuickShortcuts(
    visibleTools: Set<MiniAppId>,
    favoriteTools: Set<MiniAppId>,
    recentTools: List<MiniAppId> = emptyList(),
    onOpenTool: (MiniAppId) -> Unit,
    limit: Int = 6,
) {
    val language = LocalAppLanguage.current
    val pinnedVisible = favoriteTools.filter { it in visibleTools }
    val recentVisible = recentTools.filter { it in visibleTools && it !in favoriteTools }
    val fallbackVisible = visibleTools.filterNot { it in favoriteTools || it in recentVisible }
    val quickTools = (pinnedVisible + recentVisible + fallbackVisible).take(limit)

    if (quickTools.isEmpty()) {
        LocalizedText(
            text = "No visible mini-apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        quickTools.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { index, tool ->
                    val colors = when ((quickTools.indexOf(tool) + index) % 4) {
                        0 -> listOf(Color(0xFF4C1D95), Color(0xFF7C3AED))
                        1 -> listOf(Color(0xFF5B21B6), Color(0xFF2563EB))
                        2 -> listOf(Color(0xFF6D28D9), Color(0xFF9333EA))
                        else -> listOf(Color(0xFF4338CA), Color(0xFF0E7490))
                    }
                    Surface(modifier = Modifier.weight(1f).heightIn(min = 76.dp)
                        .clickable { onOpenTool(tool) }, shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
                        Row(
                            modifier = Modifier.background(Brush.linearGradient(colors)).padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(tool.icon, null, Modifier.size(24.dp), tint = Color(0xFFE9D5FF))
                            LocalizedText(compactToolTitle(tool, LocalAppLanguage.current), style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 2)
                        }
                    }
                }
                if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }
}
