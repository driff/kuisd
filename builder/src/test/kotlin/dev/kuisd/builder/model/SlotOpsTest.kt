package dev.kuisd.builder.model

import dev.kuisd.builder.catalog.mainContainers
import dev.kuisd.sdui.core.SduiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotOpsTest {
    private val scaffoldSpec = mainContainers.getValue("scaffold")
    private val topBarSlot = scaffoldSpec.slots.first { it.id == "topBar" }

    private fun node(type: String, id: String) = SduiNode(type = type, id = id)

    private fun ids(nodes: List<SduiNode>) = nodes.map { it.id }

    @Test
    fun `project parte como el motor - bars a su slot y resto a content en orden`() {
        val scaffold = SduiNode(
            type = "scaffold",
            id = "root",
            children = listOf(
                node("text", "text-1"),
                node("topAppBar", "topAppBar-1"),
                node("text", "text-2"),
                node("bottomBar", "bottomBar-1"),
            ),
        )

        val slots = SlotOps.project(scaffold, scaffoldSpec)

        assertEquals(listOf("topAppBar-1"), ids(slots.getValue("topBar")))
        assertEquals(listOf("bottomBar-1"), ids(slots.getValue("bottomBar")))
        assertEquals(listOf("text-1", "text-2"), ids(slots.getValue("content")))
    }

    @Test
    fun `project con 2 topAppBar - solo el primero en topBar y el segundo cae a content visible`() {
        val scaffold = SduiNode(
            type = "scaffold",
            id = "root",
            children = listOf(
                node("topAppBar", "topAppBar-1"),
                node("topAppBar", "topAppBar-2"),
                node("text", "text-1"),
            ),
        )

        val slots = SlotOps.project(scaffold, scaffoldSpec)

        assertEquals(listOf("topAppBar-1"), ids(slots.getValue("topBar")))
        // El 2.º topAppBar no entra al slot único (lleno) ⇒ cae a content, VISIBLE (no fantasma).
        assertEquals(listOf("topAppBar-2", "text-1"), ids(slots.getValue("content")))
    }

    @Test
    fun `setSingleSlot topBar con uno existente - reemplaza sin duplicar y deja content intacto`() {
        val scaffold = SduiNode(
            type = "scaffold",
            id = "root",
            children = listOf(
                node("topAppBar", "topAppBar-old"),
                node("text", "text-1"),
                node("bottomBar", "bottomBar-1"),
            ),
        )

        val result = SlotOps.setSingleSlot(scaffold, scaffoldSpec, topBarSlot, node("topAppBar", "topAppBar-new"))

        val topBars = result.children.filter { it.type == "topAppBar" }
        assertEquals(listOf("topAppBar-new"), ids(topBars))
        assertEquals(
            listOf("topAppBar-new", "text-1", "bottomBar-1"),
            ids(result.children),
        )
    }

    @Test
    fun `setSingleSlot topBar sin existente - lo añade en orden canonico`() {
        val scaffold = SduiNode(
            type = "scaffold",
            id = "root",
            children = listOf(
                node("text", "text-1"),
                node("bottomBar", "bottomBar-1"),
            ),
        )

        val result = SlotOps.setSingleSlot(scaffold, scaffoldSpec, topBarSlot, node("topAppBar", "topAppBar-1"))

        assertEquals(
            listOf("topAppBar-1", "text-1", "bottomBar-1"),
            ids(result.children),
        )
    }

    @Test
    fun `normalize reordena children desordenados al orden canonico`() {
        val scaffold = SduiNode(
            type = "scaffold",
            id = "root",
            children = listOf(
                node("bottomBar", "bottomBar-1"),
                node("text", "text-1"),
                node("topAppBar", "topAppBar-1"),
            ),
        )

        val result = SlotOps.normalize(scaffold, scaffoldSpec)

        assertEquals(
            listOf("topAppBar-1", "text-1", "bottomBar-1"),
            ids(result.children),
        )
    }

    @Test
    fun `normalize con 2 topAppBar - primero como topBar y segundo degradado a content`() {
        val scaffold = SduiNode(
            type = "scaffold",
            id = "root",
            children = listOf(
                node("topAppBar", "topAppBar-1"),
                node("topAppBar", "topAppBar-2"),
                node("text", "text-1"),
            ),
        )

        val result = SlotOps.normalize(scaffold, scaffoldSpec)

        // El 1.º queda como topBar; el 2.º se conserva, degradado al content (al final del content).
        assertEquals(
            listOf("topAppBar-1", "topAppBar-2", "text-1"),
            ids(result.children),
        )
        assertTrue("topAppBar-2" in ids(result.children))
    }

    @Test
    fun `containsMainContainer true con scaffold anidado y false sin el`() {
        val withScaffold = SduiNode(
            type = "column",
            id = "root",
            children = listOf(
                node("text", "text-1"),
                SduiNode(type = "scaffold", id = "nested"),
            ),
        )
        val withoutScaffold = SduiNode(
            type = "column",
            id = "root",
            children = listOf(
                node("text", "text-1"),
                node("text", "text-2"),
            ),
        )

        assertTrue(SlotOps.containsMainContainer(withScaffold))
        assertFalse(SlotOps.containsMainContainer(withoutScaffold))
    }
}
