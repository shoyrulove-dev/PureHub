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
        val jsonArray = JSONArray(raw)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)
                add(
                    VaultEntry(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        username = item.getString("username"),
                        password = item.getString("password"),
                    ),
                )
            }
        }
    }

    fun saveEntries(entries: List<VaultEntry>) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            jsonArray.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
                    .put("username", entry.username)
                    .put("password", entry.password),
            )
        }
        sharedPreferences.edit().putString(KEY_ENTRIES, jsonArray.toString()).apply()
    }

    companion object {
        private const val KEY_ENTRIES = "entries"
    }
}
