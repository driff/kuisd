package dev.kuisd.sdui

import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.JsonPrimitive

/** Lee una prop de tipo string del [SduiNode]; devuelve null si falta o no es string (HU-2.2). */
fun SduiNode.stringProp(key: String): String? =
    (props[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
