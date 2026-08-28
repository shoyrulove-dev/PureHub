package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class SuiteMode(val label: String) {
    QUICK("Quick"),
    PRO("Pro"),
}

@Composable
internal fun SuiteModeSwitch(
    mode: SuiteMode,
    onModeChanged: (SuiteMode) -> Unit,
    proHint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuiteMode.entries.forEach { value ->
                FilterChip(
                    selected = mode == value,
                    onClick = { onModeChanged(value) },
                    label = { LocalizedText(value.label) },
                    leadingIcon = if (mode == value) {
                        { Icon(Icons.Rounded.CheckCircle, contentDescription = null) }
                    } else null,
                )
            }
        }
        LocalizedText(
            text = if (mode == SuiteMode.QUICK) "The essentials, ready immediately." else proHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PrivacyReceipt(action: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .52f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                LocalizedText(action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LocalizedText(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
