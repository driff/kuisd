package dev.kuisd.sdui

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/** Cliente SDUI: recupera el [SduiEnvelope] de una pantalla del BFF (HU-1). */
class SduiClient(
    private val baseUrl: String = defaultBaseUrl,
    private val http: HttpClient = sduiHttpClient(),
) {
    suspend fun fetchScreen(screenId: String): SduiEnvelope {
        val body = http.get("$baseUrl/screen/$screenId").bodyAsText()
        return DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), body)
    }
}
