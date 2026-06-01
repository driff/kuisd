package dev.kuisd.server.routing

import dev.kuisd.sdui.core.KUISD_VERSION_HEADER
import dev.kuisd.server.screens.ClientCapability
import dev.kuisd.server.screens.ScreenContext
import dev.kuisd.server.screens.ScreenNotFoundException
import dev.kuisd.server.screens.ScreenRegistry
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** BFF SDUI: sirve el árbol de la pantalla solicitada, serializado con el contrato compartido. */
fun Route.screenRoutes(registry: ScreenRegistry) {
    get("/screen/{id}") {
        val id = call.parameters["id"].orEmpty()
        val capability = ClientCapability(
            schemaVersion = call.request.headers[KUISD_VERSION_HEADER]?.toIntOrNull(),
        )
        val builder = registry.builderFor(id) ?: throw ScreenNotFoundException(id)
        call.respond(builder.build(ScreenContext(id, capability)))
    }
}
