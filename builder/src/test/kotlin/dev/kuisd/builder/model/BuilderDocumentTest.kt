package dev.kuisd.builder.model

import dev.kuisd.sdui.core.SduiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuilderDocumentTest {
    @Test
    fun deleting_an_ancestor_of_the_selection_resets_selection_to_root() {
        val doc = BuilderDocument()
        doc.insert(SduiNode(type = "column")) // → column-1 bajo la raíz (contenedor)
        doc.select("column-1")
        doc.insert(SduiNode(type = "text")) // → text-1 dentro de column-1
        doc.select("text-1")

        doc.delete("column-1") // borra el ancestro del seleccionado

        assertEquals("root", doc.selectedId)
        assertNull(TreeOps.findById(doc.root, "text-1"))
    }

    @Test
    fun insert_into_selected_container_nests_the_node() {
        val doc = BuilderDocument()
        doc.insert(SduiNode(type = "column"))
        doc.select("column-1")
        doc.insert(SduiNode(type = "button"))

        val column = TreeOps.findById(doc.root, "column-1")
        assertEquals(listOf("button-1"), column?.children?.map { it.id })
    }

    @Test
    fun ensureUniqueTree_makes_all_subtree_ids_unique() {
        val existing = mutableSetOf("text-1")
        // Subárbol con un id que colisiona y un hijo con el mismo id duplicado internamente.
        val subtree = SduiNode(
            type = "text",
            id = "text-1",
            children = listOf(SduiNode(type = "text", id = "text-1")),
        )
        val result = TreeOps.ensureUniqueTree(subtree, existing)
        val ids = TreeOps.collectIds(result)

        assertEquals(2, ids.size) // dos ids distintos
        assertEquals(0, ids.intersect(setOf("text-1")).size) // ninguno colisiona con el existente
    }
}
