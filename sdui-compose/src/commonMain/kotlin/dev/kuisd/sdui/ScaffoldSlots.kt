package dev.kuisd.sdui

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.serializer

/** Slots resueltos de un `scaffold` por el `type` de cada child (HU-1.2/1.5). */
internal data class ScaffoldSlots(
    val topBar: SduiNode?,
    val bottomBar: SduiNode?,
    val content: List<SduiNode>,
)

/**
 * Particiona [children] en (topBar?, bottomBar?, content[]) por su `type` (HU-1.2/1.5):
 *  - `topBar` = PRIMER child con `type == "topAppBar"`; los duplicados se descartan.
 *  - `bottomBar` = PRIMER child con `type == "bottomBar"`; los duplicados se descartan.
 *  - `content` = el resto de children (ni `topAppBar` ni `bottomBar`) en orden.
 *
 * Determinista (primero gana ante duplicados); función pura, testeable sin Compose.
 */
internal fun partitionScaffoldSlots(children: List<SduiNode>): ScaffoldSlots {
    var topBar: SduiNode? = null
    var bottomBar: SduiNode? = null
    val content = mutableListOf<SduiNode>()
    children.forEach { child ->
        when (child.type) {
            "topAppBar" -> if (topBar == null) topBar = child
            "bottomBar" -> if (bottomBar == null) bottomBar = child
            else -> content += child
        }
    }
    return ScaffoldSlots(topBar = topBar, bottomBar = bottomBar, content = content)
}

/** Selección pura del ítem de `bottomBar` (HU-3.3/3.5): null/no-resuelto ⇒ false. */
internal fun isItemSelected(selectedValue: String?, itemValue: String): Boolean =
    selectedValue != null && selectedValue == itemValue

/**
 * Decodifica las `props` de [node] a [T] reutilizando `DefaultSduiJson`. Misma política que
 * `RegisteredComponent.Render` (props inválidas ⇒ `null` sin crash + `sduiLog` diagnóstico), pero con
 * `serializer<T>()` reificado porque los hijos decodificados a mano (p.ej. `bottomBarItem`) no tienen
 * un `RegisteredComponent` al que delegar (HU-3, HU-4.2).
 */
internal inline fun <reified T : Any> decodeOrNull(node: SduiNode): T? =
    runCatching { DefaultSduiJson.decodeFromJsonElement(serializer<T>(), node.props) }
        .onFailure { sduiLog("props inválidas para type='${node.type}', se ignora: ${it.message}") }
        .getOrNull()
