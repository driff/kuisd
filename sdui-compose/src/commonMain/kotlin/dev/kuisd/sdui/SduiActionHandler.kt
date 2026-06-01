package dev.kuisd.sdui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import dev.kuisd.sdui.core.UiAction

/** Seam de salida del motor: el motor delega aquí las acciones de un nodo sin interpretarlas (HU-1.2, DIP). */
fun interface SduiActionHandler {
    fun handle(actions: List<UiAction>)
}

/** Handler inyectado por el host; default no-op para usar `RenderNode` fuera de un host (HU-1.2). */
val LocalSduiActionHandler: ProvidableCompositionLocal<SduiActionHandler> =
    staticCompositionLocalOf { SduiActionHandler { } }
