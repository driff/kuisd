package dev.kuisd.app

import androidx.compose.material3.SnackbarHostState
import dev.kuisd.sdui.VariableScope
import dev.kuisd.sdui.core.DismissOverlay
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog
import dev.kuisd.sdui.core.ShowSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OverlayActionHandlerTest {
    private fun newHandler(overlay: OverlayController) = OverlayActionHandler(
        overlay = overlay,
        snackbar = SnackbarHostState(),
        scope = CoroutineScope(Dispatchers.Unconfined),
        vars = VariableScope { null },
    )

    @Test
    fun supports_the_four_overlay_actions() {
        val h = newHandler(OverlayController())
        assertTrue(h.supports(ShowDialog(title = "t")))
        assertTrue(h.supports(ShowBottomSheet()))
        assertTrue(h.supports(ShowSnackbar(message = "m")))
        assertTrue(h.supports(DismissOverlay))
    }

    @Test
    fun does_not_support_other_actions() {
        val h = newHandler(OverlayController())
        assertFalse(h.supports(Navigate(route = "x")))
    }

    @Test
    fun showDialog_mutates_controller() {
        val overlay = OverlayController()
        newHandler(overlay).handle(listOf(ShowDialog(title = "hello")))
        assertEquals(ShowDialog(title = "hello"), overlay.dialog)
        assertNull(overlay.sheet)
    }

    @Test
    fun showBottomSheet_mutates_controller() {
        val overlay = OverlayController()
        newHandler(overlay).handle(listOf(ShowBottomSheet()))
        assertEquals(ShowBottomSheet(), overlay.sheet)
        assertNull(overlay.dialog)
    }

    @Test
    fun dismissOverlay_clears_controller() {
        val overlay = OverlayController()
        overlay.showDialog(ShowDialog(title = "t"))
        newHandler(overlay).handle(listOf(DismissOverlay))
        assertNull(overlay.dialog)
        assertNull(overlay.sheet)
    }
}
