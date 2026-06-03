package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

/**
 * Catálogo abierto de imágenes locales del motor (spec 013): el server referencia imágenes por nombre
 * (`"kuisd_logo"`); el cliente las resuelve contra este registry. Misma mecánica que `IconRegistry`
 * (008): composición por [plus] (override semantics). El valor es un **factory `@Composable`** porque
 * `painterResource` (Compose Resources) es `@Composable`. El motor no empaqueta assets: el registry base
 * es vacío y la app provee el suyo.
 */
class ImageRegistry internal constructor(
    internal val byName: Map<String, @Composable () -> Painter>,
) {
    fun get(name: String): (@Composable () -> Painter)? = byName[name]

    operator fun plus(other: ImageRegistry): ImageRegistry =
        ImageRegistry(byName + other.byName)

    companion object {
        val Empty: ImageRegistry = ImageRegistry(emptyMap())
    }
}

class ImageRegistryBuilder
    @PublishedApi
    internal constructor() {
        @PublishedApi internal val byName = mutableMapOf<String, @Composable () -> Painter>()

        fun register(name: String, painter: @Composable () -> Painter) {
            byName[name] = painter
        }
    }

fun imageRegistry(block: ImageRegistryBuilder.() -> Unit): ImageRegistry =
    ImageRegistry(ImageRegistryBuilder().apply(block).byName.toMap())

/**
 * Seam de SOLO LECTURA del registry de imágenes locales. La app provee el suyo (por defecto vacío); el
 * motor permanece agnóstico (no incluye assets propios).
 */
val LocalImageRegistry: ProvidableCompositionLocal<ImageRegistry> =
    staticCompositionLocalOf { ImageRegistry.Empty }
