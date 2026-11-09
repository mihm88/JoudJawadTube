package com.joudjawadtube.player.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY isYoutube DESC, isUsb DESC, folderName ASC, title ASC")
    fun observeAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos")
    suspend fun getAllVideosOnce(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY title ASC")
    suspend fun getVideosByFolder(folderPath: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE playlistId = :playlistId ORDER BY dateModified ASC, title ASC")
    suspend fun getVideosByPlaylistId(playlistId: String): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE isYoutube = 0")
    suspend fun clearLocalVideos()

    @Query("DELETE FROM videos WHERE isYoutube = 1")
    suspend fun clearYoutubeVideos()

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: String)

    @Query("DELETE FROM videos WHERE playlistId = :playlistId")
    suspend fun deleteVideosByPlaylistId(playlistId: String)

    @Query("DELETE FROM videos WHERE isYoutube = 0 AND id NOT IN (:activeIds)")
    suspend fun deleteMissingLocalVideos(activeIds: List<String>)

    @Query("UPDATE videos SET youtubeVideoId = :ytId, thumbnailUrl = :thumbUrl WHERE id = :videoId")
    suspend fun updateResolvedYouTubeThumbnail(videoId: String, ytId: String, thumbUrl: String)

    @Query("UPDATE videos SET customThumbnailPath = :customPath WHERE id = :videoId")
    suspend fun updateCustomThumbnail(videoId: String, customPath: String)

    /**
     * Diff-synchronizes local/USB videos without deleting saved YouTube videos or losing resolved
     * YouTube thumbnail URLs / custom thumbnail overrides.
     */
    @Transaction
    suspend fun replaceCatalog(scannedLocalVideos: List<VideoEntity>) {
        val existingAll = getAllVideosOnce().associateBy { it.id }
        val merged = scannedLocalVideos.map { scanned ->
            val prev = existingAll[scanned.id]
            if (prev != null) {
                scanned.copy(
                    youtubeVideoId = scanned.youtubeVideoId.ifBlank { prev.youtubeVideoId },
                    thumbnailUrl = scanned.thumbnailUrl.ifBlank { prev.thumbnailUrl },
                    customThumbnailPath = scanned.customThumbnailPath.ifBlank { prev.customThumbnailPath }
                )
            } else {
                scanned
            }
        }
        if (merged.isEmpty()) {
            clearLocalVideos()
        } else {
            val activeIds = merged.map { it.id }
            // SQLite max host parameters is 999, chunk if large
            if (activeIds.size <= 900) {
                deleteMissingLocalVideos(activeIds)
            } else {
                clearLocalVideos()
            }
            insertVideos(merged)
        }
    }

    @Query("SELECT * FROM playback_progress")
    fun observeAllProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE videoKey = :videoKey LIMIT 1")
    suspend fun getProgress(videoKey: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query("UPDATE playback_progress SET positionMs = 0, lastPlayedAt = 0, playCount = 0")
    suspend fun clearWatchHistory()

    @Query("UPDATE playback_progress SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Query("SELECT * FROM saved_saf_folders ORDER BY addedAt DESC")
    fun observeSafFolders(): Flow<List<SavedSafFolderEntity>>

    @Query("SELECT * FROM saved_saf_folders ORDER BY addedAt DESC")
    suspend fun getSafFolders(): List<SavedSafFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafFolder(folder: SavedSafFolderEntity)

    @Query("DELETE FROM saved_saf_folders WHERE uriString = :uriString")
    suspend fun deleteSafFolder(uriString: String)

    @Query("SELECT * FROM saved_youtube_playlists ORDER BY addedAt DESC")
    fun observeSavedPlaylists(): Flow<List<SavedPlaylistEntity>>

    @Query("SELECT * FROM saved_youtube_playlists ORDER BY addedAt DESC")
    suspend fun getSavedPlaylists(): List<SavedPlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: SavedPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<SavedPlaylistEntity>)

    @Query("UPDATE saved_youtube_playlists SET category = :category WHERE playlistId = :playlistId")
    suspend fun updatePlaylistCategory(playlistId: String, category: String)

    @Query("DELETE FROM saved_youtube_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM saved_youtube_playlists WHERE channelId = :channelId")
    suspend fun deletePlaylistsByChannelId(channelId: String)

    @Query("DELETE FROM saved_youtube_playlists")
    suspend fun clearAllPlaylists()

    @Query("SELECT * FROM saved_channels ORDER BY importedAt DESC")
    fun observeSavedChannels(): Flow<List<SavedChannelEntity>>

    @Query("SELECT * FROM saved_channels ORDER BY importedAt DESC")
    suspend fun getSavedChannels(): List<SavedChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: SavedChannelEntity)

    @Query("UPDATE saved_channels SET category = :category WHERE channelId = :channelId")
    suspend fun updateChannelCategory(channelId: String, category: String)

    @Query("DELETE FROM saved_channels WHERE channelId = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Query("DELETE FROM saved_channels")
    suspend fun clearAllChannels()
}
