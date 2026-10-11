package com.devboi.drivelink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devboi.drivelink.ui.UIConstants
import com.devboi.drivelink.ui.components.MomentaryButton
import com.devboi.drivelink.ui.theme.ButtonActiveBg
import com.devboi.drivelink.ui.theme.ButtonNormalBg
import com.devboi.drivelink.ui.theme.PageBg
import com.devboi.drivelink.ui.theme.PedalClutch
import com.devboi.drivelink.ui.theme.TextPrimary
import com.devboi.drivelink.ui.theme.TextSecondary
import com.devboi.drivelink.ui.theme.UIElemPrimary
import com.devboi.drivelink.viewmodel.MainViewModel

@Composable
fun Flight(vm: MainViewModel, modifier: Modifier = Modifier) {
    var upHeld by remember { mutableStateOf(false) }
    var downHeld by remember { mutableStateOf(false) }

    DisposableEffect(vm) {
        onDispose {
            upHeld = false
            downHeld = false
            vm.onCollectiveChange(false, false)
            for (button in 0..3) vm.onButtonRelease(button)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(PageBg).padding(UIConstants.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (vm.motionSuspended.value) "手机接近水平，陀螺仪暂停。抬起后自动回中。"
                else if (vm.flightReady.value) "横握手机，前后俯仰，左右横滚。"
                else "等待姿态数据，请检查手机传感器。",
                color = if (vm.flightReady.value) TextSecondary else PedalClutch,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = vm::calibrateFlight,
                enabled = vm.flightReady.value && !vm.motionSuspended.value,
                colors = ButtonDefaults.buttonColors(containerColor = UIElemPrimary)
            ) { Text("以当前姿势回中") }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
            ) {
                MomentaryButton(
                    modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp),
                    label = "总距升高，按住升高，松手停止",
                    onPress = { upHeld = true; vm.onCollectiveChange(upHeld, downHeld) },
                    onRelease = { upHeld = false; vm.onCollectiveChange(upHeld, downHeld) },
                    background = ButtonNormalBg,
                    pressedBackground = ButtonActiveBg
                ) { Text("总距升高", color = TextPrimary, textAlign = TextAlign.Center) }

                MomentaryButton(
                    modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp),
                    label = "总距降低，按住降低，松手停止",
                    onPress = { downHeld = true; vm.onCollectiveChange(upHeld, downHeld) },
                    onRelease = { downHeld = false; vm.onCollectiveChange(upHeld, downHeld) },
                    background = ButtonNormalBg,
                    pressedBackground = ButtonActiveBg
                ) { Text("总距降低", color = TextPrimary, textAlign = TextAlign.Center) }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
            ) {
                MomentaryButton(
                    modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp),
                    label = "解锁载具",
                    onPress = { vm.onButtonPress(0) },
                    onRelease = { vm.onButtonRelease(0) },
                    background = ButtonNormalBg,
                    pressedBackground = ButtonActiveBg
                ) { Text("解锁载具", color = TextPrimary, textAlign = TextAlign.Center) }

                MomentaryButton(
                    modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp),
                    label = "部署补给箱",
                    onPress = { vm.onButtonPress(2) },
                    onRelease = { vm.onButtonRelease(2) },
                    background = ButtonNormalBg,
                    pressedBackground = ButtonActiveBg
                ) { Text("部署补给箱", color = TextPrimary, textAlign = TextAlign.Center) }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
            ) {
                MomentaryButton(
                    modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp),
                    label = "攻击",
                    onPress = { vm.onButtonPress(3) },
                    onRelease = { vm.onButtonRelease(3) },
                    background = ButtonNormalBg,
                    pressedBackground = ButtonActiveBg
                ) { Text("攻击", color = TextPrimary, textAlign = TextAlign.Center) }

                MomentaryButton(
                    modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp),
                    label = "对抗装置",
                    onPress = { vm.onButtonPress(1) },
                    onRelease = { vm.onButtonRelease(1) },
                    background = ButtonNormalBg,
                    pressedBackground = ButtonActiveBg
                ) { Text("对抗装置", color = TextPrimary, textAlign = TextAlign.Center) }
            }
        }
    }
}
