package dev.kuisd.app

import dev.kuisd.sdui.core.Track
import dev.kuisd.sdui.core.UiAction

/**
 * Seam de analytics mínimo (spec 009): registra las acciones [Track] vía [appLog]. Elimina la deuda
 * de "acción muerta" (antes `Track` caía a no-soportada en silencio). Una app real sustituiría el
 * log por su SDK de analytics aquí.
 */
internal class TrackActionHandler : SubHandler {
    override fun supports(action: UiAction): Boolean = action is Track

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        if (action is Track) appLog("track: ${action.event} ${action.props}")
    }
}
