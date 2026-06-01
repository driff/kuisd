package dev.kuisd.sdui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavBackStackTest {
    @Test
    fun starts_with_only_the_initial_entry() {
        val stack = NavBackStack(NavEntry("home"))
        assertEquals(NavEntry("home"), stack.current)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun push_changes_current_and_enables_back() {
        val stack = NavBackStack(NavEntry("home"))
        stack.push(NavEntry("details"))
        assertEquals(NavEntry("details"), stack.current)
        assertTrue(stack.canGoBack)
    }

    @Test
    fun pop_returns_to_previous_entry() {
        val stack = NavBackStack(NavEntry("home"))
        stack.push(NavEntry("details"))
        assertTrue(stack.pop())
        assertEquals(NavEntry("home"), stack.current)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun pop_on_root_returns_false_and_keeps_root() {
        val stack = NavBackStack(NavEntry("home"))
        assertFalse(stack.pop())
        assertEquals(NavEntry("home"), stack.current)
        assertEquals(1, stack.entries.size)
    }

    @Test
    fun entries_are_kept_in_lifo_order() {
        val stack = NavBackStack(NavEntry("home"))
        stack.push(NavEntry("details"))
        stack.push(NavEntry("more"))
        assertEquals(listOf(NavEntry("home"), NavEntry("details"), NavEntry("more")), stack.entries)
    }
}
