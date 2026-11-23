package com.joudjawadtube.player

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.joudjawadtube.player.data.local.PlaybackProgressEntity
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.SavedSafFolderEntity
import com.joudjawadtube.player.data.local.VideoDao
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.network.NetworkMonitor_Antigravity
import com.joudjawadtube.player.data.repository.KidsChannelItem
import com.joudjawadtube.player.data.repository.KidsPlaylistItem
import com.joudjawadtube.player.data.repository.VideoRepository
import com.joudjawadtube.player.data.settings.AppLanguage
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.updater.AppUpdater_Antigravity
import com.joudjawadtube.player.ui.catalog.CatalogAction
import com.joudjawadtube.player.ui.catalog.CatalogUiState
import com.joudjawadtube.player.ui.catalog.CatalogViewModel
import com.joudjawadtube.player.ui.components.SidebarSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule_Antigravity : TestWatcher() {
    private val testDispatcher = UnconfinedTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class InMemorySharedPreferences_Antigravity(
    initialValues: Map<String, Any?> = emptyMap()
) : SharedPreferences {

    private val values = ConcurrentHashMap<String, Any?>().apply {
        initialValues.forEach { (k, v) -> if (v != null) put(k, v) }
    }
    private val listeners = ConcurrentHashMap.newKeySet<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = HashMap(values)

    override fun getString(key: String?, defValue: String?): String? =
        (values[key] as? String) ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (values[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (values[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (values[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (values[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = key != null && values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putStringSet(key: String?, valuesSet: MutableSet<String>?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = valuesSet?.toSet()
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = null
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            if (clearRequested) {
                values.clear()
            }
            val changedKeys = mutableListOf<String>()
            for ((k, v) in pending) {
                if (v == null) {
                    values.remove(k)
                } else {
                    values[k] = v
                }
                changedKeys.add(k)
            }
            changedKeys.forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(this@InMemorySharedPreferences_Antigravity, key) }
            }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) listeners.remove(listener)
    }
}

private class TestAppContext_Antigravity(
    private val prefs: SharedPreferences
) : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    override fun getSystemService(name: String): Any? = null
}

private class FakeVideoDao_Antigravity : VideoDao {
    private val videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    private val progress = MutableStateFlow<List<PlaybackProgressEntity>>(emptyList())
    private val safFolders = MutableStateFlow<List<SavedSafFolderEntity>>(emptyList())
    private val playlists = MutableStateFlow<List<SavedPlaylistEntity>>(emptyList())
    private val channels = MutableStateFlow<List<SavedChannelEntity>>(emptyList())

    override fun observeAllVideos(): Flow<List<VideoEntity>> = videos
    override suspend fun getAllVideosOnce(): List<VideoEntity> = videos.value
    override suspend fun getVideoCount(): Int = videos.value.size
    override suspend fun getVideoById(videoId: String): VideoEntity? = videos.value.firstOrNull { it.id == videoId }
    override suspend fun getVideosByFolder(folderPath: String): List<VideoEntity> =
        videos.value.filter { it.folderPath == folderPath }

    override suspend fun getVideosByPlaylistId(playlistId: String): List<VideoEntity> =
        videos.value.filter { it.playlistId == playlistId }

    override suspend fun insertVideos(videos: List<VideoEntity>) {
        val byId = this.videos.value.associateBy { it.id }.toMutableMap()
        videos.forEach { byId[it.id] = it }
        this.videos.value = byId.values.toList()
    }

    override suspend fun insertVideo(video: VideoEntity) {
        insertVideos(listOf(video))
    }

    override suspend fun clearLocalVideos() {
        videos.value = videos.value.filter { it.isYoutube }
    }

    override suspend fun clearYoutubeVideos() {
        videos.value = videos.value.filter { !it.isYoutube }
    }

    override suspend fun deleteVideoById(videoId: String) {
        videos.value = videos.value.filterNot { it.id == videoId }
    }

    override suspend fun deleteVideosByPlaylistId(playlistId: String) {
        videos.value = videos.value.filterNot { it.playlistId == playlistId }
    }

    override suspend fun deleteMissingLocalVideos(activeIds: List<String>) {
        val active = activeIds.toSet()
        videos.value = videos.value.filter { it.isYoutube || it.id in active }
    }

    override suspend fun updateResolvedYouTubeThumbnail(videoId: String, ytId: String, thumbUrl: String) {
        videos.value = videos.value.map {
            if (it.id == videoId) it.copy(youtubeVideoId = ytId, thumbnailUrl = thumbUrl) else it
        }
    }

    override suspend fun updateCustomThumbnail(videoId: String, customPath: String) {
        videos.value = videos.value.map {
            if (it.id == videoId) it.copy(customThumbnailPath = customPath) else it
        }
    }

    override fun observeAllProgress(): Flow<List<PlaybackProgressEntity>> = progress
    override suspend fun getProgress(videoKey: String): PlaybackProgressEntity? =
        progress.value.firstOrNull { it.videoKey == videoKey }

    override suspend fun getProgressForKeys(keys: List<String>): List<PlaybackProgressEntity> {
        val keySet = keys.toSet()
        return progress.value.filter { it.videoKey in keySet }
    }

    override suspend fun upsertProgress(progress: PlaybackProgressEntity) {
        val byKey = this.progress.value.associateBy { it.videoKey }.toMutableMap()
        byKey[progress.videoKey] = progress
        this.progress.value = byKey.values.toList()
    }

    override suspend fun clearWatchHistory() {
        progress.value = progress.value.map { it.copy(positionMs = 0L, lastPlayedAt = 0L, playCount = 0) }
    }

    override suspend fun clearAllFavorites() {
        progress.value = progress.value.map { it.copy(isFavorite = false) }
    }

    override fun observeSafFolders(): Flow<List<SavedSafFolderEntity>> = safFolders
    override suspend fun getSafFolders(): List<SavedSafFolderEntity> = safFolders.value
    override suspend fun insertSafFolder(folder: SavedSafFolderEntity) {
        safFolders.value = (safFolders.value.filterNot { it.uriString == folder.uriString } + folder)
    }

    override suspend fun deleteSafFolder(uriString: String) {
        safFolders.value = safFolders.value.filterNot { it.uriString == uriString }
    }

    override fun observeSavedPlaylists(): Flow<List<SavedPlaylistEntity>> = playlists
    override suspend fun getSavedPlaylists(): List<SavedPlaylistEntity> = playlists.value
    override suspend fun insertPlaylist(playlist: SavedPlaylistEntity) {
        insertPlaylists(listOf(playlist))
    }

    override suspend fun insertPlaylists(playlists: List<SavedPlaylistEntity>) {
        val byId = this.playlists.value.associateBy { it.playlistId }.toMutableMap()
        playlists.forEach { byId[it.playlistId] = it }
        this.playlists.value = byId.values.toList()
    }

    override suspend fun updatePlaylistCategory(playlistId: String, category: String) {
        playlists.value = playlists.value.map {
            if (it.playlistId == playlistId) it.copy(category = category) else it
        }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlists.value = playlists.value.filterNot { it.playlistId == playlistId }
    }

    override suspend fun deletePlaylistsByChannelId(channelId: String) {
        playlists.value = playlists.value.filterNot { it.channelId == channelId }
    }

    override suspend fun clearAllPlaylists() {
        playlists.value = emptyList()
    }

    override fun observeSavedChannels(): Flow<List<SavedChannelEntity>> = channels
    override suspend fun getSavedChannels(): List<SavedChannelEntity> = channels.value
    override suspend fun insertChannel(channel: SavedChannelEntity) {
        val byId = channels.value.associateBy { it.channelId }.toMutableMap()
        byId[channel.channelId] = channel
        channels.value = byId.values.toList()
    }

    override suspend fun updateChannelCategory(channelId: String, category: String) {
        channels.value = channels.value.map {
            if (it.channelId == channelId) it.copy(category = category) else it
        }
    }

    override suspend fun deleteChannel(channelId: String) {
        channels.value = channels.value.filterNot { it.channelId == channelId }
    }

    override suspend fun clearAllChannels() {
        channels.value = emptyList()
    }
}

class CatalogViewModelTest_Antigravity {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule_Antigravity()

    private lateinit var fakeDao: FakeVideoDao_Antigravity
    private lateinit var testContext: Context
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var networkMonitor: NetworkMonitor_Antigravity
    private lateinit var repository: VideoRepository
    private lateinit var viewModelStore: ViewModelStore

    private val sampleKidsChannels = listOf(
        KidsChannelItem(
            id = "ch1",
            channelId = "UC_ISLAMIC_1",
            handle = "@islamickids",
            channelName = "Islamic Kids",
            lang = "en",
            category = "Islamic Kids",
            uploadsPlaylistId = "UU_ISLAMIC_1",
            playlists = listOf(KidsPlaylistItem(id = "PL_ISLAMIC", title = "Quran Stories"))
        ),
        KidsChannelItem(
            id = "ch2",
            channelId = "UC_ENGLISH_2",
            handle = "@englishabc",
            channelName = "English Phonics",
            lang = "en",
            category = "English Learning",
            uploadsPlaylistId = "UU_ENGLISH_2",
            playlists = listOf(KidsPlaylistItem(id = "PL_ENGLISH", title = "ABC Rhymes"))
        )
    )

    private val videoIslamic = VideoEntity(
        id = "yt_PL_ISLAMIC_vid11111111",
        videoKey = "yt_vid11111111",
        uri = "https://www.youtube.com/watch?v=vid11111111",
        filePath = "",
        title = "Prophet Stories Episode 1",
        fileName = "Prophet Stories [vid11111111].mp4",
        folderName = "Islamic Kids",
        folderPath = "youtube_playlist_PL_ISLAMIC",
        volumeLabel = "YouTube Playlist",
        isUsb = false,
        durationMs = 300_000L,
        sizeBytes = 0L,
        width = 1920,
        height = 1080,
        mimeType = "video/mp4",
        dateModified = 2_000L,
        isYoutube = true,
        youtubeVideoId = "vid11111111",
        playlistId = "PL_ISLAMIC",
        playlistTitle = "Quran Stories"
    )

    private val videoEnglish = VideoEntity(
        id = "yt_PL_ENGLISH_vid22222222",
        videoKey = "yt_vid22222222",
        uri = "https://www.youtube.com/watch?v=vid22222222",
        filePath = "",
        title = "ABC Alphabet Phonics Song",
        fileName = "ABC Song [vid22222222].mp4",
        folderName = "English Phonics",
        folderPath = "youtube_playlist_PL_ENGLISH",
        volumeLabel = "YouTube Playlist",
        isUsb = false,
        durationMs = 180_000L,
        sizeBytes = 0L,
        width = 1920,
        height = 1080,
        mimeType = "video/mp4",
        dateModified = 1_000L,
        isYoutube = true,
        youtubeVideoId = "vid22222222",
        playlistId = "PL_ENGLISH",
        playlistTitle = "ABC Rhymes"
    )

    @Before
    fun setUp() {
        val prefs = InMemorySharedPreferences_Antigravity(
            mapOf(
                "auto_scan_startup" to false,
                "auto_scan_usb_mount" to false
            )
        )
        testContext = TestAppContext_Antigravity(prefs)
        fakeDao = FakeVideoDao_Antigravity()
        settingsRepository = AppSettingsRepository(testContext)
        networkMonitor = NetworkMonitor_Antigravity(testContext)
        repository = VideoRepository(testContext, fakeDao)
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        networkMonitor.close()
    }

    private fun createViewModel(
        kidsLoader: suspend (AppLanguage) -> List<KidsChannelItem> = { sampleKidsChannels }
    ): CatalogViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CatalogViewModel(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    appUpdater = AppUpdater_Antigravity(testContext),
                    networkMonitor = networkMonitor,
                    kidsChannelsLoader = kidsLoader,
                    isBuiltInChannelCheck = { cid, _ -> sampleKidsChannels.any { it.channelId == cid } },
                    currentVersionProvider = { "1.2.3" },
                    observeUsbEvents = false,
                    videoCountProvider = { fakeDao.getVideoCount() },
                    autoCheckUpdatesOnInit = false,
                    autoScanOnInit = false
                ) as T
            }
        }
        return ViewModelProvider(viewModelStore, factory)[CatalogViewModel::class.java]
    }

    private suspend fun awaitState(
        viewModel: CatalogViewModel,
        predicate: (CatalogUiState) -> Boolean
    ): CatalogUiState = withContext(Dispatchers.Default) {
        withTimeout(5_000L) {
            viewModel.uiState.first(predicate)
        }
    }

    @Test
    fun init_loadsKidsChannelsIntoUiState() = runTest {
        val viewModel = createViewModel()
        val state = awaitState(viewModel) { it.kidsChannels.size == 2 }

        assertEquals(2, state.kidsChannels.size)
        assertEquals("Islamic Kids", state.kidsChannels[0].channelName)
        assertEquals("English Phonics", state.kidsChannels[1].channelName)
    }

    @Test
    fun updateSearchQueryAndSelectTopCategory_filtersVideosAndRows() = runTest {
        fakeDao.insertPlaylist(
            SavedPlaylistEntity(
                playlistId = "PL_ISLAMIC",
                title = "Quran Stories",
                author = "Islamic Kids",
                sourceUrl = "https://www.youtube.com/playlist?list=PL_ISLAMIC",
                category = "Islamic Kids"
            )
        )
        fakeDao.insertPlaylist(
            SavedPlaylistEntity(
                playlistId = "PL_ENGLISH",
                title = "ABC Rhymes",
                author = "English Phonics",
                sourceUrl = "https://www.youtube.com/playlist?list=PL_ENGLISH",
                category = "English Learning"
            )
        )
        fakeDao.insertVideos(listOf(videoIslamic, videoEnglish))

        val viewModel = createViewModel()
        val initial = awaitState(viewModel) { it.filteredVideos.size == 2 }
        assertEquals(2, initial.filteredVideos.size)

        // Filter via CatalogAction.UpdateSearchQuery
        viewModel.onAction(CatalogAction.UpdateSearchQuery("Alphabet"))
        val searched = awaitState(viewModel) {
            it.searchQuery == "Alphabet" && it.filteredVideos.size == 1
        }
        assertEquals(videoEnglish.id, searched.filteredVideos.single().id)
        assertTrue(searched.filteredRows.any { row -> row.videos.any { it.id == videoEnglish.id } })
        assertFalse(searched.filteredRows.any { row -> row.videos.any { it.id == videoIslamic.id } })

        // Clear search and filter via CatalogAction.SelectTopCategory
        viewModel.onAction(CatalogAction.UpdateSearchQuery(""))
        viewModel.onAction(CatalogAction.SelectTopCategory("Islamic Kids"))
        val categorized = awaitState(viewModel) {
            it.searchQuery.isEmpty() &&
                it.selectedTopCategory == "Islamic Kids" &&
                it.filteredRows.any { row -> row.playlistId == "PL_ISLAMIC" } &&
                it.filteredRows.none { row -> row.playlistId == "PL_ENGLISH" }
        }
        assertTrue(categorized.filteredRows.all { row ->
            row.videos.all { it.playlistId == "PL_ISLAMIC" }
        })
    }

    @Test
    fun selectSectionAndToggleFavorite_updatesStateAndRepository() = runTest {
        fakeDao.insertVideos(listOf(videoIslamic, videoEnglish))
        val viewModel = createViewModel()
        val loaded = awaitState(viewModel) { it.snapshot.allVideos.size == 2 }
        val targetItem = loaded.snapshot.allVideos.first { it.id == videoIslamic.id }

        viewModel.onAction(CatalogAction.ToggleFavorite(targetItem))
        val favLoaded = awaitState(viewModel) { state ->
            state.snapshot.favoriteVideos.any { it.id == videoIslamic.id }
        }
        assertEquals(1, favLoaded.snapshot.favoriteVideos.size)

        viewModel.onAction(CatalogAction.SelectSection(SidebarSection.FAVORITES))
        val favSectionState = awaitState(viewModel) { state ->
            state.selectedSection == SidebarSection.FAVORITES && state.filteredVideos.size == 1
        }
        assertEquals(videoIslamic.id, favSectionState.filteredVideos.single().id)
    }

    @Test
    fun openVideoAndClosePlayer_updatesActivePlaybackSession() = runTest {
        val viewModel = createViewModel()
        val item = VideoItem(entity = videoEnglish, progress = null)

        assertNull(viewModel.activePlaybackSession.value)

        viewModel.onAction(CatalogAction.OpenVideo(item, listOf(item)))
        val session = viewModel.activePlaybackSession.value
        assertNotNull(session)
        assertEquals(item.id, session!!.currentVideo.id)
        assertEquals(1, session.playlist.size)

        viewModel.closePlayer()
        assertNull(viewModel.activePlaybackSession.value)
    }

    @Test
    fun updateSettings_persistsAndPropagatesToUiState() = runTest {
        val viewModel = createViewModel()
        awaitState(viewModel) { it.settings.parentalGateEnabled }

        viewModel.onAction(
            CatalogAction.UpdateSettings {
                it.copy(
                    parentalGateEnabled = false,
                    watchTimeLimitMinutes = 30,
                    backgroundPlaybackEnabled = false
                )
            }
        )

        val updated = awaitState(viewModel) {
            !it.settings.parentalGateEnabled &&
                it.settings.watchTimeLimitMinutes == 30 &&
                !it.settings.backgroundPlaybackEnabled
        }
        assertFalse(updated.settings.parentalGateEnabled)
        assertEquals(30, updated.settings.watchTimeLimitMinutes)
        assertFalse(updated.settings.backgroundPlaybackEnabled)
    }
}
