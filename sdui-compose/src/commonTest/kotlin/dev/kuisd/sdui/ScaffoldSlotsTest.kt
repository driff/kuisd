package dev.kuisd.sdui

import dev.kuisd.sdui.core.SduiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScaffoldSlotsTest {
    @Test
    fun partitions_mixed_children_into_slots_preserving_content_order() {
        val top = SduiNode(type = "topAppBar")
        val bottom = SduiNode(type = "bottomBar")
        val a = SduiNode(type = "text", id = "a")
        val b = SduiNode(type = "column", id = "b")
        // Orden arbitrario: content intercalado con las barras.
        val slots = partitionScaffoldSlots(listOf(a, bottom, top, b))

        assertEquals(top, slots.topBar)
        assertEquals(bottom, slots.bottomBar)
        assertEquals(listOf(a, b), slots.content)
    }

    @Test
    fun without_bars_everything_goes_to_content() {
        val a = SduiNode(type = "text", id = "a")
        val b = SduiNode(type = "text", id = "b")
        val slots = partitionScaffoldSlots(listOf(a, b))

        assertNull(slots.topBar)
        assertNull(slots.bottomBar)
        assertEquals(listOf(a, b), slots.content)
    }

    @Test
    fun duplicate_top_bar_keeps_first_and_drops_second_from_content() {
        val first = SduiNode(type = "topAppBar", id = "first")
        val second = SduiNode(type = "topAppBar", id = "second")
        val content = SduiNode(type = "text", id = "c")
        val slots = partitionScaffoldSlots(listOf(first, second, content))

        assertEquals(first, slots.topBar)
        // El segundo topAppBar se descarta: NO cae a content.
        assertEquals(listOf(content), slots.content)
    }

    @Test
    fun empty_children_yield_empty_slots() {
        val slots = partitionScaffoldSlots(emptyList())

        assertNull(slots.topBar)
        assertNull(slots.bottomBar)
        assertNull(slots.fab)
        assertEquals(emptyList(), slots.content)
    }

    @Test
    fun fab_child_goes_to_fab_slot_first_wins_and_not_to_content() {
        val first = SduiNode(type = "fab", id = "first")
        val second = SduiNode(type = "fab", id = "second")
        val content = SduiNode(type = "text", id = "c")
        val slots = partitionScaffoldSlots(listOf(content, first, second))

        assertEquals(first, slots.fab)
        assertEquals(listOf(content), slots.content) // el 2.º fab se descarta, no cae a content
    }
}
