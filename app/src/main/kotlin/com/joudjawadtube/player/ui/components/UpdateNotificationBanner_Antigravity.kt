package com.joudjawadtube.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.R
import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity
import com.joudjawadtube.player.data.updater.UpdateState_Antigravity
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.liquidGlassBorder
import java.io.File
import java.util.Locale

@Composable
fun UpdateNotificationBanner_Antigravity(
    updateState: UpdateState_Antigravity,
    isDismissed: Boolean,
    onDownloadAndInstall: (UpdateInfo_Antigravity) -> Unit,
    onInstallApk: (File) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isDismissed) return
    if (updateState !is UpdateState_Antigravity.UpdateAvailable &&
        updateState !is UpdateState_Antigravity.Downloading &&
        updateState !is UpdateState_Antigravity.ReadyToInstall
    ) {
        return
    }

    val layoutInfo = LocalWindowLayoutInfo.current
    val shape = RoundedCornerShape(12.dp)
    val bannerModifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(SmartTubeCardBg)
        .liquidGlassBorder(
            shape = shape,
            fallbackColor = SmartTubeRed.copy(alpha = 0.5f)
        )
        .padding(if (layoutInfo.isCompact) 12.dp else 14.dp)

    val leadingIcon = when (updateState) {
        is UpdateState_Antigravity.UpdateAvailable -> Icons.Default.SystemUpdate
        is UpdateState_Antigravity.Downloading -> Icons.Default.CloudDownload
        is UpdateState_Antigravity.ReadyToInstall -> Icons.Default.CheckCircle
        else -> Icons.Default.SystemUpdate
    }

    val titleText = when (updateState) {
        is UpdateState_Antigravity.UpdateAvailable ->
            stringResource(R.string.home_update_banner_title, updateState.info.tagName)
        is UpdateState_Antigravity.Downloading -> {
            val downloadedMb = String.format(Locale.US, "%.1f", updateState.downloadedBytes / (1024f * 1024f))
            val totalMb = String.format(Locale.US, "%.1f", updateState.totalBytes / (1024f * 1024f))
            stringResource(R.string.settings_downloading_update, updateState.progressPercent, downloadedMb, totalMb)
        }
        is UpdateState_Antigravity.ReadyToInstall ->
            stringResource(R.string.home_update_banner_title, updateState.info.tagName)
        else -> ""
    }

    val subtitleText: String? = when (updateState) {
        is UpdateState_Antigravity.UpdateAvailable -> {
            val info = updateState.info
            val releaseLabel = info.releaseTitle.ifBlank { info.tagName }
            if (info.fileSize > 0L) {
                val sizeMb = String.format(Locale.US, "%.1f", info.fileSize / (1024f * 1024f))
                stringResource(R.string.home_update_banner_desc_size, releaseLabel, sizeMb)
            } else {
                stringResource(R.string.home_update_banner_desc, releaseLabel)
            }
        }
        is UpdateState_Antigravity.ReadyToInstall -> stringResource(R.string.settings_update_ready)
        else -> null
    }

    @Composable
    fun BannerContent(contentModifier: Modifier = Modifier) {
        Row(
            modifier = contentModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = SmartTubeRed,
                modifier = Modifier.size(if (layoutInfo.isCompact) 20.dp else 22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    color = SmartTubeTextPrimary,
                    fontSize = if (layoutInfo.isCompact) 14.sp else 15.sp,
                    lineHeight = if (layoutInfo.isCompact) 19.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (layoutInfo.isCompact) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitleText != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitleText,
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = if (layoutInfo.isCompact) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (updateState is UpdateState_Antigravity.Downloading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (updateState.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SmartTubeRed,
                        trackColor = SmartTubeTextSecondary.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }

    @Composable
    fun BannerButtons(rowModifier: Modifier = Modifier) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (updateState) {
                is UpdateState_Antigravity.UpdateAvailable -> {
                    ActionPillButton(
                        icon = Icons.Default.CloudDownload,
                        text = stringResource(R.string.home_update_action_now),
                        isPrimary = true,
                        onClick = { onDownloadAndInstall(updateState.info) }
                    )
                }
                is UpdateState_Antigravity.ReadyToInstall -> {
                    ActionPillButton(
                        icon = Icons.Default.SystemUpdate,
                        text = stringResource(R.string.home_update_install_now),
                        isPrimary = true,
                        onClick = { onInstallApk(updateState.apkFile) }
                    )
                }
                else -> Unit
            }
            ActionPillButton(
                icon = Icons.Default.Close,
                text = stringResource(R.string.home_update_later),
                isPrimary = false,
                onClick = onDismiss
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (layoutInfo.isCompact) {
            Column(
                modifier = bannerModifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BannerContent(contentModifier = Modifier.fillMaxWidth())
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
                BannerContent(contentModifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(14.dp))
                BannerButtons()
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}
