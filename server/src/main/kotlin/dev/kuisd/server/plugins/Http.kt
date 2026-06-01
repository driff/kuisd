package dev.kuisd.server.plugins

import dev.kuisd.sdui.core.KUISD_VERSION_HEADER
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders

/** Higiene HTTP + CORS deny-by-default (orígenes vía env `KUISD_CORS_HOSTS`, csv). */
fun Application.configureHttp() {
    install(DefaultHeaders)
    install(Compression)
    install(CORS) {
        corsHosts().forEach { allowHost(it, schemes = listOf("http", "https")) }
        allowHeader(KUISD_VERSION_HEADER)
    }
}

private fun corsHosts(): List<String> =
    System.getenv("KUISD_CORS_HOSTS").orEmpty()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
