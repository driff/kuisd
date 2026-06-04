package dev.kuisd.builder.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainContainersTest {
    @Test
    fun `scaffold es contenedor principal y column o text no lo son`() {
        assertTrue(isMainContainer("scaffold"))
        assertFalse(isMainContainer("column"))
        assertFalse(isMainContainer("text"))
    }

    @Test
    fun `slotForChildType mapea types reservados y cae a content`() {
        val scaffold = assertNotNull(mainContainers["scaffold"])
        assertEquals("topBar", scaffold.slotForChildType("topAppBar").id)
        assertEquals("bottomBar", scaffold.slotForChildType("bottomBar").id)
        assertEquals("content", scaffold.slotForChildType("text").id)
    }

    @Test
    fun `reservedChildTypes son topAppBar, bottomBar y fab`() {
        val scaffold = assertNotNull(mainContainers["scaffold"])
        assertEquals(setOf("topAppBar", "bottomBar", "fab"), scaffold.reservedChildTypes)
    }

    @Test
    fun `contentSlot es content y admite multiples hijos`() {
        val scaffold = assertNotNull(mainContainers["scaffold"])
        assertEquals("content", scaffold.contentSlot.id)
        assertTrue(scaffold.contentSlot.multiple)
    }

    @Test
    fun `scaffold tiene un slot fab que reserva el type fab`() {
        val scaffold = assertNotNull(mainContainers["scaffold"])
        assertTrue(scaffold.slots.any { it.id == "fab" }, "el scaffold no tiene slot 'fab'")
        assertTrue("fab" in scaffold.reservedChildTypes, "'fab' no está en reservedChildTypes")
        assertEquals("fab", scaffold.slotForChildType("fab").id)
    }
}
