package com.joudjawadtube.player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_channels")
data class SavedChannelEntity(
    @PrimaryKey
    val channelId: String,
    val handle: String,
    val title: String,
    val avatarUrl: String = "",
    val uploadsPlaylistId: String = "",
    val importedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val videoCap: Int = 200
)
