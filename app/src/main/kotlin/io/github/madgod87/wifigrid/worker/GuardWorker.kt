package io.github.madgod87.wifigrid.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.madgod87.wifigrid.data.WifiDatabase
import io.github.madgod87.wifigrid.network.HardwareUtils
import io.github.madgod87.wifigrid.network.NetworkQualityAnalyzer
import io.github.madgod87.wifigrid.network.NetworkTestUtils
import io.github.madgod87.wifigrid.network.WifiConnector
import io.github.madgod87.wifigrid.data.TestResult
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class GuardWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = WifiDatabase.getDatabase(applicationContext)
        val connector = WifiConnector(applicationContext)
        val networks = db.dao().getSelectedNetworksSnapshot().filter { it.isGuardEnabled }

        if (networks.isEmpty()) return@withContext Result.success()

        networks.forEach { wifi ->
            try {
                val connectionResult = connector.connectToSsid(wifi.ssid, wifi.password)
                if (connectionResult is WifiConnector.ConnectionResult.Success) {
                    runDiagnostic(db, wifi.ssid, connectionResult.details, connectionResult.network)
                }
            } catch (e: Exception) {
                android.util.Log.e("GUARD_MODE", "Failed to test ${wifi.ssid}", e)
            } finally {
                // Cleanup
                val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                cm.bindProcessToNetwork(null)
            }
            delay(2000)
        }

        Result.success()
    }

    private suspend fun runDiagnostic(db: WifiDatabase, ssid: String, hw: WifiConnector.HardwareDetails, network: android.net.Network?) {
        val downloadSpeed = NetworkTestUtils.runSpeedTest("https://speed.cloudflare.com/__down?bytes=5000000", network)
        val uploadSpeed = NetworkTestUtils.runUploadSpeedTest(network = network)
        val metrics = NetworkTestUtils.runLatencyMetrics(network = network)
        val dnsTime = NetworkTestUtils.runDnsTest(network = network)
        val gateway = HardwareUtils.getGatewayIp(applicationContext)
        
        var trouble: String? = null
        if (downloadSpeed <= 0.1 && metrics.lossPercent >= 90) {
            trouble = NetworkTestUtils.diagnoseNoInternet(gateway, network)
        }

        val quality = NetworkQualityAnalyzer.calculateReliability(
            metrics.avgMs, 
            metrics.jitterMs, 
            metrics.lossPercent
        )

        val result = TestResult(
            ssid = ssid,
            timestamp = System.currentTimeMillis(),
            downloadMbps = downloadSpeed,
            uploadMbps = uploadSpeed,
            latencyMs = metrics.avgMs,
            jitterMs = metrics.jitterMs,
            packetLossPercent = metrics.lossPercent,
            dnsResolutionMs = dnsTime,
            dhcpTimeMs = 0,
            rssi = hw.rssi,
            frequency = hw.frequency,
            channel = HardwareUtils.getChannelFromFrequency(hw.frequency),
            bssid = hw.bssid,
            gatewayIp = gateway,
            reliabilityScore = quality.score,
            qualityLabel = quality.label,
            troubleshootingInfo = trouble
        )
        db.dao().insertResult(result)
    }
}
