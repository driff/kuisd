package dev.kuisd.sdui

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.sduiComponent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

class TextFieldPropsTest {
    @Test
    fun decodes_full_props() {
        val component = sduiComponent<TextFieldProps>("textField")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(
                mapOf(
                    "bind" to JsonPrimitive("count"),
                    "placeholder" to JsonPrimitive("x"),
                    "label" to JsonPrimitive("L"),
                ),
            ),
        )
        assertEquals(TextFieldProps(bind = "count", placeholder = "x", label = "L"), props)
    }

    @Test
    fun decodes_defaults_from_empty_json() {
        val component = sduiComponent<TextFieldProps>("textField")
        val props = DefaultSduiJson.decodeFromJsonElement(component.serializer, JsonObject(emptyMap()))
        assertEquals(TextFieldProps(bind = "", placeholder = "", label = null), props)
    }

    @Test
    fun decodes_only_bind() {
        val component = sduiComponent<TextFieldProps>("textField")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(mapOf("bind" to JsonPrimitive("name"))),
        )
        assertEquals(TextFieldProps(bind = "name"), props)
    }
}
