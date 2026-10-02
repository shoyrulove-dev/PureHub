package com.purehub.app.feature.wifi

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal object WifiMonitorStore {
    private const val KEY = "monitor_reports_v1"
    private const val ACTIVE_KEY = "active_monitor_v2"

    fun load(preferences: SharedPreferences): List<WifiMonitorReport> = runCatching {
        val reports = JSONArray(preferences.getString(KEY, "[]"))
        List(reports.length()) { index ->
            val report = reports.getJSONObject(index)
            val samples = report.optJSONArray("samples") ?: JSONArray()
            val events = report.optJSONArray("events") ?: JSONArray()
            WifiMonitorReport(
                report.getLong("started"), report.getLong("ended"),
                List(samples.length()) { sampleIndex -> samples.getJSONObject(sampleIndex).let { sample ->
                    WifiMonitorSample(sample.getLong("time"), sample.getInt("rssi"), sample.optString("bssid"), sample.optBoolean("gateway"), sample.optBoolean("internet"), if (sample.isNull("latency")) null else sample.optInt("latency"))
                } },
                List(events.length()) { eventIndex -> events.getJSONObject(eventIndex).let { event ->
                    WifiMonitorEvent(event.getLong("time"), event.optString("type"), event.optString("detail"))
                } },
            )
        }
    }.getOrDefault(emptyList())

    fun save(preferences: SharedPreferences, reports: List<WifiMonitorReport>) {
        val output = JSONArray()
        reports.take(5).forEach { report -> output.put(JSONObject().apply {
            put("started", report.startedAt); put("ended", report.endedAt)
            put("samples", JSONArray().also { rows -> report.samples.takeLast(720).forEach { sample -> rows.put(JSONObject().apply {
                put("time", sample.timestamp); put("rssi", sample.rssi); put("bssid", sample.bssid)
                put("gateway", sample.gatewayReachable); put("internet", sample.internetReachable)
                put("latency", sample.latencyMs ?: JSONObject.NULL)
            }) } })
            put("events", JSONArray().also { rows -> report.events.takeLast(120).forEach { event -> rows.put(JSONObject().apply {
                put("time", event.timestamp); put("type", event.type); put("detail", event.detail)
            }) } })
        }) }
        preferences.edit().putString(KEY, output.toString()).apply()
    }

    fun loadActive(preferences: SharedPreferences): WifiMonitorSession? = runCatching {
        preferences.getString(ACTIVE_KEY, null)?.let(::JSONObject)?.let { value ->
            val samples = value.optJSONArray("samples") ?: JSONArray()
            val events = value.optJSONArray("events") ?: JSONArray()
            WifiMonitorSession(
                startedAt = value.getLong("started"),
                endsAt = value.getLong("ends"),
                intervalSeconds = value.optInt("interval", 10).coerceIn(5, 300),
                samples = List(samples.length()) { index -> sampleFromJson(samples.getJSONObject(index)) },
                events = List(events.length()) { index -> eventFromJson(events.getJSONObject(index)) },
            )
        }
    }.getOrNull()

    fun saveActive(preferences: SharedPreferences, session: WifiMonitorSession?) {
        if (session == null) {
            preferences.edit().remove(ACTIVE_KEY).apply()
            return
        }
        val output = JSONObject().apply {
            put("started", session.startedAt)
            put("ends", session.endsAt)
            put("interval", session.intervalSeconds)
            put("samples", JSONArray().also { rows -> session.samples.takeLast(4_320).forEach { rows.put(sampleToJson(it)) } })
            put("events", JSONArray().also { rows -> session.events.takeLast(500).forEach { rows.put(eventToJson(it)) } })
        }
        preferences.edit().putString(ACTIVE_KEY, output.toString()).apply()
    }

    fun finishActive(preferences: SharedPreferences, endedAt: Long = System.currentTimeMillis()): WifiMonitorReport? {
        val active = loadActive(preferences) ?: return null
        val report = WifiMonitorReport(active.startedAt, endedAt, active.samples, active.events)
        save(preferences, (listOf(report) + load(preferences)).take(10))
        saveActive(preferences, null)
        return report
    }

    private fun sampleToJson(sample: WifiMonitorSample) = JSONObject().apply {
        put("time", sample.timestamp); put("rssi", sample.rssi); put("bssid", sample.bssid)
        put("gateway", sample.gatewayReachable); put("internet", sample.internetReachable)
        put("latency", sample.latencyMs ?: JSONObject.NULL)
    }

    private fun eventToJson(event: WifiMonitorEvent) = JSONObject().apply {
        put("time", event.timestamp); put("type", event.type); put("detail", event.detail)
    }

    private fun sampleFromJson(sample: JSONObject) = WifiMonitorSample(
        sample.getLong("time"), sample.getInt("rssi"), sample.optString("bssid"),
        sample.optBoolean("gateway"), sample.optBoolean("internet"),
        if (sample.isNull("latency")) null else sample.optInt("latency"),
    )

    private fun eventFromJson(event: JSONObject) = WifiMonitorEvent(
        event.getLong("time"), event.optString("type"), event.optString("detail"),
    )
}
