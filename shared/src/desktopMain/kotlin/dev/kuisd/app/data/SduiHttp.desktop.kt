package dev.kuisd.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun sduiHttpClient(): HttpClient = HttpClient(CIO) { sduiConfig() }

internal actual val defaultBaseUrl: String = "http://localhost:8080"
