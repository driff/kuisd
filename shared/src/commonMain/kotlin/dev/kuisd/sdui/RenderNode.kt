package dev.kuisd.sdui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.kuisd.sdui.core.SduiNode

/** Resuelve un [SduiNode] contra el [LocalComponentRegistry] y delega su render (HU-1.2, HU-4.1). */
@Composable
fun RenderNode(node: SduiNode) {
    val entry = LocalComponentRegistry.current.rendererFor(node.type)
        ?: run {
            UnknownNode(node.type)
            return
        }
    entry.Render(node)
}

/** Marcador de reemplazo para tipos de nodo desconocidos: resiliencia / forward-compat (HU-4). */
@Composable
internal fun UnknownNode(type: String) {
    Text("Componente no soportado: $type")
}
