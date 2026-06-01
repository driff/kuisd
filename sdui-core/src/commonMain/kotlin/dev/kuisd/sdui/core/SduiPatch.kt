package dev.kuisd.sdui.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiPatch(
    val schemaVersion: Int,
    val changes: List<PatchOp>,
)

@Serializable
sealed interface PatchOp

@Serializable
@SerialName("replace")
data class Replace(
    val targetId: String,
    val node: SduiNode,
) : PatchOp

@Serializable
@SerialName("updateProps")
data class UpdateProps(
    val targetId: String,
    val props: JsonObject,
) : PatchOp

@Serializable
@SerialName("insert")
data class Insert(
    val parentId: String,
    val index: Int,
    val node: SduiNode,
) : PatchOp

@Serializable
@SerialName("remove")
data class Remove(
    val targetId: String,
) : PatchOp

@Serializable
@SerialName("setVars")
data class SetVars(
    val values: Map<String, JsonElement>,
) : PatchOp
