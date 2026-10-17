package com.purehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import android.Manifest
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
    val language = LocalAppLanguage.current
    fun tr(en: String, vi: String, zh: String, es: String) = when (language) {
        AppLanguage.English -> en; AppLanguage.Vietnamese -> vi
        AppLanguage.Chinese -> zh; AppLanguage.Spanish -> es
    }
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var panel by remember { mutableStateOf(FlashPanel.None) }
    var morse by remember { mutableStateOf("HELP") }
    var screenOn by remember { mutableStateOf(false) }
    var screenLevel by remember { mutableStateOf(100f) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAction; pendingAction = null
        if (granted) action?.invoke() else scope.launch { snackbar.showSnackbar(copy.permission) }
    }
    fun torch(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) action()
        else { pendingAction = action; permission.launch(Manifest.permission.CAMERA) }
    }
    LaunchedEffect(screenOn, state.autoOffMinutes) {
        if (screenOn && state.autoOffMinutes > 0) { delay(state.autoOffMinutes * 60_000L); screenOn = false }
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopPattern() } }
    if (screenOn) Dialog(
        onDismissRequest = { screenOn = false },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val lightWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(lightWindow, screenLevel) {
            val previous = lightWindow?.attributes?.screenBrightness
            lightWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lightWindow?.let { it.attributes = it.attributes.apply { screenBrightness = screenLevel / 100f } }
            onDispose { lightWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (previous != null) lightWindow?.let { it.attributes = it.attributes.apply { screenBrightness = previous } }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.White).padding(32.dp), contentAlignment = Alignment.BottomCenter) {
            Button(onClick = { screenOn = false }) { Text(copy.screenOff) }
        }
    }


    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { panel = FlashPanel.Menu }) { Icon(Icons.Rounded.Menu, copy.more) }
                    DropdownMenu(expanded = panel == FlashPanel.Menu, onDismissRequest = { panel = FlashPanel.None },
                        modifier = Modifier.widthIn(max = 240.dp)) {
                        listOf(FlashPanel.Strobe to copy.strobe, FlashPanel.Sos to copy.sos, FlashPanel.Screen to copy.screen, FlashPanel.Morse to copy.morse).forEach { (target, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { panel = target })
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(copy.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(when {
                        state.isPatternRunning -> tr("Signal active", "Đang phát tín hiệu", "正在发送信号", "Señal activa")
                        state.isTorchOn -> tr("Light on", "Đèn đang bật", "灯已打开", "Luz encendida")
                        else -> copy.ready
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

            }
            Button(onClick = { if (state.isPatternRunning) viewModel.stopPattern() else torch(viewModel::toggleTorch) }, modifier = Modifier.fillMaxWidth().heightIn(min = 172.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD580), contentColor = Color(0xFF241536))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(if (state.isTorchOn) Icons.Rounded.Close else Icons.Rounded.Lightbulb, null, Modifier.size(52.dp)); Text(if (state.isPatternRunning) copy.stop else if (state.isTorchOn) copy.off else copy.on, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { panel = FlashPanel.Brightness }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Bolt, null, Modifier.size(17.dp)); Text("  ${copy.brightness}") }
                OutlinedButton(onClick = { panel = FlashPanel.Timer }, modifier = Modifier.weight(1f)) { Text("${tr("Timer", "Hẹn giờ", "定时", "Temporizador")}: ${state.autoOffMinutes.takeIf { it > 0 } ?: "—"}") }
            }
            if (panel != FlashPanel.None && panel != FlashPanel.Menu) Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (panel == FlashPanel.Strobe || panel == FlashPanel.Sos || panel == FlashPanel.Morse) {
                        Text(tr("Flashing light: avoid use near people sensitive to flashes.", "Đèn nhấp nháy: tránh dùng gần người nhạy cảm với ánh sáng.", "闪烁灯光：请勿在对闪光敏感的人附近使用。", "Luz intermitente: evita usarla cerca de personas sensibles a los destellos."), style = MaterialTheme.typography.bodySmall)
                    }
                    when (panel) {
                        FlashPanel.Menu -> listOf(FlashPanel.Strobe to copy.strobe, FlashPanel.Sos to copy.sos, FlashPanel.Screen to copy.screen, FlashPanel.Morse to copy.morse).forEach { (target, label) -> OutlinedButton(onClick = { panel = target }, modifier = Modifier.fillMaxWidth()) { Text(label, Modifier.fillMaxWidth(), textAlign = TextAlign.Start) } }
                        FlashPanel.Brightness -> { Text(copy.brightness, fontWeight = FontWeight.Bold); if (state.maxTorchStrength > 1) Slider(state.torchStrength.toFloat(), { viewModel.setTorchStrength(it.toInt()) }, valueRange = 1f..state.maxTorchStrength.toFloat(), steps = (state.maxTorchStrength - 2).coerceAtLeast(0)) else Text(tr("Brightness adjustment is not supported on this device.", "Máy không hỗ trợ chỉnh độ sáng đèn LED.", "此设备不支持调节手电亮度。", "Este dispositivo no permite ajustar el brillo LED."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        FlashPanel.Timer -> listOf(0, 1, 5, 15, 30).forEach { minute -> OutlinedButton(onClick = { viewModel.setAutoOffMinutes(minute); panel = FlashPanel.None }, modifier = Modifier.fillMaxWidth()) { Text(if (minute == 0) tr("Disabled", "Không hẹn giờ", "不定时", "Sin temporizador") else "$minute ${copy.minutes}") } }
                        FlashPanel.Strobe -> { Text("${copy.strobe}: ${state.strobeHz} Hz", fontWeight = FontWeight.Bold); Slider(state.strobeHz.toFloat(), { viewModel.setStrobeHz(it.toInt()) }, valueRange = 1f..30f, steps = 28); Button({ torch(viewModel::playStrobe) }, Modifier.fillMaxWidth()) { Text(copy.strobe) } }
                        FlashPanel.Sos -> Button({ torch(viewModel::playSosPattern) }, Modifier.fillMaxWidth()) { Text(copy.sos) }
                        FlashPanel.Screen -> { Button({ viewModel.stopPattern(); screenOn = !screenOn }, Modifier.fillMaxWidth()) { Text(if (screenOn) copy.screenOff else copy.screenOn) }; Text("${copy.brightness}: ${screenLevel.toInt()}%", fontWeight = FontWeight.Bold); Slider(screenLevel, { screenLevel = it }, valueRange = 15f..100f) }
                        FlashPanel.Morse -> { androidx.compose.material3.OutlinedTextField(morse, { morse = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text(copy.morse) }, singleLine = true); Button({ torch { viewModel.playMorsePattern(morse) } }, Modifier.fillMaxWidth(), enabled = morse.any { it.uppercaseChar() in 'A'..'Z' }) { Text(copy.play) } }
                        FlashPanel.None -> Unit
                    }
                    if (state.isPatternRunning && panel != FlashPanel.Menu) OutlinedButton(onClick = viewModel::stopPattern, modifier = Modifier.fillMaxWidth()) { Text(copy.stop) }
                }
            }
            state.errorMessage?.let { error ->
                Text(if (error == "No flash unit is available on this device.")
                    tr("No LED flash. Use screen light.", "Máy không có đèn LED. Hãy dùng đèn màn hình.", "没有 LED 闪光灯，请使用屏幕灯。", "Sin flash LED. Usa la luz de pantalla.")
                else tr("Cannot switch the torch on. Check camera permission or close other camera apps.", "Không bật được đèn. Kiểm tra quyền camera hoặc đóng ứng dụng đang dùng camera.", "无法打开手电。请检查相机权限或关闭其他相机应用。", "No se puede encender. Revisa el permiso de cámara o cierra otras apps de cámara."),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
