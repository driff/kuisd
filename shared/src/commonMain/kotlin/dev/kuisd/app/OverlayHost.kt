package dev.kuisd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.kuisd.sdui.RenderNode
import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog
import dev.kuisd.sdui.core.UiAction

/**
 * Render del overlay activo del host (spec 011): snackbar, diálogo y hoja modal por encima de la
 * pantalla. Se invoca DENTRO del `CompositionLocalProvider` de `SduiHost` para que `RenderNode` del
 * contenido de la hoja herede registry/variables/tema/handler de la entrada actual. Los `onConfirm`/
 * `onDismiss`/`onAction` se despachan por [dispatch] (= `handler::handle`) tras cerrar el overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OverlayHost(
    overlay: OverlayController,
    snackbar: SnackbarHostState,
    dispatch: (List<UiAction>) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    // `AlertDialog`/`ModalBottomSheet` son ventanas Material (edge-to-edge), por eso el padding del host
    // no las afecta; el `SnackbarHost` sí, por eso `OverlayHost` se monta fuera del `Box(padding)`.
    when (val active = overlay.active) {
        is ActiveOverlay.Dialog -> DialogOverlay(active.spec, overlay, dispatch)
        is ActiveOverlay.Sheet -> SheetOverlay(active.spec, overlay, dispatch)
        null -> Unit
    }
}

/**
 * Diálogo de confirmación. Nota: un `ShowDialog` sin `confirmLabel` ni `dismissLabel` se muestra sin
 * botones (solo se cierra por scrim/back → `onDismiss`); si el server declara `onConfirm` debe
 * acompañarlo de `confirmLabel`. No se inyecta un label por defecto para no materializar texto sin i18n.
 */
@Composable
private fun DialogOverlay(d: ShowDialog, overlay: OverlayController, dispatch: (List<UiAction>) -> Unit) {
    AlertDialog(
        onDismissRequest = {
            overlay.dismissAll()
            dispatch(d.onDismiss)
        },
        title = d.title.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        text = d.text.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        confirmButton = {
            d.confirmLabel?.let { l ->
                TextButton({
                    overlay.dismissAll()
                    dispatch(d.onConfirm)
                }) { Text(l) }
            }
        },
        dismissButton = d.dismissLabel?.let { l ->
            {
                TextButton({
                    overlay.dismissAll()
                    dispatch(d.onDismiss)
                }) { Text(l) }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetOverlay(s: ShowBottomSheet, overlay: OverlayController, dispatch: (List<UiAction>) -> Unit) {
    ModalBottomSheet(onDismissRequest = {
        overlay.dismissAll()
        dispatch(s.onDismiss)
    }) {
        s.content.forEach { RenderNode(it) }
    }
}
