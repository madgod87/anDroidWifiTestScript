package io.github.madgod87.wifigrid.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object NetworkTestUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun runSpeedTest(url: String, network: android.net.Network? = null): Double = withContext(Dispatchers.IO) {
        val testClient = if (network != null) {
            client.newBuilder()
                .socketFactory(network.socketFactory)
                .build()
        } else client

        val request = Request.Builder().url(url).build()
        val startTime = System.currentTimeMillis()
        var totalBytesRead = 0L
        try {
            testClient.newCall(request).execute().use { response ->
                val body = response.body ?: return@use 0.0
                val inputStream = body.byteStream()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytesRead += bytesRead
                }
                val endTime = System.currentTimeMillis()
                val durationSeconds = (endTime - startTime) / 1000.0
                if (durationSeconds <= 0.0) return@use 0.0
                val megabits = (totalBytesRead * 8) / 1_000_000.0
                return@use megabits / durationSeconds
            }
        } catch (e: Exception) { 
            android.util.Log.e("WIFI_GRID", "Speed test failed", e)
            0.0 
        }
    }
    
    suspend fun runUploadSpeedTest(url: String = "https://speed.cloudflare.com/__up", network: android.net.Network? = null): Double = withContext(Dispatchers.IO) {
        val testClient = if (network != null) {
            client.newBuilder()
                .socketFactory(network.socketFactory)
                .build()
        } else client

        val testDataSize = 5_000_000 // 5MB test
        val content = ByteArray(testDataSize)
        java.util.Random().nextBytes(content)
        
        val mediaType = "application/octet-stream".toMediaTypeOrNull()
        val requestBody = content.toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()
        
        val startTime = System.currentTimeMillis()
        try {
            testClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use 0.0
                val endTime = System.currentTimeMillis()
                val durationSeconds = (endTime - startTime) / 1000.0
                if (durationSeconds <= 0.0) return@use 0.0
                val megabits = (testDataSize.toLong() * 8) / 1_000_000.0
                return@use megabits / durationSeconds
            }
        } catch (e: Exception) {
            android.util.Log.e("WIFI_GRID", "Upload speed test failed", e)
            0.0
        }
    }

    suspend fun runLatencyMetrics(host: String = "1.1.1.1", port: Int = 443, network: android.net.Network? = null): LatencyData = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Long>()
        var failures = 0
        val totalAttempts = 20

        repeat(totalAttempts) {
            val start = System.currentTimeMillis()
            try {
                val socket = if (network != null) network.socketFactory.createSocket() else Socket()
                socket.use { s ->
                    s.connect(InetSocketAddress(host, port), 2000)
                    val end = System.currentTimeMillis()
                    samples.add(end - start)
                }
            } catch (e: Exception) {
                failures++
            }
            delay(100)
        }

        if (samples.isEmpty() && failures == 0) return@withContext LatencyData(0, 0, 0, emptyList())
        if (samples.isEmpty()) return@withContext LatencyData(0, 0, 100, emptyList())

        val avgLatency = samples.average().toLong()
        val jitter = calculateJitter(samples)
        val packetLoss = (failures * 100) / totalAttempts

        LatencyData(avgLatency, jitter, packetLoss, samples)
    }

    private fun calculateJitter(samples: List<Long>): Long {
        if (samples.size < 2) return 0
        var totalDiff = 0L
        for (i in 0 until samples.size - 1) {
            totalDiff += Math.abs(samples[i] - samples[i + 1])
        }
        return totalDiff / (samples.size - 1)
    }

    suspend fun runDnsTest(host: String = "google.com", network: android.net.Network? = null): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            if (network != null) {
                network.getAllByName(host)
            } else {
                java.net.InetAddress.getAllByName(host)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun diagnoseNoInternet(gatewayIp: String, network: android.net.Network? = null): String = withContext(Dispatchers.IO) {
        // Step 1: Ping Gateway
        val gatewayReachable = try {
            val socket = if (network != null) network.socketFactory.createSocket() else Socket()
            socket.use { s ->
                s.connect(InetSocketAddress(gatewayIp, 80), 1500) // Many gateways have web UI
                true
            }
        } catch (e: Exception) {
            // Backup check: generic socket connect
            try {
               val socket = if (network != null) network.socketFactory.createSocket() else Socket()
               socket.use { s -> s.connect(InetSocketAddress(gatewayIp, 53), 1000); true }
            } catch (e2: Exception) { false }
        }

        if (!gatewayReachable) return@withContext "LAYER 2 FAILURE: Gateway ($gatewayIp) is unreachable. Check Router power/cabling."

        // Step 2: Ping External IP (No DNS)
        val externalIpReachable = try {
            val socket = if (network != null) network.socketFactory.createSocket() else Socket()
            socket.use { s ->
                s.connect(InetSocketAddress("1.1.1.1", 443), 1500)
                true
            }
        } catch (e: Exception) { false }

        if (!externalIpReachable) return@withContext "LAYER 3 FAILURE: Gateway is OK, but no route to Internet. ISP or Modem issue."

        // Step 3: DNS Check
        val dnsReachable = runDnsTest("google.com", network) != -1L
        if (!dnsReachable) return@withContext "DNS FAILURE: Internet is active but DNS resolution failed. Check DNS server settings."

        "ALL SYSTEMS NOMINAL: Potential protocol-specific blocking or firewall interference."
    }

    data class LatencyData(val avgMs: Long, val jitterMs: Long, val lossPercent: Int, val samples: List<Long> = emptyList())
}
