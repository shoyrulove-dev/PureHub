package com.purehub.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.flashlight.SmartFlashlightViewModel
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

private enum class FlashPanel { None, Menu, Brightness, Timer, Strobe, Sos, Screen, Morse }

private data class FlashCopy(val title: String, val on: String, val off: String, val ready: String, val more: String, val brightness: String, val autoOff: String, val strobe: String, val sos: String, val screen: String, val morse: String, val stop: String, val minutes: String, val screenOn: String, val screenOff: String, val play: String, val permission: String)

@Composable
private fun flashlightCopy(): FlashCopy = when (LocalAppLanguage.current) {
    AppLanguage.Vietnamese -> FlashCopy("Đèn pin", "Bật đèn", "Tắt đèn", "Sẵn sàng", "Thêm", "Độ sáng", "Tự tắt", "Nhấp nháy", "SOS", "Đèn màn hình", "Mã Morse", "Dừng tín hiệu", "phút", "Bật đèn màn hình", "Tắt đèn màn hình", "Phát Morse", "Cần quyền Camera để bật đèn LED.")
    AppLanguage.Chinese -> FlashCopy("手电筒", "打开手电", "关闭手电", "准备就绪", "更多", "亮度", "自动关闭", "频闪", "SOS", "屏幕灯", "摩尔斯码", "停止信号", "分钟", "打开屏幕灯", "关闭屏幕灯", "播放摩尔斯码", "使用 LED 手电需要相机权限。")
    AppLanguage.Spanish -> FlashCopy("Linterna", "Encender", "Apagar", "Lista", "Más", "Brillo", "Apagado automático", "Estroboscopio", "SOS", "Luz de pantalla", "Código Morse", "Detener señal", "min", "Encender pantalla", "Apagar pantalla", "Reproducir Morse", "Se requiere permiso de cámara para la linterna LED.")
    AppLanguage.English -> FlashCopy("Flashlight", "Turn on", "Turn off", "Ready", "More", "Brightness", "Auto-off", "Strobe", "SOS", "Screen light", "Morse", "Stop signal", "min", "Turn screen light on", "Turn screen light off", "Play Morse", "Camera permission is needed for the LED torch.")
}

@Composable
fun SmartFlashlightCard(viewModel: SmartFlashlightViewModel = viewModel()) {
    val context = LocalContext.current
    val copy = flashlightCopy()
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var panel by remember { mutableStateOf(FlashPanel.None) }
    var morse by remember { mutableStateOf("HELP") }
    var screenOn by remember { mutableStateOf(false) }
    var screenBrightness by remember { mutableStateOf(100f) }
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) scope.launch { snackbar.showSnackbar(copy.permission) } }
    fun torch(action: () -> Unit) { if (cameraGranted) action() else permission.launch(Manifest.permission.CAMERA) }

    DisposableEffect(screenOn, screenBrightness) {
        val activity = context as? Activity; val previous = activity?.window?.attributes?.screenBrightness
        if (screenOn && activity != null) { activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); activity.window.attributes = activity.window.attributes.apply { screenBrightness = (screenBrightness / 100f).coerceIn(.05f, 1f) } }
        onDispose { if (activity != null && previous != null) { activity.window.attributes = activity.window.attributes.apply { screenBrightness = previous }; activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) } }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(copy.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(if (state.isTorchOn) copy.on else copy.ready, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { panel = if (panel == FlashPanel.Menu) FlashPanel.None else FlashPanel.Menu }) { Icon(Icons.Rounded.Menu, copy.more) }
            }
            Button(onClick = { torch(viewModel::toggleTorch) }, modifier = Modifier.fillMaxWidth().heightIn(min = 172.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(if (state.isTorchOn) Icons.Rounded.Close else Icons.Rounded.Lightbulb, null, Modifier.size(52.dp)); Text(if (state.isTorchOn) copy.off else copy.on, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { panel = FlashPanel.Brightness }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Bolt, null, Modifier.size(17.dp)); Text("  ${copy.brightness}") }
                OutlinedButton(onClick = { panel = FlashPanel.Timer }, modifier = Modifier.weight(1f)) { Text("${copy.autoOff}: ${state.autoOffMinutes.takeIf { it > 0 } ?: "—"}") }
            }
            if (panel != FlashPanel.None) Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (panel) {
                        FlashPanel.Menu -> listOf(FlashPanel.Strobe to copy.strobe, FlashPanel.Sos to copy.sos, FlashPanel.Screen to copy.screen, FlashPanel.Morse to copy.morse).forEach { (target, label) -> OutlinedButton(onClick = { panel = target }, modifier = Modifier.fillMaxWidth()) { Text(label, Modifier.fillMaxWidth(), textAlign = TextAlign.Start) } }
                        FlashPanel.Brightness -> { Text(copy.brightness, fontWeight = FontWeight.Bold); if (state.maxTorchStrength > 1) Slider(state.torchStrength.toFloat(), { viewModel.setTorchStrength(it.toInt()) }, valueRange = 1f..state.maxTorchStrength.toFloat(), steps = (state.maxTorchStrength - 2).coerceAtLeast(0)) else Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        FlashPanel.Timer -> listOf(0, 1, 5, 15, 30).forEach { minute -> OutlinedButton(onClick = { viewModel.setAutoOffMinutes(minute); panel = FlashPanel.None }, modifier = Modifier.fillMaxWidth()) { Text(if (minute == 0) "—" else "$minute ${copy.minutes}") } }
                        FlashPanel.Strobe -> { Text("${copy.strobe}: ${state.strobeHz} Hz", fontWeight = FontWeight.Bold); Slider(state.strobeHz.toFloat(), { viewModel.setStrobeHz(it.toInt()) }, valueRange = 1f..30f, steps = 28); Button({ torch(viewModel::playStrobe) }, Modifier.fillMaxWidth()) { Text(copy.strobe) } }
                        FlashPanel.Sos -> Button({ torch(viewModel::playSosPattern) }, Modifier.fillMaxWidth()) { Text(copy.sos) }
                        FlashPanel.Screen -> { Button({ screenOn = !screenOn }, Modifier.fillMaxWidth()) { Text(if (screenOn) copy.screenOff else copy.screenOn) }; Text("${copy.brightness}: ${screenBrightness.toInt()}%", fontWeight = FontWeight.Bold); Slider(screenBrightness, { screenBrightness = it }, valueRange = 15f..100f) }
                        FlashPanel.Morse -> { androidx.compose.material3.OutlinedTextField(morse, { morse = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text(copy.morse) }, singleLine = true); Button({ torch { viewModel.playMorsePattern(morse) } }, Modifier.fillMaxWidth(), enabled = morse.isNotBlank()) { Text(copy.play) } }
                        FlashPanel.None -> Unit
                    }
                    if (state.isPatternRunning && panel != FlashPanel.Menu) OutlinedButton(onClick = viewModel::stopPattern, modifier = Modifier.fillMaxWidth()) { Text(copy.stop) }
                }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
