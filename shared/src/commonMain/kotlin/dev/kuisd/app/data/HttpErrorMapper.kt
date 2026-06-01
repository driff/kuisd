package dev.kuisd.app.data

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.ProblemDetail
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

/** Traduce los fallos de transporte de la capa data a un mensaje legible (HU-3.2); saca Ktor de presentación. */
internal object HttpErrorMapper {
    suspend fun message(e: Throwable): String = when (e) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            "Tiempo de espera agotado al contactar el servidor"

        is ResponseException ->
            e.response.problemMessage() ?: "Error ${e.response.status.value}"

        is SerializationException ->
            "Respuesta no válida del servidor"

        else ->
            e.message ?: "No se pudo conectar con el servidor"
    }

    /** Intenta leer el [ProblemDetail] (RFC 7807) del cuerpo de error; null si no aplica. */
    private suspend fun HttpResponse.problemMessage(): String? = runCatching {
        val problem = DefaultSduiJson.decodeFromString(ProblemDetail.serializer(), bodyAsText())
        problem.detail?.let { "${problem.title}: $it" } ?: problem.title
    }.getOrNull()
}
