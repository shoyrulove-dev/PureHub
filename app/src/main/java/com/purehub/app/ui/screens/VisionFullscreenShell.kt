package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.translateUiText
import kotlinx.coroutines.launch

/**
 * Shared immersive shell for every Vision tool. The tool remains the visual focus,
 * while its compact left drawer always offers a deterministic way back Home.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun VisionFullscreenShell(
    title: String,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    drawerActions: @Composable ColumnScope.(closeDrawer: () -> Unit) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val menuDescription = translateUiText("Vision menu", LocalAppLanguage.current)

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            // Match the compact OCR drawer: it deliberately occupies only half
            // of the handset, leaving the active camera/tool visible behind it.
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.5f)) {
                Column(Modifier.padding(16.dp)) {
                    LocalizedText(title, style = MaterialTheme.typography.titleSmall)
                    LocalizedText("Vision workspace", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NavigationDrawerItem(
                        label = { LocalizedText("Home") },
                        selected = false,
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        onClick = {
                            scope.launch { drawerState.close() }
                            onHome()
                        },
                        modifier = Modifier.padding(top = 14.dp).height(48.dp),
                    )
                    drawerActions { scope.launch { drawerState.close() } }
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            content(PaddingValues(start = 12.dp, top = 76.dp, end = 12.dp, bottom = 12.dp))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Rounded.Menu, contentDescription = menuDescription)
                }
            }
        }
    }
}
