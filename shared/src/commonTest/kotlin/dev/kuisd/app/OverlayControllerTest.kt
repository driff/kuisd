package dev.kuisd.app

import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverlayControllerTest {
    @Test
    fun showDialog_clears_sheet() {
        val c = OverlayController()
        c.showSheet(ShowBottomSheet())
        c.showDialog(ShowDialog(title = "t"))

        assertEquals(ShowDialog(title = "t"), c.dialog)
        assertNull(c.sheet)
    }

    @Test
    fun showSheet_clears_dialog() {
        val c = OverlayController()
        c.showDialog(ShowDialog(title = "t"))
        c.showSheet(ShowBottomSheet())

        assertEquals(ShowBottomSheet(), c.sheet)
        assertNull(c.dialog)
    }

    @Test
    fun dismissAll_clears_both() {
        val c = OverlayController()
        c.showDialog(ShowDialog(title = "t"))
        c.dismissAll()
        assertNull(c.dialog)
        assertNull(c.sheet)

        c.showSheet(ShowBottomSheet())
        c.dismissAll()
        assertNull(c.dialog)
        assertNull(c.sheet)
    }
}
