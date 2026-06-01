package dev.kuisd.sdui.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface UiAction

@Serializable
@SerialName("navigate")
data class Navigate(
    val route: String,
    val args: Map<String, String> = emptyMap(),
) : UiAction

@Serializable
@SerialName("navigateBack")
data object NavigateBack : UiAction

@Serializable
@SerialName("setVar")
data class SetVar(
    val name: String,
    val value: JsonElement,
) : UiAction

@Serializable
@SerialName("increment")
data class Increment(
    val name: String,
    val by: Int = 1,
    val min: Int? = null,
    val max: Int? = null,
) : UiAction

@Serializable
@SerialName("toggle")
data class Toggle(
    val name: String,
) : UiAction

@Serializable
@SerialName("network")
data class FireEndpoint(
    val endpoint: String,
    val payloadVars: List<String> = emptyList(),
) : UiAction

@Serializable
@SerialName("track")
data class Track(
    val event: String,
    val props: Map<String, String> = emptyMap(),
) : UiAction

@Serializable
@SerialName("custom")
data class CustomAction(
    val name: String,
    val payload: JsonObject = JsonObject(emptyMap()),
) : UiAction

@Serializable
@SerialName("noop")
data object NoOpAction : UiAction
