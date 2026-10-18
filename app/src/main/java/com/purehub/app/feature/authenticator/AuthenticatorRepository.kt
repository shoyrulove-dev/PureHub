package com.purehub.app.feature.authenticator

import android.content.Context
import com.purehub.app.security.KeystoreStringStore
import com.purehub.app.security.legacyEncryptedStrings
import org.json.JSONArray
import org.json.JSONObject

data class TotpAccount(
    val id: Long,
    val label: String,
    val secret: String,
    val group: String = "Personal",
)

class AuthenticatorRepository(context: Context) {
    private val preferences = KeystoreStringStore(context, "authenticator_vault") {
        legacyEncryptedStrings(context, "authenticator_vault", setOf("accounts"))
    }

    fun load(): List<TotpAccount> = AuthenticatorCodec.decode(preferences.getString("accounts", "[]").orEmpty())

    fun save(accounts: List<TotpAccount>) = preferences.putStrings(mapOf("accounts" to AuthenticatorCodec.encode(accounts)))
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
