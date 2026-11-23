package com.joudjawadtube.player

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.StrictMode
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.joudjawadtube.player.data.scanner.UsbMountReceiver
import com.joudjawadtube.player.di.AppContainer_Antigravity

class JoudJawadTubeApp : Application(), ImageLoaderFactory {

    val appContainer: AppContainer_Antigravity by lazy { AppContainer_Antigravity(this) }

    private val dynamicUsbReceiver = UsbMountReceiver()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        appContainer.settingsRepository
        registerDynamicUsbMountListener()
    }

    private fun registerDynamicUsbMountListener() {
        runCatching {
            val mediaFilter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                addDataScheme("file")
            }
            ContextCompat.registerReceiver(
                this,
                dynamicUsbReceiver,
                mediaFilter,
                ContextCompat.RECEIVER_EXPORTED
            )

            val usbFilter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            ContextCompat.registerReceiver(
                this,
                dynamicUsbReceiver,
                usbFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("video_thumbnails_cache"))
                    .maxSizeBytes(250L * 1024L * 1024L) // 250 MB persistent thumbnail cache
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
