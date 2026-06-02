package dev.kuisd.app.data

import dev.kuisd.sdui.core.SduiEnvelope

/** Impl Ktor del [ScreenSource]; `SduiClient`/`SduiHttp` quedan como detalle privado de la capa data (HU-2.1). */
internal class KtorScreenSource(
    private val client: SduiClient = SduiClient(),
) : ScreenSource {
    override suspend fun load(screenId: String, args: Map<String, String>): SduiEnvelope =
        client.fetchScreen(screenId)
}
