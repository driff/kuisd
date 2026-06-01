package dev.kuisd.sdui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.kuisd.sdui.core.SduiNode

/** Mapea un [SduiNode] a su composable equivalente, renderando children recursivamente (HU-2). */
@Composable
fun RenderNode(node: SduiNode) {
    when (node.type) {
        "column" -> Column { node.children.forEach { RenderNode(it) } }
        "row" -> Row { node.children.forEach { RenderNode(it) } }
        "text" -> Text(node.stringProp("text").orEmpty())
        "button" ->
            Button(
                onClick = {
                    // HU-2.3: se reconoce la accion onClick sin ejecutar navegacion ni red en este slice.
                    val onClick = node.actions["onClick"].orEmpty()
                    sduiLog("button onClick -> $onClick")
                },
            ) {
                Text(node.stringProp("label").orEmpty())
            }

        else -> UnknownNode(node.type)
    }
}

/** Marcador de reemplazo para tipos de nodo desconocidos: resiliencia / forward-compat (HU-2.4). */
@Composable
private fun UnknownNode(type: String) {
    Text("Componente no soportado: $type")
}
