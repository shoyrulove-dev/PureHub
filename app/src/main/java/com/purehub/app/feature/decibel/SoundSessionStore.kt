package com.purehub.app.feature.decibel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SoundPoint(val capturedAtEpochMillis: Long, val decibel: Float)

data class SoundSession(
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val minimumDecibel: Float,
    val averageDecibel: Float,
    val maximumDecibel: Float,
    val points: List<SoundPoint>,
)

class SoundSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("purehub.sound-sessions.v1", Context.MODE_PRIVATE)

    fun load(): List<SoundSession> = SoundSessionCodec.decode(preferences.getString("sessions", "[]").orEmpty())

    fun save(sessions: List<SoundSession>) {
        preferences.edit().putString("sessions", SoundSessionCodec.encode(sessions.take(10))).apply()
    }
}

object SoundSessionCodec {
    fun decode(raw: String): List<SoundSession> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val points = item.optJSONArray("points") ?: JSONArray()
            SoundSession(
                startedAtEpochMillis = item.getLong("started"),
                endedAtEpochMillis = item.getLong("ended"),
                minimumDecibel = item.getDouble("min").toFloat(),
                averageDecibel = item.getDouble("avg").toFloat(),
                maximumDecibel = item.getDouble("max").toFloat(),
                points = (0 until points.length()).mapNotNull { pointIndex ->
                    val point = points.optJSONObject(pointIndex) ?: return@mapNotNull null
                    SoundPoint(point.getLong("at"), point.getDouble("db").toFloat())
                },
            )
        }
    }.getOrDefault(emptyList())

    fun encode(sessions: List<SoundSession>): String = JSONArray().apply {
        sessions.take(10).forEach { session ->
            put(
                JSONObject()
                    .put("started", session.startedAtEpochMillis)
                    .put("ended", session.endedAtEpochMillis)
                    .put("min", session.minimumDecibel)
                    .put("avg", session.averageDecibel)
                    .put("max", session.maximumDecibel)
                    .put("points", JSONArray().apply {
                        session.points.takeLast(600).forEach { point ->
                            put(JSONObject().put("at", point.capturedAtEpochMillis).put("db", point.decibel))
                        }
                    }),
            )
        }
    }.toString()

    fun csv(session: SoundSession): String = buildString {
        appendLine("timestamp,decibel")
        session.points.forEach { appendLine("${it.capturedAtEpochMillis},${"%.1f".format(it.decibel)}") }
    }
}
