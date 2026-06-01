package dev.kuisd.app.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

/** Crea el [HttpClient] con el engine de Ktor propio de cada plataforma (HU-4.1). */
internal expect fun sduiHttpClient(): HttpClient

/** baseUrl por defecto del `:server` segun la plataforma (HU-4.2). */
internal expect val defaultBaseUrl: String

/** Config compartida: timeouts (R9) para que un servidor lento/caido no cuelgue la UI (HU-1.3). */
internal fun HttpClientConfig<*>.sduiConfig() {
    // expectSuccess: las respuestas no-2xx lanzan ResponseException, para mapear el ProblemDetail
    // del server a un mensaje legible (HU-3.2) en vez de fallar al decodificar un SduiEnvelope.
    expectSuccess = true
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 5_000
    }
}
