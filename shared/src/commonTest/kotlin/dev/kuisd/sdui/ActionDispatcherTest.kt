package dev.kuisd.sdui

import dev.kuisd.sdui.core.CustomAction
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.NavigateBack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ActionDispatcherTest {
    @Test
    fun navigate_pushes_route_with_args() {
        val stack = NavBackStack(NavEntry("home"))
        val dispatcher = NavigationActionDispatcher(stack)

        dispatcher.dispatch(listOf(Navigate(route = "details", args = mapOf("id" to "42"))))

        assertEquals(NavEntry("details", mapOf("id" to "42")), stack.current)
    }

    @Test
    fun navigate_back_pops_top() {
        val stack = NavBackStack(NavEntry("home"))
        val dispatcher = NavigationActionDispatcher(stack)
        dispatcher.dispatch(listOf(Navigate(route = "details")))

        dispatcher.dispatch(listOf(NavigateBack))

        assertEquals(NavEntry("home"), stack.current)
    }

    @Test
    fun navigate_back_on_root_is_noop() {
        val stack = NavBackStack(NavEntry("home"))
        val dispatcher = NavigationActionDispatcher(stack)

        dispatcher.dispatch(listOf(NavigateBack))

        assertEquals(NavEntry("home"), stack.current)
        assertFalse(stack.canGoBack)
        assertEquals(1, stack.entries.size)
    }

    @Test
    fun unsupported_action_does_not_alter_the_stack() {
        val stack = NavBackStack(NavEntry("home"))
        val dispatcher = NavigationActionDispatcher(stack)

        dispatcher.dispatch(listOf(CustomAction(name = "doSomething")))

        assertEquals(NavEntry("home"), stack.current)
        assertEquals(1, stack.entries.size)
    }

    @Test
    fun empty_action_list_does_not_alter_the_stack() {
        val stack = NavBackStack(NavEntry("home"))
        val dispatcher = NavigationActionDispatcher(stack)

        dispatcher.dispatch(emptyList())

        assertEquals(NavEntry("home"), stack.current)
        assertEquals(1, stack.entries.size)
    }
}
