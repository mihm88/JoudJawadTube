package com.joudjawadtube.player.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.joudjawadtube.player.R
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSurface
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan
import kotlin.random.Random

/**
 * Generates simple multiplication challenges to gate parent-only actions
 * (settings, adding sources, deleting content, updating the app).
 */
object ParentalGateChallenge_Antigravity {

    data class Challenge(
        val a: Int,
        val b: Int,
        val correctAnswer: Int,
        val options: List<Int>
    )

    fun generate(random: Random = Random.Default): Challenge {
        val a = random.nextInt(6, 10)
        val b = random.nextInt(4, 9)
        val correctAnswer = a * b

        val candidates = linkedSetOf(correctAnswer)
        val offsets = listOf(-a, a, -b, b, -2, 2, -4, 4, -1, 1, 3, -3, 6, -6)
        var attempts = 0
        while (candidates.size < 4 && attempts < 32) {
            val distractor = correctAnswer + offsets[random.nextInt(offsets.size)]
            if (distractor > 0 && distractor != correctAnswer) {
                candidates.add(distractor)
            }
            attempts++
        }

        var fallback = correctAnswer + 1
        while (candidates.size < 4) {
            if (fallback > 0 && fallback != correctAnswer) {
                candidates.add(fallback)
            }
            fallback++
        }

        val shuffled = candidates.toMutableList()
        shuffled.shuffle(random)
        return Challenge(
            a = a,
            b = b,
            correctAnswer = correctAnswer,
            options = shuffled.toList()
        )
    }
}

/**
 * Kid-safe, TV D-Pad and touch-friendly math challenge dialog for parent-only actions.
 */
@Composable
fun ParentalGateDialog_Antigravity(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var challenge by remember { mutableStateOf(ParentalGateChallenge_Antigravity.generate()) }
    var showWrongMessage by remember { mutableStateOf(false) }
    val firstOptionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstOptionFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(min = 280.dp, max = 440.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SmartTubeSurface)
                .border(
                    width = 1.dp,
                    color = SmartTubeTextPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SmartTubeRed.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SmartTubeRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.parental_gate_title),
                    color = SmartTubeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.parental_gate_prompt, challenge.a, challenge.b),
                color = SmartTubeTextSecondary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            if (showWrongMessage) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.parental_gate_wrong),
                    color = SmartTubeUsbCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val rows = challenge.options.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rows.forEachIndexed { rowIndex, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEachIndexed { colIndex, option ->
                            val isFirst = rowIndex == 0 && colIndex == 0
                            ParentalGateAnswerButton(
                                text = option.toString(),
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isFirst) Modifier.focusRequester(firstOptionFocusRequester)
                                        else Modifier
                                    ),
                                onClick = {
                                    if (option == challenge.correctAnswer) {
                                        showWrongMessage = false
                                        onSuccess()
                                    } else {
                                        showWrongMessage = true
                                        challenge = ParentalGateChallenge_Antigravity.generate()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ParentalGateCancelButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun ParentalGateAnswerButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else SmartTubeTextPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = SmartTubeTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ParentalGateCancelButton(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg.copy(alpha = 0.7f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else SmartTubeTextPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = SmartTubeTextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
