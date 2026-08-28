package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.appText

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onOpenHelp: () -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            LocalizedText(
                text = " " + appText(language, "Settings", "Cài đặt", "设置"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenHelp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = appText(language, "Help", "Trợ giúp", "帮助"),
                )
            }
        }
        LocalizedText(
            text = appText(language, "Choose which tools appear in your catalog. Your preference stays on this device.", "Chọn công cụ hiển thị trong danh mục. Lựa chọn được lưu trên thiết bị này.", "选择在工具目录中显示的工具。选择仅保存在此设备上。"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LocalizedText(appText(language, "Language", "Ngôn ngữ", "语言"), fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { item ->
                        OutlinedButton(onClick = { onLanguageChange(item) }) { LocalizedText("${if (item == language) "✓ " else ""}${item.label}") }
                    }
                }
            }
            PermissionCenterCard()
            ToolVisibilityManagerCard()
            EncryptedBackupCard()
        }
    }
}
