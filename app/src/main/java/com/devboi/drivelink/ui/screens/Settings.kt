package com.devboi.drivelink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import com.devboi.drivelink.data.models.ConnectionMode
import com.devboi.drivelink.ui.UIConstants
import com.devboi.drivelink.ui.components.CurveGraph
import com.devboi.drivelink.ui.components.DropDownItem
import com.devboi.drivelink.ui.components.Dropdown
import com.devboi.drivelink.ui.components.TextBox
import com.devboi.drivelink.ui.theme.PageBg
import com.devboi.drivelink.ui.theme.Primary
import com.devboi.drivelink.ui.theme.TextPrimary
import com.devboi.drivelink.ui.theme.TextSecondary
import com.devboi.drivelink.ui.theme.UIElemDisabled
import com.devboi.drivelink.ui.theme.UIElemPrimary
import com.devboi.drivelink.utils.CommonAngles
import com.devboi.drivelink.utils.nearlyEqual
import com.devboi.drivelink.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(vm: MainViewModel, modifier: Modifier = Modifier) {
    val resetConfirmOpen = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .padding(UIConstants.SpacingLarge, UIConstants.SpacingMedium + UIConstants.SpacingSmall)
            .verticalScroll(rememberScrollState())
        ,
        verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingLarge)
    ) {
        Text("连接与控制设置", color = TextPrimary, style = MaterialTheme.typography.titleLarge)


        // STEERING CALIBRATION
        Column(
            verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
        ) {

            Text(
                if (vm.flightMode.value) "飞机操纵杆回中" else "方向盘校准",
                color = TextPrimary
            )

            Text(
                if (vm.flightMode.value) "横握手机，保持舒适的中立姿势，再点击回中。"
                else "横握手机，让手机靠墙竖直放置，再点击校准。",
                color = TextSecondary
            )

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = UIElemPrimary
                ),
                enabled = !vm.motionSuspended.value && (!vm.flightMode.value || vm.flightReady.value),
                onClick = { if (vm.flightMode.value) vm.calibrateFlight() else vm.calibrateSteering() }
            ) {
                Text(if (vm.flightMode.value) "以当前姿势回中" else "校准方向盘")
            }
        }

        HorizontalDivider(color = Primary)

        // CONNECTION PROFILE
        Column(
            verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
        ) {

            Text(
                "连接方式",
                color = TextPrimary
            )
            key(vm.connectionMode.value) {
                Dropdown(
                    modifier = Modifier.fillMaxWidth(),
                    dropDownItems = ConnectionMode.entries.map { DropDownItem(it.displayName, it) },
                    defaultItem = DropDownItem(vm.connectionMode.value.displayName, vm.connectionMode.value),
                    onItemClick = { vm.changeConnectionMode(it.value) }
                )
            }
            Text(
                when (vm.connectionMode.value) {
                    ConnectionMode.WIFI -> "手机和电脑连接同一 Wi-Fi／局域网，填写电脑在该网络上的 IPv4 地址。"
                    ConnectionMode.USB -> "先用 USB 数据线连接电脑，在手机系统设置中开启 USB 网络共享；填写电脑 USB 网络适配器的 IPv4 地址。"
                    ConnectionMode.BLUETOOTH -> "先在系统中配对并开启手机蓝牙网络共享，再让 Windows 加入手机的个人区域网络（PAN）；填写电脑蓝牙网络适配器的 IPv4 地址。仅配对还不够。"
                },
                color = TextSecondary
            )
            Text(
                "每种方式独立保存地址；选择此项不会自动配对或开启共享。修改地址后点击键盘的完成键保存，再返回手柄。",
                color = TextSecondary
            )

            key(vm.connectionMode.value) {
                TextBox(
                    modifier = Modifier.fillMaxWidth(),
                    onSubmit = { vm.changeServerAddr(it) },
                    value = vm.serverAddr.value,
                    label = "${vm.connectionMode.value.displayName}：电脑 IPv4 地址"
                )
            }
        }

        HorizontalDivider(color = Primary)

        if (vm.flightMode.value) {
            Text("飞行陀螺仪", color = TextPrimary)
            Text("比例越大，机身对倾斜的响应越灵敏。", color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("反转 X 轴", color = TextPrimary)
                    Text("左右倾斜控制机身横滚。", color = TextSecondary)
                }
                Switch(
                    modifier = Modifier.semantics { contentDescription = "反转 X 轴，左右横滚" },
                    colors = SwitchDefaults.colors(checkedTrackColor = UIElemPrimary, uncheckedTrackColor = UIElemDisabled),
                    checked = vm.flightInvertX.value,
                    onCheckedChange = vm::changeFlightInvertX
                )
            }
            Text("X 轴映射比例 ${(vm.flightScaleX.value * 100).roundToInt()}%（左右横滚）", color = TextPrimary)
            Slider(
                value = vm.flightScaleX.value,
                onValueChange = vm::changeFlightScaleX,
                valueRange = 0.25f..2f,
                steps = 34,
                colors = SliderDefaults.colors(thumbColor = UIElemPrimary, activeTrackColor = UIElemPrimary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "X 轴映射比例，左右横滚" }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("反转 Y 轴", color = TextPrimary)
                    Text("前后倾斜控制机身俯仰。", color = TextSecondary)
                }
                Switch(
                    modifier = Modifier.semantics { contentDescription = "反转 Y 轴，前后俯仰" },
                    colors = SwitchDefaults.colors(checkedTrackColor = UIElemPrimary, uncheckedTrackColor = UIElemDisabled),
                    checked = vm.flightInvertY.value,
                    onCheckedChange = vm::changeFlightInvertY
                )
            }
            Text("Y 轴映射比例 ${(vm.flightScaleY.value * 100).roundToInt()}%（前后俯仰）", color = TextPrimary)
            Slider(
                value = vm.flightScaleY.value,
                onValueChange = vm::changeFlightScaleY,
                valueRange = 0.25f..2f,
                steps = 34,
                colors = SliderDefaults.colors(thumbColor = UIElemPrimary, activeTrackColor = UIElemPrimary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Y 轴映射比例，前后俯仰" }
            )
        } else {
            // LAYOUT SETTINGS
            Column(
                verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
            ) {
                Text(
                    "地面载具操作",
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
    
                    Column {
                        Text("油门在左侧", color = TextPrimary)
                        Text(
                            if (vm.altLayout.value) "左侧油门，右侧刹车。" else "左侧刹车，右侧油门。",
                            color = TextSecondary
                        )
                    }
    
                    Switch(
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = UIElemPrimary,
                            uncheckedTrackColor = UIElemDisabled
                        ),
                        checked = vm.altLayout.value,
                        onCheckedChange = { vm.changeIsAltLayoutSelected(it) }
                    )
                }
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
    
                    Column {
                        Text("踏板从上向下加深", color = TextPrimary)
                        Text(
                            "关闭时，从下向上滑动增加油门或刹车。",
                            color = TextSecondary
                        )
                    }
    
                    Switch(
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = UIElemPrimary,
                            uncheckedTrackColor = UIElemDisabled
                        ),
                        checked = vm.pedalsUpsideDown.value,
                        onCheckedChange = { vm.changeArePedalsUpsideDown(it) }
                    )
                }
    
            }
    
            // Max steering angle lock
            HorizontalDivider(color = Primary)
            Column(
                verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
            ) {
                Text(
                    "Max tilt angle",
                    color = TextPrimary
                )
                Text(
                    "Change how much you have to rotate your device to reach full steering lock.",
                    color = TextSecondary
                )
                Dropdown(
                    modifier = Modifier.fillMaxWidth(),
                    dropDownItems = listOf(
                        DropDownItem("120 degrees (60 degrees either side)", CommonAngles.DEG_60),
                        DropDownItem("180 degrees (90 degrees either side)", CommonAngles.DEG_90),
                        DropDownItem("240 degrees (120 degrees either side)", CommonAngles.DEG_120),
                        DropDownItem("270 degrees (135 degrees either side)", CommonAngles.DEG_135),
                        DropDownItem("360 degrees (180 degrees either side)", CommonAngles.DEG_180),
                    ),
                    defaultItem = when {
                            vm.maxSteeringAngle.value.nearlyEqual(CommonAngles.DEG_60.radians) -> DropDownItem("120 degrees (60 degrees either side)", CommonAngles.DEG_60)
                            vm.maxSteeringAngle.value.nearlyEqual(CommonAngles.DEG_90.radians) -> DropDownItem("180 degrees (90 degrees either side)", CommonAngles.DEG_90)
                            vm.maxSteeringAngle.value.nearlyEqual(CommonAngles.DEG_120.radians) -> DropDownItem("240 degrees (120 degrees either side)", CommonAngles.DEG_120)
                            vm.maxSteeringAngle.value.nearlyEqual(CommonAngles.DEG_135.radians) -> DropDownItem("270 degrees (135 degrees either side)", CommonAngles.DEG_135)
                            vm.maxSteeringAngle.value.nearlyEqual(CommonAngles.DEG_180.radians) -> DropDownItem("360 degrees (180 degrees either side)", CommonAngles.DEG_180)
                            else -> DropDownItem("270 degrees (135 degrees either side)", CommonAngles.DEG_135)
                        },
                    onItemClick = { item ->
                        vm.changeMaxSteeringAngle(item.value.radians)
                    }
                )
            }
    
            HorizontalDivider(color = Primary)
            // Steering sensitivity
            Column(
                verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
            ) {
                Text(
                    "Steering sensitivity curve",
                    color = TextPrimary
                )
    
                Row(
                    modifier = Modifier
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
                ) {
                    CurveGraph(
                        modifier = Modifier.size(300.dp, 300.dp),
                        onSlope1Change = { vm.onSlope1Change(it) },
                        onSlope2Change = { vm.onSlope2Change(it) },
                        vm
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingMedium)
                    ) {
                        Text(
                            "Presets",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
    
                        Row(horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)) {
                            Button(
                                modifier = Modifier.width(150.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UIElemPrimary
                                ),
                                onClick = {
                                    vm.onSlope1Change(1f)
                                    vm.onSlope2Change(1f)
                                }
                            ) {
                                Text("Linear")
                            }
                            Button(
                                modifier = Modifier.width(150.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UIElemPrimary
                                ),
                                onClick = {
                                    vm.onSlope1Change(0.4119241f)
                                    vm.onSlope2Change(0f)
                                }
                            ) {
                                Text("Soft")
                            }
                            Button(
                                modifier = Modifier.width(150.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UIElemPrimary
                                ),
                                onClick = {
                                    vm.onSlope1Change(1.82f)
                                    vm.onSlope2Change(0.24f)
                                }
                            ) {
                                Text("Aggressive")
                            }
                        }
    
                        Text(
                            text = "The Steering Curve controls how your steering input is translated into actual joystick input.\n" +
                                    "- The horizontal axis (input) = how much you tilt the device.\n" +
                                    "- The vertical axis (output) = how much steering input is sent.\n" +
                                    "Changing the curve changes how strongly the vehicle reacts at different input levels.\n\n" +
                                    "When to adjust:\n" +
                                    "- If steering feels too twitchy, then reduce sensitivity on the left\n" +
                                    "- If steering feels unresponsive, increase sensitivity on the left\n" +
                                    "- If turning feels inconsistent, use a more linear curve\n\n" +
                                    "Summary: Changes how quickly the steering maximum is reached as you tilt"
                            ,
                            color = TextSecondary,
                            style= TextStyle(lineBreak = LineBreak.Paragraph)
                        )
                    }
                }
            }

        }

        // Reset settings to default
        HorizontalDivider(color = Primary)
        Column(
            verticalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)
        ) {

            Text(
                "Danger Zone",
                color = TextPrimary
            )

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = UIElemPrimary
                ),
                onClick = {
                    resetConfirmOpen.value = true
                }
            ) {
                Text("Reset all to default")
            }

            when {
                resetConfirmOpen.value -> {
                    BasicAlertDialog(
                        onDismissRequest = { resetConfirmOpen.value = false }
                    ) {
                        Surface(
                            modifier = Modifier.wrapContentWidth().wrapContentHeight(),
                            tonalElevation = AlertDialogDefaults.TonalElevation,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Reset settings",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(UIConstants.SpacingSmall))
                                Text(
                                    text = "Do you want to reset all settings to their defaults?"
                                )
                                Spacer(modifier = Modifier.height(UIConstants.SpacingMedium))
                                TextButton(
                                    onClick = {
                                        vm.resetSettingsToDefault()
                                        resetConfirmOpen.value = false
                                        vm.changeScreen(Screens.Home)
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Confirm", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

