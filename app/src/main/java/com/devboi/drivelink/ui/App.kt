package com.devboi.drivelink.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.devboi.drivelink.ui.components.Nav
import com.devboi.drivelink.ui.screens.Home
import com.devboi.drivelink.ui.screens.Flight
import com.devboi.drivelink.ui.screens.Screens
import com.devboi.drivelink.ui.screens.Settings
import com.devboi.drivelink.viewmodel.MainViewModel

@Composable
fun App(vm: MainViewModel, modifier: Modifier = Modifier) {
    val onPageChange = { currentPage: Screens ->
        vm.changeScreen(when (currentPage) {
            Screens.Home -> Screens.Settings
            Screens.Settings -> {
                // Save settings when going back to home screen
                vm.saveSettings()
                
                Screens.Home
            }
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Nav(vm, vm.currentScreen.value, onPageChange)

        when (vm.currentScreen.value) {
            Screens.Home -> key(vm.resetEpoch.value) {
                if (vm.flightMode.value) Flight(vm) else Home(vm)
            }
            Screens.Settings -> Settings(vm)
        }
    }
}
