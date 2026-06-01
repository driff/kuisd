package dev.kuisd.app.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NavBackStackTest {
    @Test
    fun starts_with_only_the_initial_entry() {
        val stack = NavBackStack("home")
        assertEquals("home", stack.current.route)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun push_changes_current_and_enables_back() {
        val stack = NavBackStack("home")
        stack.push("details")
        assertEquals("details", stack.current.route)
        assertTrue(stack.canGoBack)
    }

    @Test
    fun push_carries_args() {
        val stack = NavBackStack("home")
        stack.push("details", mapOf("id" to "42"))
        assertEquals("details", stack.current.route)
        assertEquals(mapOf("id" to "42"), stack.current.args)
    }

    @Test
    fun pop_returns_to_previous_entry() {
        val stack = NavBackStack("home")
        stack.push("details")
        assertTrue(stack.pop())
        assertEquals("home", stack.current.route)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun pop_on_root_returns_false_and_keeps_root() {
        val stack = NavBackStack("home")
        assertFalse(stack.pop())
        assertEquals("home", stack.current.route)
        assertEquals(1, stack.entries.size)
    }

    @Test
    fun entries_are_kept_in_lifo_order() {
        val stack = NavBackStack("home")
        stack.push("details")
        stack.push("more")
        assertEquals(listOf("home", "details", "more"), stack.entries.map { it.route })
    }

    @Test
    fun two_entries_with_same_route_have_distinct_ids() {
        val stack = NavBackStack("home")
        stack.push("details")
        stack.pop()
        stack.push("details")
        val ids = stack.entries.map { it.id }
        assertEquals(2, stack.entries.size)
        assertNotEquals(ids[0], ids[1])
    }
}
