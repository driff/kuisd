package dev.kuisd.sdui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.UiAction

/** Interpreta una lista de [UiAction] emitida por un nodo SDUI (HU-1.1). */
fun interface ActionDispatcher {
    fun dispatch(actions: List<UiAction>)
}

/**
 * Dispatcher de navegación: `Navigate` apila, `NavigateBack` desapila; el resto de acciones
 * son no-op con log (HU-1, HU-2.1, HU-3.1).
 */
class NavigationActionDispatcher(
    private val backStack: NavBackStack,
) : ActionDispatcher {
    override fun dispatch(actions: List<UiAction>) {
        actions.forEach { action ->
            when (action) {
                is Navigate -> backStack.push(NavEntry(action.route, action.args))
                NavigateBack -> backStack.pop()
                else -> sduiLog("acción no soportada en spec 003 (no-op): ${action::class.simpleName}")
            }
        }
    }
}

/** Dispatcher inyectado por [SduiHost]; default no-op para usar `RenderNode` fuera de un host. */
val LocalActionDispatcher: ProvidableCompositionLocal<ActionDispatcher> =
    staticCompositionLocalOf { ActionDispatcher { } }
