package dev.kuisd.app.variables

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class VariableStoreTest {
    @Test
    fun set_fixes_value_for_new_key() {
        val store = VariableStore()
        store.set("x", JsonPrimitive(7))
        assertEquals(JsonPrimitive(7), store.vars["x"])
    }

    @Test
    fun seed_preserves_existing_values() {
        val store = VariableStore()
        store.set("a", JsonPrimitive(1))
        store.seed(mapOf("a" to JsonPrimitive(99), "b" to JsonPrimitive(2)))
        // 'a' ya existía -> se preserva; 'b' es nueva -> se añade.
        assertEquals(JsonPrimitive(1), store.vars["a"])
        assertEquals(JsonPrimitive(2), store.vars["b"])
    }

    @Test
    fun seed_on_empty_store_adds_all_keys() {
        val store = VariableStore()
        store.seed(mapOf("a" to JsonPrimitive(1), "b" to JsonPrimitive(2)))
        assertEquals(JsonPrimitive(1), store.vars["a"])
        assertEquals(JsonPrimitive(2), store.vars["b"])
    }

    @Test
    fun seed_preserves_multiple_existing_and_adds_multiple_new_keys() {
        // Varias claves preexistentes + varias nuevas en el mismo seed: todas las preexistentes
        // se preservan, todas las nuevas se añaden. Asegura que el merge no falla con n>1.
        val store = VariableStore()
        store.set("a", JsonPrimitive(1))
        store.set("b", JsonPrimitive(2))
        store.seed(
            mapOf(
                "a" to JsonPrimitive(99),
                "b" to JsonPrimitive(88),
                "c" to JsonPrimitive(3),
                "d" to JsonPrimitive(4),
            ),
        )
        assertEquals(JsonPrimitive(1), store.vars["a"])
        assertEquals(JsonPrimitive(2), store.vars["b"])
        assertEquals(JsonPrimitive(3), store.vars["c"])
        assertEquals(JsonPrimitive(4), store.vars["d"])
    }

    @Test
    fun seed_with_no_new_keys_does_not_reassign_vars() {
        // Cubre el camino `if (changed) vars = merged.toMap()` del seed: cuando el envelope
        // re-emite solo claves ya presentes, `vars` debe quedar como referencia idéntica (sin
        // recomposiciones espurias en Compose).
        val store = VariableStore()
        store.set("a", JsonPrimitive(1))
        store.set("b", JsonPrimitive(2))
        val before = store.vars
        store.seed(mapOf("a" to JsonPrimitive(99), "b" to JsonPrimitive(88)))
        assertSame(before, store.vars, "seed sin claves nuevas no debería reasignar vars")
    }

    @Test
    fun re_seed_with_typed_user_edit_does_not_overwrite() {
        // Simula: envelope sembró count=0, usuario tecleó "7" (SetVar -> "7"), envelope se
        // re-emite (LaunchedEffect dispara seed otra vez) -> el "7" del usuario se preserva.
        val store = VariableStore()
        store.seed(mapOf("count" to JsonPrimitive(0)))
        store.set("count", JsonPrimitive("7"))
        store.seed(mapOf("count" to JsonPrimitive(0)))
        assertEquals(JsonPrimitive("7"), store.vars["count"])
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
