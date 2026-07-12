package com.purehub.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ToolVisibilityRepository(
    private val context: Context,
) {
    val hiddenToolIds: Flow<Set<String>> = context.pureHubDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.hiddenToolIds].orEmpty()
        }

    val favoriteToolIds: Flow<Set<String>> = context.pureHubDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.favoriteToolIds].orEmpty()
        }

    suspend fun setToolVisible(
        toolId: String,
        visible: Boolean,
    ) {
        context.pureHubDataStore.edit { preferences ->
            val updated = preferences[Keys.hiddenToolIds].orEmpty().toMutableSet()
            if (visible) {
                updated.remove(toolId)
            } else {
                updated.add(toolId)
            }
            preferences[Keys.hiddenToolIds] = updated
        }
    }

    suspend fun setToolFavorite(
        toolId: String,
        favorite: Boolean,
    ) {
        context.pureHubDataStore.edit { preferences ->
            val updated = preferences[Keys.favoriteToolIds].orEmpty().toMutableSet()
            if (favorite) {
                updated.add(toolId)
            } else {
                updated.remove(toolId)
            }
            preferences[Keys.favoriteToolIds] = updated
        }
    }

    suspend fun resetLayout() {
        context.pureHubDataStore.edit { preferences ->
            preferences.remove(Keys.hiddenToolIds)
            preferences.remove(Keys.favoriteToolIds)
        }
    }

    private object Keys {
        val hiddenToolIds = stringSetPreferencesKey("hidden_tool_ids")
        val favoriteToolIds = stringSetPreferencesKey("favorite_tool_ids")
    }
}
