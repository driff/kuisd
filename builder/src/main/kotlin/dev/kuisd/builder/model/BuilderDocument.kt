package dev.kuisd.builder.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kuisd.builder.catalog.builderCatalog
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject

/**
 * Estado del documento del builder: un [SduiNode] mutable en estado de Compose más la selección
 * actual. Toda mutación delega en [TreeOps] (puro) y mantiene el invariante de árbol válido.
 */
@Stable
class BuilderDocument {
    var root: SduiNode by mutableStateOf(SduiNode(type = "column", id = "root"))
        private set

    var selectedId: String? by mutableStateOf("root")
        private set

    /** `type`s del catálogo que admiten hijos (contenedores). */
    private val containerTypes: Set<String> =
        builderCatalog.filter { it.acceptsChildren }.map { it.type }.toSet()

    fun select(id: String?) {
        selectedId = id
    }

    fun insert(template: SduiNode) {
        root = TreeOps.insert(root, selectedId, template, containerTypes)
    }

    fun delete(id: String) {
        root = TreeOps.delete(root, id)
        if (selectedId == id) selectedId = "root"
    }

    fun updateProps(id: String, props: JsonObject) {
        root = TreeOps.updateProps(root, id, props)
    }

    fun updateModifier(id: String, modifier: UiModifier) {
        root = TreeOps.updateModifier(root, id, modifier)
    }
}
