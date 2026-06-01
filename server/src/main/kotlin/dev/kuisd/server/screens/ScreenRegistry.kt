package dev.kuisd.server.screens

import dev.kuisd.sdui.core.SduiEnvelope

/** Ensambla el árbol SDUI de una pantalla. Embrión del futuro `SduiConfig.screens` de `:sdui-ktor`. */
fun interface ScreenBuilder {
    suspend fun build(ctx: ScreenContext): SduiEnvelope
}

/** Se lanza cuando se solicita una pantalla no registrada → 404 (ver ErrorHandling). */
class ScreenNotFoundException(
    screenId: String,
) : RuntimeException("Unknown screen: $screenId")

class ScreenRegistry(
    private val builders: Map<String, ScreenBuilder>,
) {
    fun builderFor(screenId: String): ScreenBuilder? = builders[screenId]
}

fun defaultScreenRegistry(): ScreenRegistry =
    ScreenRegistry(
        mapOf(
            "home" to HomeScreen,
            "details" to DetailsScreen,
            "more" to MoreScreen,
        ),
    )
