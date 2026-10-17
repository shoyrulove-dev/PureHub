package com.purehub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.appText

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onOpenHelp: () -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onJoinPlayTesters: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pureHubCanvas()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureHubHeroBrush, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = Color(0xFFFFD166),
            )
            LocalizedText(
                text = " " + appText(language, "Settings", "Cài đặt", "设置", "Ajustes"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenHelp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = appText(language, "Help", "Trợ giúp", "帮助", "Ayuda"),
                    tint = Color.White,
                )
            }
        }
        LocalizedText(
            text = appText(language, "Language, permissions and tools.", "Ngôn ngữ, quyền và công cụ.", "语言、权限和工具。", "Idioma, permisos y herramientas."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp,
            ) {
            Column(modifier = Modifier.padding(14.dp)) {
                LocalizedText(appText(language, "Language", "Ngôn ngữ", "语言", "Idioma"), fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    val languageColors = listOf(Color(0xFF2563EB), Color(0xFFE11D48), Color(0xFFF59E0B), Color(0xFF7C3AED))
                    AppLanguage.entries.forEachIndexed { index, item ->
                        Button(
                            onClick = { onLanguageChange(item) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = languageColors[index].copy(alpha = if (item == language) 1f else .62f),
                                contentColor = Color.White,
                            ),
                        ) { LocalizedText("${if (item == language) "✓ " else ""}${item.compactLabel}", maxLines = 1) }
                    }
                }
            }
            }
            PermissionCenterCard()
            ToolVisibilityManagerCard()
            EncryptedBackupCard()
            Button(
                onClick = {
                    val privacyUrl = "https://hub.blissbiovn.com/${language.code}/privacy"
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)).addCategory(Intent.CATEGORY_BROWSABLE),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                LocalizedText(appText(language, "Privacy Policy", "Chính sách quyền riêng tư", "隐私政策", "Política de privacidad"))
            }
            Button(
                onClick = onJoinPlayTesters,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
            ) {
                LocalizedText("🧪 " + appText(language, "Join Google Play testers", "Tham gia Tester CH Play", "加入 Google Play 测试", "Unirse a probadores de Google Play"))
            }
        }
    }
}
