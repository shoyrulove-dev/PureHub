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
import androidx.compose.material3.OutlinedButton
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.BuildConfig
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

private const val TELEGRAM_DEEP_LINK = "tg://resolve?domain=aaa_letan_vip_bot"
private const val TELEGRAM_WEB_LINK = "https://t.me/aaa_letan_vip_bot"
private const val GITHUB_LINK = "https://github.com/shoyrulove-dev/PureHub"
private const val GITHUB_FEEDBACK_LINK = "https://github.com/shoyrulove-dev/PureHub/issues/new/choose"
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
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!embedded) {
            FlagshipSuiteHeader(
                eyebrow = "Community flagship",
                title = "PureHub belongs to everyone",
                description = "Join the conversation, report issues and shape free, no-ad, open-source tools together.",
            )
        }

        if (BuildConfig.FLAVOR != "fdroid") {
        Card(modifier = Modifier.fillMaxWidth()) {
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                LocalizedText("Telegram community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LocalizedText(
                    "Get updates, discuss useful tools and help other PureHub users.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(TELEGRAM_DEEP_LINK, TELEGRAM_WEB_LINK) },
                ) {
                    LocalizedText("Open Telegram")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                LocalizedText("Open-source on GitHub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LocalizedText(
                    "Read the code, report bugs, suggest a mini app, improve translations or submit a pull request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(GITHUB_LINK) },
                ) {
                    LocalizedText("Open GitHub")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(GITHUB_FEEDBACK_LINK) },
                ) {
                    LocalizedText("Send feedback or report a bug")
                }
                LocalizedText(
                    "For a bug, include the tool name, Android version and repeatable steps—never private files, passwords or API keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    LocalizedText("Free for everyone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LocalizedText(
                        "Support is always voluntary. Community badges may celebrate contributors, but core tools remain available to every user.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
