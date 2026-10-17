package com.purehub.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.appText
import com.purehub.app.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

private data class PermissionExplanation(val permission: String, val label: String, val reason: String)

@Composable
fun PermissionCenterCard() {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val permissions = listOf(
        PermissionExplanation(Manifest.permission.CAMERA, appText(language, "Camera", "Camera", "相机", "Cámara"), appText(language, "Scan QR, documents and colors.", "Quét QR, tài liệu và màu.", "扫描二维码、文档和颜色。", "Escanea QR, documentos y colores.")),
        PermissionExplanation(Manifest.permission.RECORD_AUDIO, appText(language, "Microphone", "Micrô", "麦克风", "Micrófono"), appText(language, "Sound meter and recording audio.", "Đo âm thanh và thu tiếng.", "测量声音和录制音频。", "Mide sonido y graba audio.")),
        PermissionExplanation(Manifest.permission.ACCESS_FINE_LOCATION, appText(language, "Nearby Wi-Fi", "Wi-Fi gần đây", "附近 Wi-Fi", "Wi-Fi cercano"), appText(language, "Find local Wi-Fi networks.", "Tìm mạng Wi-Fi xung quanh.", "查找附近的 Wi-Fi 网络。", "Busca redes Wi-Fi cercanas.")),
        PermissionExplanation(Manifest.permission.POST_NOTIFICATIONS, appText(language, "Notifications", "Thông báo", "通知", "Notificaciones"), appText(language, "Timers, recording and reminders.", "Hẹn giờ, ghi hình và nhắc việc.", "计时器、录制和提醒。", "Temporizadores, grabación y recordatorios.")),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17243E), contentColor = Color(0xFFF5F7FF)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
                LocalizedText(appText(language, "Permissions", "Quyền ứng dụng", "应用权限", "Permisos"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            LocalizedText(
                if (BuildConfig.INTERNET_ENABLED) {
                    appText(language, "Enable only what each tool needs.", "Chỉ bật quyền công cụ cần.", "仅启用工具所需权限。", "Activa solo lo necesario.")
                } else {
                    appText(language, "Permissions are requested when needed.", "Quyền chỉ được hỏi khi cần.", "仅在需要时请求权限。", "Los permisos se piden al usarlos.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC6D3EE),
            )
            permissions.forEach { item ->
                val granted = ContextCompat.checkSelfPermission(context, item.permission) == PackageManager.PERMISSION_GRANTED
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Column(modifier = Modifier.weight(1f)) {
                        val state = if (granted) appText(language, "On", "Bật", "已开启", "Activo") else appText(language, "Off", "Tắt", "已关闭", "Inactivo")
                        LocalizedText("${item.label} · $state", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        LocalizedText(item.reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC6D3EE))
                    }
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                },
            ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); LocalizedText(" " + appText(language, "App settings", "Cài đặt ứng dụng", "应用设置", "Ajustes de la app")) }
        }
    }
}
