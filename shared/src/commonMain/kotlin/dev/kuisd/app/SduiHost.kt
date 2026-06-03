package dev.kuisd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.kuisd.app.components.appRegistry
import dev.kuisd.app.data.KtorActionEndpoint
import dev.kuisd.app.data.KtorScreenSource
import dev.kuisd.app.data.SduiClient
import dev.kuisd.app.icons.appIconsOverride
import dev.kuisd.app.nav.NavActionHandler
import dev.kuisd.app.nav.NavBackStack
import dev.kuisd.app.theme.rememberAppTheme
import dev.kuisd.app.variables.VariableActionHandler
import dev.kuisd.app.variables.VariableStore
import dev.kuisd.sdui.LocalAsyncImage
import dev.kuisd.sdui.LocalComponentRegistry
import dev.kuisd.sdui.LocalSduiActionHandler
import dev.kuisd.sdui.LocalVariables
import dev.kuisd.sdui.icons.DefaultIconRegistry
import dev.kuisd.sdui.icons.LocalIconRegistry
import dev.kuisd.sdui.theme.LocalKuisdTheme

/**
 * Host de navegación SDUI: dueño del [NavBackStack] y de un único [SduiClient] compartido por el
 * `ScreenSource` (carga de pantallas) y el `ActionEndpoint` (FireEndpoint, spec 009). Por cada
 * entrada del back stack crea un [VariableStore], un `CoroutineScope` (cancelable al salir) y un
 * [AppActionHandler] compuesto (nav + variables + track + fireEndpoint) sin doble log (spec 005).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiHost(
    startRoute: String,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { NavBackStack(startRoute) }
    val sharedClient = remember { SduiClient() }
    val source = remember(sharedClient) { KtorScreenSource(sharedClient) }
    val actionEndpoint = remember(sharedClient) { KtorActionEndpoint(sharedClient) }
    DisposableEffect(sharedClient) {
        onDispose { sharedClient.close() }
    }

    val theme = rememberAppTheme()
    val icons = remember { DefaultIconRegistry + appIconsOverride() }
    val asyncImage = rememberCoilImageLoader()

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
        key(current.id) {
            val store = remember { VariableStore() }
            val scope = rememberCoroutineScope()
            val fire = remember { FireEndpointActionHandler(scope, actionEndpoint, store) }
            val overlay = remember { OverlayController() }
            val snackbarHostState = remember { SnackbarHostState() }
            val overlayHandler = remember { OverlayActionHandler(overlay, snackbarHostState, scope, store.scope) }
            val handler = remember {
                AppActionHandler(
                    listOf(
                        NavActionHandler(backStack),
                        VariableActionHandler(store),
                        TrackActionHandler(),
                        fire,
                        overlayHandler,
                    ),
                )
            }
            // Rompe el ciclo handler<->compuesto: el FireEndpoint re-despacha por el compuesto.
            fire.dispatch = handler::handle
            // El snackbar re-despacha su onAction asíncrono por el compuesto.
            overlayHandler.dispatch = handler::handle

            CompositionLocalProvider(
                LocalSduiActionHandler provides handler,
                LocalComponentRegistry provides appRegistry,
                LocalVariables provides store.scope,
                LocalKuisdTheme provides theme,
                LocalIconRegistry provides icons,
                LocalAsyncImage provides asyncImage,
            ) {
                Box(Modifier.fillMaxSize()) {
                    // El contenido lleva el padding del Scaffold del host (vía el modifier de SduiScreen);
                    // el OverlayHost se monta a pantalla completa para que el SnackbarHost quede
                    // edge-to-edge (las ventanas de dialog/sheet ignoran el padding de todos modos).
                    SduiScreen(
                        screenId = current.route,
                        source = source,
                        store = store,
                        modifier = Modifier.padding(padding),
                    )
                    OverlayHost(overlay, snackbarHostState, dispatch = handler::handle)
                }
            }
        }
    }
}
