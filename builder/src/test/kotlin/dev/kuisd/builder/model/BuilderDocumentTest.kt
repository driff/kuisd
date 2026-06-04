package dev.kuisd.builder.model

import dev.kuisd.builder.catalog.builderCatalog
import dev.kuisd.builder.catalog.mainContainers
import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertEquals("scaffold", envelope.root.type) // raíz por defecto ahora es scaffold (HU-2.2)
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
    fun parentType_returns_container_type_of_a_child_and_null_for_root() {
        val doc = BuilderDocument()
        doc.insert(SduiNode(type = "column")) // → column-1 en el content del scaffold raíz
        doc.select("column-1")
        doc.insert(SduiNode(type = "text")) // → text-1 dentro de column-1

        assertEquals("scaffold", doc.parentType("column-1")) // raíz scaffold
        assertEquals("column", doc.parentType("text-1"))
        assertNull(doc.parentType("root")) // la raíz no tiene padre
        assertNull(doc.parentType(null))
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

    // ── T4: contenedores principales y ruteo de inserción ──────────────────────────────────────

    @Test
    fun new_document_root_is_an_empty_scaffold() {
        val doc = BuilderDocument()

        assertEquals("scaffold", doc.root.type)
        assertEquals("root", doc.root.id)
    }

    private val scaffoldSpec get() = mainContainers.getValue("scaffold")

    @Test
    fun inserting_a_topAppBar_routes_to_the_topBar_slot_and_replaces_on_repeat() {
        val doc = BuilderDocument()

        doc.insert(SduiNode(type = "topAppBar"))
        var topBar = SlotOps.project(doc.root, scaffoldSpec).getValue("topBar")
        assertEquals(1, topBar.size)
        assertEquals("topAppBar", topBar.single().type)

        // Un segundo topAppBar REEMPLAZA al anterior (slot único): sigue habiendo 1.
        doc.insert(SduiNode(type = "topAppBar"))
        topBar = SlotOps.project(doc.root, scaffoldSpec).getValue("topBar")
        assertEquals(1, topBar.size)
        assertEquals("topAppBar", topBar.single().type)
    }

    @Test
    fun inserting_a_bottomBar_routes_to_the_bottomBar_slot() {
        val doc = BuilderDocument()

        doc.insert(SduiNode(type = "bottomBar"))

        val bottomBar = SlotOps.project(doc.root, scaffoldSpec).getValue("bottomBar")
        assertEquals(1, bottomBar.size)
        assertEquals("bottomBar", bottomBar.single().type)
    }

    @Test
    fun inserting_a_text_routes_to_content() {
        val doc = BuilderDocument()

        doc.insert(SduiNode(type = "text"))

        val content = SlotOps.project(doc.root, scaffoldSpec).getValue("content")
        assertEquals(1, content.size)
        assertEquals("text", content.single().type)
    }

    @Test
    fun inserting_a_main_container_is_rejected_and_sets_lastError() {
        val doc = BuilderDocument()
        doc.insert(SduiNode(type = "topAppBar")) // estado previo: 1 nodo en topBar
        val before = doc.root

        doc.insert(SduiNode(type = "scaffold")) // contenedor principal: solo va en la raíz

        assertEquals(before, doc.root) // árbol intacto
        assertNotNull(doc.lastError)
    }

    @Test
    fun inserting_a_wrong_type_into_a_selected_single_slot_is_rejected() {
        val doc = BuilderDocument()
        doc.selectSlot("topBar")
        val before = doc.root

        doc.insert(SduiNode(type = "text")) // text no está en childTypes de topBar

        assertEquals(before, doc.root) // no muta
        assertNotNull(doc.lastError)
    }

    @Test
    fun loadWrapped_wraps_the_loaded_tree_as_scaffold_content() {
        val doc = BuilderDocument()
        val loaded = SduiNode(type = "column", id = "c", children = listOf(SduiNode(type = "text", id = "t")))

        doc.loadWrapped(SduiEnvelope(schemaVersion = 1, screenId = "builder", root = loaded), file = null)

        assertEquals("scaffold", doc.root.type)
        val content = SlotOps.project(doc.root, scaffoldSpec).getValue("content")
        assertEquals(listOf("column"), content.map { it.type })
        assertTrue(doc.isModified)
    }

    @Test
    fun inserting_a_fab_routes_to_the_fab_slot() {
        val doc = BuilderDocument()

        doc.insert(SduiNode(type = "fab"))

        val fab = SlotOps.project(doc.root, scaffoldSpec).getValue("fab")
        assertEquals(1, fab.size)
        assertEquals("fab", fab.single().type)
    }

    @Test
    fun inserting_the_topbar_nav_preset_fills_the_topBar_slot_with_its_actions_and_unique_ids() {
        val doc = BuilderDocument()
        val preset = builderCatalog.first { it.key == "preset-topbar-nav" }.template

        doc.insert(preset)

        val topBar = SlotOps.project(doc.root, scaffoldSpec).getValue("topBar")
        assertEquals(1, topBar.size)
        val bar = topBar.single()
        assertEquals("topAppBar", bar.type)
        assertEquals(mapOf("onNavigationClick" to listOf(NavigateBack)), bar.actions)
        // ids únicos en todo el árbol tras insertar.
        val ids = TreeOps.collectIds(doc.root)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun loading_a_scaffold_envelope_round_trips_slots_and_clears_modified() {
        val doc = BuilderDocument()
        val envelope = SduiEnvelope(
            schemaVersion = 1,
            screenId = "builder",
            root = SduiNode(
                type = "scaffold",
                id = "root",
                children = listOf(
                    SduiNode(type = "topAppBar", id = "bar"),
                    SduiNode(type = "text", id = "t"),
                ),
            ),
        )

        doc.load(envelope, file = null)

        val slots = SlotOps.project(doc.root, scaffoldSpec)
        assertEquals(listOf("topAppBar"), slots.getValue("topBar").map { it.type })
        assertEquals(listOf("text"), slots.getValue("content").map { it.type })
        assertTrue(slots.getValue("bottomBar").isEmpty())
        assertFalse(doc.isModified)
    }
}
