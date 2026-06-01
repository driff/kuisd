package dev.kuisd.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun sduiHttpClient(): HttpClient = HttpClient(Darwin) { sduiConfig() }

internal actual val defaultBaseUrl: String = "http://localhost:8080"
