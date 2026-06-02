package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.modifier.toHorizontalAlignment
import dev.kuisd.sdui.modifier.toModifier
import dev.kuisd.sdui.modifier.toVerticalAlignment
import dev.kuisd.sdui.theme.LocalKuisdTheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Receiver de un renderer: expone el [node] y el render recursivo de sus children (HU-2.2). */
class RenderScope internal constructor(
    val node: SduiNode,
) {
    /**
     * Modifier resuelto del `UiModifier` del nodo contra el `KuisdTheme` actual (spec 008).
     * Memoizado por `(node.modifier, theme)` para evitar recalcular en recomposiciones que no
     * tocan ni el nodo ni el theme.
     */
    val modifier: Modifier
        @Composable get() {
            val theme = LocalKuisdTheme.current
            val um = node.modifier
            return remember(um, theme) { um.toModifier(theme) }
        }

    /** Alineación horizontal de los hijos para un `Column`; `null` si el token no aplica al eje. */
    fun horizontalAlignmentOrNull(): Alignment.Horizontal? =
        node.modifier.alignment.toHorizontalAlignment()

    /** Alineación vertical de los hijos para un `Row`; `null` si el token no aplica al eje. */
    fun verticalAlignmentOrNull(): Alignment.Vertical? =
        node.modifier.alignment.toVerticalAlignment()

    @Composable
    fun renderChildren() {
        node.children.forEach { RenderNode(it) }
    }

    /**
     * Resuelve un campo string bindable contra [LocalVariables] (HU-2, HU-5):
     *  - `null`        -> `""`           (campo ausente)
     *  - `"$$x"`       -> `"$x"`         (escape: `$$` produce un `$` literal inicial)
     *  - `"$name"`     -> valor de la variable `name`, o `""` si ausente (HU-5.1)
     *  - otro          -> el literal tal cual (retrocompat 004)
     *
     * La lectura del seam se hace dentro de un `@Composable`; cuando la app expone un store en
     * snapshot-state, Compose registra la dependencia y recompone localizadamente (HU-2.3).
     */
    @Composable
    fun bind(raw: String?): String {
        val scope = LocalVariables.current
        return resolveBinding(raw) { scope.get(it) }
    }
}

/**
 * Regla pura de resolución del binding (testeable sin UI). Mantiene la convención `$nombre`
 * de la spec 005 y la coerción de display segura (HU-5.2).
 */
internal fun resolveBinding(raw: String?, lookup: (String) -> JsonElement?): String {
    if (raw == null) return ""
    if (!raw.startsWith("$")) return raw
    if (raw.startsWith("$$")) return raw.substring(1)
    val name = raw.substring(1)
    val value = lookup(name) ?: return ""
    return value.asDisplayString()
}

/** Representación textual segura de un JsonElement para display (HU-5.2). */
internal fun JsonElement.asDisplayString(): String = when (this) {
    is JsonPrimitive -> content
    else -> toString()
}
