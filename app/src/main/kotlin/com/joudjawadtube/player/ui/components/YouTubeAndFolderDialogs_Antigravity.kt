package com.joudjawadtube.player.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.scanner.LocalVideoScanner
import com.joudjawadtube.player.data.scanner.StorageVolumeHelper
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan
import java.io.File
import java.util.Locale

@Composable
fun AddYouTubeLinkDialog(
    isLoading: Boolean,
    errorMessage: String?,
    statusMessage: String?,
    onSubmitUrl: (url: String, playImmediately: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }
    var urlInput by remember { mutableStateOf("") }
    var inputFocused by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeSidebarBg)
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = null,
                        tint = SmartTubeRed,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Add YouTube Video or Playlist",
                            color = SmartTubeTextPrimary,
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Paste a YouTube video or playlist link to play in-app and save for later",
                            color = SmartTubeTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                DialogIconCloseButton(onClick = { if (!isLoading) onDismiss() })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // URL Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (inputFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                    .border(
                        width = if (inputFocused) 2.dp else 1.dp,
                        color = if (inputFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { inputFocused = it.isFocused }
                    .clickable {
                        runCatching { inputFocusRequester.requestFocus() }
                        keyboardController?.show()
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (urlInput.isEmpty()) {
                    Text(
                        text = "https://www.youtube.com/watch?v=... or playlist?list=...",
                        color = SmartTubeTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = SmartTubeTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(SmartTubeRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(inputFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clipboard & Clear helper buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPillButton(
                    icon = Icons.Default.ContentPaste,
                    text = "Paste from Clipboard",
                    isPrimary = false,
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val pasted = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                        if (!pasted.isNullOrBlank()) {
                            urlInput = pasted
                        }
                    }
                )
                if (urlInput.isNotEmpty()) {
                    ActionPillButton(
                        icon = Icons.Default.Close,
                        text = "Clear",
                        isPrimary = false,
                        onClick = { urlInput = "" }
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF6E6E),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMessage,
                    color = SmartTubeUsbCyan,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = SmartTubeRed,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Importing YouTube metadata & saving to library...",
                        color = SmartTubeTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionPillButton(
                        icon = Icons.Default.BookmarkAdd,
                        text = "Save for Later",
                        isPrimary = false,
                        onClick = { onSubmitUrl(urlInput, false) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ActionPillButton(
                        icon = Icons.Default.PlayArrow,
                        text = "Save & Play Now",
                        isPrimary = true,
                        onClick = { onSubmitUrl(urlInput, true) }
                    )
                }
            }
        }
    }
}

@Composable
fun TvFolderPickerDialog(
    onSelectFolder: (path: String, displayName: String) -> Unit,
    onLaunchSystemSafPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val volumes = remember(context) {
        StorageVolumeHelper.getMountedVolumes(context, hideBootPartitions = true)
    }

    var currentDir by remember {
        mutableStateOf<File?>(volumes.firstOrNull { it.isUsbOrRemovable }?.let { File(it.path) }
            ?: volumes.firstOrNull()?.let { File(it.path) })
    }

    val subDirs = remember(currentDir) {
        currentDir?.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            ?: emptyList()
    }

    val videoCountInCurrent = remember(currentDir) {
        currentDir?.listFiles()?.count {
            it.isFile && it.extension.lowercase(Locale.ROOT) in LocalVideoScanner.VIDEO_EXTENSIONS
        } ?: 0
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(580.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeSidebarBg)
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = SmartTubeUsbCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Select Video Folder (TV & USB Browser)",
                            color = SmartTubeTextPrimary,
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = currentDir?.absolutePath ?: "Choose a storage drive",
                            color = SmartTubeTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DialogIconCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Storage Root Volume Switcher Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                volumes.forEach { vol ->
                    ActionPillButton(
                        icon = if (vol.isUsbOrRemovable) Icons.Default.Usb else Icons.Default.Folder,
                        text = vol.label,
                        isPrimary = currentDir?.absolutePath?.startsWith(vol.path) == true,
                        onClick = { currentDir = File(vol.path) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            // Directory Navigation List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val parent = currentDir?.parentFile
                val canGoUp = parent != null && volumes.any { parent.absolutePath.startsWith(it.path) }
                if (canGoUp && parent != null) {
                    item(key = "parent_dir") {
                        FolderBrowserRow(
                            title = ".. (Parent Folder)",
                            subtitle = parent.absolutePath,
                            isUpRow = true,
                            onClick = { currentDir = parent }
                        )
                    }
                }

                if (subDirs.isEmpty()) {
                    item(key = "empty_subdirs") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No subfolders here ($videoCountInCurrent video files in this folder).",
                                color = SmartTubeTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                } else {
                    items(subDirs, key = { it.absolutePath }) { folder ->
                        FolderBrowserRow(
                            title = folder.name,
                            subtitle = folder.absolutePath,
                            isUpRow = false,
                            onClick = { currentDir = folder }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPillButton(
                    icon = Icons.Default.FolderOpen,
                    text = "System Picker",
                    isPrimary = false,
                    onClick = {
                        onDismiss()
                        onLaunchSystemSafPicker()
                    }
                )

                val selectedDir = currentDir
                if (selectedDir != null) {
                    ActionPillButton(
                        icon = Icons.Default.Check,
                        text = "Add \"${selectedDir.name.ifBlank { "Folder" }}\"",
                        isPrimary = true,
                        onClick = {
                            onSelectFolder(
                                selectedDir.absolutePath,
                                selectedDir.name.ifBlank { "Video Folder" }
                            )
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderBrowserRow(
    title: String,
    subtitle: String,
    isUpRow: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isUpRow) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Folder,
            contentDescription = null,
            tint = if (isUpRow) SmartTubeTextSecondary else SmartTubeUsbCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartTubeTextPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = SmartTubeTextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoOptionsDialog(
    video: VideoItem,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFetchYouTubeThumbnail: () -> Unit,
    onExtractFrameAtFraction: (Float) -> Unit,
    onDeleteVideo: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeSidebarBg)
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = SmartTubeTextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${video.folderName} • ${video.volumeLabel}",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                DialogIconCloseButton(onClick = onDismiss)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            OptionActionRow(
                icon = Icons.Default.PlayArrow,
                title = "Play Video Now",
                subtitle = "Open in player",
                onClick = {
                    onDismiss()
                    onPlay()
                }
            )

            OptionActionRow(
                icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (video.isFavorite) "Remove from Favorites" else "Add to Favorites",
                subtitle = "Save in Favorites tab",
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )

            OptionActionRow(
                icon = Icons.Default.OndemandVideo,
                title = "Thumbnail: Get from YouTube First",
                subtitle = "Search & download official YouTube HD thumbnail for this video",
                onClick = {
                    onFetchYouTubeThumbnail()
                    onDismiss()
                }
            )

            if (!video.isYoutube) {
                OptionActionRow(
                    icon = Icons.Default.Image,
                    title = "Thumbnail: Auto-Generate at 15% of Video",
                    subtitle = "Extract frame from local video file at 15% mark",
                    onClick = {
                        onExtractFrameAtFraction(0.15f)
                        onDismiss()
                    }
                )
                OptionActionRow(
                    icon = Icons.Default.Image,
                    title = "Thumbnail: Auto-Generate at 35% of Video",
                    subtitle = "Extract frame from local video file at 35% mark",
                    onClick = {
                        onExtractFrameAtFraction(0.35f)
                        onDismiss()
                    }
                )
            }

            if (onDeleteVideo != null) {
                OptionActionRow(
                    icon = Icons.Default.Delete,
                    title = "Remove Saved YouTube Video",
                    subtitle = "Delete this YouTube link from library",
                    onClick = {
                        onDeleteVideo()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) SmartTubeRed else SmartTubeTextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartTubeTextPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = SmartTubeTextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DialogIconCloseButton(onClick: () -> Unit) {
    var closeFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (closeFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (closeFocused) 2.dp else 1.dp,
                color = if (closeFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { closeFocused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = SmartTubeTextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
