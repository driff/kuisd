package dev.kuisd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.app.data.HttpErrorMapper
import dev.kuisd.app.data.ScreenSource
import dev.kuisd.app.variables.VariableStore
import dev.kuisd.sdui.RenderNode

/**
 * Carga el envelope de [screenId] vía un [ScreenSource] y lo renderiza con el motor.
 *
 * Recibe el [store] del host (Opción A del diseño 005): cuando llega `Content`, siembra el store
 * con `envelope.variables` para que el seam de lectura (`LocalVariables`, provisto por el host)
 * y el `VariableActionHandler` que el host compone en `AppActionHandler` apunten al mismo estado.
 */
@Composable
fun SduiScreen(
    screenId: String,
    source: ScreenSource,
    store: VariableStore,
    modifier: Modifier = Modifier,
) {
    val state by produceState<ScreenUiState>(ScreenUiState.Loading, screenId, source) {
        value = try {
            ScreenUiState.Content(source.load(screenId, emptyMap()))
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
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

        is ScreenUiState.Content -> {
            LaunchedEffect(current.envelope) {
                store.seed(current.envelope.variables)
            }
            Box(modifier = modifier) {
                RenderNode(current.envelope.root)
            }
        }
    }
}
