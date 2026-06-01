package dev.kuisd.sdui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.ProblemDetail
import dev.kuisd.sdui.core.SduiEnvelope
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/** Estado local de una pantalla SDUI (HU-3). */
sealed interface SduiUiState {
    data object Loading : SduiUiState

    data class Error(
        val message: String,
    ) : SduiUiState

    data class Content(
        val envelope: SduiEnvelope,
    ) : SduiUiState
}

/** Carga el envelope de [screenId] del BFF y lo renderiza, con estados Loading/Error/Content (HU-3). */
@Composable
fun SduiScreen(
    screenId: String,
    client: SduiClient = remember { SduiClient() },
) {
    // SduiScreen es dueño del ciclo de vida del cliente por defecto: cierra el HttpClient al salir.
    DisposableEffect(client) {
        onDispose { client.close() }
    }

    val state by produceState<SduiUiState>(SduiUiState.Loading, screenId, client) {
        value = try {
            SduiUiState.Content(client.fetchScreen(screenId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            SduiUiState.Error(error.toUserMessage())
        }
    }

    when (val current = state) {
        SduiUiState.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        is SduiUiState.Error ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${current.message}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }

        is SduiUiState.Content ->
            RenderNode(current.envelope.root)
    }
}

/** Traduce un fallo de carga a un mensaje legible (HU-3.2), aprovechando el ProblemDetail del server. */
private suspend fun Throwable.toUserMessage(): String = when (this) {
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
        "Tiempo de espera agotado al contactar el servidor"

    is ResponseException ->
        response.problemMessage() ?: "Error ${response.status.value}"

    is SerializationException ->
        "Respuesta no válida del servidor"

    else ->
        message ?: "No se pudo conectar con el servidor"
}

/** Intenta leer el [ProblemDetail] (RFC 7807) del cuerpo de error; null si no aplica. */
private suspend fun HttpResponse.problemMessage(): String? = runCatching {
    val problem = DefaultSduiJson.decodeFromString(ProblemDetail.serializer(), bodyAsText())
    problem.detail?.let { "${problem.title}: $it" } ?: problem.title
}.getOrNull()
