package com.devboi.drivelink

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.data.SettingsRepository
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.ui.screens.Home
import com.devboi.drivelink.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroundLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun swappingPedalsReleasesHeldInputAndPersistsTheSide() {
        // The test APK's files keep this check separate from the user's saved settings.
        val context = InstrumentationRegistry.getInstrumentation().context
        val settings = SettingsRepository(context)
        settings.setAltLayoutSelected(false)
        val inputs = InputRepository()
        val vm = MainViewModel(settings, inputs, InputSender(inputs))
        compose.setContent { Home(vm) }

        val brake = compose.onNodeWithContentDescription("刹车，松手回零")
        val throttle = compose.onNodeWithContentDescription("油门，松手回零")
        assertTrue(brake.fetchSemanticsNode().boundsInRoot.center.x < throttle.fetchSemanticsNode().boundsInRoot.center.x)
        brake.performTouchInput { down(center) }
        compose.runOnIdle {
            assertTrue(inputs.getInputs().brake > 0f)
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            vm.changeIsAltLayoutSelected(true)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0f, inputs.getInputs().brake, 0f)
            assertEquals(0f, inputs.getInputs().clutch, 0f)
            assertTrue(SettingsRepository(context).getAltLayoutSelected())
        }
        // A newly composed pedal must not inherit the finger held on the old one.
        throttle.performTouchInput { up() }
        assertTrue(throttle.fetchSemanticsNode().boundsInRoot.center.x < brake.fetchSemanticsNode().boundsInRoot.center.x)
        throttle.performTouchInput { down(center) }
        compose.runOnIdle { assertTrue(inputs.getInputs().throttle > 0f) }
        throttle.performTouchInput { up() }
        compose.runOnIdle { assertEquals(0f, inputs.getInputs().throttle, 0f) }
    }
}
