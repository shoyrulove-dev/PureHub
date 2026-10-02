package com.purehub.app.feature.wifi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.security.auth.x500.X500Principal

data class LanDevice(
    val ipAddress: String,
    val hostName: String,
    val latencyMs: Int?,
    val isNew: Boolean,
    val macAddress: String? = null,
    val vendor: String? = null,
    val discoverySources: Set<String> = setOf("LAN probe"),
    val services: List<String> = emptyList(),
    val ipv6Addresses: List<String> = emptyList(),
    val identityLabel: String = "Network device",
    val identityConfidence: Int = 20,
    val identityEvidence: List<String> = emptyList(),
)
data class OpenLanPort(val port: Int, val service: String, val banner: String? = null)
enum class PortScanProfile { QUICK, EXTENDED }

/** User-started, bounded /24 discovery. It never probes beyond the active LAN. */
internal object LanDiscovery {
    suspend fun scan(localIp: String?, knownIps: Set<String>): List<LanDevice> = withContext(Dispatchers.IO) {
        val parts = localIp?.split('.')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        val prefix = parts.takeIf { it.size == 4 }?.take(3)?.joinToString(".")
        val reachable = if (prefix == null) emptyList() else (1..254).chunked(24).flatMap { batch ->
            coroutineScope {
                batch.map { suffix -> async {
                    val host = "$prefix.$suffix"
                    val start = System.nanoTime()
                    runCatching {
                        val address = InetAddress.getByName(host)
                        if (!isReachable(address, host)) null else LanDevice(
                            ipAddress = host,
                            hostName = address.canonicalHostName.takeUnless { it == host }.orEmpty().ifBlank { "Local device" },
                            latencyMs = ((System.nanoTime() - start) / 1_000_000).toInt(),
                            isNew = host !in knownIps,
                        )
                    }.getOrNull()
                } }.awaitAll().filterNotNull()
            }
        }.sortedBy { it.ipAddress.substringAfterLast('.').toIntOrNull() ?: 999 }
        val neighbors = readNeighborTable()
        val ipv6 = readIpv6Neighbors()
        val passive = discoverSsdp() + discoverMdns()
        (reachable.map { device -> device.copy(macAddress = neighbors[device.ipAddress], ipv6Addresses = ipv6[device.macAddress ?: neighbors[device.ipAddress]].orEmpty()) } + passive)
            .groupBy(LanDevice::ipAddress)
            .map { (_, matches) ->
                val best = matches.maxByOrNull { it.hostName.length } ?: matches.first()
                best.copy(
                    isNew = matches.all(LanDevice::isNew) && best.ipAddress !in knownIps,
                    discoverySources = matches.flatMap { it.discoverySources }.toSet(),
                    services = matches.flatMap { it.services }.distinct(),
                    macAddress = matches.firstNotNullOfOrNull(LanDevice::macAddress),
                    ipv6Addresses = matches.flatMap { it.ipv6Addresses }.distinct(),
                )
            }.map(::enrichIdentity).sortedBy { it.ipAddress }
    }

    internal fun parseSsdpResponse(text: String, senderIp: String, knownIps: Set<String> = emptySet()): LanDevice {
        val headers = text.lineSequence().drop(1).mapNotNull { line ->
            val split = line.indexOf(':'); if (split <= 0) null else line.substring(0, split).trim().lowercase() to line.substring(split + 1).trim()
        }.toMap()
        val location = headers["location"]
        val locationHost = runCatching { URI(location).host }.getOrNull()
        val host = locationHost ?: senderIp
        val label = headers["server"]?.substringBefore('/')?.trim().orEmpty().ifBlank { headers["st"].orEmpty().substringAfterLast(':').ifBlank { "UPnP device" } }
        return LanDevice(
            host, label, null, host !in knownIps,
            discoverySources = setOf("SSDP/UPnP"),
            services = listOfNotNull(headers["server"], headers["st"], headers["usn"], location).distinct(),
        )
    }

    private fun discoverSsdp(): List<LanDevice> = runCatching {
        val request = ("M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: ssdp:all\r\n\r\n").encodeToByteArray()
        val results = mutableListOf<LanDevice>()
        DatagramSocket().use { socket ->
            socket.soTimeout = 350
            socket.send(DatagramPacket(request, request.size, InetAddress.getByName("239.255.255.250"), 1900))
            val deadline = System.currentTimeMillis() + 1_500
            while (System.currentTimeMillis() < deadline) {
                val buffer = ByteArray(4096); val packet = DatagramPacket(buffer, buffer.size)
                try { socket.receive(packet); results += parseSsdpResponse(String(packet.data, 0, packet.length), packet.address.hostAddress.orEmpty()) }
                catch (_: SocketTimeoutException) { }
            }
        }
        results.distinctBy { it.ipAddress to it.services }
    }.getOrDefault(emptyList())

    private fun discoverMdns(): List<LanDevice> = runCatching {
        val group = InetAddress.getByName("224.0.0.251")
        val queryName = encodeDnsName("_services._dns-sd._udp.local")
        val query = byteArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0) + queryName + byteArrayOf(0, 12, 0, 1)
        val found = mutableMapOf<String, MutableSet<String>>()
        MulticastSocket(null).use { socket ->
            socket.reuseAddress = true; socket.bind(InetSocketAddress(5353)); socket.soTimeout = 300
            @Suppress("DEPRECATION") socket.joinGroup(group)
            socket.send(DatagramPacket(query, query.size, group, 5353))
            val deadline = System.currentTimeMillis() + 1_200
            while (System.currentTimeMillis() < deadline) {
                val buffer = ByteArray(8192); val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                    val ip = packet.address.hostAddress.orEmpty().substringBefore('%')
                    val labels = extractDnsLabels(packet.data, packet.length)
                    found.getOrPut(ip) { linkedSetOf() }.addAll(labels)
                } catch (_: SocketTimeoutException) { }
            }
            @Suppress("DEPRECATION") socket.leaveGroup(group)
        }
        found.map { (ip, labels) -> LanDevice(ip, labels.firstOrNull { it.endsWith(".local") } ?: "mDNS device", null, true, discoverySources = setOf("mDNS"), services = labels.filter { it.startsWith("_") }.take(8)) }
    }.getOrDefault(emptyList())

    private fun encodeDnsName(name: String): ByteArray = name.split('.').flatMap { label -> listOf(label.length.toByte()) + label.encodeToByteArray().toList() }.toByteArray() + 0

    private fun extractDnsLabels(bytes: ByteArray, length: Int): Set<String> {
        val text = String(bytes, 0, length, Charsets.ISO_8859_1)
        return Regex("[_A-Za-z0-9-]{2,}(?:\\.[_A-Za-z0-9-]{2,})+\\.?").findAll(text.replace(Regex("[\\x00-\\x1F]"), "."))
            .map { it.value.trim('.').take(100) }.filter { it.contains("local", true) || it.startsWith("_") }.toSet()
    }

    suspend fun scanPorts(host: String, profile: PortScanProfile): List<OpenLanPort> = withContext(Dispatchers.IO) {
        val services = linkedMapOf(
            21 to "FTP", 22 to "SSH", 23 to "Telnet", 53 to "DNS", 80 to "HTTP",
            139 to "NetBIOS", 443 to "HTTPS", 445 to "SMB", 554 to "RTSP",
            631 to "IPP", 8008 to "HTTP", 8080 to "HTTP alt", 8443 to "HTTPS alt",
        )
        if (profile == PortScanProfile.EXTENDED) services.putAll(linkedMapOf(
            25 to "SMTP", 67 to "DHCP", 110 to "POP3", 123 to "NTP", 143 to "IMAP",
            389 to "LDAP", 515 to "Printer", 587 to "SMTP submission", 993 to "IMAPS",
            995 to "POP3S", 1883 to "MQTT", 3389 to "RDP", 5000 to "Service",
            5353 to "mDNS", 5900 to "VNC", 8000 to "HTTP alt", 8883 to "MQTTS",
            9000 to "Service", 9100 to "Printer",
        ))
        scanPortMap(host, services)
    }

    suspend fun scanCustomPorts(host: String, specification: String): List<OpenLanPort> = withContext(Dispatchers.IO) {
        val ports = parsePortSpecification(specification)
        scanPortMap(host, ports.associateWith { commonServiceName(it) })
    }

    internal fun parsePortSpecification(specification: String, maxPorts: Int = 256): List<Int> {
        val ports = linkedSetOf<Int>()
        specification.split(',').map(String::trim).filter(String::isNotBlank).forEach { token ->
            val range = token.split('-', limit = 2).map(String::trim)
            if (range.size == 1) {
                range[0].toIntOrNull()?.takeIf { it in 1..65535 }?.let(ports::add)
            } else {
                val start = range[0].toIntOrNull()
                val end = range[1].toIntOrNull()
                if (start != null && end != null) {
                    for (port in minOf(start, end)..maxOf(start, end)) {
                        if (port in 1..65535) ports += port
                        if (ports.size >= maxPorts) break
                    }
                }
            }
        }
        return ports.take(maxPorts)
    }

    suspend fun wakeOnLan(macAddress: String, localIp: String?): Boolean = withContext(Dispatchers.IO) {
        val mac = macAddress.replace(Regex("[^0-9A-Fa-f]"), "")
        if (mac.length != 12) return@withContext false
        val macBytes = ByteArray(6) { index -> mac.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
        val packet = ByteArray(102).also { bytes ->
            repeat(6) { bytes[it] = 0xFF.toByte() }
            repeat(16) { copy -> macBytes.copyInto(bytes, 6 + copy * 6) }
        }
        val broadcasts = linkedSetOf("255.255.255.255")
        localIp?.split('.')?.takeIf { it.size == 4 }?.let { broadcasts += "${it[0]}.${it[1]}.${it[2]}.255" }
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                broadcasts.forEach { host -> socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName(host), 9)) }
            }
        }.isSuccess
    }

    private suspend fun scanPortMap(host: String, services: Map<Int, String>): List<OpenLanPort> = coroutineScope {
            services.map { (port, service) -> async {
                runCatching {
                    var banner: String? = null
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), 320); socket.soTimeout = 320
                        if (port in setOf(80, 8000, 8008, 8080)) {
                            socket.getOutputStream().write("HEAD / HTTP/1.0\r\nHost: $host\r\n\r\n".encodeToByteArray())
                            banner = runCatching { socket.getInputStream().bufferedReader().readLine()?.take(80) }.getOrNull()
                        } else if (port == 22) banner = runCatching { socket.getInputStream().bufferedReader().readLine()?.take(80) }.getOrNull()
                    }
                    val richBanner = when (port) {
                        80, 8000, 8008, 8080 -> probeHttpIdentity(host, port, false) ?: banner
                        443, 8443 -> probeHttpIdentity(host, port, true) ?: banner
                        else -> banner
                    }
                    OpenLanPort(port, service, richBanner)
                }.getOrNull()
            } }.awaitAll().filterNotNull().sortedBy(OpenLanPort::port)
    }

    private fun commonServiceName(port: Int): String = when (port) {
        21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"; 25 -> "SMTP"; 53 -> "DNS"; 80 -> "HTTP"
        110 -> "POP3"; 123 -> "NTP"; 139 -> "NetBIOS"; 143 -> "IMAP"; 389 -> "LDAP"
        443 -> "HTTPS"; 445 -> "SMB"; 515 -> "Printer"; 554 -> "RTSP"; 631 -> "IPP"
        993 -> "IMAPS"; 995 -> "POP3S"; 1883 -> "MQTT"; 3389 -> "RDP"; 5353 -> "mDNS"
        5900 -> "VNC"; 8080 -> "HTTP alt"; 8443 -> "HTTPS alt"; 8883 -> "MQTTS"; 9100 -> "Printer"
        else -> "TCP"
    }

    internal fun enrichIdentity(device: LanDevice, ports: List<OpenLanPort> = emptyList()): LanDevice {
        val evidence = buildList {
            device.vendor?.takeUnless { it.startsWith("Vendor") }?.let { add(it) }
            addAll(device.services.take(5))
            addAll(ports.mapNotNull(OpenLanPort::banner).take(3))
        }
        val haystack = (listOf(device.hostName, device.vendor.orEmpty()) + device.services + ports.map { "${it.service} ${it.banner.orEmpty()}" }).joinToString(" ").lowercase()
        val label = when {
            ports.any { it.port in setOf(515, 631, 9100) } || listOf("printer", "ipp", "airprint").any(haystack::contains) -> "Printer"
            ports.any { it.port == 554 } || listOf("camera", "onvif", "rtsp").any(haystack::contains) -> "Camera / media"
            listOf("chromecast", "googlecast", "roku", "airplay", "appletv").any(haystack::contains) -> "Streaming device"
            listOf("homekit", "hap", "mqtt", "esphome", "tuya").any(haystack::contains) -> "Smart home / IoT"
            listOf("unifi", "ubiquiti", "omada", "openwrt", "routeros", "gateway").any(haystack::contains) -> "Router / access point"
            ports.any { it.port in setOf(445, 139) } -> "Computer / NAS"
            ports.any { it.port in setOf(80, 443, 8000, 8008, 8080, 8443) } -> "Web-managed device"
            else -> "Network device"
        }
        val confidence = (
            20 +
                (if (!device.vendor.isNullOrBlank()) 20 else 0) +
                (if (device.services.isNotEmpty()) 25 else 0) +
                (if (ports.isNotEmpty()) 25 else 0)
            ).coerceAtMost(95)
        return device.copy(identityLabel = label, identityConfidence = confidence, identityEvidence = evidence.distinct())
    }

    private fun probeHttpIdentity(host: String, port: Int, tls: Boolean): String? = runCatching {
        val scheme = if (tls) "https" else "http"
        val connection = URL("$scheme://$host:$port/").openConnection() as HttpURLConnection
        connection.connectTimeout = 650; connection.readTimeout = 650
        connection.setRequestProperty("User-Agent", "PureHub local device identity probe")
        val code = connection.responseCode
        val server = connection.getHeaderField("Server")?.take(80)
        val certificate = (connection as? HttpsURLConnection)?.serverCertificates?.firstOrNull()
            ?.let { it as? java.security.cert.X509Certificate }
            ?.subjectX500Principal?.getName(X500Principal.RFC2253)?.substringAfter("CN=")?.substringBefore(',')?.take(80)
        val title = runCatching {
            connection.inputStream.bufferedReader().use { reader ->
                val body = CharArray(8_192); val count = reader.read(body)
                if (count <= 0) null else Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(String(body, 0, count))?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim()?.take(80)
            }
        }.getOrNull()
        connection.disconnect()
        listOfNotNull("HTTP $code", server, title, certificate).joinToString(" · ")
    }.getOrNull()

    private fun isReachable(address: InetAddress, host: String): Boolean {
        if (runCatching { address.isReachable(160) }.getOrDefault(false)) return true
        return listOf(80, 443, 22, 445).any { port ->
            runCatching {
                Socket().use { socket -> socket.connect(InetSocketAddress(host, port), 90) }
                true
            }.getOrDefault(false)
        }
    }

    private fun readNeighborTable(): Map<String, String> {
        val macPattern = Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")
        val commands = listOf(listOf("ip", "neigh", "show"), listOf("cat", "/proc/net/arp"))
        return commands.firstNotNullOfOrNull { command ->
            runCatching {
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                val rows = process.inputStream.bufferedReader().readLines()
                process.waitFor()
                rows.mapNotNull { row ->
                    val ip = row.trim().substringBefore(' ').takeIf { value -> value.split('.').size == 4 }
                    val mac = macPattern.find(row)?.value?.uppercase()
                    if (ip != null && mac != null && mac != "00:00:00:00:00:00") ip to mac else null
                }.toMap().takeIf(Map<String, String>::isNotEmpty)
            }.getOrNull()
        }.orEmpty()
    }

    private fun readIpv6Neighbors(): Map<String, List<String>> {
        val macPattern = Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")
        val ipv6Pattern = Regex("(?i)(?:[0-9a-f]{0,4}:){2,7}[0-9a-f]{0,4}")
        return runCatching {
            val process = ProcessBuilder("ip", "-6", "neigh", "show").redirectErrorStream(true).start()
            val rows = process.inputStream.bufferedReader().readLines(); process.waitFor()
            rows.mapNotNull { row ->
                val mac = macPattern.find(row)?.value?.uppercase() ?: return@mapNotNull null
                val address = ipv6Pattern.find(row)?.value ?: return@mapNotNull null
                mac to address.substringBefore('%')
            }.groupBy({ it.first }, { it.second })
        }.getOrDefault(emptyMap())
    }

}
