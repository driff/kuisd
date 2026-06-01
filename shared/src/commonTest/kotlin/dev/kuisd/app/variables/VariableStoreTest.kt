package dev.kuisd.app.variables

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class VariableStoreTest {
    @Test
    fun set_fixes_value_for_new_key() {
        val store = VariableStore()
        store.set("x", JsonPrimitive(7))
        assertEquals(JsonPrimitive(7), store.vars["x"])
    }

    @Test
    fun seed_replaces_the_whole_map() {
        val store = VariableStore()
        store.set("a", JsonPrimitive(1))
        store.seed(mapOf("b" to JsonPrimitive(2)))
        assertEquals(null, store.vars["a"])
        assertEquals(JsonPrimitive(2), store.vars["b"])
    }

    @Test
    fun toggle_missing_variable_sets_true() {
        val store = VariableStore()
        store.toggle("flag")
        assertEquals(JsonPrimitive(true), store.vars["flag"])
    }

    @Test
    fun toggle_true_becomes_false() {
        val store = VariableStore()
        store.seed(mapOf("flag" to JsonPrimitive(true)))
        store.toggle("flag")
        assertEquals(JsonPrimitive(false), store.vars["flag"])
    }

    @Test
    fun toggle_non_boolean_coerces_to_false_then_true() {
        val store = VariableStore()
        store.seed(mapOf("flag" to JsonPrimitive("not a bool")))
        store.toggle("flag")
        assertEquals(JsonPrimitive(true), store.vars["flag"])
    }

    @Test
    fun increment_sums_by() {
        val store = VariableStore()
        store.seed(mapOf("n" to JsonPrimitive(3)))
        store.increment("n", by = 2, min = null, max = null)
        assertEquals(JsonPrimitive(5), store.vars["n"])
    }

    @Test
    fun increment_clamps_to_max() {
        val store = VariableStore()
        store.seed(mapOf("n" to JsonPrimitive(9)))
        store.increment("n", by = 5, min = 0, max = 10)
        assertEquals(JsonPrimitive(10), store.vars["n"])
    }

    @Test
    fun increment_clamps_to_min() {
        val store = VariableStore()
        store.seed(mapOf("n" to JsonPrimitive(1)))
        store.increment("n", by = -5, min = 0, max = 10)
        assertEquals(JsonPrimitive(0), store.vars["n"])
    }

    @Test
    fun increment_missing_variable_starts_at_zero() {
        val store = VariableStore()
        store.increment("n", by = 3, min = null, max = null)
        assertEquals(JsonPrimitive(3), store.vars["n"])
    }

    @Test
    fun increment_non_integer_starts_at_zero() {
        val store = VariableStore()
        store.seed(mapOf("n" to JsonPrimitive("hola")))
        store.increment("n", by = 4, min = null, max = null)
        assertEquals(JsonPrimitive(4), store.vars["n"])
    }
}
