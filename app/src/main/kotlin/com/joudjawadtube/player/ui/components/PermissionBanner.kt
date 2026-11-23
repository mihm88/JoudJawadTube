package com.joudjawadtube.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.R
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSurface
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary

@Composable
fun PermissionBanner(
    hasReadVideoPermission: Boolean,
    hasAllFilesAccess: Boolean,
    onRequestReadPermission: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onPickSafFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (dismissed || (hasReadVideoPermission && hasAllFilesAccess)) return

    val layoutInfo = LocalWindowLayoutInfo.current
    val bannerModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(SmartTubeCardBg)
        .border(1.dp, SmartTubeRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        .padding(if (layoutInfo.isCompact) 12.dp else 14.dp)

    @Composable
    fun BannerButtons(rowModifier: Modifier = Modifier) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasReadVideoPermission) {
                ActionPillButton(
                    icon = Icons.Default.Security,
                    text = stringResource(R.string.permission_media_access),
                    isPrimary = true,
                    onClick = onRequestReadPermission
                )
            }
            if (!hasAllFilesAccess) {
                ActionPillButton(
                    icon = Icons.Default.Usb,
                    text = stringResource(R.string.permission_usb_access),
                    isPrimary = hasReadVideoPermission,
                    onClick = onRequestAllFilesAccess
                )
            }
            ActionPillButton(
                icon = Icons.Default.FolderOpen,
                text = stringResource(R.string.action_add_folder),
                isPrimary = false,
                onClick = onPickSafFolder
            )
            ActionPillButton(
                icon = Icons.Default.Close,
                text = stringResource(R.string.action_close),
                isPrimary = false,
                onClick = { dismissed = true }
            )
        }
    }

    if (layoutInfo.isCompact) {
        Column(
            modifier = bannerModifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.permission_banner_title),
                    color = SmartTubeTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.permission_banner_desc),
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BannerButtons(
                rowModifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )
        }
    } else {
        Row(
            modifier = bannerModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_banner_title),
                    color = SmartTubeTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.permission_banner_desc),
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(14.dp))
            BannerButtons()
        }
    }
}

@Composable
fun ActionPillButton(
    icon: ImageVector,
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutInfo = LocalWindowLayoutInfo.current
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused && isPrimary -> SmartTubeRed
        isFocused -> SmartTubeCardFocusedBg
        isPrimary -> SmartTubeRed.copy(alpha = 0.88f)
        else -> SmartTubeSurface
    }
    val contentColor = if (isPrimary) Color.White else SmartTubeTextPrimary

    Row(
        modifier = modifier
            .heightIn(min = if (layoutInfo.isTouchDevice) 48.dp else 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else SmartTubeTextSecondary.copy(alpha = 0.24f),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
