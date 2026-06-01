package dev.kuisd.app.data

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/** Cliente SDUI: recupera el [SduiEnvelope] de una pantalla del BFF (HU-1). */
internal class SduiClient(
    private val baseUrl: String = defaultBaseUrl,
    private val http: HttpClient = sduiHttpClient(),
) {
    suspend fun fetchScreen(screenId: String): SduiEnvelope {
        val body = http.get("$baseUrl/screen/$screenId").bodyAsText()
        return DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), body)
    }

    /** Libera el engine HTTP. Llamar cuando el cliente (y su `HttpClient`) deje de usarse. */
    fun close() {
        http.close()
    }
}
