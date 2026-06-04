package dev.kuisd.builder.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kuisd.builder.catalog.builderCatalog
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.File

private const val DEFAULT_SCHEMA_VERSION = 1
private const val DEFAULT_SCREEN_ID = "builder"
private val emptyRoot: SduiNode get() = SduiNode(type = "column", id = "root")

/**
 * Estado del documento del builder: el árbol [SduiNode] mutable más la selección, ahora con **identidad
 * de archivo** ([currentFile]) y **estado de modificación** ([isModified]). Preserva los campos del
 * [SduiEnvelope] que el builder no edita (`schemaVersion`, `screenId`, `variables`, `meta`) para un
 * round-trip fiel (HU-5). Toda mutación delega en [TreeOps] (puro) y marca el documento como modificado.
 */
@Stable
class BuilderDocument {
    var root: SduiNode by mutableStateOf(emptyRoot)
        private set

    var selectedId: String? by mutableStateOf("root")
        private set

    var currentFile: File? by mutableStateOf(null)
        private set

    var isModified: Boolean by mutableStateOf(false)
        private set

    // Metadatos del envelope preservados (no editables en v1): se reproducen al guardar (HU-5).
    private var schemaVersion: Int = DEFAULT_SCHEMA_VERSION
    private var screenId: String = DEFAULT_SCREEN_ID
    private var variables: Map<String, JsonElement> = emptyMap()
    private var meta: Map<String, String> = emptyMap()

    /** `type`s del catálogo que admiten hijos (contenedores). */
    private val containerTypes: Set<String> =
        builderCatalog.filter { it.acceptsChildren }.map { it.type }.toSet()

    /** Reconstruye el [SduiEnvelope] del documento (árbol actual + metadatos preservados). */
    fun toEnvelope(): SduiEnvelope =
        SduiEnvelope(
            schemaVersion = schemaVersion,
            screenId = screenId,
            root = root,
            variables = variables,
            meta = meta,
        )

    /**
     * Reemplaza el documento por [envelope]: garantiza ids únicos en todo el árbol (invariante del
     * builder), preserva sus metadatos, asocia [file] como archivo actual, selecciona la raíz y marca
     * el documento como NO modificado (HU-2.2/HU-2.3/HU-2.4).
     */
    fun load(envelope: SduiEnvelope, file: File?) {
        root = TreeOps.ensureUniqueTree(envelope.root, mutableSetOf())
        schemaVersion = envelope.schemaVersion
        screenId = envelope.screenId
        variables = envelope.variables
        meta = envelope.meta
        selectedId = root.id
        currentFile = file
        isModified = false
    }

    /** Documento en blanco (columna raíz `root`), sin archivo, no modificado, metadatos por defecto (HU-3.1). */
    fun newDocument() {
        root = emptyRoot
        schemaVersion = DEFAULT_SCHEMA_VERSION
        screenId = DEFAULT_SCREEN_ID
        variables = emptyMap()
        meta = emptyMap()
        selectedId = root.id
        currentFile = null
        isModified = false
    }

    /** Tras guardar OK: fija [currentFile] y limpia [isModified] (HU-4.2). */
    fun markSaved(file: File) {
        currentFile = file
        isModified = false
    }

    fun select(id: String?) {
        selectedId = id
    }

    fun insert(template: SduiNode) {
        root = TreeOps.insert(root, selectedId, template, containerTypes)
        isModified = true
    }

    fun delete(id: String) {
        root = TreeOps.delete(root, id)
        // Resetea la selección si el nodo seleccionado dejó de existir (p.ej. al borrar un ancestro).
        // Usa root.id (no el literal "root"): la raíz cargada de un archivo puede tener otro id.
        val current = selectedId
        if (current == null || TreeOps.findById(root, current) == null) selectedId = root.id
        isModified = true
    }

    fun updateProps(id: String, props: JsonObject) {
        root = TreeOps.updateProps(root, id, props)
        isModified = true
    }

    fun updateModifier(id: String, modifier: UiModifier) {
        root = TreeOps.updateModifier(root, id, modifier)
        isModified = true
    }
}
