package dev.kuisd.sdui

import androidx.compose.ui.layout.ContentScale
import dev.kuisd.sdui.modifier.toContentScale
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentScaleTest {
    @Test
    fun maps_known_keys() {
        assertEquals(ContentScale.Crop, "crop".toContentScale())
        assertEquals(ContentScale.Fit, "fit".toContentScale())
        assertEquals(ContentScale.FillBounds, "fillBounds".toContentScale())
        assertEquals(ContentScale.Inside, "inside".toContentScale())
        assertEquals(ContentScale.None, "none".toContentScale())
    }

    @Test
    fun unknown_key_defaults_to_fit() {
        assertEquals(ContentScale.Fit, "".toContentScale())
        assertEquals(ContentScale.Fit, "weird".toContentScale())
    }
}
