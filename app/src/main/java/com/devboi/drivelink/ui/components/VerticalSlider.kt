package com.devboi.drivelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import com.devboi.drivelink.ui.theme.PedalUnfilled
import com.devboi.drivelink.ui.theme.Primary

fun calculateSliderPos(offset: Float, size: Float, bottomStart: Boolean): Float {
    if (size <= 0f) return 0f
    val fraction = offset / size
    return if (bottomStart) (1f - fraction).coerceIn(0f, 1f) else fraction.coerceIn(0f, 1f)
}

@Composable
fun VerticalSlider(
    height: Dp, width: Dp,
    bottomStart: Boolean = true,
    fillColor: Color,
    onValueChange: (Float) -> Unit,
    label: String = "踏板"
) {
    var elemSizeY by remember { mutableFloatStateOf(0f) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val latestChange by rememberUpdatedState(onValueChange)

    DisposableEffect(bottomStart) {
        onDispose {
            sliderValue = 0f
            latestChange(0f)
        }
    }

    Box(
        modifier = Modifier
            .background(PedalUnfilled)
            .size(width = width, height = height)
            .border(Dp.Hairline, Primary)
            .onSizeChanged { elemSizeY = it.height.toFloat() }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(sliderValue, 0f..1f)
                setProgress {
                    sliderValue = it.coerceIn(0f, 1f)
                    latestChange(sliderValue)
                    true
                }
            }
            .pointerInput(bottomStart) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    sliderValue = calculateSliderPos(down.position.y, elemSizeY, bottomStart)
                    latestChange(sliderValue)
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.find { it.id == down.id }
                            if (change == null || !change.pressed) break
                            sliderValue = calculateSliderPos(change.position.y, elemSizeY, bottomStart)
                            latestChange(sliderValue)
                            change.consume()
                        }
                    } finally {
                        sliderValue = 0f
                        latestChange(0f)
                    }
                }
            },
        contentAlignment = if (bottomStart) Alignment.BottomEnd else Alignment.TopEnd
    ) {
        Box(Modifier.background(fillColor).fillMaxWidth().fillMaxHeight(sliderValue))
    }
}
