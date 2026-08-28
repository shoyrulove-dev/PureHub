package com.purehub.app.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val code: String, val label: String) {
    English("en", "English"),
    Vietnamese("vi", "Tiếng Việt"),
    Chinese("zh", "中文"),
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }

// v2 intentionally re-opens the language welcome screen for users who installed
// builds from before full English/Vietnamese/Chinese UI support was available.
private const val LANGUAGE_PREFS = "purehub.app-language.v2"
private const val LANGUAGE_KEY = "language"
private const val LANGUAGE_CHOSEN_KEY = "language_chosen"

fun Context.loadAppLanguage(): AppLanguage {
    val value = getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, AppLanguage.English.code)
    return AppLanguage.entries.firstOrNull { it.code == value } ?: AppLanguage.English
}

fun Context.hasChosenAppLanguage(): Boolean = getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE).getBoolean(LANGUAGE_CHOSEN_KEY, false)

fun Context.saveAppLanguage(language: AppLanguage) {
    getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE).edit()
        .putString(LANGUAGE_KEY, language.code)
        .putBoolean(LANGUAGE_CHOSEN_KEY, true)
        .apply()
}

fun appText(language: AppLanguage, english: String, vietnamese: String, chinese: String): String = when (language) {
    AppLanguage.English -> english
    AppLanguage.Vietnamese -> vietnamese
    AppLanguage.Chinese -> chinese
}

fun AppLanguage.locale(): Locale = when (this) {
    AppLanguage.English -> Locale.ENGLISH
    AppLanguage.Vietnamese -> Locale.forLanguageTag("vi-VN")
    AppLanguage.Chinese -> Locale.SIMPLIFIED_CHINESE
}
