package com.joudjawadtube.player.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.R
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary

/**
 * Search button that stays collapsed as a focusable icon so D-pad navigation
 * never pops up the soft keyboard until the user explicitly presses OK / taps.
 */
@Composable
fun SearchIconBar_Antigravity(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    height: Dp = 38.dp,
    expandedWidth: Dp = 300.dp,
    modifier: Modifier = Modifier
) {
    var isOpen by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    var restoreIconFocus by remember { mutableStateOf(false) }
    var iconFocused by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    var hadQueryWhileOpen by remember { mutableStateOf(query.isNotEmpty()) }

    val iconFocusRequester = remember { FocusRequester() }
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Collapse back to the icon if an external "Clear search" clears the query
    LaunchedEffect(query, isOpen, fieldFocused) {
        if (query.isNotEmpty()) {
            hadQueryWhileOpen = true
        } else if (isOpen && hadQueryWhileOpen && !fieldFocused) {
            hadQueryWhileOpen = false
            isOpen = false
        }
    }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            runCatching { fieldFocusRequester.requestFocus() }
            keyboardController?.show()
        } else {
            hadQueryWhileOpen = false
            if (restoreIconFocus) {
                restoreIconFocus = false
                runCatching { iconFocusRequester.requestFocus() }
            }
        }
    }

    BackHandler(enabled = isOpen) {
        keyboardController?.hide()
        onQueryChange("")
        restoreIconFocus = true
        isOpen = false
    }

    if (!isOpen) {
        val hasActiveQuery = query.isNotEmpty()
        Box(
            modifier = modifier
                .size(height)
                .clip(CircleShape)
                .background(
                    when {
                        iconFocused -> SmartTubeCardFocusedBg
                        hasActiveQuery -> SmartTubeRed.copy(alpha = 0.22f)
                        else -> SmartTubeCardBg
                    }
                )
                .border(
                    width = if (iconFocused) 2.dp else 1.dp,
                    color = when {
                        iconFocused -> SmartTubeFocusBorder
                        hasActiveQuery -> SmartTubeRed
                        else -> Color.White.copy(alpha = 0.08f)
                    },
                    shape = CircleShape
                )
                .focusRequester(iconFocusRequester)
                .onFocusChanged { iconFocused = it.isFocused }
                .clickable { isOpen = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_placeholder),
                tint = if (iconFocused || hasActiveQuery) SmartTubeRed else SmartTubeTextSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
    } else {
        val widthModifier = if (expandedWidth != Dp.Unspecified) {
            modifier.width(expandedWidth)
        } else {
            modifier.fillMaxWidth()
        }
        Row(
            modifier = widthModifier.height(height),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = SmartTubeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(SmartTubeRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (query.isBlank()) {
                            restoreIconFocus = true
                            isOpen = false
                        } else {
                            onSubmit()
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(fieldFocusRequester)
                    .onFocusChanged { fieldFocused = it.isFocused || it.hasFocus },
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(height / 2))
                            .background(if (fieldFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                            .border(
                                width = if (fieldFocused) 2.dp else 1.dp,
                                color = if (fieldFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(height / 2)
                            )
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (fieldFocused) SmartTubeRed else SmartTubeTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    color = SmartTubeTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            var closeFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(height)
                    .clip(CircleShape)
                    .background(if (closeFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                    .border(
                        width = if (closeFocused) 2.dp else 1.dp,
                        color = if (closeFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .onFocusChanged { closeFocused = it.isFocused }
                    .clickable {
                        keyboardController?.hide()
                        onQueryChange("")
                        restoreIconFocus = true
                        isOpen = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.search_clear),
                    tint = if (closeFocused) Color.White else SmartTubeTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
