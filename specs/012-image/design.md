# Diseño — `image` (carga de imágenes remotas)

> Spec ID: 012 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Un componente `image` en `CorePack` que delega la carga a un **seam** `LocalAsyncImageLoader` (mismo patrón
que `LocalIconRegistry`/`LocalComponentRegistry`, 008/004). El motor (`:sdui-compose`) queda **agnóstico
al loader** y sin dependencia de red: define el seam + un default neutro. La app (`:shared`) implementa
el seam con **Coil 3** (`coil-compose` + `coil-network-ktor3`) y lo provee en `SduiHost`. El mapeo
`contentScale` (string→`ContentScale`) es una función pura testeable. `url`/`contentDescription` son
bindables (`bind`, 005); el `UiModifier` (tamaño/forma, 008) se aplica al composable de la imagen.

## Arquitectura
- **`:sdui-compose`** (`commonMain`):
  - `AsyncImage.kt` (nuevo) — `fun interface AsyncImageLoader` (`@Composable`), `LocalAsyncImageLoader`
    (`staticCompositionLocalOf` con default `DefaultAsyncImageLoader`), y `String.toContentScale()`.
  - `CorePack.kt` (editar) — `ImageProps` + `register("image")` + `ImageRenderer` privado.
- **`:shared`** (`commonMain`): `CoilAsyncImageLoader` (impl del seam con `SubcomposeAsyncImage`), y un
  `rememberCoilImageLoader()` que construye el `coil3.ImageLoader` (con `KtorNetworkFetcherFactory`).
  `SduiHost.kt` provee `LocalAsyncImageLoader` en el `CompositionLocalProvider`.
- **catálogo de versiones** (`gradle/libs.versions.toml`): añadir `coil`.
- **`:server`** (editar, demo): un `image` en una pantalla piloto.
- `:sdui-compose` NO gana dependencias de Coil/red (HU-2.4).

```
image node ─► ImageRenderer (CorePack)
                 └─ LocalAsyncImageLoader.current.Image(url=bind, desc=bind, scale, modifier=UiModifier)
                       ├─ (app)   CoilAsyncImageLoader → SubcomposeAsyncImage(Coil) [loading/error]
                       └─ (motor) DefaultAsyncImageLoader → Box(modifier) neutro (sin host/tests)
```

## Componentes y contratos

### Seam (`:sdui-compose/AsyncImage.kt`)
```kotlin
/** Seam de carga de imágenes (spec 012): la app lo implementa (Coil); el motor es agnóstico. */
fun interface AsyncImageLoader {
    @Composable
    fun Image(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier)
}

/** Default neutro: un hueco del tamaño del modifier. Permite usar `image` sin host (tests/preview). */
val DefaultAsyncImageLoader = AsyncImageLoader { _, _, _, modifier -> Box(modifier) }

val LocalAsyncImageLoader: ProvidableCompositionLocal<AsyncImageLoader> =
    staticCompositionLocalOf { DefaultAsyncImageLoader }

// `toContentScale` vive en el paquete `dev.kuisd.sdui.modifier` (junto a `toHorizontalAlignment`).
/** Mapea el string del contrato a `ContentScale` de Compose (default Fit). Pura, testeable. */
internal fun String.toContentScale(): ContentScale = when (this) {
    "crop" -> ContentScale.Crop
    "fillBounds" -> ContentScale.FillBounds
    "inside" -> ContentScale.Inside
    "none" -> ContentScale.None
    else -> ContentScale.Fit
}
```

### `image` (`:sdui-compose/CorePack.kt`)
```kotlin
@Serializable
data class ImageProps(
    val url: String = "",
    val contentScale: String = "fit",          // crop|fit|fillBounds|inside|none
    val contentDescription: String? = null,
)

register(sduiComponent<ImageProps>("image")) { p -> ImageRenderer(p, modifier) }

@Composable
private fun RenderScope.ImageRenderer(p: ImageProps, baseModifier: Modifier) {
    LocalAsyncImageLoader.current.Image(
        url = bind(p.url),
        contentDescription = p.contentDescription?.let { bind(it) },
        contentScale = p.contentScale.toContentScale(),
        modifier = baseModifier,
    )
}
```
- **Decisión:** `url` reactivo vía `bind` (HU-3.3): al cambiar la variable, recompone y el loader recarga.

### Impl Coil (`:shared`)
```kotlin
internal class CoilAsyncImageLoader(private val loader: coil3.ImageLoader) : AsyncImageLoader {
    @Composable
    override fun Image(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(url).build(),
            imageLoader = loader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } },
            error = { Box(Modifier.fillMaxSize()) },   // fallback neutro (HU-1.6)
        )
    }
}

@Composable
internal fun rememberCoilImageLoader(): AsyncImageLoader {
    val ctx = LocalPlatformContext.current
    val coil = remember(ctx) {
        ImageLoader.Builder(ctx)
            .components { add(KtorNetworkFetcherFactory()) }   // usa el engine Ktor de la plataforma
            .build()
    }
    return remember(coil) { CoilAsyncImageLoader(coil) }
}
// `DisposableEffect(coil) { onDispose { coil.shutdown() } }` libera caches + HttpClient al desmontar.
```
- **Decisión:** `KtorNetworkFetcherFactory()` usa su propio cliente Ktor (engine de plataforma ya en el
  classpath: okhttp/darwin/cio). Reusar el `HttpClient` de `SduiClient` (hoy `internal`, sin exponer)
  queda como follow-up; el caché/fetch de imágenes es independiente del de pantallas.

### Cableado en `SduiHost.kt`
En el `CompositionLocalProvider` (a nivel de host, fuera de `key` — el loader no depende de la entrada):
```kotlin
val asyncImage = rememberCoilImageLoader()
…
CompositionLocalProvider(
    /* … los 5 actuales … */,
    LocalAsyncImageLoader provides asyncImage,
) { … }
```

## Modelo de datos y estados
- Sin estado nuevo del motor. El estado de carga/error lo gestiona Coil dentro del seam de la app.
- Helper puro: `toContentScale` (testeable sin UI).

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| `io.coil-kt.coil3:coil-compose` | 3.4.0 | `:shared` commonMain | `AsyncImage`/`SubcomposeAsyncImage` KMP |
| `io.coil-kt.coil3:coil-network-ktor3` | 3.4.0 | `:shared` commonMain | fetcher de red sobre Ktor 3 |
> Solo en `:shared`. `:sdui-compose` NO añade dependencias (HU-2.4). Verificar compat con CMP 1.11.0 /
> Kotlin 2.3.21 en android/ios/desktop al fijar la versión.

## Riesgos y mitigaciones
- **Compat de versión de Coil 3 con CMP 1.11.0** → fijar una 3.x estable y verificar resolución +
  compilación en los 4 targets; si falla, ajustar versión. Riesgo acotado a `:shared`.
- **Sin red en CI/sandbox** → los tests unitarios cubren `toContentScale` + registro; la carga real se
  valida en smoke manual (no en tests automatizados, que no deben depender de red).
- **`image` sin host** (RenderNode aislado) → `DefaultAsyncImageLoader` (Box neutro), sin crash (HU-2.2).
- **URL vacía/ inválida** → Coil emite el slot `error` (Box neutro); sin crash (HU-1.6).
- **Tamaño intrínseco** → sin `UiModifier` de tamaño, la imagen usa su tamaño intrínseco; se documenta
  que el server debería fijar `width`/`height` o `fillMaxWidth` para layouts predecibles.

## Estrategia de verificación
- **Unit `:sdui-compose` (commonTest):** `ContentScaleTest` (`toContentScale`: cada clave + default);
  ampliar `ComponentRegistryTest` (`CorePack.rendererFor("image") != null`).
- **Compilación multiplataforma:** `:shared` compila en desktop (y se confirma que resuelve Coil); el
  default seam permite que `:sdui-compose` compile/teste sin Coil.
- **e2e `:server`:** añadir un `image` (URL pública estable) a una pantalla piloto; `:server:build` +
  `curl` que el árbol emite `type:"image"` con `url`.
- **Smoke manual:** `:desktopApp:run` — la imagen carga; con URL rota se ve el fallback.
- **Calidad:** `:sdui-compose:jvmTest`, `:shared:desktopTest`, `:server:build`, `detekt`, `ktlintCheck`.
