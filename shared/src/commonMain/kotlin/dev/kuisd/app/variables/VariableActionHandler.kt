package dev.kuisd.app.variables

import dev.kuisd.app.SubHandler
import dev.kuisd.sdui.core.Increment
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.Toggle
import dev.kuisd.sdui.core.UiAction

/**
 * Interpreta las acciones de variables (HU-3.2/3.3/3.4) sobre un [VariableStore]. Las acciones que
 * no soporta las ignora **en silencio**: el `AppActionHandler` compuesto decide qué loguear.
 */
internal class VariableActionHandler(
    private val store: VariableStore,
) : SubHandler {
    override fun supports(action: UiAction): Boolean =
        action is SetVar || action is Toggle || action is Increment

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        when (action) {
            is SetVar -> store.set(action.name, action.value)
            is Toggle -> store.toggle(action.name)
            is Increment -> store.increment(action.name, action.by, action.min, action.max)
            else -> { /* no soportado: silencio */ }
        }
    }
}
