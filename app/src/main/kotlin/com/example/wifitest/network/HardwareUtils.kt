package com.example.wifitest.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import com.example.wifitest.data.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object HardwareUtils {

    @Suppress("DEPRECATION")
    fun triggerScan(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.startScan()
    }

    data class ScannedNetwork(
        val ssid: String,
        val rssi: Int,
        val capabilities: String
    )

    @Suppress("DEPRECATION")
    fun getNearbySsids(context: Context): List<ScannedNetwork> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.scanResults
            .filter { it.SSID.isNotBlank() }
            .map { ScannedNetwork(it.SSID, it.level, it.capabilities) }
            .sortedByDescending { it.rssi }
            .distinctBy { it.ssid }
    }

    @Suppress("DEPRECATION")
    fun getSavedSsids(context: Context): Set<String> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val saved = mutableSetOf<String>()
        try {
            wifiManager.configuredNetworks?.forEach { config ->
                config.SSID?.let { saved.add(it.removePrefix("\"").removeSuffix("\"")) }
            }
            android.util.Log.d("WIFI_HARDWARE", "Found ${saved.size} configured networks")
        } catch (e: Exception) {
            android.util.Log.e("WIFI_HARDWARE", "Error reading configured networks", e)
        }
        
        // Also add the currently connected SSID as definitely "known"
        getCurrentSsid(context)?.let { saved.add(it) }
        
        return saved
    }

    @Suppress("DEPRECATION")
    fun getCurrentSsid(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    capabilities.transportInfo as? android.net.wifi.WifiInfo
                } else {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifiManager.connectionInfo
                }
                return wifiInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it != "<unknown ssid>" }
            }
        }
        
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return info?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it != "<unknown ssid>" }
    }

    fun getGatewayIp(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        val gateway = linkProperties?.routes?.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway
        return gateway?.hostAddress ?: "0.0.0.0"
    }



    suspend fun exportToCsv(context: Context, results: List<TestResult>): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Wifi_Tests_$timestamp.csv"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext null
            
            val file = File(downloadsDir, fileName)
            val writer = FileWriter(file)
            
            writer.append("Timestamp,SSID,BSSID,RSSI,Freq,LinkSpeed,Gateway,Mbps,Latency,Jitter,Loss,Score,Label\n")
            
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            results.forEach { res ->
                writer.append("${df.format(Date(res.timestamp))},")
                writer.append("${res.ssid},${res.bssid},${res.rssi},${res.frequency},")
                writer.append("${res.linkSpeed},${res.gatewayIp},${res.downloadMbps},")
                writer.append("${res.latencyMs},${res.jitterMs},${res.packetLossPercent},")
                writer.append("${res.reliabilityScore},${res.qualityLabel}\n")
            }
            
            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
