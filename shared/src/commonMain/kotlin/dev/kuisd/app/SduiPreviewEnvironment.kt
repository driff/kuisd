package dev.kuisd.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.kuisd.app.components.appRegistry
import dev.kuisd.app.icons.appIconsOverride
import dev.kuisd.app.image.appImageRegistry
import dev.kuisd.app.theme.rememberAppTheme
import dev.kuisd.sdui.LocalComponentRegistry
import dev.kuisd.sdui.LocalImageRegistry
import dev.kuisd.sdui.LocalSduiActionHandler
import dev.kuisd.sdui.LocalVariables
import dev.kuisd.sdui.SduiActionHandler
import dev.kuisd.sdui.VariableScope
import dev.kuisd.sdui.icons.DefaultIconRegistry
import dev.kuisd.sdui.icons.LocalIconRegistry
import dev.kuisd.sdui.theme.LocalKuisdTheme

/**
 * Provee los seams reales de la app (registry de componentes, tema, iconos, imágenes y variables) con un
 * [SduiActionHandler] **inyectable**, para renderizar un árbol SDUI fuera del flujo de navegación del
 * [SduiHost] (spec 014: preview del builder). Reutiliza los mismos catálogos `internal` de la app, así el
 * render es idéntico a producción; el llamador decide qué handler usar (p.ej. uno no-op que loguea).
 *
 * Nota: no se provee `LocalAsyncImageLoader` (imágenes remotas) porque su loader Coil vive ligado al
 * ciclo del host; un preview usa el default agnóstico del motor. Las imágenes locales (`name`) sí
 * funcionan vía `LocalImageRegistry`.
 */
@Composable
fun SduiPreviewEnvironment(
    actionHandler: SduiActionHandler,
    variables: VariableScope = VariableScope { null },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComponentRegistry provides appRegistry,
        LocalKuisdTheme provides rememberAppTheme(),
        LocalIconRegistry provides remember { DefaultIconRegistry + appIconsOverride() },
        LocalImageRegistry provides remember { appImageRegistry() },
        LocalVariables provides variables,
        LocalSduiActionHandler provides actionHandler,
        content = content,
    )
}
