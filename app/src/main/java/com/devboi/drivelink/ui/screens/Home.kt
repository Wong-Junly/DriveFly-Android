package com.devboi.drivelink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.devboi.drivelink.ui.UIConstants
import com.devboi.drivelink.ui.components.MomentaryButton
import com.devboi.drivelink.ui.components.VerticalSlider
import com.devboi.drivelink.ui.theme.ButtonActiveBg
import com.devboi.drivelink.ui.theme.ButtonNormalBg
import com.devboi.drivelink.ui.theme.PageBg
import com.devboi.drivelink.ui.theme.PedalBrake
import com.devboi.drivelink.ui.theme.PedalThrottle
import com.devboi.drivelink.ui.theme.TextPrimary
import com.devboi.drivelink.viewmodel.MainViewModel

@Composable
fun Home(vm: MainViewModel, modifier: Modifier = Modifier) {
    val throttleOnLeft = vm.altLayout.value

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(PageBg)
            .padding(UIConstants.SpacingMedium)
    ) {
        val columnWidth = (maxWidth - UIConstants.SpacingMedium * 2) / 3
        val pedalHeight = maxHeight

        // Recreate held controls when their sides change so previous inputs are released.
        key(throttleOnLeft) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
            ) {
                Box(
                    modifier = Modifier.size(columnWidth, pedalHeight),
                    contentAlignment = Alignment.Center
                ) {
                    VerticalSlider(
                        height = pedalHeight,
                        width = columnWidth,
                        bottomStart = !vm.pedalsUpsideDown.value,
                        fillColor = if (throttleOnLeft) PedalThrottle else PedalBrake,
                        label = if (throttleOnLeft) "油门，松手回零" else "刹车，松手回零",
                        onValueChange = if (throttleOnLeft) vm::onThrottleChange else vm::onBrakeChange
                    )
                    Text(
                        if (throttleOnLeft) "油门" else "刹车",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
                ) {
                    MomentaryButton(
                        modifier = Modifier.weight(2f).fillMaxWidth(),
                        label = "解锁载具",
                        onPress = { vm.onButtonPress(0) },
                        onRelease = { vm.onButtonRelease(0) },
                        background = ButtonNormalBg,
                        pressedBackground = ButtonActiveBg
                    ) { Text("解锁载具", color = TextPrimary, textAlign = TextAlign.Center) }

                    MomentaryButton(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        label = "部署补给箱",
                        onPress = { vm.onButtonPress(2) },
                        onRelease = { vm.onButtonRelease(2) },
                        background = ButtonNormalBg,
                        pressedBackground = ButtonActiveBg
                    ) { Text("部署补给箱", color = TextPrimary, textAlign = TextAlign.Center) }

                    MomentaryButton(
                        modifier = Modifier.weight(2f).fillMaxWidth(),
                        label = "对抗装置",
                        onPress = { vm.onButtonPress(1) },
                        onRelease = { vm.onButtonRelease(1) },
                        background = ButtonNormalBg,
                        pressedBackground = ButtonActiveBg
                    ) { Text("对抗装置", color = TextPrimary, textAlign = TextAlign.Center) }
                }

                Box(
                    modifier = Modifier.size(columnWidth, pedalHeight),
                    contentAlignment = Alignment.Center
                ) {
                    VerticalSlider(
                        height = pedalHeight,
                        width = columnWidth,
                        bottomStart = !vm.pedalsUpsideDown.value,
                        fillColor = if (throttleOnLeft) PedalBrake else PedalThrottle,
                        label = if (throttleOnLeft) "刹车，松手回零" else "油门，松手回零",
                        onValueChange = if (throttleOnLeft) vm::onBrakeChange else vm::onThrottleChange
                    )
                    Text(
                        if (throttleOnLeft) "刹车" else "油门",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
