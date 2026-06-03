package dev.kuisd.app

import androidx.compose.material3.SnackbarDuration
import dev.kuisd.sdui.VariableScope
import dev.kuisd.sdui.core.ShowSnackbar
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class OverlaySnackbarTest {
    @Test
    fun resolveSnackbarMessage_uses_var_when_present() {
        val scope = VariableScope { name -> mapOf("user" to JsonPrimitive("Ana"))[name] }
        val action = ShowSnackbar(message = "literal", messageVar = "\$user")

        assertEquals("Ana", resolveSnackbarMessage(action, scope))
    }

    @Test
    fun resolveSnackbarMessage_falls_back_to_literal_when_var_absent() {
        val scope = VariableScope { null }
        val action = ShowSnackbar(message = "literal", messageVar = "\$missing")

        assertEquals("literal", resolveSnackbarMessage(action, scope))
    }

    @Test
    fun resolveSnackbarMessage_uses_literal_when_no_var() {
        val scope = VariableScope { JsonPrimitive("nope") }
        val action = ShowSnackbar(message = "literal")

        assertEquals("literal", resolveSnackbarMessage(action, scope))
    }

    @Test
    fun resolveSnackbarMessage_stringifies_non_primitive_var() {
        val obj = buildJsonObject { put("k", "v") }
        val scope = VariableScope { name -> mapOf("data" to obj)[name] }
        val action = ShowSnackbar(message = "literal", messageVar = "\$data")

        assertEquals(obj.toString(), resolveSnackbarMessage(action, scope))
    }

    @Test
    fun toSnackbarDuration_maps_known_and_unknown() {
        assertEquals(SnackbarDuration.Short, "short".toSnackbarDuration())
        assertEquals(SnackbarDuration.Long, "long".toSnackbarDuration())
        assertEquals(SnackbarDuration.Indefinite, "indefinite".toSnackbarDuration())
        assertEquals(SnackbarDuration.Short, "weird".toSnackbarDuration())
    }
}
