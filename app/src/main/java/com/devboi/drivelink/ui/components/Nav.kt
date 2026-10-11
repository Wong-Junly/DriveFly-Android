package com.devboi.drivelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.devboi.drivelink.ui.UIConstants
import com.devboi.drivelink.ui.screens.Screens
import com.devboi.drivelink.ui.theme.NavBg
import com.devboi.drivelink.ui.theme.ButtonNormalBg
import com.devboi.drivelink.ui.theme.TextPrimary
import com.devboi.drivelink.ui.theme.UIElemPrimary
import com.devboi.drivelink.viewmodel.MainViewModel

@Composable
fun Nav(vm: MainViewModel, currentPage: Screens, onTogglePage: (Screens) -> Unit, modifier: Modifier = Modifier) {
    val connected by vm.isConnected.collectAsState()
    val connectionError by vm.connectionError.collectAsState()

    Column(modifier.fillMaxWidth().background(NavBg).padding(horizontal = UIConstants.SpacingMedium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DriveFly", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (connected) "已连接" else "等待电脑",
                    color = if (connected) Color(0xFF81C784) else Color(0xFFFFB74D),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(UIConstants.SpacingSmall)) {
                FilterChip(
                    selected = !vm.flightMode.value,
                    onClick = { vm.changeFlightMode(false) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = ButtonNormalBg, selectedContainerColor = UIElemPrimary),
                    label = { Text("地面载具", color = TextPrimary) }
                )
                FilterChip(
                    selected = vm.flightMode.value,
                    onClick = { vm.changeFlightMode(true) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = ButtonNormalBg, selectedContainerColor = UIElemPrimary),
                    label = { Text("飞机", color = TextPrimary) }
                )
            }

            IconButton(onClick = { onTogglePage(currentPage) }) {
                Icon(
                    imageVector = if (currentPage == Screens.Home) Icons.Default.Settings else Icons.Default.Home,
                    contentDescription = if (currentPage == Screens.Home) "连接与控制设置" else "返回手柄",
                    tint = TextPrimary
                )
            }
        }
        if (!vm.flightMode.value && vm.motionSuspended.value) {
            Text(
                "手机接近水平，已暂停体感；抬起后自动回中。",
                color = Color(0xFFFFB74D),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = UIConstants.SpacingSmall)
            )
        }
        if (!connected && connectionError != null) {
            Text(
                text = connectionError.orEmpty(),
                color = Color(0xFFFFB74D),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = UIConstants.SpacingSmall)
            )
        }
    }
}
