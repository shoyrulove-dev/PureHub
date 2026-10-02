package com.purehub.app.feature.wifi

import java.net.HttpURLConnection
import java.net.URL

/** Local, unauthenticated fingerprinting only. Never sends router data to a cloud service. */
internal object RouterInspector {
    fun inspect(gateway: String?): RouterInsight? {
        if (gateway.isNullOrBlank()) return null
        val candidates = listOf("http://$gateway/", "https://$gateway/")
        return candidates.firstNotNullOfOrNull { address -> probe(address, gateway) }
    }

    private fun probe(address: String, gateway: String): RouterInsight? = runCatching {
        val started = System.nanoTime()
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 1_500
        connection.readTimeout = 1_500
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "PureHub local router inspector")
        val status = connection.responseCode
        val headers = connection.headerFields.entries.joinToString(" ") { "${it.key} ${it.value}" }
        val location = connection.getHeaderField("Location").orEmpty()
        val evidence = "$headers $location".lowercase()
        val platform = when {
            "unifi" in evidence || "ubnt" in evidence -> "UniFi"
            "omada" in evidence || "tplink" in evidence || "tp-link" in evidence -> "TP-Link Omada"
            "openwrt" in evidence || "luci" in evidence -> "OpenWrt / LuCI"
            "mikrotik" in evidence || "routeros" in evidence -> "MikroTik RouterOS"
            "asus" in evidence -> "ASUSWRT"
            status in 200..499 -> "Local router web UI"
            else -> return null
        }
        val elapsed = ((System.nanoTime() - started) / 1_000_000).toInt()
        RouterInsight(gateway, platform, address, elapsed, "HTTP $status; local gateway fingerprint")
    }.getOrNull()
}
