package dev.kuisd.server

import dev.kuisd.server.plugins.configureErrorHandling
import dev.kuisd.server.plugins.configureHttp
import dev.kuisd.server.plugins.configureMonitoring
import dev.kuisd.server.plugins.configureSerialization
import dev.kuisd.server.routing.actionRoutes
import dev.kuisd.server.routing.healthRoutes
import dev.kuisd.server.routing.screenRoutes
import dev.kuisd.server.screens.defaultScreenRegistry
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

const val DEFAULT_PORT: Int = 8080

fun main() {
    embeddedServer(Netty, port = DEFAULT_PORT, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val isDev = System.getenv("KUISD_DEV") == "true"
    val registry = defaultScreenRegistry()

    configureMonitoring()
    configureHttp()
    configureSerialization()
    configureErrorHandling(isDev)

    routing {
        healthRoutes()
        screenRoutes(registry)
        actionRoutes()
    }
}
