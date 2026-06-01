package dev.kuisd.sdui.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import dev.kuisd.sdui.core.ColorToken
import dev.kuisd.sdui.core.ElevationToken
import dev.kuisd.sdui.core.RadiusToken
import dev.kuisd.sdui.core.SpaceToken

/**
 * Theme tokenizado del motor (spec 008). Inmutable; 4 mapas resuelven los tokens semánticos del
 * contrato (`:sdui-core`) a valores de Compose. Se compone con [plus] (override semantics: las
 * claves del lado derecho ganan). Mismo patrón que `ComponentRegistry` (spec 004): extensión por
 * composición, no por herencia.
 */
class KuisdTheme internal constructor(
    internal val colors: Map<ColorToken, Color>,
    internal val shapes: Map<RadiusToken, Shape>,
    internal val spaces: Map<SpaceToken, Dp>,
    internal val elevations: Map<ElevationToken, Dp>,
) {
    fun resolveColorOrNull(token: ColorToken?): Color? = token?.let { colors[it] }

    fun resolveShapeOrNull(token: RadiusToken?): Shape? = token?.let { shapes[it] }

    fun resolveSpaceOrNull(token: SpaceToken?): Dp? = token?.let { spaces[it] }

    fun resolveElevationOrNull(token: ElevationToken?): Dp? = token?.let { elevations[it] }

    operator fun plus(other: KuisdTheme): KuisdTheme = KuisdTheme(
        colors = colors + other.colors,
        shapes = shapes + other.shapes,
        spaces = spaces + other.spaces,
        elevations = elevations + other.elevations,
    )

    companion object {
        val Empty: KuisdTheme = KuisdTheme(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

class KuisdThemeBuilder
    @PublishedApi
    internal constructor() {
        @PublishedApi internal val colors = mutableMapOf<ColorToken, Color>()

        @PublishedApi internal val shapes = mutableMapOf<RadiusToken, Shape>()

        @PublishedApi internal val spaces = mutableMapOf<SpaceToken, Dp>()

        @PublishedApi internal val elevations = mutableMapOf<ElevationToken, Dp>()

        fun color(token: ColorToken, value: Color) {
            colors[token] = value
        }

        fun shape(token: RadiusToken, value: Shape) {
            shapes[token] = value
        }

        fun space(token: SpaceToken, value: Dp) {
            spaces[token] = value
        }

        fun elevation(token: ElevationToken, value: Dp) {
            elevations[token] = value
        }
    }

fun kuisdTheme(block: KuisdThemeBuilder.() -> Unit): KuisdTheme {
    val b = KuisdThemeBuilder().apply(block)
    return KuisdTheme(
        colors = b.colors.toMap(),
        shapes = b.shapes.toMap(),
        spaces = b.spaces.toMap(),
        elevations = b.elevations.toMap(),
    )
}

/**
 * Seam de SOLO LECTURA del theme. La app lo provee desde el host; el motor lee tokens contra
 * este seam para resolver colores/shapes/spaces/elevations. Default `Empty` → resoluciones
 * devuelven `null` y los renderers caen a defaults de Compose (degradación visible, no crash).
 */
val LocalKuisdTheme: ProvidableCompositionLocal<KuisdTheme> =
    staticCompositionLocalOf { KuisdTheme.Empty }
