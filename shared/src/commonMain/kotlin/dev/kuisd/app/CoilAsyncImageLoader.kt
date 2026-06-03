package dev.kuisd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.kuisd.sdui.AsyncImageLoader

/**
 * Impl del seam `LocalAsyncImage` (spec 012) con Coil 3 sobre Ktor. Los estados de carga/error se
 * pintan en los slots de `SubcomposeAsyncImage` (HU-1.5/1.6). La red vive aquí (capa app); el motor
 * (`:sdui-compose`) es agnóstico al loader.
 */
internal class CoilAsyncImageLoader(
    private val loader: ImageLoader,
) : AsyncImageLoader {
    @Composable
    override fun Image(
        url: String,
        contentDescription: String?,
        contentScale: ContentScale,
        modifier: Modifier,
    ) {
        SubcomposeAsyncImage(
            model = url,
            imageLoader = loader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } },
            error = { Box(Modifier.fillMaxSize()) },
        )
    }
}

/** Construye el loader Coil (fetcher Ktor), memoizado por el `PlatformContext`. */
@Composable
internal fun rememberCoilImageLoader(): AsyncImageLoader {
    val context = LocalPlatformContext.current
    val loader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    return remember(loader) { CoilAsyncImageLoader(loader) }
}
