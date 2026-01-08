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
        val capabilities: String,
        val frequency: Int,
        val channel: Int,
        val congestionScore: Int = 0
    )

    fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency == 2484 -> 14
            frequency in 2407..2472 -> (frequency - 2407) / 5 + 1
            frequency in 5170..5925 -> (frequency - 5170) / 5 + 34
            else -> 0
        }
    }

    @Suppress("DEPRECATION")
    fun getNearbySsids(context: Context): List<ScannedNetwork> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val results = wifiManager.scanResults
        
        return results
            .filter { it.SSID.isNotBlank() }
            .map { 
                val channel = getChannelFromFrequency(it.frequency)
                // Calculate congestion: count how many other APs are on the same channel
                val congestion = results.count { res -> 
                    getChannelFromFrequency(res.frequency) == channel 
                }
                ScannedNetwork(it.SSID, it.level, it.capabilities, it.frequency, channel, congestion) 
            }
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
            
            writer.append("Timestamp,SSID,BSSID,RSSI,Channel,Gateway,DL_Mbps,UL_Mbps,Latency,Jitter,Loss,Score,Label\n")
            
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            results.forEach { res ->
                writer.append("${df.format(Date(res.timestamp))},")
                writer.append("${res.ssid},${res.bssid},${res.rssi},${res.channel},")
                writer.append("${res.gatewayIp},${res.downloadMbps},${res.uploadMbps},")
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

    suspend fun exportToPdf(context: Context, results: List<TestResult>): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Mission_Summary_$timestamp.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext null
            val file = File(downloadsDir, fileName)

            val pdfDocument = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint()
            val titlePaint = android.graphics.Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }
            val textPaint = android.graphics.Paint().apply {
                textSize = 12f
                color = android.graphics.Color.DKGRAY
            }

            // Create page
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Header
            canvas.drawText("WI-FI GRID MISSION SUMMARY", 40f, 60f, titlePaint)
            canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 40f, 85f, textPaint)
            canvas.drawLine(40f, 100f, 555f, 100f, textPaint)

            // Table Header
            var y = 130f
            paint.isFakeBoldText = true
            canvas.drawText("SSID", 40f, y, paint)
            canvas.drawText("DL (Mbps)", 180f, y, paint)
            canvas.drawText("UL (Mbps)", 260f, y, paint)
            canvas.drawText("Ping", 340f, y, paint)
            canvas.drawText("Score", 420f, y, paint)
            canvas.drawText("Label", 480f, y, paint)

            y += 20f
            canvas.drawLine(40f, y-10f, 555f, y-10f, textPaint)
            paint.isFakeBoldText = false

            // Draw Results (Last 25 for space)
            results.take(25).forEach { res ->
                canvas.drawText(res.ssid.take(20), 40f, y, textPaint)
                canvas.drawText(String.format("%.1f", res.downloadMbps), 180f, y, textPaint)
                canvas.drawText(String.format("%.1f", res.uploadMbps), 260f, y, textPaint)
                canvas.drawText("${res.latencyMs}ms", 340f, y, textPaint)
                canvas.drawText("${res.reliabilityScore}%", 420f, y, textPaint)
                canvas.drawText(res.qualityLabel, 480f, y, textPaint)
                y += 25f
                if (y > 800) return@forEach // Basic overflow protection
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
