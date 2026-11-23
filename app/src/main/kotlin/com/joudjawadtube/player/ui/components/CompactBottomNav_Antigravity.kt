package com.joudjawadtube.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.R
import com.joudjawadtube.player.ui.theme.LiquidGlassModalBackdropEffect_Antigravity
import com.joudjawadtube.player.ui.theme.LocalJoudColors
import com.joudjawadtube.player.ui.theme.SmartTubeModalCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeModalScrim
import com.joudjawadtube.player.ui.theme.SmartTubeModalSurface
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactBottomNav_Antigravity(
    selectedSection: SidebarSection,
    channelsCount: Int = 0,
    playlistsCount: Int = 0,
    usbDriveCount: Int = 0,
    onSelectSection: (SidebarSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val secondarySections = remember {
        listOf(
            SidebarSection.KIDS_PICKS,
            SidebarSection.PLAYLISTS,
            SidebarSection.CATEGORIES,
            SidebarSection.CONTINUE_WATCHING,
            SidebarSection.FOLDERS,
            SidebarSection.USB_DRIVES,
            SidebarSection.FOUR_K,
            SidebarSection.SETTINGS
        )
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    val isMoreSelected = selectedSection in secondarySections || showMoreSheet

    val colors = LocalJoudColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (colors.isLiquidGlass) SmartTubeModalSurface else SmartTubeSidebarBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (colors.isLiquidGlass) 1.2.dp else 1.dp)
                .background(
                    if (colors.isLiquidGlass) colors.glassBorderHighlight else SmartTubeTextSecondary.copy(alpha = 0.16f)
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactBottomNavTab(
                icon = SidebarSection.HOME.icon,
                label = SidebarSection.HOME.title,
                isSelected = selectedSection == SidebarSection.HOME,
                badge = null,
                onClick = { onSelectSection(SidebarSection.HOME) },
                modifier = Modifier.weight(1f)
            )
            CompactBottomNavTab(
                icon = SidebarSection.CHANNELS.icon,
                label = SidebarSection.CHANNELS.title,
                isSelected = selectedSection == SidebarSection.CHANNELS,
                badge = channelsCount.takeIf { it > 0 }?.toString(),
                onClick = { onSelectSection(SidebarSection.CHANNELS) },
                modifier = Modifier.weight(1f)
            )
            CompactBottomNavTab(
                icon = SidebarSection.FAVORITES.icon,
                label = SidebarSection.FAVORITES.title,
                isSelected = selectedSection == SidebarSection.FAVORITES,
                badge = null,
                onClick = { onSelectSection(SidebarSection.FAVORITES) },
                modifier = Modifier.weight(1f)
            )
            CompactBottomNavTab(
                icon = Icons.Default.GridView,
                label = stringResource(R.string.nav_more_parents),
                isSelected = isMoreSelected,
                badge = null,
                onClick = { showMoreSheet = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showMoreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
            shape = sheetShape,
            containerColor = SmartTubeModalSurface,
            contentColor = SmartTubeTextPrimary,
            scrimColor = SmartTubeModalScrim,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (colors.isLiquidGlass) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0x337DD3FC),
                                            Color(0xB8E0F2FE),
                                            Color(0x337DD3FC)
                                        )
                                    )
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (colors.isLiquidGlass) Color(0x99E0F2FE)
                                else SmartTubeTextSecondary.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        ) {
            LiquidGlassModalBackdropEffect_Antigravity()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (colors.isLiquidGlass) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x1EE0F2FE),
                                        Color.Transparent
                                    )
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.nav_more_sheet_title),
                    color = SmartTubeTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                HorizontalDivider(
                    color = if (colors.isLiquidGlass) Color(0x33E0F2FE) else SmartTubeTextSecondary.copy(alpha = 0.16f)
                )
                secondarySections.forEach { section ->
                    val isActive = selectedSection == section
                    val rowBadge = when (section) {
                        SidebarSection.PLAYLISTS -> playlistsCount.takeIf { it > 0 }?.toString()
                        SidebarSection.USB_DRIVES -> usbDriveCount.takeIf { it > 0 }?.toString()
                        else -> null
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isActive -> SmartTubeRed.copy(alpha = 0.18f)
                                    colors.isLiquidGlass -> SmartTubeModalCardBg.copy(alpha = 0.55f)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (colors.isLiquidGlass) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = if (isActive) SmartTubeRed.copy(alpha = 0.55f) else Color(0x28E0F2FE),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                showMoreSheet = false
                                onSelectSection(section)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title,
                                tint = if (isActive) SmartTubeRed else SmartTubeTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = section.title,
                                color = if (isActive) SmartTubeRed else SmartTubeTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (rowBadge != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SmartTubeRed.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = rowBadge,
                                    color = SmartTubeRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CompactBottomNavTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) SmartTubeRed else SmartTubeTextSecondary
    val labelColor = if (isSelected) SmartTubeTextPrimary else SmartTubeTextSecondary
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) SmartTubeRed.copy(alpha = 0.14f) else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(SmartTubeRed)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            if (badge != null) {
                Text(
                    text = badge,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
