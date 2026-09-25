package com.purehub.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.appText

const val PLAY_TESTER_GROUP_URL = "https://groups.google.com/g/purehub-testers"

private const val TESTER_PREFS = "purehub.play-testers.v1"
private const val TESTER_PROMPT_DISMISSED = "prompt_dismissed"

fun Context.shouldShowPlayTesterInvite(): Boolean =
    !getSharedPreferences(TESTER_PREFS, Context.MODE_PRIVATE).getBoolean(TESTER_PROMPT_DISMISSED, false)

fun Context.dismissPlayTesterInvite() {
    getSharedPreferences(TESTER_PREFS, Context.MODE_PRIVATE).edit().putBoolean(TESTER_PROMPT_DISMISSED, true).apply()
}

fun Context.openPlayTesterGroup() {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_TESTER_GROUP_URL)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayTesterInviteSheet(
    language: AppLanguage,
    onJoin: () -> Unit,
    onLater: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onLater) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LocalizedText("🧪", style = MaterialTheme.typography.headlineMedium)
            LocalizedText(
                appText(
                    language,
                    "Help test PureHub on Google Play",
                    "Tham gia thử nghiệm PureHub trên CH Play",
                    "加入 PureHub Google Play 测试",
                    "Ayuda a probar PureHub en Google Play",
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            LocalizedText(
                appText(
                    language,
                    "We are preparing the closed test required before launch. Join the tester group to receive the Play opt-in link when it is ready.",
                    "PureHub đang chuẩn bị closed test trước khi phát hành. Hãy vào nhóm tester để nhận link opt-in CH Play khi sẵn sàng.",
                    "PureHub 正在准备正式发布前所需的封闭测试。加入测试群，准备好后即可收到 Google Play 加入链接。",
                    "Estamos preparando la prueba cerrada antes del lanzamiento. Únete al grupo para recibir el enlace de acceso de Google Play cuando esté listo.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                LocalizedText(appText(language, "Join tester group", "Tham gia nhóm tester", "加入测试群", "Unirme al grupo de prueba"))
            }
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                LocalizedText(appText(language, "Later", "Để sau", "以后再说", "Más tarde"))
            }
        }
    }
}
