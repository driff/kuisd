package dev.kuisd.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog

/**
 * Estado reactivo del overlay activo de la pantalla actual (spec 011). Vive en la app, no en el motor.
 * Un único overlay a la vez (HU-1.6/2.5): mostrar diálogo limpia la hoja y viceversa. Se crea dentro
 * del bloque `key(current.id)` del host, de modo que navegar descarta el overlay (reset-on-nav, HU-4.3).
 */
@Stable
internal class OverlayController {
    var dialog by mutableStateOf<ShowDialog?>(null)
        private set
    var sheet by mutableStateOf<ShowBottomSheet?>(null)
        private set

    /** Muestra el diálogo [a] cerrando cualquier hoja activa. */
    fun showDialog(a: ShowDialog) {
        sheet = null
        dialog = a
    }

    /** Muestra la hoja [a] cerrando cualquier diálogo activo. */
    fun showSheet(a: ShowBottomSheet) {
        dialog = null
        sheet = a
    }

    /** Cierra el overlay activo (diálogo y hoja). */
    fun dismissAll() {
        dialog = null
        sheet = null
    }
}
