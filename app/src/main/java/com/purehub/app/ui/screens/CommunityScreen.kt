package com.purehub.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.BuildConfig
import com.purehub.app.R
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalSnackbarHostState
import com.purehub.app.ui.appText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import org.json.JSONObject

private const val TELEGRAM_DEEP_LINK = "tg://resolve?domain=PureHubCom"
private const val TELEGRAM_WEB_LINK = "https://t.me/PureHubCom"
private const val GITHUB_LINK = "https://github.com/shoyrulove-dev/PureHub"
private const val GITHUB_FEEDBACK_LINK = "https://github.com/shoyrulove-dev/PureHub/issues/new/choose"
private const val PURE_EARN_LINK = "https://earn.blissbiovn.com/?utm_source=purehub_android&utm_medium=community&utm_campaign=pureearn_launch"
private const val MINIGAME_TICKET_URL = "https://hub.blissbiovn.com/public-api/minigame/ticket"
private const val MINIGAME_PAGE_URL = "https://hub.blissbiovn.com/vi/minigame"

@Composable
fun CommunityScreen(
    innerPadding: PaddingValues,
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    var requestingTicket by remember { mutableStateOf(false) }

    fun openUri(primary: String, fallback: String? = null) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(primary)))
        } catch (_: ActivityNotFoundException) {
            fallback?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pureHubCanvas()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!embedded) {
            Column(
                modifier = Modifier.fillMaxWidth().background(PureHubHeroBrush, RoundedCornerShape(22.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color(0xFFFFD166))
                LocalizedText(appText(language, "Community", "Cộng đồng", "社区", "Comunidad"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                LocalizedText(appText(language, "Connect and get support.", "Kết nối và nhận hỗ trợ.", "交流并获得支持。", "Conecta y recibe ayuda."), color = Color(0xFFE9DDFF))
            }
        }

        if (BuildConfig.MINIGAME_ENABLED) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF172E52), contentColor = Color(0xFFF4F8FF)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                LocalizedText(appText(language, "PureHub Minigame · Vietnam", "Minigame PureHub · Việt Nam", "PureHub 小游戏 · 越南"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LocalizedText(appText(language, "Pick two digits before 18:00. The first 5 correct players receive a 10,000 VND phone card.", "Dự đoán 2 số trước 18:00. 5 người đúng sớm nhất nhận thẻ điện thoại 10.000đ mỗi ngày.", "18:00 前预测两位数字。最早猜对的 5 位用户将获得 10,000 越南盾电话卡。"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(modifier = Modifier.fillMaxWidth(), enabled = !requestingTicket, onClick = {
                    scope.launch {
                        requestingTicket = true
                        val result = withContext(Dispatchers.IO) {
                            requestMinigameTicket(Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty())
                        }
                        requestingTicket = false
                        result.url?.let(::openUri) ?: snackbar.showSnackbar(result.error ?: "Không thể cấp vé tham gia. Hãy thử lại.")
                    }
                }) { LocalizedText(if (requestingTicket) appText(language, "Getting your ticket…", "Đang lấy vé tham gia…", "正在获取参与凭证…") else appText(language, "Pick a number", "Chọn số dự đoán", "选择预测号码")) }
                LocalizedText(appText(language, "For users in Vietnam. Gmail only identifies a beta entry and is not used for marketing.", "Dành cho người dùng Việt Nam. Gmail chỉ dùng nhận diện lượt beta, không dùng marketing.", "仅面向越南用户。Gmail 仅用于识别测试资格，不用于营销。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17102B), contentColor = Color.White),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(painterResource(R.drawable.ic_pure_earn), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(58.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        LocalizedText(appText(language, "Pure Earn", "Pure Earn", "Pure Earn", "Pure Earn"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                        LocalizedText(appText(language, "Rewards community", "Cộng đồng phần thưởng", "奖励社区", "Comunidad de recompensas"), color = Color(0xFFDCCBFF), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                LocalizedText(appText(language,
                    "Pure Earn is separate. PureHub does not share your app data.",
                    "Pure Earn là dịch vụ riêng. PureHub không chia sẻ dữ liệu ứng dụng.",
                    "Pure Earn 是独立服务。PureHub 不会共享应用数据。",
                    "Pure Earn es independiente. PureHub no comparte tus datos."),
                    color = Color(0xFFBEB1D8), style = MaterialTheme.typography.bodySmall)
                Button(modifier = Modifier.fillMaxWidth(), onClick = { openUri(PURE_EARN_LINK) }) {
                    LocalizedText(appText(language, "Open Pure Earn", "Mở Pure Earn", "打开 Pure Earn", "Abrir Pure Earn"))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF172E52), contentColor = Color(0xFFF4F8FF)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                LocalizedText(appText(language, "Telegram community", "Cộng đồng Telegram", "Telegram 社区", "Comunidad de Telegram"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LocalizedText(
                    appText(language, "Updates, discussion and support.", "Tin mới, trao đổi và hỗ trợ.", "更新、交流与支持。", "Novedades, debate y ayuda."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC8D9F5),
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(TELEGRAM_DEEP_LINK, TELEGRAM_WEB_LINK) },
                ) {
                    LocalizedText(appText(language, "Open Telegram", "Mở Telegram", "打开 Telegram", "Abrir Telegram"))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E5A), contentColor = Color(0xFFFFF7FF)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = Color(0xFFF0ABFC))
                LocalizedText(appText(language, "Open source", "Mã nguồn mở", "开源项目", "Código abierto"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LocalizedText(
                    appText(language, "Read the code, report bugs or suggest improvements.", "Xem mã nguồn, báo lỗi hoặc đề xuất cải tiến.", "查看代码、报告问题或提出改进建议。", "Consulta el código, informa errores o propone mejoras."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD5CDE5),
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(GITHUB_LINK) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8B4FE), contentColor = Color(0xFF25103B)),
                ) {
                    LocalizedText(appText(language, "Open GitHub", "Mở GitHub", "打开 GitHub", "Abrir GitHub"))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(GITHUB_FEEDBACK_LINK) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    LocalizedText(appText(language, "Send feedback", "Gửi góp ý", "发送反馈", "Enviar comentarios"))
                }
                LocalizedText(
                    appText(language, "For bugs, include the tool, Android version and steps. Never send private data.", "Khi báo lỗi, ghi tên công cụ, bản Android và các bước. Không gửi dữ liệu riêng tư.", "报告问题时请注明工具、Android 版本和操作步骤。请勿发送隐私数据。", "Para informar un error, indica la herramienta, versión de Android y pasos. No envíes datos privados."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBFB5D3),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF35205C), contentColor = Color(0xFFF8F2FF)),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    LocalizedText(appText(language, "Contribute", "Đóng góp", "参与贡献", "Contribuye"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LocalizedText(
                        appText(language, "Share ideas to improve PureHub.", "Chia sẻ ý tưởng cho PureHub.", "分享建议，改进 PureHub。", "Comparte ideas para mejorar PureHub."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD8C7F2),
                    )
                }
            }
        }
    }
}

private data class MinigameTicketResult(val url: String? = null, val error: String? = null)

private fun requestMinigameTicket(androidId: String): MinigameTicketResult {
    if (androidId.length !in 8..128) return MinigameTicketResult(error = "Không đọc được mã thiết bị Android.")
    return runCatching {
        val connection = URL(MINIGAME_TICKET_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "PureHub-Android/${BuildConfig.VERSION_NAME}")
            val body = JSONObject().put("android_id", androidId).toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val payload = runCatching { JSONObject(response) }.getOrNull()
            if (status !in 200..299) {
                return MinigameTicketResult(error = payload?.optString("detail")?.takeIf(String::isNotBlank) ?: "Máy chủ chưa cấp được vé tham gia ($status).")
            }
            val ticket = payload?.optString("ticket")?.takeIf(String::isNotBlank)
                ?: return MinigameTicketResult(error = "Phản hồi vé tham gia không hợp lệ.")
            val encodedTicket = URLEncoder.encode(ticket, Charsets.UTF_8.name())
            MinigameTicketResult(url = "$MINIGAME_PAGE_URL?ticket=$encodedTicket&source=android-app")
        } finally {
            connection.disconnect()
        }
    }.getOrElse { MinigameTicketResult(error = "Không kết nối được máy chủ minigame. Kiểm tra mạng và thử lại.") }
}
