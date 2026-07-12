package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.catalog.MiniAppTab
import com.purehub.app.feature.lunar.LunarCalendarConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ZenTimeScreen(
    innerPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenMiniApp: (com.purehub.app.feature.catalog.MiniAppId) -> Unit,
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale.getDefault())
    val lunarDescription = LunarCalendarConverter.describeDate(today)
    val visibleTools = rememberVisibleTools()
    val favoriteTools = rememberFavoriteTools()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = today.format(formatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = lunarDescription.lunarDate.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = onOpenSettings,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Tools",
                    )
                }
            }
        }

        HomeQuickShortcuts(
            visibleTools = visibleTools,
            favoriteTools = favoriteTools,
            onOpenTool = onOpenMiniApp,
        )

        MiniAppIconStrip(
            tab = MiniAppTab.ZEN_TIME,
            visibleTools = visibleTools,
            onToolClick = onOpenMiniApp,
        )
    }
}
