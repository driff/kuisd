package dev.kuisd.server.routing

import dev.kuisd.sdui.core.ActionResponse
import dev.kuisd.sdui.core.SetVar
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Endpoints de acción (spec 009): el cliente dispara `FireEndpoint` → POST aquí → devolvemos un
 * [ActionResponse] con acciones que el cliente re-despacha + un mensaje legible. Distinto prefijo
 * (`/action/...`) que las pantallas (`/screen/...`).
 */
fun Route.actionRoutes() {
    post("/action/greet") {
        val body = call.receive<JsonObject>()
        val name = body["name"]?.jsonPrimitive?.content.orEmpty().trim()
        if (name.isEmpty()) throw BadRequestException("name requerido")
        call.respond(
            ActionResponse(
                message = "Hola, $name!",
                actions = listOf(SetVar("greeted", JsonPrimitive(true))),
            ),
        )
    }
}
