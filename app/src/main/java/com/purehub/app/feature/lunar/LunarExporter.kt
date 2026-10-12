package com.purehub.app.feature.lunar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object LunarExporter {
    fun shareDayIcs(context: Context, date: LocalDate) {
        val lunar = LunarCalendarConverter.convertSolarToLunar(date, LunarPreferences.region(context))
        val body = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//PureHub//Lunar//EN\r\nBEGIN:VEVENT\r\nUID:${date}@purehub\r\nDTSTART;VALUE=DATE:${date.toString().replace("-", "")}\r\nSUMMARY:Lunar ${lunar.day}/${lunar.month}/${lunar.year}\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        shareFile(context, write(context, "purehub-lunar-${date}.ics", body.toByteArray()), "text/calendar")
    }

    fun shareMonthPng(context: Context, month: YearMonth) {
        val bitmap = renderMonth(month, LunarPreferences.region(context))
        val file = File(context.cacheDir, "purehub-lunar-${month}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shareFile(context, file, "image/png")
    }

    fun shareMonthPdf(context: Context, month: YearMonth) {
        val bitmap = renderMonth(month, LunarPreferences.region(context))
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create())
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
        val file = File(context.cacheDir, "purehub-lunar-${month}.pdf")
        file.outputStream().use(document::writeTo)
        document.close()
        shareFile(context, file, "application/pdf")
    }

    fun shareEncryptedBackup(context: Context, json: String, passphrase: String) {
        shareFile(context, write(context, "purehub-lunar-backup.phl", encrypt(json.toByteArray(), passphrase)), "application/octet-stream")
    }

    fun decryptBackup(bytes: ByteArray, passphrase: String): String {
        require(bytes.size > 33 && bytes.copyOfRange(0, 5).decodeToString() == "PHLC1") { "Invalid PureHub Lunar backup" }
        val salt = bytes.copyOfRange(5, 21); val iv = bytes.copyOfRange(21, 33); val encrypted = bytes.copyOfRange(33, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).decodeToString()
    }

    private fun renderMonth(month: YearMonth, region: LunarRegion): Bitmap {
        val width = 1240; val height = 1754
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(25, 18, 46))
        paint.color = Color.rgb(245, 185, 66); paint.textSize = 48f; paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("PUREHUB · LUNAR CALENDAR", 70f, 95f, paint)
        paint.color = Color.WHITE; paint.textSize = 72f
        canvas.drawText(month.toString(), 70f, 185f, paint)
        val labels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val cellWidth = (width - 100f) / 7f
        paint.textSize = 28f; paint.color = Color.rgb(207, 197, 232)
        labels.forEachIndexed { index, label -> canvas.drawText(label, 55f + index * cellWidth, 260f, paint) }
        val leading = month.atDay(1).dayOfWeek.value - 1
        repeat(month.lengthOfMonth()) { offset ->
            val date = month.atDay(offset + 1); val lunar = LunarCalendarConverter.convertSolarToLunar(date, region)
            val index = leading + offset; val column = index % 7; val row = index / 7
            val left = 50f + column * cellWidth; val top = 305f + row * 190f
            paint.style = Paint.Style.FILL; paint.color = Color.rgb(49, 39, 70); canvas.drawRoundRect(left, top, left + cellWidth - 12f, top + 166f, 22f, 22f, paint)
            paint.color = Color.WHITE; paint.textSize = 42f; paint.typeface = Typeface.DEFAULT_BOLD; canvas.drawText(date.dayOfMonth.toString(), left + 18f, top + 58f, paint)
            paint.color = Color.rgb(207, 197, 232); paint.textSize = 29f; paint.typeface = Typeface.DEFAULT; canvas.drawText("${lunar.day}/${lunar.month}${if (lunar.isLeapMonth) "*" else ""}", left + 18f, top + 108f, paint)
        }
        paint.color = Color.rgb(207, 197, 232); paint.textSize = 28f
        canvas.drawText("Calculated locally · UTC+${region.timeZone.toInt()}", 70f, height - 70f, paint)
        return bitmap
    }

    private fun write(context: Context, name: String, bytes: ByteArray): File = File(context.cacheDir, name).apply { writeBytes(bytes) }
    private fun encrypt(bytes: ByteArray, passphrase: String): ByteArray {
        require(passphrase.length >= 8) { "Passphrase must contain at least 8 characters" }
        val random = SecureRandom(); val salt = ByteArray(16).also(random::nextBytes); val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        return "PHLC1".encodeToByteArray() + salt + iv + cipher.doFinal(bytes)
    }
    private fun key(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 120_000, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }
    private fun shareFile(context: Context, file: File, type: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { this.type = type; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Lunar"))
    }
}
