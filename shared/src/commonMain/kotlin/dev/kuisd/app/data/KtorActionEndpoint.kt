package dev.kuisd.app.data

import dev.kuisd.sdui.core.ActionResponse
import kotlinx.serialization.json.JsonObject

/** Impl Ktor del [ActionEndpoint]; delega en [SduiClient] el detalle HTTP (spec 009). */
internal class KtorActionEndpoint(
    private val client: SduiClient,
) : ActionEndpoint {
    override suspend fun fire(request: ActionRequest): ActionResponse =
        client.postAction(request.endpoint, request.method, JsonObject(request.payload))
}
