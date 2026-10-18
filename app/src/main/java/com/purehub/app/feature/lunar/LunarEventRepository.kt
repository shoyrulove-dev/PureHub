package com.purehub.app.feature.lunar

import android.content.Context
import com.purehub.app.security.KeystoreStringStore
import com.purehub.app.security.legacyEncryptedStrings
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class LunarLeapRule { MainMonth, LeapMonth, Both }
enum class LunarEventCategory { Family, Worship, Birthday, Personal }
enum class LunarEventRecurrence { Monthly, Yearly }

data class LunarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val day: Int,
    val month: Int,
    val leapRule: LunarLeapRule = LunarLeapRule.MainMonth,
    val remindDaysBefore: Int = 0,
    val reminderOffsets: List<Int> = listOf(remindDaysBefore),
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val category: LunarEventCategory = LunarEventCategory.Family,
    val recurrence: LunarEventRecurrence = LunarEventRecurrence.Yearly,
    val enabled: Boolean = true,
    val deletedAtEpochMs: Long? = null,
)

class LunarEventRepository(context: Context) {
    private val preferences = KeystoreStringStore(context, "purehub_lunar_events") {
        legacyEncryptedStrings(context, "purehub_lunar_events", setOf(KEY))
    }

    fun load(): List<LunarEvent> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]"))
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    LunarEvent(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = item.getString("title"),
                        day = item.getInt("day"),
                        month = item.getInt("month"),
                        leapRule = runCatching { LunarLeapRule.valueOf(item.optString("leapRule")) }.getOrDefault(LunarLeapRule.MainMonth),
                        remindDaysBefore = item.optInt("remindDaysBefore", 0).coerceIn(0, 30),
                        reminderOffsets = item.optJSONArray("reminderOffsets")?.let { offsets ->
                            (0 until offsets.length()).map { offsets.optInt(it) }.filter { it in 0..30 }.distinct().sortedDescending()
                        }.orEmpty().ifEmpty { listOf(item.optInt("remindDaysBefore", 0).coerceIn(0, 30)) },
                        reminderHour = item.optInt("reminderHour", 8).coerceIn(0, 23),
                        reminderMinute = item.optInt("reminderMinute", 0).coerceIn(0, 59),
                        category = runCatching { LunarEventCategory.valueOf(item.optString("category")) }.getOrDefault(LunarEventCategory.Family),
                        recurrence = runCatching { LunarEventRecurrence.valueOf(item.optString("recurrence")) }.getOrDefault(LunarEventRecurrence.Yearly),
                        enabled = item.optBoolean("enabled", true),
                        deletedAtEpochMs = if (item.has("deletedAtEpochMs") && !item.isNull("deletedAtEpochMs")) item.optLong("deletedAtEpochMs") else null,
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(events: List<LunarEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(JSONObject().apply {
                put("id", event.id); put("title", event.title); put("day", event.day); put("month", event.month)
                put("leapRule", event.leapRule.name); put("remindDaysBefore", event.remindDaysBefore)
                put("reminderOffsets", JSONArray(event.reminderOffsets)); put("reminderHour", event.reminderHour); put("reminderMinute", event.reminderMinute)
                put("category", event.category.name); put("recurrence", event.recurrence.name); put("enabled", event.enabled)
                event.deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
            })
        }
        preferences.putStrings(mapOf(KEY to array.toString()))
    }

    fun exportJson(): String = JSONObject().put("version", 2).put("events", JSONArray(preferences.getString(KEY, "[]"))).toString(2)

    fun importJson(raw: String): List<LunarEvent> {
        val source = JSONObject(raw).getJSONArray("events")
        preferences.putStrings(mapOf(KEY to source.toString()))
        return load()
    }

    companion object { private const val KEY = "events" }
}
