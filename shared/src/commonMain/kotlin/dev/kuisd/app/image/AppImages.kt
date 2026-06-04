package dev.kuisd.app.image

import dev.kuisd.sdui.ImageRegistry
import dev.kuisd.sdui.imageRegistry
import dev.kuisd.shared.resources.Res
import dev.kuisd.shared.resources.kuisd_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Registry de imágenes locales de la app (spec 013): assets empaquetados en Compose Resources. Demuestra
 * el patrón composicional del `ImageRegistry` (espejo de `appIconsOverride`). El motor no incluye assets.
 */
internal fun appImageRegistry(): ImageRegistry = imageRegistry {
    register("kuisd_logo") { painterResource(Res.drawable.kuisd_logo) }
}
