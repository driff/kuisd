package dev.kuisd.sdui

import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodePropsTest {
    @Test
    fun stringProp_returns_value_for_string_prop() {
        val node = SduiNode(
            type = "text",
            props = JsonObject(mapOf("text" to JsonPrimitive("Bienvenido a kuisd"))),
        )
        assertEquals("Bienvenido a kuisd", node.stringProp("text"))
    }

    @Test
    fun stringProp_returns_null_when_missing() {
        val node = SduiNode(type = "text")
        assertNull(node.stringProp("text"))
    }

    @Test
    fun stringProp_returns_null_for_non_string_prop() {
        val node = SduiNode(
            type = "text",
            props = JsonObject(mapOf("count" to JsonPrimitive(42))),
        )
        assertNull(node.stringProp("count"))
    }
}
