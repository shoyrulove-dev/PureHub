package com.purehub.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val TELEGRAM_DEEP_LINK = "tg://resolve?domain=aaa_letan_vip_bot"
private const val TELEGRAM_WEB_LINK = "https://t.me/aaa_letan_vip_bot"
private const val GITHUB_LINK = "https://github.com/shoyrulove-dev/PureHub"

@Composable
fun CommunityScreen(
    innerPadding: PaddingValues,
    embedded: Boolean = false,
) {
    val context = LocalContext.current

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
            Text(
                text = "COMMUNITY BUILT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "PureHub belongs to everyone",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "No Pro code, no paywall and no ads. Telegram connects people; GitHub keeps the product transparent and open.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Telegram community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Get updates, discuss useful tools and help other PureHub users.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(TELEGRAM_DEEP_LINK, TELEGRAM_WEB_LINK) },
                ) {
                    Text("Open Telegram")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Open-source on GitHub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Read the code, report bugs, suggest a mini app, improve translations or submit a pull request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openUri(GITHUB_LINK) },
                ) {
                    Text("Open GitHub")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Free for everyone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Support is always voluntary. Community badges may celebrate contributors, but core tools remain available to every user.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
