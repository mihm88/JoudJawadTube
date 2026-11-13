package com.joudjawadtube.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary

@Composable
fun CompactBottomNav_Antigravity(
    selectedSection: SidebarSection,
    channelsCount: Int = 0,
    playlistsCount: Int = 0,
    usbDriveCount: Int = 0,
    onSelectSection: (SidebarSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        SidebarSection.HOME,
        SidebarSection.KIDS_PICKS,
        SidebarSection.CHANNELS,
        SidebarSection.PLAYLISTS,
        SidebarSection.CATEGORIES,
        SidebarSection.FAVORITES,
        SidebarSection.CONTINUE_WATCHING,
        SidebarSection.FOLDERS,
        SidebarSection.USB_DRIVES,
        SidebarSection.SETTINGS
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SmartTubeSidebarBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { section ->
                val isSelected = selectedSection == section
                val tint = if (isSelected) SmartTubeRed else SmartTubeTextSecondary
                val labelColor = if (isSelected) SmartTubeTextPrimary else SmartTubeTextSecondary
                val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                val badge = when (section) {
                    SidebarSection.CHANNELS -> channelsCount.takeIf { it > 0 }?.toString()
                    SidebarSection.PLAYLISTS -> playlistsCount.takeIf { it > 0 }?.toString()
                    SidebarSection.USB_DRIVES -> usbDriveCount.takeIf { it > 0 }?.toString()
                    else -> null
                }

                Column(
                    modifier = Modifier
                        .widthIn(min = 66.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) SmartTubeRed.copy(alpha = 0.14f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectSection(section) }
                        .padding(vertical = 5.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(SmartTubeRed)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.title,
                            tint = tint,
                            modifier = Modifier.size(19.dp)
                        )
                        if (badge != null) {
                            Text(
                                text = badge,
                                color = tint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = section.title,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
