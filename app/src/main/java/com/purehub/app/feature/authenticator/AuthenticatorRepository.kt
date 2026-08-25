package com.purehub.app.feature.authenticator

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

data class TotpAccount(
    val id: Long,
    val label: String,
    val secret: String,
    val group: String = "Personal",
)

class AuthenticatorRepository(context: Context) {
    @Suppress("DEPRECATION")
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "authenticator_vault",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): List<TotpAccount> = AuthenticatorCodec.decode(preferences.getString("accounts", "[]").orEmpty())

    fun save(accounts: List<TotpAccount>) {
        check(preferences.edit().putString("accounts", AuthenticatorCodec.encode(accounts)).commit()) {
            "Authenticator vault could not be committed."
        }
    }
}

internal object AuthenticatorCodec {
    fun decode(raw: String): List<TotpAccount> = runCatching {
        val array = JSONArray(raw)
        buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val id = item.optLong("id", -1L).takeIf { it >= 0L } ?: return@repeat
                val label = item.optString("label").take(120).takeIf(String::isNotBlank) ?: return@repeat
                val secret = item.optString("secret").take(512).takeIf(String::isNotBlank) ?: return@repeat
                add(TotpAccount(id, label, secret, item.optString("group", "Personal").take(40).ifBlank { "Personal" }))
            }
        }
    }.getOrDefault(emptyList())

    fun encode(accounts: List<TotpAccount>): String = JSONArray().apply {
        accounts.take(200).forEach { account ->
            put(
                JSONObject()
                    .put("id", account.id)
                    .put("label", account.label.take(120))
                    .put("secret", account.secret.take(512))
                    .put("group", account.group.take(40)),
            )
        }
    }.toString()
}
