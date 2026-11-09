package com.joudjawadtube.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.joudjawadtube.player.data.settings.SidebarMode
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan

enum class SidebarSection(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    KIDS_PICKS("Kids Picks", Icons.Default.Star),
    CHANNELS("Channels", Icons.Default.VideoLibrary),
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay),
    CATEGORIES("Categories", Icons.Default.Category),
    CONTINUE_WATCHING("History", Icons.Default.History),
    USB_DRIVES("USB Drives", Icons.Default.Usb),
    FOLDERS("Folders", Icons.Default.Folder),
    FOUR_K("4K / UHD", Icons.Default.Hd),
    FAVORITES("Favorites", Icons.Default.Favorite),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun SmartTubeSidebar(
    selectedSection: SidebarSection,
    usbDriveCount: Int,
    channelsCount: Int = 0,
    playlistsCount: Int = 0,
    isScanning: Boolean,
    isExpanded: Boolean,
    sidebarMode: SidebarMode = SidebarMode.OVERLAY_ON_FOCUS,
    onSelectSection: (SidebarSection) -> Unit,
    onAddYouTubeLink: () -> Unit = {},
    onAddSafFolder: () -> Unit,
    onRescanStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sidebarHasFocus by remember { mutableStateOf(false) }

    val alwaysExpanded = isExpanded || sidebarMode == SidebarMode.ALWAYS_EXPANDED
    val effectiveExpanded = when (sidebarMode) {
        SidebarMode.ALWAYS_EXPANDED -> true
        SidebarMode.ALWAYS_COLLAPSED -> false
        SidebarMode.OVERLAY_ON_FOCUS -> alwaysExpanded || sidebarHasFocus
    }

    // Reserve a constant layout width so expanding on focus NEVER re-layouts the video grid!
    val reservedSlotWidth = if (alwaysExpanded) 232.dp else 76.dp
    val drawerVisualWidth = if (effectiveExpanded) 232.dp else 76.dp
    val isFloatingOverlay = effectiveExpanded && !alwaysExpanded

    Box(
        modifier = modifier
            .width(reservedSlotWidth)
            .fillMaxHeight()
            .zIndex(if (isFloatingOverlay) 30f else 1f)
    ) {
        Column(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .width(drawerVisualWidth)
                .fillMaxHeight()
                .then(
                    if (isFloatingOverlay) {
                        Modifier
                            .shadow(elevation = 24.dp)
                            .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f))
                    } else {
                        Modifier
                    }
                )
                .background(SmartTubeSidebarBg)
                .onFocusChanged { sidebarHasFocus = it.hasFocus }
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Joud&Jawad Tube Brand Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (effectiveExpanded) Arrangement.Start else Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SmartTubeRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Joud&Jawad Tube",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (effectiveExpanded) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Joud&Jawad",
                                color = SmartTubeTextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "TUBE • TV & USB",
                                color = SmartTubeRed,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                SidebarSection.entries.forEach { section ->
                    val badgeText = when {
                        section == SidebarSection.USB_DRIVES && usbDriveCount > 0 -> usbDriveCount.toString()
                        section == SidebarSection.CHANNELS && channelsCount > 0 -> channelsCount.toString()
                        section == SidebarSection.PLAYLISTS && playlistsCount > 0 -> playlistsCount.toString()
                        else -> null
                    }
                    val badgeColor = when (section) {
                        SidebarSection.USB_DRIVES -> SmartTubeUsbCyan
                        SidebarSection.PLAYLISTS -> SmartTubeUsbCyan
                        else -> SmartTubeRed
                    }

                    SidebarNavItem(
                        icon = section.icon,
                        label = section.title,
                        isSelected = selectedSection == section,
                        isExpanded = effectiveExpanded,
                        badgeText = badgeText,
                        badgeColor = badgeColor,
                        onClick = { onSelectSection(section) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(1.dp))

                SidebarNavItem(
                    icon = Icons.Default.AddLink,
                    label = "+ YouTube Link",
                    isSelected = false,
                    isExpanded = effectiveExpanded,
                    onClick = onAddYouTubeLink
                )

                SidebarNavItem(
                    icon = Icons.Default.CreateNewFolder,
                    label = "Add Folder",
                    isSelected = false,
                    isExpanded = effectiveExpanded,
                    onClick = onAddSafFolder
                )

                SidebarNavItem(
                    icon = Icons.Default.Refresh,
                    label = if (isScanning) "Scanning..." else "Rescan USB",
                    isSelected = false,
                    isExpanded = effectiveExpanded,
                    onClick = onRescanStorage
                )
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    badgeText: String? = null,
    badgeColor: Color = SmartTubeRed,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> SmartTubeCardFocusedBg
            isSelected -> SmartTubeRed.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        label = "navItemBg"
    )
    val contentColor = when {
        isFocused || isSelected -> SmartTubeTextPrimary
        else -> SmartTubeTextSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(SmartTubeRed)
            )
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) SmartTubeRed else contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.22f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) SmartTubeRed else contentColor,
                modifier = Modifier
                    .size(21.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
