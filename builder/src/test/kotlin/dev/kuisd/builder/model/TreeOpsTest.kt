package dev.kuisd.builder.model

import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TreeOpsTest {
    private val containerTypes = setOf("column", "row", "card", "surface", "scaffold")
    private val root = SduiNode(type = "column", id = "root")

    private fun text() = SduiNode(type = "text", props = props("text" to "hola"))

    @Test
    fun `insert con parent contenedor lo añade a ese nodo`() {
        // root → column(c1) seleccionado; insertar text dentro de c1.
        val withContainer = TreeOps.insert(root, "root", SduiNode(type = "column"), containerTypes)
        val containerId = withContainer.children.single().id
        assertNotNull(containerId)

        val result = TreeOps.insert(withContainer, containerId, text(), containerTypes)
        val container = TreeOps.findById(result, containerId)
        assertNotNull(container)
        assertEquals(1, container.children.size)
        assertEquals("text", container.children.single().type)
        // No se duplica en la raíz.
        assertTrue(result.children.none { it.type == "text" })
    }

    @Test
    fun `insert con parent no-contenedor cae a la raiz`() {
        val withText = TreeOps.insert(root, "root", text(), containerTypes)
        val textId = withText.children.single { it.type == "text" }.id
        assertNotNull(textId)

        // Insertar con parentId = un text (no contenedor) → va a la raíz.
        val result = TreeOps.insert(withText, textId, SduiNode(type = "button"), containerTypes)
        assertTrue(result.children.any { it.type == "button" })
        assertTrue(TreeOps.findById(result, textId)!!.children.isEmpty())
    }

    @Test
    fun `insert con parentId null cae a la raiz`() {
        val result = TreeOps.insert(root, null, text(), containerTypes)
        assertEquals(1, result.children.size)
        assertEquals("text", result.children.single().type)
    }

    @Test
    fun `ids unicos tras varias inserciones del mismo type`() {
        var tree = root
        repeat(3) { tree = TreeOps.insert(tree, null, text(), containerTypes) }
        val ids = tree.children.map { it.id }
        assertEquals(listOf("text-1", "text-2", "text-3"), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `delete elimina el subarbol`() {
        val container = SduiNode(type = "column")
        val withContainer = TreeOps.insert(root, "root", container, containerTypes)
        val containerId = withContainer.children.single().id!!
        val withChild = TreeOps.insert(withContainer, containerId, text(), containerTypes)
        val textId = TreeOps.findById(withChild, containerId)!!.children.single().id!!

        val result = TreeOps.delete(withChild, containerId)
        assertNull(TreeOps.findById(result, containerId))
        assertNull(TreeOps.findById(result, textId)) // hijo también eliminado
    }

    @Test
    fun `delete nunca borra la raiz`() {
        val result = TreeOps.delete(root, "root")
        assertEquals(root, result)
    }

    @Test
    fun `updateProps cambia el nodo correcto`() {
        val withText = TreeOps.insert(root, null, text(), containerTypes)
        val id = withText.children.single().id!!
        val newProps = props("text" to "adios")
        val result = TreeOps.updateProps(withText, id, newProps)
        assertEquals(newProps, TreeOps.findById(result, id)!!.props)
    }

    @Test
    fun `updateModifier cambia el nodo correcto`() {
        val withText = TreeOps.insert(root, null, text(), containerTypes)
        val id = withText.children.single().id!!
        val modifier = UiModifier(fillMaxWidth = true)
        val result = TreeOps.updateModifier(withText, id, modifier)
        assertEquals(modifier, TreeOps.findById(result, id)!!.modifier)
    }

    @Test
    fun `findById busca en profundidad`() {
        val tree = SduiNode(
            type = "column",
            id = "root",
            children = listOf(SduiNode(type = "row", id = "r", children = listOf(text().copy(id = "t")))),
        )
        assertEquals("t", TreeOps.findById(tree, "t")?.id)
        assertNull(TreeOps.findById(tree, "missing"))
    }

    @Test
    fun `collectIds recoge todos los ids no nulos`() {
        val tree = SduiNode(
            type = "column",
            id = "root",
            children = listOf(
                SduiNode(type = "text", id = "a"),
                SduiNode(type = "text"), // sin id
                SduiNode(type = "row", id = "b", children = listOf(SduiNode(type = "text", id = "c"))),
            ),
        )
        assertEquals(setOf("root", "a", "b", "c"), TreeOps.collectIds(tree))
    }

    @Test
    fun `ensureId asigna id determinista cuando falta o colisiona`() {
        val assigned = TreeOps.ensureId(SduiNode(type = "text"), setOf("text-1"))
        assertEquals("text-2", assigned.id)

        // Si el id ya es único, lo conserva.
        val kept = TreeOps.ensureId(SduiNode(type = "text", id = "keep"), setOf("text-1"))
        assertEquals("keep", kept.id)
    }

    private fun props(vararg pairs: Pair<String, String>): JsonObject =
        JsonObject(pairs.associate { (k, v) -> k to JsonPrimitive(v) })
}
