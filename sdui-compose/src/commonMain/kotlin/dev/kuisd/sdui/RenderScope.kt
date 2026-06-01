package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Receiver de un renderer: expone el [node] y el render recursivo de sus children (HU-2.2). */
class RenderScope internal constructor(
    val node: SduiNode,
) {
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
