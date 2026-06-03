package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.sduiComponent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ComponentRegistryTest {
    private val noopRenderer: @Composable RenderScope.(TextProps) -> Unit = {}

    @Test
    fun rendererFor_returns_component_by_type() {
        val registry = componentRegistry {
            register(sduiComponent<TextProps>("text"), noopRenderer)
        }
        val entry = registry.rendererFor("text")
        assertNotNull(entry)
        assertEquals("text", entry.component.type)
    }

    @Test
    fun rendererFor_returns_null_for_absent_type() {
        val registry = componentRegistry {
            register(sduiComponent<TextProps>("text"), noopRenderer)
        }
        assertNull(registry.rendererFor("badge"))
    }

    @Test
    fun plus_combines_registries() {
        val base = componentRegistry {
            register(sduiComponent<TextProps>("text"), noopRenderer)
        }
        val extra = componentRegistry {
            register(sduiComponent<ButtonProps>("button")) {}
        }
        val combined = base + extra
        assertNotNull(combined.rendererFor("text"))
        assertNotNull(combined.rendererFor("button"))
    }

    @Test
    fun decodes_text_props_from_json() {
        val component = sduiComponent<TextProps>("text")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(mapOf("text" to JsonPrimitive("hola"))),
        )
        assertEquals(TextProps("hola"), props)
    }

    @Test
    fun decodes_text_props_with_defaults_from_empty_json() {
        val component = sduiComponent<TextProps>("text")
        val props = DefaultSduiJson.decodeFromJsonElement(component.serializer, JsonObject(emptyMap()))
        assertEquals(TextProps(""), props)
    }

    @Test
    fun invalid_props_decode_to_null_via_runcatching() {
        val component = sduiComponent<TextProps>("text")
        val props = runCatching {
            DefaultSduiJson.decodeFromJsonElement(
                component.serializer,
                JsonObject(mapOf("text" to JsonPrimitive(42))),
            )
        }.getOrNull()
        assertNull(props)
    }

    @Test
    fun corepack_registers_scaffold_components() {
        assertNotNull(CorePack.rendererFor("scaffold"))
        assertNotNull(CorePack.rendererFor("topAppBar"))
        assertNotNull(CorePack.rendererFor("bottomBar"))
        // `bottomBarItem` no es componente standalone: se decodifica dentro de `bottomBar`.
        assertNull(CorePack.rendererFor("bottomBarItem"))
    }

    @Test
    fun corepack_registers_image() {
        assertNotNull(CorePack.rendererFor("image"))
    }
}
