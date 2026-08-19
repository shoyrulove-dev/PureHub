package com.purehub.app.feature.backup

import android.content.Context
import androidx.room.withTransaction
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.data.local.entity.ExpenseEntryEntity
import com.purehub.app.data.local.entity.HabitCheckInEntity
import com.purehub.app.data.local.entity.HabitEntity
import com.purehub.app.feature.vault.PasswordVaultRepository
import com.purehub.app.feature.vault.VaultEntry
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

class EncryptedBackupManager(private val context: Context) {
    private val database = PureHubDatabaseProvider.get(context)
    private val vault = PasswordVaultRepository(context)

    suspend fun export(passphrase: CharArray): String {
        require(passphrase.size >= 8) { "Use at least 8 characters for the backup passphrase." }
        val payload = JSONObject()
            .put("format", 1)
            .put("createdAt", System.currentTimeMillis())
            .put("habits", JSONArray(database.habitDao().getAllHabits().map(::habitJson)))
            .put("checkIns", JSONArray(database.habitCheckInDao().getAllCheckIns().map(::checkInJson)))
            .put("expenses", JSONArray(database.expenseDao().getAllExpenses().map(::expenseJson)))
            .put("vault", JSONArray(vault.loadEntries().map(::vaultJson)))
            .put("ocrHistory", context.getSharedPreferences("purehub.ocr-studio.v2", 0).getString("history", "[]"))
            .put("qrHistory", context.getSharedPreferences("purehub.qr-studio.v2", 0).getString("history", "[]"))
        return encrypt(payload.toString().toByteArray(Charsets.UTF_8), passphrase)
    }

    suspend fun import(raw: String, passphrase: CharArray) {
        require(passphrase.size >= 8) { "Enter the backup passphrase (at least 8 characters)." }
        val payload = JSONObject(decrypt(raw, passphrase).toString(Charsets.UTF_8))
        require(payload.optInt("format") == 1) { "Unsupported PureHub backup version." }
        val habits = payload.getJSONArray("habits").objects(::habitFromJson)
        val checkIns = payload.getJSONArray("checkIns").objects(::checkInFromJson)
        val expenses = payload.getJSONArray("expenses").objects(::expenseFromJson)
        val vaultEntries = payload.getJSONArray("vault").objects(::vaultFromJson)
        database.withTransaction {
            database.habitCheckInDao().deleteAllCheckIns()
            database.habitDao().deleteAllHabits()
            database.expenseDao().deleteAllExpenses()
            habits.forEach { database.habitDao().upsertHabit(it) }
            checkIns.forEach { database.habitCheckInDao().upsertCheckIn(it) }
            expenses.forEach { database.expenseDao().insertExpense(it) }
        }
        vault.saveEntries(vaultEntries)
        context.getSharedPreferences("purehub.ocr-studio.v2", 0).edit().putString("history", payload.optString("ocrHistory", "[]")).commit()
        context.getSharedPreferences("purehub.qr-studio.v2", 0).edit().putString("history", payload.optString("qrHistory", "[]")).commit()
    }

    data class BackupStatus(val exportedAtMillis: Long = 0, val fingerprint: String = "")

    fun backupStatus(): BackupStatus {
        val preferences = context.getSharedPreferences("purehub.encrypted-backup", Context.MODE_PRIVATE)
        return BackupStatus(
            exportedAtMillis = preferences.getLong("last_exported_at", 0),
            fingerprint = preferences.getString("last_export_fingerprint", "").orEmpty(),
        )
    }

    fun recordSuccessfulExport(raw: String) {
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(12)
        context.getSharedPreferences("purehub.encrypted-backup", Context.MODE_PRIVATE).edit()
            .putLong("last_exported_at", System.currentTimeMillis())
            .putString("last_export_fingerprint", fingerprint)
            .apply()
    }

    private fun encrypt(data: ByteArray, passphrase: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return JSONObject().put("magic", "PUREHUB_BACKUP")
            .put("salt", Base64.getEncoder().encodeToString(salt))
            .put("iv", Base64.getEncoder().encodeToString(iv))
            .put("data", Base64.getEncoder().encodeToString(cipher.doFinal(data))).toString()
    }

    private fun decrypt(raw: String, passphrase: CharArray): ByteArray {
        val envelope = JSONObject(raw)
        require(envelope.optString("magic") == "PUREHUB_BACKUP") { "This is not a PureHub backup." }
        val decoder = Base64.getDecoder()
        val salt = decoder.decode(envelope.getString("salt"))
        val iv = decoder.decode(envelope.getString("iv"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(decoder.decode(envelope.getString("data")))
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase, salt, 180_000, 256)).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun habitJson(v: HabitEntity) = JSONObject().put("id", v.id).put("name", v.name).put("description", v.description).put("color", v.colorHex).put("target", v.targetDaysPerWeek).put("created", v.createdAtEpochMillis).put("archived", v.isArchived)
    private fun checkInJson(v: HabitCheckInEntity) = JSONObject().put("id", v.id).put("habitId", v.habitId).put("day", v.completedOn).put("note", v.note).put("created", v.createdAtEpochMillis)
    private fun expenseJson(v: ExpenseEntryEntity) = JSONObject().put("id", v.id).put("title", v.title).put("amount", v.amountMinor).put("category", v.category).put("note", v.note).put("happened", v.happenedAtEpochMillis).put("created", v.createdAtEpochMillis)
    private fun vaultJson(v: VaultEntry) = JSONObject().put("id", v.id).put("title", v.title).put("username", v.username).put("password", v.password)
    private fun habitFromJson(v: JSONObject) = HabitEntity(v.getLong("id"), v.getString("name"), v.optString("description"), v.optString("color", "#7A9E7E"), v.optInt("target", 7), v.getLong("created"), v.optBoolean("archived"))
    private fun checkInFromJson(v: JSONObject) = HabitCheckInEntity(v.getLong("id"), v.getLong("habitId"), v.getString("day"), v.optString("note"), v.getLong("created"))
    private fun expenseFromJson(v: JSONObject) = ExpenseEntryEntity(v.getLong("id"), v.getString("title"), v.getLong("amount"), v.optString("category", "General"), v.optString("note"), v.getLong("happened"), v.getLong("created"))
    private fun vaultFromJson(v: JSONObject) = VaultEntry(v.getString("id"), v.getString("title"), v.optString("username"), v.getString("password"))
    private fun <T> JSONArray.objects(mapper: (JSONObject) -> T) = (0 until length()).map { mapper(getJSONObject(it)) }
}
