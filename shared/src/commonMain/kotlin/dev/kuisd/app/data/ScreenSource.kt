package dev.kuisd.app.data

import dev.kuisd.sdui.core.SduiEnvelope

/** Seam de entrada de datos (DIP): la app depende de esta abstracción, no de Ktor/SduiClient (HU-2.1). */
fun interface ScreenSource {
    // Nota: un `fun interface` (SAM) no admite valores por defecto en su método abstracto; el default
    // `args = emptyMap()` del diseño se materializa en los call sites pasando `emptyMap()`.
    suspend fun load(screenId: String, args: Map<String, String>): SduiEnvelope
}
