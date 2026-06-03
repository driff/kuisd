package dev.kuisd.app

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import dev.kuisd.sdui.VariableScope
import dev.kuisd.sdui.core.DismissOverlay
import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog
import dev.kuisd.sdui.core.ShowSnackbar
import dev.kuisd.sdui.core.UiAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Texto del snackbar: messageVar (resuelto del store) o el literal message (HU-3.4). */
internal fun resolveSnackbarMessage(a: ShowSnackbar, scope: VariableScope): String =
    a.messageVar?.removePrefix("$")?.let { scope.get(it)?.asPlainString() } ?: a.message

/** Stringify seguro de un JsonElement sin depender del `asDisplayString` interno del motor. */
internal fun JsonElement.asPlainString(): String = (this as? JsonPrimitive)?.content ?: toString()

/** Mapea el string de duración del contrato al enum de Material (default Short). */
internal fun String.toSnackbarDuration(): SnackbarDuration = when (this) {
    "long" -> SnackbarDuration.Long
    "indefinite" -> SnackbarDuration.Indefinite
    else -> SnackbarDuration.Short
}

/**
 * Sub-handler de overlays (spec 011, patrón 009): traduce las acciones de overlay en mutaciones del
 * [OverlayController] y dispara el snackbar en [scope]. El estado y el render viven en la app; el
 * motor solo despacha la acción por `LocalSduiActionHandler`.
 */
internal class OverlayActionHandler(
    private val overlay: OverlayController,
    private val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
    private val vars: VariableScope,
) : SubHandler {
    /**
     * Re-despacho del `onAction` del snackbar (su resultado llega asíncrono). Lo inyecta el host TRAS
     * construir el `AppActionHandler`, rompiendo el ciclo handler<->compuesto. Default no-op: el
     * handler es usable y testeable sin host. `internal set` para no exponer la mutación fuera del módulo.
     */
    var dispatch: (List<UiAction>) -> Unit = {}
        internal set

    override fun supports(action: UiAction): Boolean =
        action is ShowDialog || action is ShowBottomSheet || action is ShowSnackbar || action is DismissOverlay

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        when (action) {
            is ShowDialog -> overlay.showDialog(action)
            is ShowBottomSheet -> overlay.showSheet(action)
            is DismissOverlay -> overlay.dismissAll()
            is ShowSnackbar -> scope.launch {
                val result = snackbar.showSnackbar(
                    message = resolveSnackbarMessage(action, vars),
                    actionLabel = action.actionLabel,
                    duration = action.duration.toSnackbarDuration(),
                )
                if (result == SnackbarResult.ActionPerformed) dispatch(action.onAction)
            }
            else -> Unit
        }
    }
}
