package dev.kuisd.sdui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveBindingTest {
    private val none: (String) -> JsonElement? = { null }

    @Test
    fun null_raw_returns_empty_string() {
        assertEquals("", resolveBinding(null, none))
    }

    @Test
    fun literal_without_dollar_returns_itself() {
        assertEquals("hola", resolveBinding("hola", none))
    }

    @Test
    fun dollar_name_resolves_string_variable() {
        val vars = mapOf("name" to JsonPrimitive("Ada"))
        assertEquals("Ada", resolveBinding("\$name") { vars[it] })
    }

    @Test
    fun dollar_name_resolves_int_variable() {
        val vars = mapOf("count" to JsonPrimitive(3))
        assertEquals("3", resolveBinding("\$count") { vars[it] })
    }

    @Test
    fun dollar_name_resolves_bool_variable() {
        val vars = mapOf("flag" to JsonPrimitive(true))
        assertEquals("true", resolveBinding("\$flag") { vars[it] })
    }

    @Test
    fun dollar_name_for_missing_variable_returns_empty_string() {
        assertEquals("", resolveBinding("\$missing", none))
    }

    @Test
    fun double_dollar_escapes_to_single_dollar_prefix() {
        assertEquals("\$literal", resolveBinding("\$\$literal", none))
    }

    @Test
    fun complex_json_element_degrades_to_toString_safely() {
        val vars = mapOf("arr" to JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
        // Aceptamos el toString() de JsonArray como degradación segura — no debe crashear.
        val result = resolveBinding("\$arr") { vars[it] }
        assertEquals("[1,2]", result)
    }
}
