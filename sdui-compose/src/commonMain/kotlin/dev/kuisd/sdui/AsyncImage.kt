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
 *
 * Alcance v1: solo imágenes **remotas por URL**; imágenes locales/empaquetadas quedan fuera (un
 * `ImageRegistry` análogo al `IconRegistry` sería el follow-up).
 */
fun interface AsyncImageLoader {
    @Composable
    fun Image(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier)
}

/** Default neutro: un hueco del tamaño del `modifier`. Permite usar `image` sin host (tests/preview). */
val DefaultAsyncImageLoader = AsyncImageLoader { _, _, _, modifier -> Box(modifier) }

/**
 * Seam de SOLO LECTURA del loader de imágenes; la app provee el suyo (Coil) por `SduiHost`.
 *
 * Nota de layout: un `image` con `fillMaxWidth` **sin alto acotado** sigue el tamaño intrínseco de la
 * imagen tras cargar; durante la carga el placeholder puede colapsar a 0 de alto. El server debería
 * fijar un alto/`aspectRatio` para layouts predecibles (no hay `aspectRatio` en el contrato v1).
 */
val LocalAsyncImageLoader: ProvidableCompositionLocal<AsyncImageLoader> =
    staticCompositionLocalOf { DefaultAsyncImageLoader }
