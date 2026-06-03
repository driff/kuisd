package dev.kuisd.app

import androidx.compose.material3.SnackbarHostState
import dev.kuisd.app.nav.NavActionHandler
import dev.kuisd.app.nav.NavBackStack
import dev.kuisd.app.variables.VariableActionHandler
import dev.kuisd.app.variables.VariableStore
import dev.kuisd.sdui.VariableScope
import dev.kuisd.sdui.core.CustomAction
import dev.kuisd.sdui.core.Increment
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.ShowDialog
import dev.kuisd.sdui.core.Track
import dev.kuisd.sdui.core.UiAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppActionHandlerTest {
    @Test
    fun mixed_list_dispatches_each_action_to_the_right_sub_handler() {
        val stack = NavBackStack("home")
        val store = VariableStore()
        val app = AppActionHandler(
            listOf(NavActionHandler(stack), VariableActionHandler(store)),
        )

        app.handle(
            listOf(
                Increment(name = "n", by = 2, min = null, max = null),
                Navigate(route = "details"),
            ),
        )

        assertEquals(JsonPrimitive(2), store.vars["n"])
        assertEquals("details", stack.current.route)
    }

    @Test
    fun preserves_action_order() {
        val store = VariableStore()
        val app = AppActionHandler(listOf(VariableActionHandler(store)))

        app.handle(
            listOf(
                SetVar("x", JsonPrimitive(1)),
                Increment("x", by = 10, min = null, max = null),
            ),
        )

        assertEquals(JsonPrimitive(11), store.vars["x"])
    }

    @Test
    fun unsupported_action_is_a_noop_without_crash() {
        val stack = NavBackStack("home")
        val store = VariableStore()
        val app = AppActionHandler(
            listOf(NavActionHandler(stack), VariableActionHandler(store)),
        )

        app.handle(listOf(CustomAction(name = "weird")))

        assertEquals("home", stack.current.route)
        assertTrue(store.vars.isEmpty())
    }

    @Test
    fun track_is_supported_and_not_a_noop_fallthrough() {
        val track = TrackActionHandler()
        assertTrue(track.supports(Track(event = "screen_view")))

        // El compuesto con TrackActionHandler no debe tratar Track como "no soportada".
        val store = VariableStore()
        val app = AppActionHandler(listOf(track, VariableActionHandler(store)))
        // No crashea y no toca el store de variables.
        app.handle(listOf(Track(event = "tapped", props = mapOf("id" to "cta"))))
        assertTrue(store.vars.isEmpty())
    }

    @Test
    fun overlay_action_is_handled_when_overlay_handler_present() {
        val overlay = OverlayController()
        val overlayHandler = OverlayActionHandler(
            overlay = overlay,
            snackbar = SnackbarHostState(),
            scope = CoroutineScope(Dispatchers.Unconfined),
            vars = VariableScope { null },
        )
        val app = AppActionHandler(listOf(overlayHandler))

        app.handle(listOf(ShowDialog(title = "hi")))

        assertNotNull(overlay.active)
    }

    @Test
    fun sub_handler_supports_predicates_are_disjoint_for_known_actions() {
        val stack = NavBackStack("home")
        val store = VariableStore()
        val nav = NavActionHandler(stack)
        val vars = VariableActionHandler(store)

        val navOnly: List<UiAction> = listOf(Navigate(route = "x"))
        val varOnly: List<UiAction> = listOf(Increment("n"))
        for (a in navOnly) {
            assertTrue(nav.supports(a))
            assertFalse(vars.supports(a))
        }
        for (a in varOnly) {
            assertFalse(nav.supports(a))
            assertTrue(vars.supports(a))
        }
    }
}
