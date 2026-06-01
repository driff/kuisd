package dev.kuisd.app

import dev.kuisd.sdui.SduiActionHandler
import dev.kuisd.sdui.core.UiAction

/**
 * Sub-handler de aplicación: contrato interno que extiende [SduiActionHandler] (motor) con un
 * predicado de soporte. Permite al [AppActionHandler] compuesto despachar **sin doble log**:
 * los sub-handlers ignoran en silencio las acciones que no soportan, y el compuesto loguea solo
 * las que ningún sub-handler aceptó.
 */
internal interface SubHandler : SduiActionHandler {
    fun supports(action: UiAction): Boolean
}

/**
 * Handler compuesto que el `SduiHost` provee por `LocalSduiActionHandler` (HU-4.1). Recorre las
 * acciones y delega cada una al primer sub-handler que la soporte; si ninguno la soporta, hace
 * no-op + log (HU-4.3). Preserva el orden de las acciones recibidas (HU-4.2).
 */
internal class AppActionHandler(
    private val children: List<SubHandler>,
) : SduiActionHandler {
    override fun handle(actions: List<UiAction>) {
        actions.forEach { action ->
            val matched = children.firstOrNull { it.supports(action) }
            if (matched != null) {
                matched.handle(listOf(action))
            } else {
                appLog("acción no soportada (no-op): ${action::class.simpleName}")
            }
        }
    }
}
