package com.purehub.app.feature.vault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

data class VaultEntry(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
)

class PasswordVaultRepository(
    context: Context,
) {
    @Suppress("DEPRECATION")
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "purehub_vault",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun loadEntries(): List<VaultEntry> {
        val raw = sharedPreferences.getString(KEY_ENTRIES, "[]").orEmpty()
        return VaultEntryCodec.decode(raw)
    }

    fun saveEntries(entries: List<VaultEntry>) {
        check(sharedPreferences.edit().putString(KEY_ENTRIES, VaultEntryCodec.encode(entries)).commit()) {
            "Encrypted vault storage could not be committed."
        }
    }

    companion object {
        private const val KEY_ENTRIES = "entries"
    }
}

internal object VaultEntryCodec {
    private const val MAX_ENTRIES = 100
    private const val MAX_TITLE_LENGTH = 120
    private const val MAX_USERNAME_LENGTH = 320
    private const val MAX_SECRET_LENGTH = 4096

    fun decode(raw: String): List<VaultEntry> = runCatching {
        val jsonArray = JSONArray(raw)
        buildList {
            for (index in 0 until minOf(jsonArray.length(), MAX_ENTRIES)) {
                val item = jsonArray.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val title = item.optString("title").take(MAX_TITLE_LENGTH).takeIf { it.isNotBlank() } ?: continue
                val password = item.optString("password").take(MAX_SECRET_LENGTH).takeIf { it.isNotEmpty() } ?: continue
                add(VaultEntry(id, title, item.optString("username").take(MAX_USERNAME_LENGTH), password))
            }
        }
    }.getOrElse { emptyList() }

    fun encode(entries: List<VaultEntry>): String = JSONArray().apply {
        entries.take(MAX_ENTRIES).forEach { entry ->
            put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title.take(MAX_TITLE_LENGTH))
                    .put("username", entry.username.take(MAX_USERNAME_LENGTH))
                    .put("password", entry.password.take(MAX_SECRET_LENGTH)),
            )
        }
    }.toString()
}
