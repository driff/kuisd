package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SduiEnvelope(
    val schemaVersion: Int,
    val screenId: String,
    val root: SduiNode,
    val variables: Map<String, JsonElement> = emptyMap(),
    val meta: Map<String, String> = emptyMap(),
)
