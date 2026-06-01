package dev.kuisd.sdui

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun sduiHttpClient(): HttpClient = HttpClient(Darwin) { sduiConfig() }

actual val defaultBaseUrl: String = "http://localhost:8080"
