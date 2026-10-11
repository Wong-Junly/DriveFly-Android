package com.devboi.drivelink

import com.devboi.drivelink.ui.components.calculateSliderPos
import org.junit.Assert.assertEquals
import org.junit.Test

class SliderTest {
    @Test fun startsNeutralAndClampsBothDirections() {
        assertEquals(0f, calculateSliderPos(0f, 0f, true), 0f)
        assertEquals(1f, calculateSliderPos(0f, 100f, true), 0f)
        assertEquals(0f, calculateSliderPos(100f, 100f, true), 0f)
        assertEquals(0f, calculateSliderPos(0f, 100f, false), 0f)
        assertEquals(1f, calculateSliderPos(100f, 100f, false), 0f)
        assertEquals(0f, calculateSliderPos(200f, 100f, true), 0f)
        assertEquals(1f, calculateSliderPos(-50f, 100f, true), 0f)
    }
}
