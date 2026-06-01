package dev.kuisd.sdui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.SduiEnvelope

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
    val state by produceState<SduiUiState>(SduiUiState.Loading, screenId, client) {
        value = runCatching { client.fetchScreen(screenId) }
            .fold(
                onSuccess = { SduiUiState.Content(it) },
                onFailure = { SduiUiState.Error(it.message ?: "Error desconocido") },
            )
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
