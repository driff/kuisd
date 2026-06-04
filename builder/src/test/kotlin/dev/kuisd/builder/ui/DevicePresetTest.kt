package dev.kuisd.builder.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevicePresetTest {
    @Test
    fun `los tres presets tienen dimensiones positivas`() {
        DevicePreset.entries.forEach { preset ->
            assertTrue(preset.width.value > 0f, "${preset.label} width > 0")
            assertTrue(preset.height.value > 0f, "${preset.label} height > 0")
        }
    }

    @Test
    fun `el dispositivo por defecto es Phone`() {
        assertEquals(DevicePreset.Phone, DefaultDevice)
    }
}
