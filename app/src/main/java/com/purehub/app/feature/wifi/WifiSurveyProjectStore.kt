package com.purehub.app.feature.wifi

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class WifiSurveyCalibration(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val distanceMeters: Float,
) {
    val normalizedDistance: Float
        get() = kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    val metersPerNormalizedUnit: Float?
        get() = normalizedDistance.takeIf { it > .001f }?.let { distanceMeters / it }
}

data class WifiSurveyProject(
    val id: String,
    val name: String,
    val floor: String,
    val planUri: String? = null,
    val calibration: WifiSurveyCalibration? = null,
    val points: List<WifiSurveyPoint> = emptyList(),
)

internal object WifiSurveyProjectStore {
    private const val PROJECTS_KEY = "projects_v2"
    private const val ACTIVE_KEY = "active_project_v2"

    fun load(preferences: SharedPreferences, legacyPoints: List<WifiSurveyPoint>): Pair<List<WifiSurveyProject>, String> {
        val projects = runCatching {
            val array = JSONArray(preferences.getString(PROJECTS_KEY, "[]"))
            List(array.length()) { index -> projectFromJson(array.getJSONObject(index)) }
        }.getOrDefault(emptyList()).ifEmpty {
            listOf(
                WifiSurveyProject(
                    id = UUID.randomUUID().toString(),
                    name = "Home survey",
                    floor = "Floor 1",
                    planUri = preferences.getString("plan_uri", null),
                    points = legacyPoints,
                ),
            )
        }
        val active = preferences.getString(ACTIVE_KEY, null).takeIf { id -> projects.any { it.id == id } }
            ?: projects.first().id
        save(preferences, projects, active)
        return projects to active
    }

    fun save(preferences: SharedPreferences, projects: List<WifiSurveyProject>, activeId: String) {
        val array = JSONArray().also { output -> projects.forEach { output.put(projectToJson(it)) } }
        preferences.edit().putString(PROJECTS_KEY, array.toString()).putString(ACTIVE_KEY, activeId).apply()
    }

    private fun projectToJson(project: WifiSurveyProject) = JSONObject().apply {
        put("id", project.id); put("name", project.name); put("floor", project.floor)
        put("planUri", project.planUri ?: JSONObject.NULL)
        project.calibration?.let { value -> put("calibration", JSONObject().apply {
            put("x1", value.x1); put("y1", value.y1); put("x2", value.x2); put("y2", value.y2); put("meters", value.distanceMeters)
        }) }
        put("points", JSONArray().also { rows -> project.points.forEach { point -> rows.put(pointToJson(point)) } })
    }

    private fun pointToJson(point: WifiSurveyPoint) = JSONObject().apply {
        put("x", point.x); put("y", point.y); put("rssi", point.rssi); put("ssid", point.ssid)
        put("timestamp", point.timestamp); put("accessPoints", point.observedAccessPoints)
        put("latencyMs", point.latencyMs ?: JSONObject.NULL)
        put("packetLoss", point.packetLossPercent ?: JSONObject.NULL)
        put("downloadMbps", point.downloadMbps ?: JSONObject.NULL)
        put("uploadMbps", point.uploadMbps ?: JSONObject.NULL)
        put("readings", JSONArray().also { rows -> point.readings.forEach { reading -> rows.put(JSONObject().apply {
            put("bssid", reading.bssid); put("ssid", reading.ssid); put("rssi", reading.rssi)
            put("sir", reading.estimatedSirDb ?: JSONObject.NULL)
        }) } })
    }

    private fun projectFromJson(value: JSONObject): WifiSurveyProject {
        val points = value.optJSONArray("points") ?: JSONArray()
        val calibration = value.optJSONObject("calibration")?.let {
            WifiSurveyCalibration(it.getDouble("x1").toFloat(), it.getDouble("y1").toFloat(), it.getDouble("x2").toFloat(), it.getDouble("y2").toFloat(), it.getDouble("meters").toFloat())
        }
        return WifiSurveyProject(
            value.getString("id"), value.optString("name", "Survey"), value.optString("floor", "Floor 1"),
            value.optString("planUri").takeUnless { it.isBlank() || it == "null" }, calibration,
            List(points.length()) { pointFromJson(points.getJSONObject(it)) },
        )
    }

    private fun pointFromJson(value: JSONObject): WifiSurveyPoint {
        val readings = value.optJSONArray("readings") ?: JSONArray()
        return WifiSurveyPoint(
            value.getDouble("x").toFloat(), value.getDouble("y").toFloat(), value.getInt("rssi"), value.optString("ssid"),
            value.optLong("timestamp"), value.optInt("accessPoints"),
            List(readings.length()) { index -> readings.getJSONObject(index).let {
                WifiSurveyReading(it.optString("bssid"), it.optString("ssid"), it.optInt("rssi"), if (it.isNull("sir")) null else it.optDouble("sir"))
            } },
            if (value.isNull("latencyMs")) null else value.optInt("latencyMs"),
            if (value.isNull("packetLoss")) null else value.optInt("packetLoss"),
            if (value.isNull("downloadMbps")) null else value.optDouble("downloadMbps"),
            if (value.isNull("uploadMbps")) null else value.optDouble("uploadMbps"),
        )
    }
}
