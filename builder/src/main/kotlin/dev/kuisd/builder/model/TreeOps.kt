package dev.kuisd.builder.model

import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject

/**
 * Operaciones PURAS sobre el árbol de [SduiNode] (sin Compose). Toda mutación es inmutable: se
 * reconstruye el camino afectado con `copy(...)` y se devuelve un nuevo árbol válido y serializable.
 */
internal object TreeOps {
    /** Busca en profundidad el primer nodo con [id]; `null` si no existe. */
    fun findById(root: SduiNode, id: String): SduiNode? =
        if (root.id == id) root else root.children.firstNotNullOfOrNull { findById(it, id) }

    /** Recolecta todos los ids no nulos del árbol. */
    fun collectIds(root: SduiNode): Set<String> {
        val acc = mutableSetOf<String>()

        fun visit(node: SduiNode) {
            node.id?.let { acc += it }
            node.children.forEach(::visit)
        }
        visit(root)
        return acc
    }

    /**
     * Garantiza un id único para [node]: si su id es null o colisiona con [existing], asigna
     * `"${type}-${n}"` con `n` = primer entero ≥1 cuyo id no esté en [existing] (determinista).
     */
    fun ensureId(node: SduiNode, existing: Set<String>): SduiNode {
        val current = node.id
        if (current != null && current !in existing) return node
        var n = 1
        while ("${node.type}-$n" in existing) n++
        return node.copy(id = "${node.type}-$n")
    }

    /**
     * Inserta [template] (con id único) como hijo de [parentId] si ese nodo existe y su `type` está
     * en [containerTypes]; en otro caso lo añade a los hijos de la raíz. Devuelve el nuevo árbol.
     */
    fun insert(
        root: SduiNode,
        parentId: String?,
        template: SduiNode,
        containerTypes: Set<String>,
    ): SduiNode {
        val node = ensureId(template, collectIds(root))
        val parent = parentId?.let { findById(root, it) }
        val targetId = if (parent != null && parent.type in containerTypes) parentId else root.id
        return if (targetId != null && targetId != root.id) {
            appendChild(root, targetId, node)
        } else {
            root.copy(children = root.children + node)
        }
    }

    /** Elimina el nodo con [id] y su subárbol; NUNCA borra la raíz. Inmutable. */
    fun delete(root: SduiNode, id: String): SduiNode {
        if (root.id == id) return root
        return root.copy(
            children = root.children
                .filterNot { it.id == id }
                .map { delete(it, id) },
        )
    }

    /** Reemplaza las `props` del nodo con [id]. */
    fun updateProps(root: SduiNode, id: String, props: JsonObject): SduiNode =
        transform(root, id) { it.copy(props = props) }

    /** Reemplaza el `modifier` del nodo con [id]. */
    fun updateModifier(root: SduiNode, id: String, modifier: UiModifier): SduiNode =
        transform(root, id) { it.copy(modifier = modifier) }

    /** Añade [child] a los hijos del nodo con [targetId], reconstruyendo el camino. */
    private fun appendChild(node: SduiNode, targetId: String, child: SduiNode): SduiNode =
        if (node.id == targetId) {
            node.copy(children = node.children + child)
        } else {
            node.copy(children = node.children.map { appendChild(it, targetId, child) })
        }

    /** Aplica [block] al nodo con [id] (si existe), reconstruyendo el camino. */
    private fun transform(node: SduiNode, id: String, block: (SduiNode) -> SduiNode): SduiNode =
        if (node.id == id) {
            block(node)
        } else {
            node.copy(children = node.children.map { transform(it, id, block) })
        }
}
