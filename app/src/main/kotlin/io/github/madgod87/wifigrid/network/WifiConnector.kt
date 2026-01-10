package io.github.madgod87.wifigrid.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class WifiConnector(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    data class HardwareDetails(
        val rssi: Int,
        val frequency: Int,
        val linkSpeed: Int,
        val bssid: String
    )

    sealed class ConnectionResult {
        data class Success(val details: HardwareDetails, val network: android.net.Network? = null) : ConnectionResult()
        data class Failure(val reason: String) : ConnectionResult()
    }

    suspend fun connectToSsid(ssid: String, password: String? = null, timeoutMs: Long = 20000): ConnectionResult {
        android.util.Log.d("WIFI_CONN", "Attempting connection to $ssid (pass: ${password?.let { "****" } ?: "NONE"})")
        
        // Optimization: Check if already connected to this SSID
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            caps?.transportInfo as? WifiInfo
        } else null

        val currentSsid = info?.ssid?.removePrefix("\"")?.removeSuffix("\"")
        if (currentSsid == ssid) {
            android.util.Log.d("WIFI_CONN", "Already connected to $ssid")
            val details = HardwareDetails(
                rssi = info.rssi,
                frequency = info.frequency,
                linkSpeed = info.linkSpeed,
                bssid = info.bssid ?: "Connected"
            )
            return ConnectionResult.Success(details, activeNetwork)
        }

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                var callback: ConnectivityManager.NetworkCallback? = null
                try {
                    val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)

                    if (!password.isNullOrEmpty()) {
                        if (password.length < 8) {
                            android.util.Log.e("WIFI_CONN", "Passphrase too short for WPA2: ${password.length}")
                            continuation.resume(ConnectionResult.Failure("Passphrase must be at least 8 characters"))
                            return@suspendCancellableCoroutine
                        }
                        specifierBuilder.setWpa2Passphrase(password)
                    }

                    val specifier = specifierBuilder.build()

                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .setNetworkSpecifier(specifier)
                        .build()

                    callback = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            android.util.Log.i("WIFI_CONN", "Network available: $ssid")
                            val netCaps = connectivityManager.getNetworkCapabilities(network)
                            val netInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                netCaps?.transportInfo as? WifiInfo
                            } else null
                            
                            connectivityManager.bindProcessToNetwork(network)
                            
                            if (continuation.isActive) {
                                val details = HardwareDetails(
                                    rssi = netInfo?.rssi ?: 0,
                                    frequency = netInfo?.frequency ?: 0,
                                    linkSpeed = netInfo?.linkSpeed ?: 0,
                                    bssid = netInfo?.bssid ?: "Unknown"
                                )
                                continuation.resume(ConnectionResult.Success(details, network))
                            }
                        }

                        override fun onUnavailable() {
                            android.util.Log.e("WIFI_CONN", "Network unavailable: $ssid")
                            if (continuation.isActive) {
                                continuation.resume(ConnectionResult.Failure("Network unavailable or rejected. Ensure password is correct."))
                            }
                        }

                        override fun onLost(network: Network) {
                            android.util.Log.w("WIFI_CONN", "Network lost: $ssid")
                        }
                    }

                    connectivityManager.requestNetwork(request, callback)

                } catch (e: Exception) {
                    android.util.Log.e("WIFI_CONN", "Failed to build network request", e)
                    if (continuation.isActive) {
                        continuation.resume(ConnectionResult.Failure("System Error: ${e.message}"))
                    }
                }

                continuation.invokeOnCancellation {
                    android.util.Log.d("WIFI_CONN", "Connection request cancelled for $ssid")
                    try {
                        connectivityManager.bindProcessToNetwork(null)
                        callback?.let { connectivityManager.unregisterNetworkCallback(it) }
                    } catch (e: Exception) {
                        // Already unregistered
                    }
                }
            }
        } ?: ConnectionResult.Failure("Connection timed out after ${timeoutMs/1000}s. Check if network is in range.")
    }
}
