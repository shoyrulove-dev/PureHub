package com.purehub.app.feature.wifi

import android.net.DhcpInfo
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

data class WifiDiagnosticResult(
    val timestamp: Long,
    val gatewayMs: Int?,
    val internetMs: Int?,
    val jitterMs: Int?,
    val packetLossPercent: Int?,
    val dnsMs: Int?,
    val dnsProvider: String?,
    val loadedLatencyMs: Int?,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val speedServer: String = "Cloudflare edge · speed.cloudflare.com",
    val testProfile: String = WifiSpeedProfile.BALANCED.label,
    val durationMs: Long = 0,
    val transferredBytes: Long = 0,
    val downloadSamplesMbps: List<Double> = emptyList(),
    val uploadSamplesMbps: List<Double> = emptyList(),
)

internal object WifiDiagnostics {
    fun ipv4(value: Int): String? {
        if (value == 0) return null
        return listOf(value and 0xff, value shr 8 and 0xff, value shr 16 and 0xff, value shr 24 and 0xff).joinToString(".")
    }

    fun gatewayHost(info: DhcpInfo?): String? = info?.gateway?.let(::ipv4)

    fun run(
        gateway: String?,
        profile: WifiSpeedProfile = WifiSpeedProfile.BALANCED,
        endpoints: List<WifiSpeedEndpoint> = listOf(cloudflareEndpoint),
        progress: (WifiSpeedProgress) -> Unit = {},
    ): WifiDiagnosticResult {
        val testStarted = System.currentTimeMillis()
        progress(WifiSpeedProgress("Latency", 4))
        val gatewaySamples = gateway?.let { sampleTcp(it, 80, 3) }.orEmpty()
        val internetSamples = sampleTcp("1.1.1.1", 443, 4)
        progress(WifiSpeedProgress("DNS", 12))
        val dnsResult = listOf("Cloudflare" to "1.1.1.1", "Google" to "8.8.8.8", "Quad9" to "9.9.9.9")
            .mapNotNull { (name, host) -> dnsQueryMs(host)?.let { name to it } }.minByOrNull { it.second }
        progress(WifiSpeedProgress("Selecting server", 15))
        val endpoint = selectEndpoint(endpoints.ifEmpty { listOf(cloudflareEndpoint) })
        val download = downloadWithLoadedLatency(profile, endpoint, progress)
        val upload = runCatching { uploadMbps(profile, endpoint, progress) }.getOrNull()
        val totalBytes = download.bytes + (upload?.bytes ?: 0L)
        progress(WifiSpeedProgress("Complete", 100, upload?.mbps, totalBytes))
        return WifiDiagnosticResult(
            System.currentTimeMillis(), average(gatewaySamples), average(internetSamples), jitter(internetSamples),
            if (internetSamples.isEmpty()) 100 else (4 - internetSamples.size) * 25,
            dnsResult?.second, dnsResult?.first, download.loadedLatencyMs, download.mbps.takeIf { it > 0 }, upload?.mbps,
            speedServer = "${endpoint.label} · ${URL(endpoint.normalizedBaseUrl()).host}",
            testProfile = profile.label,
            durationMs = System.currentTimeMillis() - testStarted,
            transferredBytes = totalBytes,
            downloadSamplesMbps = download.samples,
            uploadSamplesMbps = upload?.samples.orEmpty(),
        )
    }

    fun quickHealth(gateway: String?): Pair<Boolean, Int?> {
        val gatewayReachable = gateway?.let { sampleTcp(it, 80, 1).isNotEmpty() } ?: false
        return gatewayReachable to sampleTcp("1.1.1.1", 443, 1).firstOrNull()
    }

    private fun sampleTcp(host: String, port: Int, attempts: Int): List<Int> = (1..attempts).mapNotNull {
        val start = System.nanoTime()
        runCatching { Socket().use { socket -> socket.connect(InetSocketAddress(host, port), 1_500); elapsedMs(start) } }.getOrNull()
    }

    private fun downloadWithLoadedLatency(profile: WifiSpeedProfile, endpoint: WifiSpeedEndpoint, progress: (WifiSpeedProgress) -> Unit): DownloadResult {
        val bytes = AtomicLong(0); val samples = mutableListOf<Double>(); val started = System.nanoTime()
        val perStream = (profile.maxBytes / profile.parallelStreams).coerceAtLeast(1_048_576)
        val streams = List(profile.parallelStreams) { index ->
            thread(start = true, name = "purehub-down-$index") { runCatching { downloadStream(endpoint, perStream, bytes) } }
        }
        Thread.sleep(180)
        val loadedLatency = average(sampleTcp("1.1.1.1", 443, 4))
        var previousBytes = 0L; var previousTime = System.nanoTime()
        while (streams.any(Thread::isAlive)) {
            Thread.sleep(250)
            val now = System.nanoTime(); val current = bytes.get()
            val live = (current - previousBytes) * 8_000.0 / (now - previousTime).coerceAtLeast(1)
            if (live.isFinite() && live > 0) samples += live
            previousBytes = current; previousTime = now
            progress(WifiSpeedProgress("Download", (18 + current * 52 / profile.maxBytes).toInt().coerceIn(18, 70), live, current))
        }
        streams.forEach(Thread::join)
        val elapsed = elapsedMs(started).coerceAtLeast(1)
        return DownloadResult(bytes.get() * 8.0 / (elapsed * 1_000.0), bytes.get(), loadedLatency, samples.takeLast(48))
    }

    private fun downloadStream(endpoint: WifiSpeedEndpoint, requestedBytes: Int, counter: AtomicLong) {
        (URL("${endpoint.normalizedBaseUrl()}/__down?bytes=$requestedBytes").openConnection() as HttpURLConnection).run {
            connectTimeout = 8_000; readTimeout = 18_000
            inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) { val read = input.read(buffer); if (read < 0) break; counter.addAndGet(read.toLong()) }
            }
        }
    }

    private fun uploadMbps(profile: WifiSpeedProfile, endpoint: WifiSpeedEndpoint, progress: (WifiSpeedProgress) -> Unit): UploadResult {
        val targetBytes = (profile.maxBytes / 4).coerceIn(512 * 1024, 16 * 1_048_576)
        val payload = ByteArray(targetBytes) { (it % 251).toByte() }; val started = System.nanoTime(); val samples = mutableListOf<Double>()
        (URL("${endpoint.normalizedBaseUrl()}/__up").openConnection() as HttpURLConnection).run {
            requestMethod = "POST"; doOutput = true; setFixedLengthStreamingMode(payload.size); connectTimeout = 8_000; readTimeout = 18_000
            outputStream.use { output ->
                var offset = 0
                while (offset < payload.size) {
                    val count = minOf(64 * 1024, payload.size - offset); output.write(payload, offset, count); offset += count
                    val live = offset * 8.0 / (elapsedMs(started).coerceAtLeast(1) * 1_000.0); samples += live
                    progress(WifiSpeedProgress("Upload", 72 + offset * 26 / payload.size, live, offset.toLong()))
                }
            }
            runCatching { inputStream.close() }
        }
        return UploadResult(payload.size * 8.0 / (elapsedMs(started).coerceAtLeast(1) * 1_000.0), payload.size.toLong(), samples.takeLast(48))
    }

    private fun dnsQueryMs(server: String): Int? = runCatching {
        val id = (System.nanoTime() and 0xffff).toInt()
        val name = "cloudflare.com".split('.').flatMap { label -> listOf(label.length.toByte()) + label.encodeToByteArray().toList() }
        val query = byteArrayOf((id shr 8).toByte(), id.toByte(), 1, 0, 0, 1, 0, 0, 0, 0, 0, 0) + name + byteArrayOf(0, 0, 1, 0, 1)
        val response = ByteArray(512); val start = System.nanoTime()
        DatagramSocket().use { socket -> socket.soTimeout = 1_500; socket.send(DatagramPacket(query, query.size, InetAddress.getByName(server), 53)); socket.receive(DatagramPacket(response, response.size)) }
        elapsedMs(start)
    }.getOrNull()

    internal fun selectEndpoint(endpoints: List<WifiSpeedEndpoint>): WifiSpeedEndpoint = endpoints
        .distinctBy { it.normalizedBaseUrl() }
        .mapNotNull { endpoint -> preflightMs(endpoint)?.let { endpoint to it } }
        .minByOrNull { it.second }
        ?.first
        ?: endpoints.firstOrNull()
        ?: cloudflareEndpoint

    private fun preflightMs(endpoint: WifiSpeedEndpoint): Int? = runCatching {
        val started = System.nanoTime()
        (URL("${endpoint.normalizedBaseUrl()}/__down?bytes=1024").openConnection() as HttpURLConnection).run {
            connectTimeout = 2_500
            readTimeout = 2_500
            setRequestProperty("Cache-Control", "no-store")
            inputStream.use { it.readBytes() }
            disconnect()
        }
        elapsedMs(started)
    }.getOrNull()

    private fun elapsedMs(start: Long) = ((System.nanoTime() - start) / 1_000_000).toInt()
    private fun average(samples: List<Int>) = samples.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    private fun jitter(samples: List<Int>) = if (samples.size < 2) null else samples.zipWithNext().map { abs(it.second - it.first) }.average().roundToInt()
    private data class DownloadResult(val mbps: Double, val bytes: Long, val loadedLatencyMs: Int?, val samples: List<Double>)
    private data class UploadResult(val mbps: Double, val bytes: Long, val samples: List<Double>)

    val cloudflareEndpoint = WifiSpeedEndpoint("cloudflare", "Cloudflare edge", "https://speed.cloudflare.com", builtIn = true)
}
