package com.example.wifitest.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.wifitest.R
import com.example.wifitest.data.TestResult
import com.example.wifitest.data.WifiDatabase
import com.example.wifitest.network.HardwareUtils
import com.example.wifitest.network.NetworkQualityAnalyzer
import com.example.wifitest.network.NetworkTestUtils
import com.example.wifitest.network.WifiConnector
import kotlinx.coroutines.*

class WifiTestService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: WifiDatabase
    private lateinit var connector: WifiConnector

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("WIFI_GRID", "Service onCreate")
        createNotificationChannel()
        db = WifiDatabase.getDatabase(this)
        connector = WifiConnector(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.i("WIFI_GRID", "Service onStartCommand received")
        
        val notification = buildNotification("Preparing Grid Environment...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
        
        sendStatus("HARDWARE LINK ESTABLISHED")
        
        scope.launch {
            try {
                android.util.Log.d("WIFI_GRID", "Starting test sequence coroutine")
                updateNotification("Scanning Grid Nodes...")
                
                // Allow OS resources to settle
                delay(1500)
                
                val health = withContext(Dispatchers.Main) { isBatteryHealthy() }
                if (!health) {
                    android.util.Log.w("WIFI_GRID", "Abort: System power low")
                    updateNotification("Critical: Power Low. Aborting.")
                    delay(3000)
                    finalizeService()
                    return@launch
                }

                android.util.Log.d("WIFI_GRID", "Fetching snapshots from DB")
                val networks = db.dao().getSelectedNetworksSnapshot()
                android.util.Log.i("WIFI_GRID", "Armed nodes detected: ${networks.size}")
                
                if (networks.isEmpty()) {
                    android.util.Log.w("WIFI_GRID", "Grid empty, nothing to test")
                    updateNotification("Grid Empty: No nodes armed.")
                    delay(2000)
                    finalizeService()
                    return@launch
                }

                networks.forEachIndexed { index, wifi ->
                    try {
                        val progress = "(${index + 1}/${networks.size})"
                        android.util.Log.i("WIFI_GRID", "Stage: Connecting to ${wifi.ssid} $progress")
                        updateNotification("Connecting: ${wifi.ssid} $progress")
                        
                        // Robust connection attempt
                        val connectionResult = withTimeoutOrNull(25000) {
                            connector.connectToSsid(wifi.ssid, wifi.password)
                        } ?: WifiConnector.ConnectionResult.Failure("Driver Timeout")
                        
                        when (connectionResult) {
                            is WifiConnector.ConnectionResult.Success -> {
                                android.util.Log.i("WIFI_GRID", "Interface bridged to ${wifi.ssid}")
                                updateNotification("Analyzing: ${wifi.ssid}")
                                runTest(wifi.ssid, connectionResult.details, connectionResult.network)
                                android.util.Log.i("WIFI_GRID", "Test cycle for ${wifi.ssid} complete")
                            }
                            is WifiConnector.ConnectionResult.Failure -> {
                                android.util.Log.e("WIFI_GRID", "Interface failure on ${wifi.ssid}: ${connectionResult.reason}")
                                updateNotification("Skipped: ${wifi.ssid} (No Link)")
                                delay(2000)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WIFI_GRID", "Exception in node cycle for ${wifi.ssid}", e)
                    } finally {
                        // Unbind to prevent leaking interface state
                        try {
                           val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                           cm.bindProcessToNetwork(null)
                        } catch (e: Exception) {}
                    }
                }
                
                android.util.Log.i("WIFI_GRID", "Mission log complete. All nodes evaluated.")
                updateNotification("Batch test complete.")
                delay(2000)
            } catch (e: Exception) {
                android.util.Log.e("WIFI_GRID", "Critical mission failure", e)
                updateNotification("System Error: Test Interrupted")
            } finally {
                finalizeService()
            }
        }
        return START_NOT_STICKY
    }

    private fun finalizeService() {
        android.util.Log.i("WIFI_GRID", "Cleaning up and stopping service")
        sendBroadcast(Intent("com.example.wifitest.TEST_FINISHED").setPackage(packageName))
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun isBatteryHealthy(): Boolean {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level == -1 || scale == -1) true
            else (level * 100 / scale.toFloat()) >= 15
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun runTest(ssid: String, hw: WifiConnector.HardwareDetails, network: android.net.Network? = null) {
        val downloadSpeed = NetworkTestUtils.runSpeedTest("https://speed.cloudflare.com/__down?bytes=10000000", network)
        val uploadSpeed = NetworkTestUtils.runUploadSpeedTest(network = network)
        val metrics = NetworkTestUtils.runLatencyMetrics(network = network)
        val dnsTime = NetworkTestUtils.runDnsTest(network = network)
        val gateway = HardwareUtils.getGatewayIp(this)
        
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
            dhcpTimeMs = 0, // Not easily measurable without low-level hook
            rssi = hw.rssi,
            frequency = hw.frequency,
            channel = HardwareUtils.getChannelFromFrequency(hw.frequency),
            bssid = hw.bssid,
            gatewayIp = gateway,
            reliabilityScore = quality.score,
            qualityLabel = quality.label,
            troubleshootingInfo = trouble,
            pingSamples = metrics.samples.joinToString(",")
        )
        db.dao().insertResult(result)
    }

    private fun sendStatus(status: String) {
        val intent = Intent("com.example.wifitest.STATUS_UPDATE")
        intent.putExtra("status", status)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun updateNotification(text: String) {
        sendStatus(text) // Sync UI and Notification
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, buildNotification(text))
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, "WIFI_TEST")
        .setContentTitle("Android Wi-Fi Test")
        .setContentText(text)
        .setSmallIcon(R.drawable.ic_wifi)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSilent(true)
        .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel("WIFI_TEST", "Wi-Fi Testing", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.i("WIFI_GRID", "Service onDestroy")
        scope.cancel()
    }
}
