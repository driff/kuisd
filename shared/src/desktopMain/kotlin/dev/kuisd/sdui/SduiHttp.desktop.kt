package dev.kuisd.sdui

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun sduiHttpClient(): HttpClient = HttpClient(CIO) { sduiConfig() }

actual val defaultBaseUrl: String = "http://localhost:8080"
