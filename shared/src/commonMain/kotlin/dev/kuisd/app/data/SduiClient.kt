package dev.kuisd.app.data

import dev.kuisd.sdui.core.ActionResponse
import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

/** Cliente SDUI: recupera el [SduiEnvelope] de una pantalla del BFF (HU-1). */
internal class SduiClient(
    private val baseUrl: String = defaultBaseUrl,
    private val http: HttpClient = sduiHttpClient(),
) {
    suspend fun fetchScreen(screenId: String): SduiEnvelope {
        val body = http.get("$baseUrl/screen/$screenId").bodyAsText()
        return DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), body)
    }

    /**
     * Dispara una acción de red contra el BFF (spec 009). `POST` envía [body] como JSON; `GET` lo
     * ignora. La respuesta se decodifica a [ActionResponse]. Un no-2xx lanza `ResponseException`
     * (expectSuccess de [sduiConfig]) que el handler mapea con `HttpErrorMapper`.
     */
    suspend fun postAction(endpoint: String, method: String, body: JsonObject): ActionResponse {
        val httpMethod = if (method.equals("GET", ignoreCase = true)) HttpMethod.Get else HttpMethod.Post
        val responseText = http.request("$baseUrl$endpoint") {
            this.method = httpMethod
            if (httpMethod == HttpMethod.Post) {
                contentType(ContentType.Application.Json)
                setBody(DefaultSduiJson.encodeToString(JsonObject.serializer(), body))
            }
        }.bodyAsText()
        return DefaultSduiJson.decodeFromString(ActionResponse.serializer(), responseText)
    }

    /** Libera el engine HTTP. Llamar cuando el cliente (y su `HttpClient`) deje de usarse. */
    fun close() {
        http.close()
    }
}
