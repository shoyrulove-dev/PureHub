package com.purehub.app.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private data class TotpAccount(val id: Long, val label: String, val secret: String)

private class AuthenticatorStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "authenticator_vault",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): List<TotpAccount> = runCatching {
        val array = JSONArray(preferences.getString("accounts", "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TotpAccount(item.getLong("id"), item.getString("label"), item.getString("secret"))
        }
    }.getOrDefault(emptyList())

    fun save(accounts: List<TotpAccount>) {
        val array = JSONArray()
        accounts.forEach { account -> array.put(JSONObject().put("id", account.id).put("label", account.label).put("secret", account.secret)) }
        preferences.edit().putString("accounts", array.toString()).apply()
    }
}

private fun base32(value: String): ByteArray {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    var buffer = 0
    var bits = 0
    val output = ArrayList<Byte>()
    value.uppercase().filter { it in alphabet }.forEach { character ->
        buffer = (buffer shl 5) or alphabet.indexOf(character)
        bits += 5
        if (bits >= 8) {
            bits -= 8
            output += ((buffer shr bits) and 0xff).toByte()
        }
    }
    return output.toByteArray()
}

private fun totp(secret: String, now: Long = System.currentTimeMillis()): String = runCatching {
    val counter = now / 30_000
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(SecretKeySpec(base32(secret), "HmacSHA1"))
    val digest = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
    val offset = digest.last().toInt() and 0x0f
    val binary = ((digest[offset].toInt() and 0x7f) shl 24) or
        ((digest[offset + 1].toInt() and 0xff) shl 16) or
        ((digest[offset + 2].toInt() and 0xff) shl 8) or
        (digest[offset + 3].toInt() and 0xff)
    (binary % 1_000_000).toString().padStart(6, '0')
}.getOrDefault("------")

private fun parseTotpSecret(value: String): String = runCatching {
    if (value.startsWith("otpauth://", true)) android.net.Uri.parse(value).getQueryParameter("secret").orEmpty() else value
}.getOrDefault(value).uppercase().replace(" ", "")

@Composable
fun AuthenticatorVaultCard() {
    val context = LocalContext.current
    val store = remember { AuthenticatorStore(context.applicationContext) }
    var accounts by remember { mutableStateOf(store.load()) }
    var unlocked by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }
    val activity = context as? Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(unlocked) { while (unlocked) { tick += 1; delay(1_000) } }
    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> unlocked = result.resultCode == Activity.RESULT_OK }
    val unlock = {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Authenticator Vault", "Confirm your device lock to view 2FA codes")
        if (intent == null) unlocked = true else unlockLauncher.launch(intent)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlagshipSuiteHeader("Security flagship", "Authenticator Vault", "Offline TOTP codes protected by Android-backed encrypted storage and your device lock.")
            if (!unlocked) {
                Button(onClick = unlock) { Text("Unlock with device security") }
                Text("No account or network connection is used.", style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${30 - (System.currentTimeMillis() / 1_000 % 30)}s", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { unlocked = false }) { Text("Lock") }
            }
            accounts.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) { Text(item.label, fontWeight = FontWeight.SemiBold); Text("Stored only on this device", style = MaterialTheme.typography.bodySmall) }
                        Text(totp(item.secret), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        IconButton(onClick = { accounts = accounts.filterNot { it.id == item.id }; store.save(accounts) }) { Text("×") }
                    }
                }
            }
            OutlinedTextField(label = { Text("Account label") }, value = label, onValueChange = { label = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(label = { Text("Base32 secret or otpauth:// URI") }, value = secret, onValueChange = { secret = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = {
                val normalized = parseTotpSecret(secret)
                if (normalized.isNotBlank() && totp(normalized) != "------") {
                    accounts = accounts + TotpAccount(System.currentTimeMillis(), label.ifBlank { "Authenticator" }, normalized)
                    store.save(accounts); label = ""; secret = ""
                }
            }, enabled = secret.isNotBlank()) { Text("Encrypt & add") }
            Text("Keep independent recovery codes. Deleted entries cannot be restored by PureHub.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
