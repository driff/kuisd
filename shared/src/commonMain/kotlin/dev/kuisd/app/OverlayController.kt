package dev.kuisd.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog

/** Overlay activo del host (spec 011): un único `Dialog` o `Sheet`, nunca ambos (HU-1.6/2.5). */
internal sealed interface ActiveOverlay {
    data class Dialog(
        val spec: ShowDialog,
    ) : ActiveOverlay

    data class Sheet(
        val spec: ShowBottomSheet,
    ) : ActiveOverlay
}

/**
 * Estado reactivo del overlay activo de la pantalla actual (spec 011). Vive en la app, no en el motor.
 * Un único overlay a la vez por construcción ([ActiveOverlay] sellado): mostrar diálogo u hoja sustituye
 * al activo. Se crea dentro del bloque `key(current.id)` del host, de modo que navegar descarta el
 * overlay (reset-on-nav, HU-4.3).
 */
@Stable
internal class OverlayController {
    var active by mutableStateOf<ActiveOverlay?>(null)
        private set

    /** Muestra el diálogo [a] como overlay activo (sustituye a cualquier hoja). */
    fun showDialog(a: ShowDialog) {
        active = ActiveOverlay.Dialog(a)
    }

    /** Muestra la hoja [a] como overlay activo (sustituye a cualquier diálogo). */
    fun showSheet(a: ShowBottomSheet) {
        active = ActiveOverlay.Sheet(a)
    }

    /** Cierra el overlay activo (diálogo u hoja). */
    fun dismissAll() {
        active = null
    }
}
