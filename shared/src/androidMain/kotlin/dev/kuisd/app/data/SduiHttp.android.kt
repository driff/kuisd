package dev.kuisd.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun sduiHttpClient(): HttpClient = HttpClient(OkHttp) { sduiConfig() }

/** Loopback del emulador Android hacia el host de desarrollo. */
internal actual val defaultBaseUrl: String = "http://10.0.2.2:8080"
