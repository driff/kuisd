package dev.kuisd.app

import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverlayControllerTest {
    @Test
    fun showDialog_replaces_sheet() {
        val c = OverlayController()
        c.showSheet(ShowBottomSheet())
        c.showDialog(ShowDialog(title = "t"))

        assertEquals(ActiveOverlay.Dialog(ShowDialog(title = "t")), c.active)
    }

    @Test
    fun showSheet_replaces_dialog() {
        val c = OverlayController()
        c.showDialog(ShowDialog(title = "t"))
        c.showSheet(ShowBottomSheet())

        assertEquals(ActiveOverlay.Sheet(ShowBottomSheet()), c.active)
    }

    @Test
    fun dismissAll_clears_active() {
        val c = OverlayController()
        c.showDialog(ShowDialog(title = "t"))
        c.dismissAll()
        assertNull(c.active)

        c.showSheet(ShowBottomSheet())
        c.dismissAll()
        assertNull(c.active)
    }
}
