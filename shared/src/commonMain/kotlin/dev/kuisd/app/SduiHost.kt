package dev.kuisd.app

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
import dev.kuisd.app.components.appRegistry
import dev.kuisd.app.data.KtorScreenSource
import dev.kuisd.app.nav.NavActionHandler
import dev.kuisd.app.nav.NavBackStack
import dev.kuisd.sdui.LocalComponentRegistry
import dev.kuisd.sdui.LocalSduiActionHandler

/**
 * Host de navegación SDUI: dueño del [NavBackStack] y de un único `KtorScreenSource` compartido (HU-4.2).
 * Renderiza la pantalla del tope vía [SduiScreen] y ofrece una afordancia "atrás" cross-platform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiHost(
    startRoute: String,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { NavBackStack(startRoute) }
    val source = remember { KtorScreenSource() }
    DisposableEffect(source) {
        onDispose { source.close() }
    }
    val handler = remember { NavActionHandler(backStack) }

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
        CompositionLocalProvider(
            LocalSduiActionHandler provides handler,
            LocalComponentRegistry provides appRegistry,
        ) {
            key(current.id) {
                SduiScreen(
                    screenId = current.route,
                    source = source,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
