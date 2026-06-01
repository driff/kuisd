package dev.kuisd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.app.data.HttpErrorMapper
import dev.kuisd.app.data.ScreenSource
import dev.kuisd.sdui.RenderNode
import kotlinx.coroutines.CancellationException

/** Carga el envelope de [screenId] vía un [ScreenSource] y lo renderiza con el motor (HU-3). */
@Composable
fun SduiScreen(
    screenId: String,
    source: ScreenSource,
    modifier: Modifier = Modifier,
) {
    val state by produceState<ScreenUiState>(ScreenUiState.Loading, screenId, source) {
        value = try {
            ScreenUiState.Content(source.load(screenId, emptyMap()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            ScreenUiState.Error(HttpErrorMapper.message(error))
        }
    }

    when (val current = state) {
        ScreenUiState.Loading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        is ScreenUiState.Error ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${current.message}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }

        is ScreenUiState.Content ->
            Box(modifier = modifier) {
                RenderNode(current.envelope.root)
            }
    }
}
