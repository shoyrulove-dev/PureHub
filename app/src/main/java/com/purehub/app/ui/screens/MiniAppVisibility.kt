package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purehub.app.data.ToolVisibilityRepository
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.feature.catalog.MiniAppTab
import com.purehub.app.feature.catalog.miniAppsByTab
import kotlinx.coroutines.launch

@Composable
fun rememberVisibleTools(): Set<MiniAppId> {
    val context = LocalContext.current
    val repository = remember { ToolVisibilityRepository(context.applicationContext) }
    val hiddenIds = repository.hiddenToolIds.collectAsStateWithLifecycle(initialValue = emptySet())
    return MiniAppId.entries.filterTo(mutableSetOf()) { it.name !in hiddenIds.value }
}

@Composable
fun rememberFavoriteTools(): Set<MiniAppId> {
    val context = LocalContext.current
    val repository = remember { ToolVisibilityRepository(context.applicationContext) }
    val favoriteIds = repository.favoriteToolIds.collectAsStateWithLifecycle(initialValue = emptySet())
    return MiniAppId.entries.filterTo(mutableSetOf()) { it.name in favoriteIds.value }
}

@Composable
fun MiniAppIconStrip(
    tab: MiniAppTab,
    visibleTools: Set<MiniAppId>,
    selectedTool: MiniAppId? = null,
    onToolClick: ((MiniAppId) -> Unit)? = null,
) {
    val tools = miniAppsByTab.getValue(tab).filter { it in visibleTools }
    if (tools.isEmpty()) return

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tools, key = { it.name }) { tool ->
            if (onToolClick == null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = tab.accentContainer,
                ) {
                    Box(
                        modifier = Modifier.padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            modifier = Modifier.size(20.dp),
                            tint = tab.accent,
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (tool == selectedTool) tab.accent else tab.accentContainer,
                    onClick = { onToolClick(tool) },
                ) {
                    Box(
                        modifier = Modifier.padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = if (tool == selectedTool) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                tab.accent
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniAppEmptyState() {
    Text(
        text = "No tools enabled",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ToolVisibilityManagerCard() {
    val context = LocalContext.current
    val repository = remember { ToolVisibilityRepository(context.applicationContext) }
    val hiddenIds = repository.hiddenToolIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val favoriteIds = repository.favoriteToolIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Manage Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            repository.resetLayout()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset layout",
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                placeholder = { Text("Search mini-app") },
            )

            MiniAppTab.entries.forEach { tab ->
                val filteredTools = miniAppsByTab.getValue(tab).filter { tool ->
                    query.isBlank() || tool.title.contains(query.trim(), ignoreCase = true)
                }
                if (filteredTools.isEmpty()) return@forEach

                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tab.accent,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTools.forEach { tool ->
                        val visible = tool.name !in hiddenIds.value
                        val favorite = tool.name in favoriteIds.value
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = tool.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Box {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.setToolFavorite(tool.name, !favorite)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PushPin,
                                            contentDescription = if (favorite) "Unpin from Home" else "Pin to Home",
                                            tint = if (favorite) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.setToolVisible(tool.name, !visible)
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (visible) {
                                            Icons.Outlined.Visibility
                                        } else {
                                            Icons.Outlined.VisibilityOff
                                        },
                                        contentDescription = if (visible) "Hide tool" else "Show tool",
                                        tint = if (visible) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
