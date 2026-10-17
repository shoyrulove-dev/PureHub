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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.appText
import com.purehub.app.ui.translateUiText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun rememberRecentTools(): List<MiniAppId> {
    val context = LocalContext.current
    val repository = remember { ToolVisibilityRepository(context.applicationContext) }
    val recentIds = repository.recentToolIds.collectAsStateWithLifecycle(initialValue = emptyList())
    return recentIds.value.mapNotNull { id -> MiniAppId.entries.firstOrNull { it.name == id } }
}

@Composable
fun MiniAppIconStrip(
    tab: MiniAppTab,
    visibleTools: Set<MiniAppId>,
    selectedTool: MiniAppId? = null,
    onToolClick: ((MiniAppId) -> Unit)? = null,
) {
    val language = LocalAppLanguage.current
    val tools = miniAppsByTab.getValue(tab).filter { it in visibleTools }
    if (tools.isEmpty()) return

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tools, key = { it.name }) { tool ->
            if (onToolClick == null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = tab.uiAccentContainer(),
                ) {
                    Box(
                        modifier = Modifier.padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = translateUiText(tool.title, language),
                            modifier = Modifier.size(20.dp),
                            tint = tab.uiAccent(),
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (tool == selectedTool) tab.uiAccent() else tab.uiAccentContainer(),
                    onClick = { onToolClick(tool) },
                ) {
                    Box(
                        modifier = Modifier.padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = translateUiText(tool.title, language),
                            tint = if (tool == selectedTool) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                tab.uiAccent()
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
    LocalizedText(
        text = "No tools enabled",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ToolVisibilityManagerCard() {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val repository = remember { ToolVisibilityRepository(context.applicationContext) }
    val hiddenIds = repository.hiddenToolIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val favoriteIds = repository.favoriteToolIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF241A3A),
            contentColor = Color(0xFFF7F2FF),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LocalizedText(
                    text = appText(language, "Manage tools", "Quản lý công cụ", "管理工具", "Gestionar herramientas"),
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
                        contentDescription = appText(language, "Reset layout", "Đặt lại bố cục", "重置布局", "Restablecer diseño"),
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
                placeholder = { LocalizedText(appText(language, "Search tools", "Tìm công cụ", "搜索工具", "Buscar herramientas")) },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFF1EAFE),
                    focusedBorderColor = Color(0xFFC084FC),
                    unfocusedBorderColor = Color(0xFF8B78AD),
                    focusedLeadingIconColor = Color(0xFFC084FC),
                    unfocusedLeadingIconColor = Color(0xFFD8C9EF),
                    focusedPlaceholderColor = Color(0xFFBFAFD8),
                    unfocusedPlaceholderColor = Color(0xFFBFAFD8),
                    cursorColor = Color(0xFFC084FC),
                ),
            )

            MiniAppTab.entries.forEach { tab ->
                val filteredTools = miniAppsByTab.getValue(tab).filter { tool ->
                    query.isBlank() || tool.title.contains(query.trim(), ignoreCase = true)
                }
                if (filteredTools.isEmpty()) return@forEach

                LocalizedText(
                    text = tab.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tab.uiAccent(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTools.forEach { tool ->
                        val visible = tool.name !in hiddenIds.value
                        val favorite = tool.name in favoriteIds.value
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = tab.uiAccentContainer(),
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
                                    contentDescription = translateUiText(tool.title, language),
                                    modifier = Modifier.size(20.dp),
                                    tint = tab.uiAccent(),
                                )
                                LocalizedText(
                                    text = compactToolTitle(tool, language),
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
                                            contentDescription = if (favorite) {
                                                appText(language, "Unpin from Home", "Bỏ ghim khỏi Trang chủ", "从主页取消固定", "Desfijar de Inicio")
                                            } else {
                                                appText(language, "Pin to Home", "Ghim vào Trang chủ", "固定到主页", "Fijar en Inicio")
                                            },
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
                                        contentDescription = if (visible) {
                                            appText(language, "Hide tool", "Ẩn công cụ", "隐藏工具", "Ocultar herramienta")
                                        } else {
                                            appText(language, "Show tool", "Hiện công cụ", "显示工具", "Mostrar herramienta")
                                        },
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
