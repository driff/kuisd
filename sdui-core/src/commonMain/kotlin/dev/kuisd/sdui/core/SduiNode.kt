package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiNode(
    val type: String,
    val id: String? = null,
    val props: JsonObject = JsonObject(emptyMap()),
    val modifier: UiModifier = UiModifier(),
    val actions: Map<String, List<UiAction>> = emptyMap(),
    val children: List<SduiNode> = emptyList(),
)
