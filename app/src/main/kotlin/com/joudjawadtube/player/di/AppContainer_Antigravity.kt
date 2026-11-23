package com.joudjawadtube.player.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joudjawadtube.player.data.local.AppDatabase
import com.joudjawadtube.player.data.network.NetworkMonitor_Antigravity
import com.joudjawadtube.player.data.repository.KidsCatalogRepository_Antigravity
import com.joudjawadtube.player.data.repository.VideoRepository
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.settings.WatchTimerManager_Antigravity
import com.joudjawadtube.player.data.thumbnail.ThumbnailManager
import com.joudjawadtube.player.data.updater.AppUpdater_Antigravity
import com.joudjawadtube.player.ui.catalog.CatalogViewModel

/**
 * Application-scoped dependency container holding shared singletons and ViewModel factories.
 */
class AppContainer_Antigravity(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    val videoRepository: VideoRepository by lazy {
        VideoRepository(appContext, database.videoDao())
    }

    val settingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository.getInstance(appContext)
    }

    val appUpdater: AppUpdater_Antigravity by lazy {
        AppUpdater_Antigravity(appContext)
    }

    val networkMonitor: NetworkMonitor_Antigravity by lazy {
        NetworkMonitor_Antigravity(appContext)
    }

    val watchTimerManager: WatchTimerManager_Antigravity by lazy {
        WatchTimerManager_Antigravity.shared
    }

    val catalogViewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
                    return CatalogViewModel(
                        repository = videoRepository,
                        settingsRepository = settingsRepository,
                        appUpdater = appUpdater,
                        networkMonitor = networkMonitor,
                        kidsChannelsLoader = { lang ->
                            KidsCatalogRepository_Antigravity.loadKidsChannels(appContext, lang)
                        },
                        isBuiltInChannelCheck = { channelId, lang ->
                            KidsCatalogRepository_Antigravity.loadKidsChannels(appContext, lang)
                                .any { it.channelId == channelId }
                        },
                        currentVersionProvider = {
                            runCatching {
                                val pInfo = appContext.packageManager.getPackageInfo(
                                    appContext.packageName,
                                    0
                                )
                                pInfo.versionName ?: "1.0.0"
                            }.getOrDefault("1.0.0")
                        },
                        thumbnailResolver = { video, settings, forceRefresh ->
                            ThumbnailManager.resolveOrGenerateThumbnail(
                                context = appContext,
                                video = video,
                                settings = settings,
                                videoDao = database.videoDao(),
                                forceRefresh = forceRefresh
                            )
                        },
                        thumbnailCacheClearer = {
                            ThumbnailManager.clearThumbnailCache(appContext)
                        },
                        customFrameExtractor = { video, fraction ->
                            val targetMs = if (video.durationMs > 5_000L) {
                                (video.durationMs * fraction).toLong()
                            } else {
                                3_000L
                            }
                            ThumbnailManager.generateCustomFrameThumbnailAt(
                                context = appContext,
                                video = video,
                                positionMs = targetMs,
                                videoDao = database.videoDao()
                            )
                        },
                        observeUsbEvents = true,
                        videoCountProvider = { database.videoDao().getVideoCount() }
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
