package com.joudjawadtube.player.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.data.repository.KidsCatalogRepository_Antigravity
import com.joudjawadtube.player.data.repository.KidsChannelItem
import com.joudjawadtube.player.data.repository.KidsPlaylistItem
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeGold4K
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary

@Composable
fun KidsPicksScreen_Antigravity(
    savedPlaylistIds: Set<String>,
    onOpenPlaylist: (playlistId: String, title: String) -> Unit,
    onSavePlaylistToLibrary: (playlistId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val channels = remember { KidsCatalogRepository_Antigravity.loadKidsChannels(context) }
    var selectedLangFilter by remember { mutableStateOf<String?>("all") } // "all", "ar", "en"

    val filteredChannels = remember(selectedLangFilter, channels) {
        when (selectedLangFilter) {
            "ar" -> channels.filter { it.lang == "ar" }
            "en" -> channels.filter { it.lang == "en" }
            else -> channels
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SmartTubeGold4K.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Kids Picks",
                        tint = SmartTubeGold4K,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Kids Picks (Ages 5–9)",
                        color = SmartTubeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "14 Hand-picked Educational Channels (Arabic & English)",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Language Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LangFilterChip(
                    label = "All Channels",
                    isSelected = selectedLangFilter == "all",
                    onClick = { selectedLangFilter = "all" }
                )
                LangFilterChip(
                    label = "عربي (7)",
                    isSelected = selectedLangFilter == "ar",
                    onClick = { selectedLangFilter = "ar" }
                )
                LangFilterChip(
                    label = "English (7)",
                    isSelected = selectedLangFilter == "en",
                    onClick = { selectedLangFilter = "en" }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            items(
                items = filteredChannels,
                key = { it.channelId }
            ) { channel ->
                KidsChannelRowSection(
                    channel = channel,
                    savedPlaylistIds = savedPlaylistIds,
                    onOpenPlaylist = onOpenPlaylist,
                    onSavePlaylistToLibrary = onSavePlaylistToLibrary
                )
            }
        }
    }
}

@Composable
private fun KidsChannelRowSection(
    channel: KidsChannelItem,
    savedPlaylistIds: Set<String>,
    onOpenPlaylist: (playlistId: String, title: String) -> Unit,
    onSavePlaylistToLibrary: (playlistId: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = channel.channelName,
                    color = SmartTubeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (channel.lang == "ar") SmartTubeRed.copy(alpha = 0.2f) else Color(0xFF1E3A8A))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (channel.lang == "ar") "عربي" else "English",
                        color = if (channel.lang == "ar") SmartTubeRed else Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${channel.category} • ${channel.ageRange}",
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(
                items = channel.playlists,
                key = { "${channel.channelId}_${it.id}" }
            ) { playlist ->
                val isSaved = savedPlaylistIds.contains(playlist.id)
                KidsPlaylistCard(
                    playlist = playlist,
                    channel = channel,
                    isSaved = isSaved,
                    onOpen = { onOpenPlaylist(playlist.id, playlist.title) },
                    onSave = { onSavePlaylistToLibrary(playlist.id) }
                )
            }
        }
    }
}

@Composable
private fun KidsPlaylistCard(
    playlist: KidsPlaylistItem,
    channel: KidsChannelItem,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onOpen() }
            .padding(12.dp)
    ) {
        // Thumbnail placeholder banner with play icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2230)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isFocused) SmartTubeRed else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = playlist.title,
            color = SmartTubeTextPrimary,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = channel.channelName,
            color = SmartTubeTextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save / Saved indicator button
        if (isSaved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Saved",
                    tint = SmartTubeGold4K,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "In My Library",
                    color = SmartTubeGold4K,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add to Library", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LangFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSelected -> SmartTubeRed
                    isFocused -> SmartTubeCardFocusedBg
                    else -> SmartTubeCardBg
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
