package dev.kuisd.sdui

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun sduiHttpClient(): HttpClient = HttpClient(OkHttp) { sduiConfig() }

/** Loopback del emulador Android hacia el host de desarrollo. */
actual val defaultBaseUrl: String = "http://10.0.2.2:8080"
