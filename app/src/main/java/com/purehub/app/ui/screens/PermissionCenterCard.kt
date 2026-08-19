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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private data class PermissionExplanation(val permission: String, val label: String, val reason: String)

@Composable
fun PermissionCenterCard() {
    val context = LocalContext.current
    val permissions = listOf(
        PermissionExplanation(Manifest.permission.CAMERA, "Camera", "QR Studio, document scans and Color Grabber."),
        PermissionExplanation(Manifest.permission.RECORD_AUDIO, "Microphone", "Decibel Meter and Speaker Cleaner."),
        PermissionExplanation(Manifest.permission.ACCESS_FINE_LOCATION, "Nearby Wi-Fi location", "Wi-Fi Analyzer on Android versions that require it."),
        PermissionExplanation(Manifest.permission.POST_NOTIFICATIONS, "Notifications", "An active Pomodoro timer, only when you enable it."),
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
                Text("Permission Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("Permissions are requested only from the tool that needs them. PureHub has no INTERNET permission.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            permissions.forEach { item ->
                val granted = ContextCompat.checkSelfPermission(context, item.permission) == PackageManager.PERMISSION_GRANTED
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.label} · ${if (granted) "Allowed" else "Not allowed"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(item.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                },
            ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); Text(" Open Android app settings") }
        }
    }
}
