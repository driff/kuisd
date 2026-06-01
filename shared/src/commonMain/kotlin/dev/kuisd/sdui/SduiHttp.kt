package dev.kuisd.sdui

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

/** Crea el [HttpClient] con el engine de Ktor propio de cada plataforma (HU-4.1). */
expect fun sduiHttpClient(): HttpClient

/** baseUrl por defecto del `:server` segun la plataforma (HU-4.2). */
expect val defaultBaseUrl: String

/** Config compartida: timeouts (R9) para que un servidor lento/caido no cuelgue la UI (HU-1.3). */
internal fun HttpClientConfig<*>.sduiConfig() {
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 5_000
    }
}
