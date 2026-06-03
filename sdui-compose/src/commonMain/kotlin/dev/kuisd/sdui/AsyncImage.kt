package dev.kuisd.sdui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Seam de carga de imágenes (spec 012): la app lo implementa (Coil) y el motor queda **agnóstico** al
 * loader, sin dependencia de red. Mismo patrón de seam que `LocalIconRegistry`/`LocalComponentRegistry`.
 */
fun interface AsyncImageLoader {
    @Composable
    fun Image(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier)
}

/** Default neutro: un hueco del tamaño del `modifier`. Permite usar `image` sin host (tests/preview). */
val DefaultAsyncImageLoader = AsyncImageLoader { _, _, _, modifier -> Box(modifier) }

/** Seam de SOLO LECTURA del loader de imágenes; la app provee el suyo (Coil) por `SduiHost`. */
val LocalAsyncImage: ProvidableCompositionLocal<AsyncImageLoader> =
    staticCompositionLocalOf { DefaultAsyncImageLoader }

/** Mapea el string del contrato a `ContentScale` de Compose (default `Fit`). Pura, testeable. */
internal fun String.toContentScale(): ContentScale = when (this) {
    "crop" -> ContentScale.Crop
    "fillBounds" -> ContentScale.FillBounds
    "inside" -> ContentScale.Inside
    "none" -> ContentScale.None
    else -> ContentScale.Fit
}
