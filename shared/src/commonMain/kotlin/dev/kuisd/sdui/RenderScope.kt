package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import dev.kuisd.sdui.core.SduiNode

/** Receiver de un renderer: expone el [node] y el render recursivo de sus children (HU-2.2). */
class RenderScope internal constructor(
    val node: SduiNode,
) {
    @Composable
    fun renderChildren() {
        node.children.forEach { RenderNode(it) }
    }
}
