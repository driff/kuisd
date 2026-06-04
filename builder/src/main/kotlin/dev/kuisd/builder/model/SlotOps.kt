package dev.kuisd.builder.model

import dev.kuisd.builder.catalog.MainContainerSpec
import dev.kuisd.builder.catalog.SlotSpec
import dev.kuisd.builder.catalog.isMainContainer
import dev.kuisd.sdui.core.SduiNode

/**
 * Operaciones PURAS (sin Compose) que tratan los `children` planos de un contenedor principal como
 * "slots" (topBar, content, bottomBar). El criterio de partición ESPEJA al del motor: para cada slot
 * único, el PRIMER child cuyo `type` esté en sus `childTypes` gana; el resto de children no reservados
 * van a content, en orden de aparición.
 */
internal object SlotOps {
    /**
     * Proyecta los `children` de [node] sobre los slots de [spec]: devuelve un mapa `slotId → nodos`
     * con una entrada por CADA slot del spec (lista vacía si no hay nodos).
     *
     * - Slot único (`multiple == false`, `childTypes` no vacío): a lo sumo el PRIMER child cuyo `type`
     *   esté en `childTypes` (lista de 0 o 1 elemento).
     * - Slot content (`childTypes` vacío): TODO lo que ningún slot único tomó, en orden de aparición.
     *
     * Un 2.º child de un type reservado (p. ej. un segundo `topAppBar`) no entra al slot único (ya lleno),
     * así que cae a content: queda VISIBLE (seleccionable/borrable) y no como nodo fantasma. La proyección
     * es la fuente única de la partición; [normalize] solo reordena el resultado al orden canónico.
     */
    fun project(node: SduiNode, spec: MainContainerSpec): Map<String, List<SduiNode>> {
        val taken = mutableSetOf<SduiNode>()
        // Primer child de cada slot único (por identidad, para no confundir nodos estructuralmente iguales).
        val pickedBySlot = spec.slots
            .filter { it.childTypes.isNotEmpty() }
            .associateWith { slot -> node.children.firstOrNull { it.type in slot.childTypes && taken.add(it) } }
        val content = node.children.filterNot { it in taken } // todo lo no tomado por un slot único
        return spec.slots.associate { slot ->
            slot.id to if (slot.childTypes.isEmpty()) content else listOfNotNull(pickedBySlot[slot])
        }
    }

    /**
     * Reemplaza/inserta el nodo único de [slot] (precondición: `slot.multiple == false`): elimina de
     * `root.children` TODOS los children cuyo `type` esté en `slot.childTypes` y añade [child]; luego
     * normaliza para dejar el orden canónico. No duplica.
     */
    fun setSingleSlot(
        root: SduiNode,
        spec: MainContainerSpec,
        slot: SlotSpec,
        child: SduiNode,
    ): SduiNode {
        require(!slot.multiple) { "setSingleSlot requiere un slot único (multiple == false): ${slot.id}" }
        val withoutSlot = root.children.filterNot { it.type in slot.childTypes }
        return normalize(root.copy(children = withoutSlot + child), spec)
    }

    /**
     * Reordena `root.children` al orden canónico `[topBar?, content…, bottomBar?]` aplanando [project]
     * en el orden declarado de los slots. Los children "extra" de un slot único (2.º+ del mismo type
     * reservado) caen a content vía [project], así que se conservan y siguen visibles (no fantasmas).
     */
    fun normalize(root: SduiNode, spec: MainContainerSpec): SduiNode {
        val slots = project(root, spec)
        return root.copy(children = spec.slots.flatMap { slots.getValue(it.id) })
    }

    /** `true` si [node] o CUALQUIER descendiente tiene un `type` que es contenedor principal. */
    fun containsMainContainer(node: SduiNode): Boolean =
        isMainContainer(node.type) || node.children.any { containsMainContainer(it) }
}
