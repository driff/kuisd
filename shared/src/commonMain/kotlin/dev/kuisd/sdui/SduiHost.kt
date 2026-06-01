package dev.kuisd.sdui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Host de navegación SDUI: dueño del [NavBackStack] y de un único [SduiClient] compartido (HU-4.2).
 * Renderiza la pantalla del tope vía [SduiScreen] y ofrece una afordancia "atrás" cross-platform (HU-2.2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiHost(
    startRoute: String,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { NavBackStack(NavEntry(startRoute)) }
    val dispatcher = remember { NavigationActionDispatcher(backStack) }
    val client = remember { SduiClient() }
    DisposableEffect(client) {
        onDispose { client.close() }
    }

    val current = backStack.current
    Scaffold(
        modifier = modifier,
        topBar = {
            if (backStack.canGoBack) {
                TopAppBar(
                    title = { Text(current.route) },
                    navigationIcon = {
                        TextButton(onClick = { backStack.pop() }) { Text("‹ Atrás") }
                    },
                )
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalActionDispatcher provides dispatcher) {
            key(current.route) {
                SduiScreen(
                    screenId = current.route,
                    client = client,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
