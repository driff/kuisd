package dev.kuisd.builder.catalog

import dev.kuisd.sdui.CorePack
import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuilderCatalogTest {
    @Test
    fun `cada plantilla hace round-trip por DefaultSduiJson`() {
        for (entry in builderCatalog) {
            val json = DefaultSduiJson.encodeToString(SduiNode.serializer(), entry.template)
            val decoded = DefaultSduiJson.decodeFromString(SduiNode.serializer(), json)
            assertEquals(entry.template, decoded, "round-trip falló para type='${entry.type}'")
        }
    }

    @Test
    fun `cada type esta registrado en CorePack`() {
        for (entry in builderCatalog) {
            assertNotNull(
                CorePack.rendererFor(entry.type),
                "type='${entry.type}' no está registrado en CorePack",
            )
        }
    }

    @Test
    fun `todas las categorias estan presentes`() {
        val present = builderCatalog.map { it.category }.toSet()
        assertEquals(Category.entries.toSet(), present)
    }

    @Test
    fun `ningun field tiene key vacia`() {
        for (entry in builderCatalog) {
            for (field in entry.fields) {
                assertTrue(field.key.isNotBlank(), "field con key vacía en type='${entry.type}'")
            }
        }
    }

    @Test
    fun `bottomBar esta en el catalogo`() {
        assertNotNull(catalogByType["bottomBar"], "falta la entrada 'bottomBar'")
    }

    @Test
    fun `scaffold tiene campo contentDirection`() {
        val scaffold = assertNotNull(catalogByType["scaffold"], "falta la entrada 'scaffold'")
        assertTrue(
            scaffold.fields.any { it.key == "contentDirection" },
            "scaffold no tiene un campo con key 'contentDirection'",
        )
    }
}
