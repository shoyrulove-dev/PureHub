@file:Suppress("DEPRECATION")

package com.purehub.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small encrypted string store backed directly by Android Keystore AES-GCM. */
class KeystoreStringStore(
    context: Context,
    private val namespace: String,
    legacyValues: (() -> Map<String, String>)? = null,
) {
    private val preferences = context.getSharedPreferences("$namespace.keystore.v2", Context.MODE_PRIVATE)
    private val keyAlias = "purehub.$namespace.aes.v2"

    init {
        if (!preferences.getBoolean(MIGRATED, false)) {
            // A legacy read failure must abort migration so the next launch can retry;
            // marking an empty migration as complete could hide existing user data.
            val values = legacyValues?.invoke().orEmpty()
            val editor = preferences.edit()
            values.forEach { (key, value) -> editor.putString(key, encrypt(value)) }
            check(editor.putBoolean(MIGRATED, true).commit()) { "Could not migrate $namespace secure storage." }
        }
    }

    fun getString(key: String, fallback: String? = null): String? = preferences.getString(key, null)
        ?.let(::decrypt)
        ?: fallback

    fun putStrings(values: Map<String, String>) {
        val editor = preferences.edit()
        values.forEach { (key, value) -> editor.putString(key, encrypt(value)) }
        check(editor.commit()) { "Could not commit $namespace secure storage." }
    }

    fun replace(values: Map<String, String>) {
        val editor = preferences.edit().clear().putBoolean(MIGRATED, true)
        values.forEach { (key, value) -> editor.putString(key, encrypt(value)) }
        check(editor.commit()) { "Could not replace $namespace secure storage." }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(payload)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.getDecoder().decode(value)
        require(payload.size > IV_BYTES) { "Invalid encrypted payload." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload.copyOfRange(0, IV_BYTES)))
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size)).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        synchronized(KEY_LOCK) {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }
        }
    }

    companion object {
        private const val MIGRATED = "__migrated_v2"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private val KEY_LOCK = Any()
    }
}

/** Reads the former AndroidX encrypted file for a non-destructive one-time migration. */
fun legacyEncryptedStrings(context: Context, fileName: String, keys: Set<String>): Map<String, String> {
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val preferences = EncryptedSharedPreferences.create(
        context,
        fileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    return keys.mapNotNull { key -> preferences.getString(key, null)?.let { key to it } }.toMap()
}
