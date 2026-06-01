package dev.kuisd.app.nav

import dev.kuisd.sdui.core.CustomAction
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.NavigateBack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NavActionHandlerTest {
    @Test
    fun navigate_pushes_route_with_args() {
        val stack = NavBackStack("home")
        val handler = NavActionHandler(stack)

        handler.handle(listOf(Navigate(route = "details", args = mapOf("id" to "42"))))

        assertEquals("details", stack.current.route)
        assertEquals(mapOf("id" to "42"), stack.current.args)
    }

    @Test
    fun navigate_back_pops_top() {
        val stack = NavBackStack("home")
        val handler = NavActionHandler(stack)
        handler.handle(listOf(Navigate(route = "details")))

        handler.handle(listOf(NavigateBack))

        assertEquals("home", stack.current.route)
    }

    @Test
    fun navigate_back_on_root_is_noop() {
        val stack = NavBackStack("home")
        val handler = NavActionHandler(stack)

        handler.handle(listOf(NavigateBack))

        assertEquals("home", stack.current.route)
        assertFalse(stack.canGoBack)
        assertEquals(1, stack.entries.size)
    }

    @Test
    fun unsupported_action_does_not_alter_the_stack() {
        val stack = NavBackStack("home")
        val handler = NavActionHandler(stack)

        handler.handle(listOf(CustomAction(name = "doSomething")))

        assertEquals("home", stack.current.route)
        assertEquals(1, stack.entries.size)
    }

    @Test
    fun empty_action_list_does_not_alter_the_stack() {
        val stack = NavBackStack("home")
        val handler = NavActionHandler(stack)

        handler.handle(emptyList())

        assertEquals("home", stack.current.route)
        assertEquals(1, stack.entries.size)
    }
}
