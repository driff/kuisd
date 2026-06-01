package dev.kuisd.server.plugins

import dev.kuisd.sdui.core.DefaultSduiJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

/** Reutiliza el `Json` del contrato (:sdui-core) — mismo classDiscriminator y políticas que el cliente. */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(DefaultSduiJson)
    }
}
