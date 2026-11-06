package com.joudjawadtube.player.data.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UsbStorageEvent(
    val action: String,
    val pathOrDevice: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class UsbMountReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val path = intent.data?.path ?: intent.dataString ?: action
        when (action) {
            Intent.ACTION_MEDIA_MOUNTED,
            Intent.ACTION_MEDIA_UNMOUNTED,
            Intent.ACTION_MEDIA_EJECT,
            Intent.ACTION_MEDIA_REMOVED,
            Intent.ACTION_MEDIA_BAD_REMOVAL,
            UsbManager.ACTION_USB_DEVICE_ATTACHED,
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                _usbEvents.value = UsbStorageEvent(
                    action = action,
                    pathOrDevice = path,
                    timestampMs = System.currentTimeMillis()
                )
            }
        }
    }

    companion object {
        private val _usbEvents = MutableStateFlow<UsbStorageEvent?>(null)
        val usbEvents: StateFlow<UsbStorageEvent?> = _usbEvents.asStateFlow()

        fun notifyManualRefresh() {
            _usbEvents.value = UsbStorageEvent(
                action = "MANUAL_RESCAN",
                pathOrDevice = "user",
                timestampMs = System.currentTimeMillis()
            )
        }
    }
}
