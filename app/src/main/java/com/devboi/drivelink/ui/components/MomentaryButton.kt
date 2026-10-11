package com.devboi.drivelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devboi.drivelink.ui.theme.Primary

@Composable
fun MomentaryButton(
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    label: String = "游戏按钮",
    borderRadius: Int = 10,
    background: Color = Color.LightGray,
    pressedBackground: Color = Color.DarkGray,
    content: @Composable BoxScope.() -> Unit
) {
    val pressed = remember { mutableStateOf(false) }
    val latestPress by rememberUpdatedState(onPress)
    val latestRelease by rememberUpdatedState(onRelease)

    DisposableEffect(Unit) {
        onDispose {
            if (pressed.value) {
                pressed.value = false
                latestRelease()
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(borderRadius.dp))
            .border(Dp.Hairline, Primary, RoundedCornerShape(borderRadius.dp))
            .background(if (pressed.value) pressedBackground else background)
            .semantics {
                role = Role.Button
                contentDescription = label
                stateDescription = if (pressed.value) "已按下" else "已松开"
                // Accessibility clicks toggle a hold so it survives the next network tick.
                onClick(label = "按下或松开") {
                    pressed.value = !pressed.value
                    if (pressed.value) latestPress() else latestRelease()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pressed.value = true
                    latestPress()
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.find { it.id == down.id }
                            if (change == null || !change.pressed) break
                            change.consume()
                        }
                    } finally {
                        pressed.value = false
                        latestRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}
