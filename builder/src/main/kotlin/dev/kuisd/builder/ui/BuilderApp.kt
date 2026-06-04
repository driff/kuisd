package dev.kuisd.builder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.app.SduiPreviewEnvironment
import dev.kuisd.builder.catalog.isMainContainer
import dev.kuisd.builder.export.encodeToJson
import dev.kuisd.builder.io.BuilderFileStore
import dev.kuisd.builder.model.BuilderDocument
import dev.kuisd.builder.model.SlotOps
import dev.kuisd.builder.model.TreeOps
import dev.kuisd.sdui.RenderNode
import dev.kuisd.sdui.core.SduiEnvelope
import java.io.File

private const val PREVIEW_WEIGHT = 0.55f
private const val PANEL_WEIGHT = 0.45f
private val EXPORT_PANEL_MAX_HEIGHT = 220.dp

/** Mensaje de error mostrado en un diálogo, con una acción de arreglo opcional ([fix]: label → acción). */
data class ErrorMsg(
    val text: String,
    val fix: Pair<String, () -> Unit>? = null,
)

/** App del builder: toolbar de archivo + preview (izquierda) + paleta/outline/inspector/export (derecha). */
@Composable
internal fun BuilderApp() {
    val document = remember { BuilderDocument() }
    val handler = remember { LoggingActionHandler() }
    // Acción pendiente de confirmar (descarte de cambios) y mensaje de error de E/S, ambos efímeros.
    var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) }
    var errorMessage: ErrorMsg? by remember { mutableStateOf(null) }

    /** Ejecuta [action] directamente, o la aplaza tras confirmación si hay cambios sin guardar. */
    fun guardDiscard(action: () -> Unit) {
        if (document.isModified) pendingAction = action else action()
    }

    fun openFlow() {
        val file = openFileDialog() ?: return // cancelar: no altera el documento (HU-2.6)
        BuilderFileStore.read(file)
            .onSuccess { env -> resolveOpenedEnvelope(env, file, document)?.let { errorMessage = it } }
            .onFailure { errorMessage = ioErrorMessage("abrir", file, it) }
    }

    fun saveTo(chosen: File?) {
        val file = chosen ?: return // cancelar (HU-1.6)
        // write normaliza la extensión y devuelve el File realmente escrito → markSaved con esa ruta.
        BuilderFileStore.write(file, document.toEnvelope())
            .onSuccess { document.markSaved(it) }
            .onFailure { errorMessage = ioErrorMessage("guardar", file, it) }
    }

    fun save() = saveTo(document.currentFile ?: saveFileDialog(suggestedName(document))) // HU-1.4

    fun saveAs() = saveTo(saveFileDialog(suggestedName(document)))

    MaterialTheme {
        Surface {
            Column(Modifier.fillMaxSize()) {
                Toolbar(
                    document = document,
                    onNuevo = { guardDiscard(document::newDocument) },
                    onAbrir = { guardDiscard(::openFlow) },
                    onGuardar = ::save,
                    onGuardarComo = ::saveAs,
                )
                HorizontalDivider()
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(PREVIEW_WEIGHT).fillMaxHeight().padding(12.dp)) {
                        SduiPreviewEnvironment(handler) { RenderNode(document.root) }
                    }
                    VerticalDivider()
                    RightColumn(
                        document = document,
                        onError = { errorMessage = it },
                        modifier = Modifier.weight(PANEL_WEIGHT).fillMaxHeight(),
                    )
                }
            }
        }
    }

    pendingAction?.let { action ->
        ConfirmDiscardDialog(
            onConfirm = {
                pendingAction = null
                action()
            },
            onDismiss = { pendingAction = null },
        )
    }
    errorMessage?.let { message ->
        ErrorDialog(message = message, onDismiss = { errorMessage = null })
    }
}

/**
 * Aplica la regla de raíz a un envelope cargado (HU-4): si la raíz es contenedor principal lo carga y
 * devuelve `null`; si no, devuelve el [ErrorMsg] a mostrar (con acción de arreglo salvo que haya un
 * contenedor principal anidado, en cuyo caso no se puede envolver).
 */
private fun resolveOpenedEnvelope(env: SduiEnvelope, file: File, document: BuilderDocument): ErrorMsg? = when {
    isMainContainer(env.root.type) -> { // 4.1
        document.load(env, file)
        null
    }
    SlotOps.containsMainContainer(env.root) -> ErrorMsg( // 4.4
        "La raíz no es un contenedor principal y contiene un scaffold anidado: no se puede arreglar.",
    )
    else -> ErrorMsg( // 4.2 + 4.3
        text = "La raíz debe ser un contenedor principal (p. ej. un Scaffold).",
        fix = "Arreglar (envolver en scaffold)" to { document.loadWrapped(env, file) },
    )
}

/** Nombre propuesto al guardar: el del archivo actual o uno por defecto. */
private fun suggestedName(document: BuilderDocument): String = document.currentFile?.name ?: "blueprint.json"

/** Mensaje legible para un fallo de E/S al [action] («abrir»/«guardar») el archivo [file]. */
private fun ioErrorMessage(action: String, file: File, cause: Throwable): ErrorMsg =
    ErrorMsg(text = "No se pudo $action «${file.name}»: ${cause.message ?: cause::class.simpleName}")

@Composable
private fun Toolbar(
    document: BuilderDocument,
    onNuevo: () -> Unit,
    onAbrir: () -> Unit,
    onGuardar: () -> Unit,
    onGuardarComo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onNuevo) { Text("Nuevo") }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = onAbrir) { Text("Abrir") }
        Spacer(Modifier.width(6.dp))
        Button(onClick = onGuardar) { Text("Guardar") }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = onGuardarComo) { Text("Guardar como…") }
        Spacer(Modifier.width(12.dp))
        val name = document.currentFile?.name ?: "(sin guardar)"
        val marker = if (document.isModified) " •" else ""
        Text(
            text = "$name$marker",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConfirmDiscardDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descartar cambios") },
        text = { Text("Hay cambios sin guardar. ¿Descartarlos?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ErrorDialog(message: ErrorMsg, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message.text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Aceptar") } },
        dismissButton = message.fix?.let { (label, action) ->
            {
                TextButton(
                    onClick = {
                        action()
                        onDismiss()
                    },
                ) {
                    Text(label)
                }
            }
        },
    )
}

@Composable
private fun RightColumn(document: BuilderDocument, onError: (ErrorMsg) -> Unit, modifier: Modifier) {
    var exported by remember { mutableStateOf<String?>(null) }
    Column(modifier) {
        SectionTitle("Paleta")
        PalettePane(
            onAdd = {
                document.insert(it.template)
                document.lastError?.let { msg ->
                    onError(ErrorMsg(msg))
                    document.clearError()
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        SectionTitle("Outline")
        OutlinePane(
            root = document.root,
            selectedId = document.selectedId,
            selectedSlotId = document.selectedSlotId,
            onSelect = document::select,
            onSelectSlot = document::selectSlot,
            onDelete = document::delete,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        SectionTitle("Inspector")
        val selected = document.selectedId?.let { TreeOps.findById(document.root, it) }
        InspectorPane(
            node = selected,
            onProps = { props -> document.selectedId?.let { document.updateProps(it, props) } },
            onModifier = { mod -> document.selectedId?.let { document.updateModifier(it, mod) } },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        Button(
            onClick = { exported = document.toEnvelope().encodeToJson() },
            modifier = Modifier.padding(8.dp),
        ) {
            Text("Exportar JSON")
        }
        exported?.let { json ->
            Box(
                Modifier.heightIn(max = EXPORT_PANEL_MAX_HEIGHT).fillMaxWidth()
                    .padding(8.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(json, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
