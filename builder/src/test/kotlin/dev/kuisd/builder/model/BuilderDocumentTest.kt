package dev.kuisd.builder.model

import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun fresh_document_exports_builder_defaults() {
        val envelope = BuilderDocument().toEnvelope()

        assertEquals(1, envelope.schemaVersion)
        assertEquals("builder", envelope.screenId)
        assertEquals("root", envelope.root.id)
        assertTrue(envelope.variables.isEmpty())
        assertTrue(envelope.meta.isEmpty())
    }

    @Test
    fun load_sets_tree_metadata_root_selection_and_clears_modified() {
        val doc = BuilderDocument()
        doc.insert(SduiNode(type = "text")) // ensucia el documento
        assertTrue(doc.isModified)

        val envelope = SduiEnvelope(
            schemaVersion = 3,
            screenId = "home",
            root = SduiNode(type = "column", id = "screen", children = listOf(SduiNode(type = "text", id = "t"))),
            variables = mapOf("k" to JsonPrimitive("v")),
            meta = mapOf("author" to "kuisd"),
        )
        val file = File("/tmp/blueprint.json")

        doc.load(envelope, file)

        assertEquals("screen", doc.root.id)
        assertEquals("screen", doc.selectedId)
        assertEquals(file, doc.currentFile)
        assertFalse(doc.isModified)
        // Round-trip de metadatos: toEnvelope reproduce lo cargado (HU-5).
        assertEquals(envelope, doc.toEnvelope())
    }

    @Test
    fun load_assigns_unique_ids_to_nodes_missing_them() {
        val doc = BuilderDocument()
        val envelope = SduiEnvelope(
            schemaVersion = 1,
            screenId = "builder",
            root = SduiNode(type = "column", children = listOf(SduiNode(type = "text"), SduiNode(type = "text"))),
        )

        doc.load(envelope, file = null)

        val ids = TreeOps.collectIds(doc.root)
        assertEquals(3, ids.size) // raíz + dos hijos, todos con id único
        assertNull(doc.currentFile)
    }

    @Test
    fun mutating_marks_modified_and_markSaved_clears_it() {
        val doc = BuilderDocument()
        assertFalse(doc.isModified)

        doc.insert(SduiNode(type = "text"))
        assertTrue(doc.isModified)

        val file = File("/tmp/saved.json")
        doc.markSaved(file)
        assertFalse(doc.isModified)
        assertEquals(file, doc.currentFile)

        doc.updateProps("text-1", JsonObject(mapOf("text" to JsonPrimitive("x"))))
        assertTrue(doc.isModified)
    }

    @Test
    fun newDocument_resets_to_empty_root_without_file() {
        val doc = BuilderDocument()
        doc.load(
            SduiEnvelope(schemaVersion = 2, screenId = "home", root = SduiNode(type = "column", id = "screen")),
            File("/tmp/x.json"),
        )

        doc.newDocument()

        assertEquals("root", doc.root.id)
        assertEquals("root", doc.selectedId)
        assertNull(doc.currentFile)
        assertFalse(doc.isModified)
        assertTrue(doc.root.children.isEmpty())
    }

    @Test
    fun delete_resets_selection_to_loaded_root_id_not_literal_root() {
        val doc = BuilderDocument()
        // Raíz cargada con id ≠ "root": el fallback de delete debe usar root.id.
        doc.load(
            SduiEnvelope(
                schemaVersion = 1,
                screenId = "builder",
                root = SduiNode(type = "column", id = "screen", children = listOf(SduiNode(type = "text", id = "t"))),
            ),
            file = null,
        )
        doc.select("t")

        doc.delete("t")

        assertEquals("screen", doc.selectedId)
    }
}
