package dev.kuisd.app.variables

import dev.kuisd.app.AppActionHandler
import dev.kuisd.sdui.core.Increment
import dev.kuisd.sdui.core.SetVar
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Simula el flujo two-way del `textField` sin UI: el motor (en `:sdui-compose`) emitiría un
 * `SetVar(bind, JsonPrimitive(<newText>))` por cada keystroke; aquí lo invocamos directamente
 * sobre el `AppActionHandler` con un `VariableActionHandler` para validar que el ciclo cierra.
 */
class TextFieldTwoWayFlowTest {
    @Test
    fun set_var_string_updates_existing_int_variable() {
        val store = VariableStore().apply { seed(mapOf("count" to JsonPrimitive(0))) }
        val app = AppActionHandler(listOf(VariableActionHandler(store)))

        app.handle(listOf(SetVar("count", JsonPrimitive("7"))))

        assertEquals(JsonPrimitive("7"), store.vars["count"])
    }

    @Test
    fun set_var_creates_missing_variable() {
        val store = VariableStore()
        val app = AppActionHandler(listOf(VariableActionHandler(store)))

        app.handle(listOf(SetVar("name", JsonPrimitive("Ana"))))

        assertEquals(JsonPrimitive("Ana"), store.vars["name"])
    }

    @Test
    fun increment_after_string_set_var_coerces_via_int_or_null() {
        val store = VariableStore().apply { seed(mapOf("count" to JsonPrimitive(0))) }
        val app = AppActionHandler(listOf(VariableActionHandler(store)))

        app.handle(listOf(SetVar("count", JsonPrimitive("7"))))
        app.handle(listOf(Increment("count", by = 1, min = 0, max = 10)))

        assertEquals(JsonPrimitive(8), store.vars["count"])
    }

    @Test
    fun increment_after_non_numeric_set_var_starts_from_zero() {
        val store = VariableStore().apply { seed(mapOf("count" to JsonPrimitive(0))) }
        val app = AppActionHandler(listOf(VariableActionHandler(store)))

        app.handle(listOf(SetVar("count", JsonPrimitive("abc"))))
        app.handle(listOf(Increment("count", by = 1, min = 0, max = 10)))

        assertEquals(JsonPrimitive(1), store.vars["count"])
    }
}
