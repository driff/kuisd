package dev.kuisd.builder.ui

import androidx.compose.runtime.mutableStateListOf
import dev.kuisd.sdui.SduiActionHandler
import dev.kuisd.sdui.core.UiAction

/**
 * Handler del preview (spec 014): NO navega ni hace red — acumula las acciones despachadas en un log
 * observable para mostrarlas en un panel. El preview del builder es una vista de diseño.
 */
class LoggingActionHandler : SduiActionHandler {
    val log = mutableStateListOf<String>()

    override fun handle(actions: List<UiAction>) {
        actions.forEach { log.add(it::class.simpleName ?: "UiAction") }
    }
}
