package com.purehub.app.feature.wifi

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class ControllerTelemetry(
    val accessPointCount: Int? = null,
    val clientCount: Int? = null,
    val channelUtilizationPercent: Int? = null,
    val wanOnline: Boolean? = null,
)

internal fun parseControllerTelemetry(devicesBody: String, clientsBody: String): ControllerTelemetry {
    val deviceRoot = parseControllerJson(devicesBody)
    val clientRoot = parseControllerJson(clientsBody)
    val devices = controllerCollection(deviceRoot)
    val accessPoints = devices.count { item ->
        val descriptor = listOf("type", "deviceType", "category", "role", "model", "modelKey")
            .mapNotNull { key -> item.optString(key).takeIf(String::isNotBlank) }
            .joinToString(" ").lowercase()
        descriptor.contains("access point") || descriptor.contains("wireless ap") || descriptor.contains("uap") || descriptor.contains("eap") || descriptor == "ap"
    }.takeIf { it > 0 }
    val channelValues = mutableListOf<Double>()
    collectControllerNumbers(deviceRoot, setOf("channelutilization", "channel_utilization", "channelusage", "cu_total", "utilization"), channelValues)
    val normalizedUtilization = channelValues.mapNotNull { value ->
        when (value) {
            in 0.0..1.0 -> value * 100.0
            in 1.0..100.0 -> value
            else -> null
        }
    }
    return ControllerTelemetry(
        accessPointCount = accessPoints,
        clientCount = controllerTotal(clientRoot) ?: controllerCollection(clientRoot).size.takeIf { it > 0 },
        channelUtilizationPercent = normalizedUtilization.average().takeIf(Double::isFinite)?.toInt(),
    )
}

private fun parseControllerJson(body: String): Any? = runCatching {
    body.trim().takeIf(String::isNotBlank)?.let { if (it.startsWith("[")) JSONArray(it) else JSONObject(it) }
}.getOrNull()

private fun controllerCollection(value: Any?): List<JSONObject> {
    if (value is JSONArray) return (0 until value.length()).mapNotNull(value::optJSONObject)
    if (value !is JSONObject) return emptyList()
    listOf("data", "items", "records", "devices", "clients", "result").forEach { key ->
        val child = value.opt(key)
        if (child is JSONArray) {
            val objects = (0 until child.length()).mapNotNull(child::optJSONObject)
            if (objects.isNotEmpty()) return objects
        }
        val nested = controllerCollection(child)
        if (nested.isNotEmpty()) return nested
    }
    return emptyList()
}

private fun controllerTotal(value: Any?): Int? = when (value) {
    is JSONObject -> value.keys().asSequence().mapNotNull { key ->
        val child = value.opt(key)
        if (key.lowercase() in setOf("totalrows", "total", "totalcount", "count") && child is Number) child.toInt().takeIf { it >= 0 }
        else controllerTotal(child)
    }.firstOrNull()
    is JSONArray -> (0 until value.length()).asSequence().mapNotNull { controllerTotal(value.opt(it)) }.firstOrNull()
    else -> null
}

private fun collectControllerNumbers(value: Any?, keys: Set<String>, output: MutableList<Double>) {
    when (value) {
        is JSONObject -> value.keys().forEach { key ->
            val child = value.opt(key)
            if (key.lowercase() in keys && child is Number) output += child.toDouble()
            collectControllerNumbers(child, keys, output)
        }
        is JSONArray -> (0 until value.length()).forEach { collectControllerNumbers(value.opt(it), keys, output) }
    }
}

internal object RouterControllerStore {
    private const val FILE = "purehub.router-controller.secure.v1"

    private fun preferences(context: Context): SharedPreferences {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            FILE,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun load(context: Context): RouterControllerConfig? = runCatching {
        val values = preferences(context)
        val baseUrl = values.getString("base_url", null)?.takeIf(String::isNotBlank) ?: return null
        RouterControllerConfig(
            platform = RouterControllerPlatform.valueOf(values.getString("platform", RouterControllerPlatform.UNIFI.name).orEmpty()),
            baseUrl = baseUrl,
            username = values.getString("username", "").orEmpty(),
            secret = values.getString("secret", "").orEmpty(),
        )
    }.getOrNull()

    fun save(context: Context, config: RouterControllerConfig?) {
        val editor = preferences(context).edit().clear()
        if (config != null) editor
            .putString("platform", config.platform.name)
            .putString("base_url", config.baseUrl.trim().trimEnd('/'))
            .putString("username", config.username)
            .putString("secret", config.secret)
        editor.apply()
    }
}

/** Optional, local and read-only controller probes. No credentials leave the configured controller URL. */
internal object RouterControllerClient {
    private data class Response(val code: Int, val body: String)

    fun inspect(config: RouterControllerConfig): RouterControllerStatus {
        val started = System.nanoTime()
        return runCatching {
            val status = when (config.platform) {
                RouterControllerPlatform.UNIFI -> inspectUniFi(config)
                RouterControllerPlatform.OMADA -> inspectOmada(config)
                RouterControllerPlatform.OPENWRT -> inspectOpenWrt(config)
            }
            status.copy(responseMs = ((System.nanoTime() - started) / 1_000_000).toInt())
        }.getOrElse { error ->
            RouterControllerStatus(config.platform, false, false, null, safeError(error))
        }
    }

    private fun inspectUniFi(config: RouterControllerConfig): RouterControllerStatus {
        val root = "${base(config)}/proxy/network/integration/v1"
        val auth = mapOf("X-API-KEY" to config.secret)
        val sites = request("$root/sites", "GET", auth)
        if (sites.code !in 200..299) return failure(config.platform, sites.code, "API key was not accepted", "integration API is unavailable on this version")
        val siteId = firstString(sites.body, setOf("id", "siteId", "site_id", "_id"))
        val devices = siteId?.let { request("$root/sites/$it/devices", "GET", auth) }
        val clients = siteId?.let { request("$root/sites/$it/clients", "GET", auth) }
        return success(config.platform, "UniFi local API connected · read-only telemetry", parseControllerTelemetry(devices.successBody(), clients.successBody()))
    }

    private fun inspectOmada(config: RouterControllerConfig): RouterControllerStatus {
        val controllerId = config.username.trim()
        require(controllerId.isNotBlank()) { "Enter the Omada controller ID in the account field" }
        val root = "${base(config)}/openapi/v1/$controllerId"
        val auth = mapOf("Authorization" to "AccessToken=${config.secret}")
        val sites = request("$root/sites?page=1&pageSize=100", "GET", auth)
        if (sites.code !in 200..299) return failure(config.platform, sites.code, "access token was not accepted", "verify controller ID and Open API path")
        val siteId = firstString(sites.body, setOf("siteId", "site_id", "id", "_id"))
        val devices = siteId?.let { request("$root/sites/$it/devices?page=1&pageSize=100", "GET", auth) }
        val clients = siteId?.let { request("$root/sites/$it/clients?page=1&pageSize=100", "GET", auth) }
        return success(config.platform, "Omada Open API connected · read-only telemetry", parseControllerTelemetry(devices.successBody(), clients.successBody()))
    }

    private fun inspectOpenWrt(config: RouterControllerConfig): RouterControllerStatus {
        val login = JSONObject().apply {
            put("jsonrpc", "2.0"); put("id", 1); put("method", "call")
            put("params", JSONArray().put("00000000000000000000000000000000").put("session").put("login").put(JSONObject().apply {
                put("username", config.username); put("password", config.secret)
            }))
        }
        val response = request("${base(config)}/ubus", "POST", body = login.toString())
        val session = runCatching { JSONObject(response.body).getJSONArray("result").optJSONObject(1)?.optString("ubus_rpc_session") }.getOrNull()
        if (session.isNullOrBlank()) return failure(config.platform, response.code, "credentials were not accepted", "ubus login is unavailable")
        val wireless = ubusCall(config, session, "network.wireless", "status")
        val wan = ubusCall(config, session, "network.interface.wan", "status")
        val telemetry = parseControllerTelemetry(wireless, "").let {
            it.copy(accessPointCount = it.accessPointCount ?: countOpenWrtRadios(wireless), wanOnline = openWrtWanOnline(wan))
        }
        return success(config.platform, "OpenWrt ubus connected · read-only telemetry", telemetry)
    }

    private fun success(platform: RouterControllerPlatform, summary: String, telemetry: ControllerTelemetry) = RouterControllerStatus(
        platform = platform,
        reachable = true,
        authenticated = true,
        responseMs = null,
        summary = summary,
        accessPointCount = telemetry.accessPointCount,
        clientCount = telemetry.clientCount,
        channelUtilizationPercent = telemetry.channelUtilizationPercent,
        wanOnline = telemetry.wanOnline,
    )

    private fun failure(platform: RouterControllerPlatform, code: Int, authMessage: String, notFoundMessage: String) = RouterControllerStatus(
        platform, true, false, null, when (code) {
            401, 403 -> "Controller reached · $authMessage"
            404 -> "Controller reached · $notFoundMessage"
            else -> "Controller reached · HTTP $code"
        },
    )

    private fun ubusCall(config: RouterControllerConfig, session: String, service: String, method: String): String {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0"); put("id", 2); put("method", "call")
            put("params", JSONArray().put(session).put(service).put(method).put(JSONObject()))
        }
        return request("${base(config)}/ubus", "POST", body = payload.toString()).body
    }

    private fun request(address: String, method: String, headers: Map<String, String> = emptyMap(), body: String? = null): Response {
        val connection = connection(address, method)
        headers.filterValues(String::isNotBlank).forEach(connection::setRequestProperty)
        if (body != null) {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.encodeToByteArray()) }
        }
        val code = connection.responseCode
        val responseBody = streamText(connection)
        connection.disconnect()
        return Response(code, responseBody)
    }

    private fun Response?.successBody(): String = this?.body?.takeIf { this.code in 200..299 }.orEmpty()

    private fun connection(address: String, method: String): HttpURLConnection = (URL(address).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 4_000
        readTimeout = 4_000
        instanceFollowRedirects = false
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "PureHub local read-only controller connector")
    }

    private fun base(config: RouterControllerConfig): String {
        val value = config.baseUrl.trim().trimEnd('/')
        require(value.startsWith("https://") || value.startsWith("http://")) { "Controller URL must start with https:// or http://" }
        return value
    }

    private fun streamText(connection: HttpURLConnection): String = runCatching {
        (if (connection.responseCode >= 400) connection.errorStream else connection.inputStream)?.bufferedReader()?.use { it.readText().take(128_000) }.orEmpty()
    }.getOrDefault("")

    private fun firstString(body: String, keys: Set<String>): String? = runCatching { firstString(parseJson(body), keys) }.getOrNull()

    private fun firstString(value: Any?, keys: Set<String>): String? = when (value) {
        is JSONObject -> value.keys().asSequence().mapNotNull { key ->
            val child = value.opt(key)
            if (key in keys && child != null && child != JSONObject.NULL && child.toString().isNotBlank()) child.toString() else firstString(child, keys)
        }.firstOrNull()
        is JSONArray -> (0 until value.length()).asSequence().mapNotNull { firstString(value.opt(it), keys) }.firstOrNull()
        else -> null
    }

    private fun countOpenWrtRadios(body: String): Int? = runCatching {
        val root = parseJson(body) as? JSONObject
        root?.optJSONArray("result")?.optJSONObject(1)?.keys()?.asSequence()?.count { it.startsWith("radio", true) }?.takeIf { it > 0 }
    }.getOrNull()

    private fun openWrtWanOnline(body: String): Boolean? = runCatching {
        val root = parseJson(body) as? JSONObject
        root?.optJSONArray("result")?.optJSONObject(1)?.takeIf { it.has("up") }?.optBoolean("up")
    }.getOrNull()

    private fun parseJson(body: String): Any = body.trim().let { if (it.startsWith("[")) JSONArray(it) else JSONObject(it) }

    private fun safeError(error: Throwable): String = when (error) {
        is javax.net.ssl.SSLException -> "TLS verification failed · install a trusted controller certificate"
        else -> error.message?.take(140) ?: "Controller connection failed"
    }
}
