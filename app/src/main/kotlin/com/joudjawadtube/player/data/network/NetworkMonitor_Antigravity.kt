package com.joudjawadtube.player.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable

/**
 * Monitors default network connectivity via [ConnectivityManager] and exposes
 * a reactive [isOnline] state flow for offline-friendly UI and playback hints.
 */
class NetworkMonitor_Antigravity(context: Context) : Closeable {

    private val connectivityManager: ConnectivityManager? =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(computeIsOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    @Volatile
    private var registered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = computeIsOnline()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = hasValidatedInternet(networkCapabilities)
        }

        override fun onLost(network: Network) {
            _isOnline.value = computeIsOnline()
        }

        override fun onUnavailable() {
            _isOnline.value = false
        }
    }

    init {
        val cm = connectivityManager
        if (cm != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(networkCallback)
                } else {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    cm.registerNetworkCallback(request, networkCallback)
                }
                registered = true
            }
        }
    }

    private fun computeIsOnline(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return hasValidatedInternet(capabilities)
    }

    private fun hasValidatedInternet(capabilities: NetworkCapabilities): Boolean {
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            true
        }
    }

    fun unregister() {
        if (registered) {
            runCatching {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            }
            registered = false
        }
    }

    override fun close() {
        unregister()
    }
}
