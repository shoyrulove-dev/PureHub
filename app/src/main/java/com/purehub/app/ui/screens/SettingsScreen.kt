package com.purehub.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.appText
import com.purehub.app.BuildConfig
import androidx.core.content.ContextCompat

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onOpenHelp: () -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onJoinPlayTesters: () -> Unit,
) {
    val context = LocalContext.current
    val diagnostics = buildLocalDiagnosticReport(context, language)
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
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics { selected = item == language },
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF24143F),
                contentColor = Color.White,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color(0xFFD8B4FE),
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            LocalizedText(
                                appText(language, "App information", "Thông tin ứng dụng", "应用信息", "Información de la app"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            LocalizedText(
                                "PureHub ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                color = Color(0xFFD8B4FE),
                            )
                        }
                    }
                    LocalizedText(
                        appText(
                            language,
                            "A report includes only the app version, phone model, Android version and selected language.",
                            "Báo cáo chỉ gồm phiên bản app, mẫu điện thoại, phiên bản Android và ngôn ngữ đã chọn.",
                            "报告仅包含应用版本、手机型号、Android 版本和所选语言。",
                            "El informe solo incluye la versión de la app, el modelo, Android y el idioma seleccionado.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = .76f),
                    )
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PureHub diagnostics", diagnostics))
                            Toast.makeText(
                                context,
                                appText(language, "Diagnostics copied", "Đã sao chép chẩn đoán", "诊断信息已复制", "Diagnóstico copiado"),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                        LocalizedText(" " + appText(language, "Copy diagnostics", "Sao chép chẩn đoán", "复制诊断信息", "Copiar diagnóstico"))
                    }
                    Button(
                        onClick = {
                            val title = "PureHub ${BuildConfig.VERSION_NAME} issue"
                            val body = "$diagnostics\n\n## What happened\n\n## Steps to reproduce\n\nDo not attach private files, passwords, QR contents, scans, or API keys."
                            val issueUrl = Uri.parse("https://github.com/shoyrulove-dev/PureHub/issues/new").buildUpon()
                                .appendQueryParameter("title", title)
                                .appendQueryParameter("body", body)
                                .build()
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, issueUrl).addCategory(Intent.CATEGORY_BROWSABLE))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                    ) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(20.dp))
                        LocalizedText(" " + appText(language, "Report a problem", "Báo lỗi", "报告问题", "Informar un problema"))
                    }
                }
            }
            EncryptedBackupCard()
            ToolVisibilityManagerCard()
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

internal fun buildLocalDiagnosticReport(context: Context, language: AppLanguage): String {
    val permissionStates = listOf(
        "camera" to Manifest.permission.CAMERA,
        "microphone" to Manifest.permission.RECORD_AUDIO,
        "location" to Manifest.permission.ACCESS_FINE_LOCATION,
        "notifications" to Manifest.permission.POST_NOTIFICATIONS,
    ).joinToString(", ") { (label, permission) ->
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        "$label=${if (granted) "on" else "off"}"
    }
    return buildString {
        appendLine("## Environment")
        appendLine("- PureHub: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("- Language: ${language.code}")
        append("- Permissions: $permissionStates")
    }
}
