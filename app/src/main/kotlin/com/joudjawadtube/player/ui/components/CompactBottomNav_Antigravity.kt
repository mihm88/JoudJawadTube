package com.joudjawadtube.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary

@Composable
fun CompactBottomNav_Antigravity(
    selectedSection: SidebarSection,
    onSelectSection: (SidebarSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        SidebarSection.HOME,
        SidebarSection.CHANNELS,
        SidebarSection.PLAYLISTS,
        SidebarSection.CATEGORIES,
        SidebarSection.USB_DRIVES,
        SidebarSection.SETTINGS
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(SmartTubeSidebarBg)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { section ->
            val isSelected = selectedSection == section
            val tint = if (isSelected) SmartTubeRed else SmartTubeTextSecondary
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectSection(section) }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = section.title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = section.title,
                    color = tint,
                    fontSize = 10.sp,
                    fontWeight = fontWeight,
                    maxLines = 1
                )
            }
        }
    }
}
