package dev.kuisd.app.variables

import dev.kuisd.sdui.core.Increment
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.Toggle
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VariableActionHandlerTest {
    @Test
    fun set_var_mutates_the_store() {
        val store = VariableStore()
        val handler = VariableActionHandler(store)

        handler.handle(listOf(SetVar(name = "x", value = JsonPrimitive(42))))

        assertEquals(JsonPrimitive(42), store.vars["x"])
    }

    @Test
    fun toggle_mutates_the_store() {
        val store = VariableStore()
        val handler = VariableActionHandler(store)
        store.seed(mapOf("flag" to JsonPrimitive(false)))

        handler.handle(listOf(Toggle(name = "flag")))

        assertEquals(JsonPrimitive(true), store.vars["flag"])
    }

    @Test
    fun increment_mutates_the_store_with_clamp() {
        val store = VariableStore()
        val handler = VariableActionHandler(store)
        store.seed(mapOf("n" to JsonPrimitive(9)))

        handler.handle(listOf(Increment(name = "n", by = 5, min = 0, max = 10)))

        assertEquals(JsonPrimitive(10), store.vars["n"])
    }

    @Test
    fun unsupported_action_does_not_alter_the_store() {
        val store = VariableStore()
        val handler = VariableActionHandler(store)
        store.seed(mapOf("n" to JsonPrimitive(5)))

        handler.handle(listOf(Navigate(route = "x")))

        assertEquals(JsonPrimitive(5), store.vars["n"])
    }

    @Test
    fun supports_only_variable_actions() {
        val store = VariableStore()
        val handler = VariableActionHandler(store)

        assertTrue(handler.supports(SetVar("x", JsonPrimitive(1))))
        assertTrue(handler.supports(Toggle("f")))
        assertTrue(handler.supports(Increment("n")))
        assertFalse(handler.supports(Navigate(route = "x")))
    }
}
