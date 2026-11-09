package com.joudjawadtube.player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey
    val videoKey: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val playCount: Int = 0
)

@Entity(tableName = "saved_saf_folders")
data class SavedSafFolderEntity(
    @PrimaryKey
    val uriString: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_youtube_playlists")
data class SavedPlaylistEntity(
    @PrimaryKey
    val playlistId: String,
    val title: String,
    val author: String = "YouTube",
    val sourceUrl: String,
    val thumbnailUrl: String = "",
    val videoCount: Int = 0,
    val category: String = "General",
    val channelId: String = "",
    val isAutoCreatedFromChannel: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
