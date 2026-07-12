package com.purehub.app.feature.billsplitter

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val BILL_PRESET_DATASTORE = "bill_splitter_presets"
private val Context.billPresetStore by preferencesDataStore(name = BILL_PRESET_DATASTORE)

data class BillPreset(
    val name: String,
    val totalBill: String,
    val taxAmount: String,
    val tipAmount: String,
    val peopleCount: Int,
    val items: List<BillPresetItem>,
)

data class BillPresetItem(
    val name: String,
    val amount: String,
    val assignedPeople: List<Int>,
)

class BillPresetRepository(
    private val context: Context,
) {
    private val key = stringPreferencesKey("presets_json")

    val presets: Flow<List<BillPreset>> = context.billPresetStore.data.map { preferences ->
        BillPresetCodec.decode(preferences[key].orEmpty())
    }

    suspend fun savePreset(preset: BillPreset) {
        if (preset.name.isBlank()) return
        context.billPresetStore.edit { preferences ->
            val current = BillPresetCodec.decode(preferences[key].orEmpty())
                .filterNot { it.name.equals(preset.name, ignoreCase = true) }
            preferences[key] = BillPresetCodec.encode(current + preset)
        }
    }

    suspend fun renamePreset(oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank()) return
        context.billPresetStore.edit { preferences ->
            val updated = BillPresetCodec.decode(preferences[key].orEmpty()).map { preset ->
                if (preset.name.equals(oldName, ignoreCase = true)) {
                    preset.copy(name = newName.trim())
                } else {
                    preset
                }
            }
            preferences[key] = BillPresetCodec.encode(updated.distinctBy { it.name.lowercase() })
        }
    }

    suspend fun deletePreset(name: String) {
        if (name.isBlank()) return
        context.billPresetStore.edit { preferences ->
            val updated = BillPresetCodec.decode(preferences[key].orEmpty())
                .filterNot { it.name.equals(name, ignoreCase = true) }
            preferences[key] = BillPresetCodec.encode(updated)
        }
    }
}

object BillPresetCodec {
    fun encode(presets: List<BillPreset>): String {
        return JSONArray().apply {
            presets.forEach { preset ->
                put(
                    JSONObject().apply {
                        put("name", preset.name)
                        put("totalBill", preset.totalBill)
                        put("taxAmount", preset.taxAmount)
                        put("tipAmount", preset.tipAmount)
                        put("peopleCount", preset.peopleCount)
                        put(
                            "items",
                            JSONArray().apply {
                                preset.items.forEach { item ->
                                    put(
                                        JSONObject().apply {
                                            put("name", item.name)
                                            put("amount", item.amount)
                                            put("assignedPeople", JSONArray(item.assignedPeople))
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }.toString()
    }

    fun decode(raw: String): List<BillPreset> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val root = JSONArray(raw)
            buildList {
                for (index in 0 until root.length()) {
                    val json = root.getJSONObject(index)
                    val itemsJson = json.getJSONArray("items")
                    val items = buildList {
                        for (itemIndex in 0 until itemsJson.length()) {
                            val itemJson = itemsJson.getJSONObject(itemIndex)
                            val assignedJson = itemJson.getJSONArray("assignedPeople")
                            add(
                                BillPresetItem(
                                    name = itemJson.getString("name"),
                                    amount = itemJson.getString("amount"),
                                    assignedPeople = buildList {
                                        for (assignedIndex in 0 until assignedJson.length()) {
                                            add(assignedJson.getInt(assignedIndex))
                                        }
                                    },
                                ),
                            )
                        }
                    }
                    add(
                        BillPreset(
                            name = json.getString("name"),
                            totalBill = json.getString("totalBill"),
                            taxAmount = json.getString("taxAmount"),
                            tipAmount = json.getString("tipAmount"),
                            peopleCount = json.getInt("peopleCount"),
                            items = items,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList()).sortedBy { it.name.lowercase() }
    }
}
