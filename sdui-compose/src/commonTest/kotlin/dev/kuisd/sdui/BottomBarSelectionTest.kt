package dev.kuisd.sdui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarSelectionTest {
    @Test
    fun matching_value_is_selected() {
        assertTrue(isItemSelected(selectedValue = "home", itemValue = "home"))
    }

    @Test
    fun non_matching_value_is_not_selected() {
        assertFalse(isItemSelected(selectedValue = "feed", itemValue = "home"))
    }

    @Test
    fun null_selected_value_is_not_selected() {
        assertFalse(isItemSelected(selectedValue = null, itemValue = "home"))
    }
}
