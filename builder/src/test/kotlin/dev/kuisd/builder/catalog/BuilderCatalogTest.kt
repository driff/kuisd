package dev.kuisd.builder.catalog

import dev.kuisd.sdui.CorePack
import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `catalogByType tiene una sola entrada para topAppBar (base, no preset)`() {
        val entry = assertNotNull(catalogByType["topAppBar"], "falta la entrada base 'topAppBar'")
        assertFalse(entry.isPreset, "la entrada de catalogByType['topAppBar'] no debe ser un preset")
        assertEquals("topAppBar", entry.key, "la base de topAppBar debe tener key == type")
    }

    @Test
    fun `todas las key de builderCatalog son unicas`() {
        val keys = builderCatalog.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "hay keys duplicadas en builderCatalog: $keys")
    }

    @Test
    fun `los types base (no preset) son unicos`() {
        // catalogByType = associateBy { type } sobre las entradas base: un type base duplicado se perdería
        // en silencio. Este invariante lo evita.
        val baseTypes = builderCatalog.filterNot { it.isPreset }.map { it.type }
        assertEquals(baseTypes.size, baseTypes.toSet().size, "hay types base duplicados: $baseTypes")
    }

    @Test
    fun `existe la categoria Presets en el catalogo`() {
        assertTrue(
            builderCatalog.any { it.category == Category.Presets },
            "no hay ninguna entrada en Category.Presets",
        )
    }

    @Test
    fun `scaffold tiene campo contentDirection`() {
        val scaffold = assertNotNull(catalogByType["scaffold"], "falta la entrada 'scaffold'")
        assertTrue(
            scaffold.fields.any { it.key == "contentDirection" },
            "scaffold no tiene un campo con key 'contentDirection'",
        )
    }

    @Test
    fun `hojas tienen fillMaxWidth y padding`() {
        for (type in listOf("text", "button", "image")) {
            val entry = assertNotNull(catalogByType[type], "falta la entrada '$type'")
            assertTrue(
                entry.fields.any { it.key == "fillMaxWidth" && it.target == FieldTarget.Modifier },
                "'$type' no tiene field 'fillMaxWidth' (Modifier)",
            )
            assertTrue(
                entry.fields.any { it.key == "padding" && it.target == FieldTarget.Modifier },
                "'$type' no tiene field 'padding' (Modifier)",
            )
        }
    }

    @Test
    fun `column tiene alignment horizontal`() {
        val column = assertNotNull(catalogByType["column"], "falta la entrada 'column'")
        val alignment = assertNotNull(
            column.fields.firstOrNull { it.key == "alignment" },
            "'column' no tiene field 'alignment'",
        )
        assertEquals(
            listOf(
                Tokens.Alignment.Start.ref,
                Tokens.Alignment.CenterHorizontally.ref,
                Tokens.Alignment.End.ref,
            ),
            alignment.options,
        )
    }

    @Test
    fun `row tiene alignment vertical`() {
        val row = assertNotNull(catalogByType["row"], "falta la entrada 'row'")
        val alignment = assertNotNull(
            row.fields.firstOrNull { it.key == "alignment" },
            "'row' no tiene field 'alignment'",
        )
        assertEquals(
            listOf(
                Tokens.Alignment.Top.ref,
                Tokens.Alignment.CenterVertically.ref,
                Tokens.Alignment.Bottom.ref,
            ),
            alignment.options,
        )
    }
}
