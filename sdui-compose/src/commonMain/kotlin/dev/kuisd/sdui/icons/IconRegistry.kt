package dev.kuisd.sdui.icons

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Catálogo abierto de iconos del motor (spec 008). El server referencia iconos por nombre
 * (`"home"`, `"arrowBack"`); el cliente los resuelve contra este registry. Misma mecánica que
 * `ComponentRegistry` y `KuisdTheme`: composición por [plus] (override semantics).
 */
class IconRegistry internal constructor(
    internal val byName: Map<String, ImageVector>,
) {
    fun get(name: String): ImageVector? = byName[name]

    operator fun plus(other: IconRegistry): IconRegistry =
        IconRegistry(byName + other.byName)

    companion object {
        val Empty: IconRegistry = IconRegistry(emptyMap())
    }
}

class IconRegistryBuilder
    @PublishedApi
    internal constructor() {
        @PublishedApi internal val byName = mutableMapOf<String, ImageVector>()

        fun register(name: String, vector: ImageVector) {
            byName[name] = vector
        }
    }

fun iconRegistry(block: IconRegistryBuilder.() -> Unit): IconRegistry =
    IconRegistry(IconRegistryBuilder().apply(block).byName.toMap())

/**
 * Seam de SOLO LECTURA del registry de iconos. La app provee el suyo (por defecto
 * `DefaultIconRegistry`, opcionalmente compuesto con un override propio).
 */
val LocalIconRegistry: ProvidableCompositionLocal<IconRegistry> =
    staticCompositionLocalOf { DefaultIconRegistry }
