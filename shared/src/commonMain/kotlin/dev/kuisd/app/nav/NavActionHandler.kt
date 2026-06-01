package dev.kuisd.app.nav

import dev.kuisd.app.appLog
import dev.kuisd.sdui.SduiActionHandler
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.UiAction

/**
 * Handler de navegación: `Navigate` apila, `NavigateBack` desapila; el resto de acciones
 * son no-op con log (HU-4.1).
 */
internal class NavActionHandler(
    private val backStack: NavBackStack,
) : SduiActionHandler {
    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        when (action) {
            is Navigate -> backStack.push(action.route, action.args)
            NavigateBack -> backStack.pop()
            else -> appLog("acción no soportada en spec 003 (no-op): ${action::class.simpleName}")
        }
    }
}
