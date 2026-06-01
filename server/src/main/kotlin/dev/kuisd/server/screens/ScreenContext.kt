package dev.kuisd.server.screens

/** Capacidades declaradas por el cliente (hoy solo la versión de esquema; ver KUISD_VERSION_HEADER). */
data class ClientCapability(
    val schemaVersion: Int?,
)

/** Contexto que recibe un [ScreenBuilder] para ensamblar una pantalla. */
data class ScreenContext(
    val screenId: String,
    val capability: ClientCapability,
)
