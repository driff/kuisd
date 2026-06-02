package dev.kuisd.app.data

import dev.kuisd.sdui.core.ActionResponse
import kotlinx.serialization.json.JsonElement

/** Cuerpo de una petición de acción: pares `var -> valor actual` leídos del store (spec 009). */
internal data class ActionRequest(
    val endpoint: String,
    val method: String,
    val payload: Map<String, JsonElement>,
)

/**
 * Seam de salida (DIP): el `FireEndpointActionHandler` depende de esta abstracción, no de Ktor.
 * Testeable con un fake. La impl real ([KtorActionEndpoint]) vive en la capa `data`.
 */
internal fun interface ActionEndpoint {
    suspend fun fire(request: ActionRequest): ActionResponse
}
