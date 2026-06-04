package dev.kuisd.builder.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kuisd.builder.catalog.MainContainerSpec
import dev.kuisd.builder.catalog.builderCatalog
import dev.kuisd.builder.catalog.isMainContainer
import dev.kuisd.builder.catalog.mainContainers
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.File

private const val DEFAULT_SCHEMA_VERSION = 1
private const val DEFAULT_SCREEN_ID = "builder"
private val emptyRoot: SduiNode get() = SduiNode(type = "scaffold", id = "root")

/**
 * Estado del documento del builder: el árbol [SduiNode] mutable más la selección, ahora con **identidad
 * de archivo** ([currentFile]) y **estado de modificación** ([isModified]). Preserva los campos del
 * [SduiEnvelope] que el builder no edita (`schemaVersion`, `screenId`, `variables`, `meta`) para un
 * round-trip fiel (HU-5). Toda mutación delega en [TreeOps]/[SlotOps] (puros) y marca el documento como
 * modificado.
 */
@Stable
@Suppress("TooManyFunctions") // State-holder cohesivo: muchas operaciones pequeñas sobre un único documento.
class BuilderDocument {
    var root: SduiNode by mutableStateOf(emptyRoot)
        private set

    var selectedId: String? by mutableStateOf("root")
        private set

    /** Slot seleccionado (topBar/content/bottomBar) cuando la raíz es contenedor principal; null si no. */
    var selectedSlotId: String? by mutableStateOf(null)
        private set

    var currentFile: File? by mutableStateOf(null)
        private set

    var isModified: Boolean by mutableStateOf(false)
        private set

    /** Mensaje de la última acción rechazada; lo consume la UI y luego llama [clearError]. */
    var lastError: String? by mutableStateOf(null)
        private set

    // Metadatos del envelope preservados (no editables en v1): se reproducen al guardar (HU-5).
    private var schemaVersion: Int = DEFAULT_SCHEMA_VERSION
    private var screenId: String = DEFAULT_SCREEN_ID
    private var variables: Map<String, JsonElement> = emptyMap()
    private var meta: Map<String, String> = emptyMap()

    /** `type`s del catálogo que admiten hijos (contenedores). */
    private val containerTypes: Set<String> =
        builderCatalog.filter { it.acceptsChildren }.map { it.type }.toSet()

    /** Spec del contenedor principal de la raíz actual, o null si la raíz no es contenedor principal. */
    private val rootSpec: MainContainerSpec? get() = mainContainers[root.type]

    /** Tipo del contenedor padre del nodo [id] (null si es la raíz o no existe). Para el inspector (spec 019). */
    fun parentType(id: String?): String? = id?.let { TreeOps.findParent(root, it)?.type }

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
     * builder), normaliza los slots si la raíz es contenedor principal (HU-4.1), preserva los metadatos,
     * asocia [file] como archivo actual, selecciona la raíz y marca el documento como NO modificado.
     */
    fun load(envelope: SduiEnvelope, file: File?) {
        var loaded = TreeOps.ensureUniqueTree(envelope.root, mutableSetOf())
        mainContainers[loaded.type]?.let { loaded = SlotOps.normalize(loaded, it) }
        root = loaded
        schemaVersion = envelope.schemaVersion
        screenId = envelope.screenId
        variables = envelope.variables
        meta = envelope.meta
        selectedId = root.id
        selectedSlotId = null
        currentFile = file
        isModified = false
        lastError = null
    }

    /**
     * Envuelve el árbol de [envelope] como ÚNICO content de un `scaffold` nuevo (HU-4.3). Precondición:
     * el árbol NO contiene contenedores principales (lo valida el llamador con [SlotOps.containsMainContainer]).
     * Preserva los metadatos del envelope, marca el documento como modificado (el envoltorio difiere del
     * original) y selecciona la raíz.
     */
    fun loadWrapped(envelope: SduiEnvelope, file: File?) {
        val wrapped = SduiNode(type = "scaffold", id = "root", children = listOf(envelope.root))
        root = TreeOps.ensureUniqueTree(wrapped, mutableSetOf())
        schemaVersion = envelope.schemaVersion
        screenId = envelope.screenId
        variables = envelope.variables
        meta = envelope.meta
        selectedId = root.id
        selectedSlotId = null
        currentFile = file
        isModified = true
        lastError = null
    }

    /** Documento en blanco (raíz `scaffold` `root`), sin archivo, no modificado, metadatos por defecto (HU-3.1). */
    fun newDocument() {
        root = emptyRoot
        schemaVersion = DEFAULT_SCHEMA_VERSION
        screenId = DEFAULT_SCREEN_ID
        variables = emptyMap()
        meta = emptyMap()
        selectedId = root.id
        selectedSlotId = null
        currentFile = null
        isModified = false
        lastError = null
    }

    /** Tras guardar OK: fija [currentFile] y limpia [isModified] (HU-4.2). */
    fun markSaved(file: File) {
        currentFile = file
        isModified = false
    }

    /** Selecciona el nodo [id] (deselecciona cualquier slot). */
    fun select(id: String?) {
        selectedId = id
        selectedSlotId = null
    }

    /** Selecciona el slot [slotId] de la raíz (deselecciona cualquier nodo). */
    fun selectSlot(slotId: String) {
        selectedSlotId = slotId
        selectedId = null
    }

    /** Limpia el último error tras consumirlo la UI. */
    fun clearError() {
        lastError = null
    }

    /**
     * Inserta [template] enrutando según el tipo y el estado de selección (HU-2.5/HU-3.3/3.4/3.5):
     * - Un contenedor principal solo puede ir en la raíz: se rechaza con [lastError].
     * - Con un slot único seleccionado: se exige que el `type` esté en sus `childTypes`; si no, se rechaza.
     * - Un type reservado por un slot único se auto-rutea a su slot aunque no esté seleccionado.
     * - En otro caso (content) delega en [TreeOps.insert].
     */
    fun insert(template: SduiNode) {
        val type = template.type
        val spec = rootSpec
        val selectedSlot = spec?.slots?.firstOrNull { !it.multiple && it.id == selectedSlotId }
        when {
            isMainContainer(type) ->
                lastError = "Un $type solo puede ir en la raíz." // HU-2.5
            selectedSlot != null && type !in selectedSlot.childTypes ->
                lastError = "En «${selectedSlot.label}» solo va ${selectedSlot.childTypes.joinToString("/")}."
            selectedSlot != null && spec != null -> { // slot único seleccionado y type acorde (HU-3.3/3.4)
                root = SlotOps.setSingleSlot(root, spec, selectedSlot, uniquify(template))
                isModified = true
            }
            spec != null && type in spec.reservedChildTypes -> { // auto-rutea a su slot único
                root = SlotOps.setSingleSlot(root, spec, spec.slotForChildType(type), uniquify(template))
                isModified = true
            }
            else -> { // content (HU-3.5): con slot seleccionado, selectedId es null ⇒ cae a la raíz=content.
                root = TreeOps.insert(root, selectedId, template, containerTypes)
                isModified = true
            }
        }
    }

    fun delete(id: String) {
        root = TreeOps.delete(root, id)
        // Resetea la selección si el nodo seleccionado dejó de existir (p.ej. al borrar un ancestro).
        // Usa root.id (no el literal "root"): la raíz cargada de un archivo puede tener otro id.
        val current = selectedId
        if (current == null || TreeOps.findById(root, current) == null) selectedId = root.id
        selectedSlotId = null
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

    /** Asigna ids únicos a [node] y su subárbol contra los ids ya presentes en el documento. */
    private fun uniquify(node: SduiNode): SduiNode =
        TreeOps.ensureUniqueTree(node, TreeOps.collectIds(root).toMutableSet())
}
