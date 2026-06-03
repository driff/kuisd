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
    val method: String = "POST",
    val payloadVars: List<String> = emptyList(),
    val statusVar: String? = null,
    val resultVar: String? = null,
    val onSuccess: List<UiAction> = emptyList(),
    val onError: List<UiAction> = emptyList(),
) : UiAction

/**
 * Respuesta de un endpoint de acción (spec 009): acciones a despachar en el cliente tras la
 * petición + un mensaje legible opcional (p.ej. para enlazar con un `text` vía `resultVar`).
 */
@Serializable
data class ActionResponse(
    val actions: List<UiAction> = emptyList(),
    val message: String? = null,
)

/** Literales del ciclo de estado de un [FireEndpoint], compartidos por server y cliente. */
object EndpointStatus {
    const val IDLE = "idle"
    const val LOADING = "loading"
    const val SUCCESS = "success"
    const val ERROR = "error"
}

/**
 * Overlays imperativos (spec 011). El estado y el render viven en el host (capa app); el motor solo
 * despacha estas acciones por `LocalSduiActionHandler`. Un diálogo/hoja activo a la vez.
 */
@Serializable
@SerialName("showDialog")
data class ShowDialog(
    val title: String = "",
    val text: String = "",
    val confirmLabel: String? = null,
    val onConfirm: List<UiAction> = emptyList(),
    val dismissLabel: String? = null,
    val onDismiss: List<UiAction> = emptyList(),
) : UiAction

@Serializable
@SerialName("showBottomSheet")
data class ShowBottomSheet(
    val content: List<SduiNode> = emptyList(),
    val onDismiss: List<UiAction> = emptyList(),
) : UiAction

@Serializable
@SerialName("showSnackbar")
data class ShowSnackbar(
    val message: String = "",
    val messageVar: String? = null,
    val actionLabel: String? = null,
    val onAction: List<UiAction> = emptyList(),
    val duration: String = "short", // "short" | "long" | "indefinite"
) : UiAction

/** Cierra el diálogo/hoja activo si lo hay (no-op si no hay ninguno). */
@Serializable
@SerialName("dismissOverlay")
data object DismissOverlay : UiAction

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
