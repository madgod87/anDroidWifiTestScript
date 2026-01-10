package io.github.madgod87.wifigrid.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import io.github.madgod87.wifigrid.data.TestResult
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

    @Suppress("DEPRECATION")
    fun getCurrentRssi(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo.rssi
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
            val fileName = "GRID_Mission_Summary_$timestamp.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext null
            val file = File(downloadsDir, fileName)

            val pdfDocument = android.graphics.pdf.PdfDocument()
            
            // Colors (Light Mode Futuristic)
            val colorPureWhite = android.graphics.Color.WHITE
            val colorGridSteel = android.graphics.Color.parseColor("#E1E6EB")
            val colorNeonCyan = android.graphics.Color.parseColor("#00B8D4") // Slightly darkened for white bg
            val colorNeonPurple = android.graphics.Color.parseColor("#6200EA")
            val colorCyberPink = android.graphics.Color.parseColor("#D81B60")
            val colorDeepSpace = android.graphics.Color.parseColor("#121212")

            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Background & Grid
            canvas.drawColor(colorPureWhite)
            val gridPaint = android.graphics.Paint().apply {
                color = colorGridSteel
                strokeWidth = 0.5f
                alpha = 80
            }
            for (i in 0..595 step 40) canvas.drawLine(i.toFloat(), 0f, i.toFloat(), 842f, gridPaint)
            for (i in 0..842 step 40) canvas.drawLine(0f, i.toFloat(), 595f, i.toFloat(), gridPaint)

            // 2. Futuristic Header
            val headerPaint = android.graphics.Paint().apply {
                color = colorDeepSpace
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, 595f, 90f, headerPaint)
            
            val logoPaint = android.graphics.Paint().apply {
                color = colorNeonCyan
                strokeWidth = 2.5f
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            
            // Draw Stylized 'WG' Diamond Grid Logo
            val logoPath = android.graphics.Path().apply {
                // Outer Diamond
                moveTo(50f, 15f); lineTo(80f, 45f); lineTo(50f, 75f); lineTo(20f, 45f); close()
                // Internal Signal Arcs
                moveTo(35f, 35f); quadTo(50f, 20f, 65f, 35f)
                moveTo(42f, 42f); quadTo(50f, 34f, 58f, 42f)
            }
            canvas.drawPath(logoPath, logoPaint)
            
            // Draw 'WG' Letters inside logo area or near it
            val letterPaint = android.graphics.Paint().apply {
                textSize = 14f
                isFakeBoldText = true; color = colorNeonCyan; isAntiAlias = true
            }
            canvas.drawText("WG", 40f, 60f, letterPaint)

            val titlePaint = android.graphics.Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = android.graphics.Color.WHITE
                letterSpacing = 0.15f
            }
            canvas.drawText("WI-FI GRID", 85f, 40f, titlePaint)
            
            val subTitlePaint = android.graphics.Paint().apply {
                textSize = 9f
                color = android.graphics.Color.WHITE
                alpha = 180
            }
            canvas.drawText("QUANTUM SYSTEM DIAGNOSTIC REPORT", 85f, 55f, subTitlePaint)

            val metaHeaderPaint = android.graphics.Paint().apply {
                textSize = 8f
                color = colorNeonCyan
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            canvas.drawText("MISSION ID: #${timestamp.takeLast(6)}", 560f, 35f, metaHeaderPaint)
            canvas.drawText("DATE: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 560f, 50f, metaHeaderPaint)

            // 3. Narrative Data Section
            var currentPage = page
            var currentCanvas = canvas
            var y = 120f
            val sectionPaint = android.graphics.Paint().apply {
                textSize = 12f
                isFakeBoldText = true; color = colorDeepSpace
            }

            results.forEachIndexed { _, res ->
                // Check for page break
                if (y > 700) {
                    pdfDocument.finishPage(currentPage)
                    val nextPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    currentPage = pdfDocument.startPage(nextPageInfo)
                    currentCanvas = currentPage.canvas
                    
                    // Redraw Grid on new page
                    currentCanvas.drawColor(colorPureWhite)
                    for (i in 0..595 step 40) currentCanvas.drawLine(i.toFloat(), 0f, i.toFloat(), 842f, gridPaint)
                    for (i in 0..842 step 40) currentCanvas.drawLine(0f, i.toFloat(), 595f, i.toFloat(), gridPaint)
                    
                    y = 50f // Start higher on subsequent pages
                }

                // Node Card Frame
                val cardPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#F5F7FA") }
                currentCanvas.drawRoundRect(30f, y - 15f, 565f, y + 130f, 12f, 12f, cardPaint)
                currentCanvas.drawRoundRect(30f, y - 15f, 565f, y + 130f, 12f, 12f, android.graphics.Paint().apply {
                    color = colorNeonPurple; strokeWidth = 1f; style = android.graphics.Paint.Style.STROKE; alpha = 50
                })

                // Node Meta
                currentCanvas.drawText("NODE: ${res.ssid}", 45f, y+5f, sectionPaint)
                val smallLabelPaint = android.graphics.Paint().apply { textSize = 8f; color = android.graphics.Color.GRAY }
                currentCanvas.drawText("BSSID: ${res.bssid}  |  CH: ${res.channel} (${res.rssi} dBm)", 45f, y + 18f, smallLabelPaint)

                // High Level Metrics
                val metricPaint = android.graphics.Paint().apply { textSize = 11f; isFakeBoldText = true; color = colorDeepSpace }
                currentCanvas.drawText("DL: ${String.format("%.1f", res.downloadMbps)} Mbps", 430f, y+5f, metricPaint)
                currentCanvas.drawText("UL: ${String.format("%.1f", res.uploadMbps)} Mbps", 430f, y+18f, metricPaint)

                // 4. Ping Grid (The "20 Pings")
                currentCanvas.drawText("LATENCY MATRIX (20 SAMPLE STREAM)", 45f, y + 40f, android.graphics.Paint().apply { textSize = 9f; isFakeBoldText = true; color = colorNeonPurple })
                val samples = res.pingSamples?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                var gridX = 45f
                var gridY = y + 52f
                samples.take(20).forEachIndexed { i, s ->
                    val sVal = s.toLongOrNull() ?: 0L
                    val sColor = if(sVal < 50) colorNeonCyan else if(sVal < 150) colorNeonPurple else colorCyberPink
                    val sPaint = android.graphics.Paint().apply { textSize = 8f; color = sColor; isFakeBoldText = true }
                    currentCanvas.drawText("${s}ms", gridX, gridY, sPaint)
                    gridX += 35f
                    if ((i + 1) % 10 == 0) { gridX = 45f; gridY += 12f }
                }

                // 5. Mini Sparkline for this result
                if (samples.size > 1) {
                    val graphPaint = android.graphics.Paint().apply { color = colorNeonCyan; strokeWidth = 1.5f; style = android.graphics.Paint.Style.STROKE; isAntiAlias = true }
                    val graphPath = android.graphics.Path()
                    val maxPing = (samples.mapNotNull { it.toFloatOrNull() }.maxOrNull() ?: 100f).coerceAtLeast(50f)
                    val graphW = 120f
                    val graphH = 30f
                    val graphX = 430f
                    val graphBottom = y + 70f
                    
                    samples.take(20).forEachIndexed { i, s ->
                        val sv = s.toFloatOrNull() ?: 0f
                        val px = graphX + (i * (graphW / 19f))
                        val py = graphBottom - (sv / maxPing * graphH).coerceAtMost(graphH)
                        if (i == 0) graphPath.moveTo(px, py) else graphPath.lineTo(px, py)
                    }
                    currentCanvas.drawPath(graphPath, graphPaint)
                    currentCanvas.drawText("LATENCY SPARK", graphX, graphBottom + 10f, smallLabelPaint)
                }

                // Reliability Score Gauge
                val score = res.reliabilityScore
                val scoreColor = if(score > 80) colorNeonCyan else if(score > 50) colorNeonPurple else colorCyberPink
                currentCanvas.drawRect(430f, y + 90f, 430f + (score * 1.25f), y + 105f, android.graphics.Paint().apply { color = scoreColor })
                currentCanvas.drawText("SCORE: $score%", 430f, y + 118f, metricPaint)

                // Troubleshooting
                if (res.troubleshootingInfo != null) {
                    currentCanvas.drawText("DIAG: ${res.troubleshootingInfo}", 45f, y + 100f, android.graphics.Paint().apply { textSize = 8f; color = colorCyberPink; isFakeBoldText = true })
                }

                y += 150f
            }

            // Footer (on last page)
            val footerPaint = android.graphics.Paint().apply { textSize = 7f; color = android.graphics.Color.GRAY; textAlign = android.graphics.Paint.Align.CENTER }
            currentCanvas.drawText("GRID OS v4.1.0 | ENCRYPTED EXPORT | PAGES: ${pdfDocument.pages.size}", 297f, 820f, footerPaint)

            pdfDocument.finishPage(currentPage)
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
