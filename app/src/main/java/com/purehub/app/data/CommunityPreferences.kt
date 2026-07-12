package com.purehub.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "purehub_preferences"

val Context.pureHubDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
)

data class CommunityPreferences(
    val proCode: String = "",
    val isUnlocked: Boolean = false,
)

class CommunityPreferencesRepository(
    private val context: Context,
) {
    val preferencesFlow: Flow<CommunityPreferences> = context.pureHubDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            CommunityPreferences(
                proCode = preferences[Keys.proCode].orEmpty(),
                isUnlocked = preferences[Keys.isUnlocked] ?: false,
            )
        }

    suspend fun saveProCode(code: String) {
        context.pureHubDataStore.edit { preferences ->
            preferences[Keys.proCode] = code
            preferences[Keys.isUnlocked] = code.isNotBlank()
        }
    }

    private object Keys {
        val proCode = stringPreferencesKey("pro_code")
        val isUnlocked = booleanPreferencesKey("is_unlocked")
    }
}
