package dev.kuisd.app.nav

import dev.kuisd.app.SubHandler
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.UiAction

/**
 * Handler de navegación (HU-4.1): `Navigate` apila, `NavigateBack` desapila. Las acciones que no
 * soporta las ignora **en silencio** — el `AppActionHandler` compuesto loguea las que ningún
 * sub-handler acepte (sin doble log).
 */
internal class NavActionHandler(
    private val backStack: NavBackStack,
) : SubHandler {
    override fun supports(action: UiAction): Boolean =
        action is Navigate || action === NavigateBack

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        when (action) {
            is Navigate -> backStack.push(action.route, action.args)
            NavigateBack -> backStack.pop()
            else -> { /* no soportado: silencio */ }
        }
    }
}
