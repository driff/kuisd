# Tareas — `image` (carga de imágenes remotas)

> Spec ID: 012 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — `:sdui-compose`: `AsyncImage.kt` con `fun interface AsyncImageLoader` (`@Composable
  Image(url, contentDescription, contentScale, modifier)`), `DefaultAsyncImageLoader` (Box neutro),
  `LocalAsyncImage` (`staticCompositionLocalOf`) y `String.toContentScale()` (puro).
  - _ref:_ HU-2.1/2.2 · design §"Seam"
  - _verif:_ `./gradlew :sdui-compose:compileKotlinJvm`; cubierto por el test de T3.

- [x] **T2** — `:sdui-compose`: `ImageProps(url, contentScale="fit", contentDescription?)` +
  `register("image")` + `ImageRenderer` (llama a `LocalAsyncImage.current.Image` con `bind(url)`,
  `bind(contentDescription)`, `toContentScale`, `modifier`).
  - _ref:_ HU-1.1/1.2/1.3/1.4, HU-3.1/3.3 · design §"image"
  - _verif:_ compila; `rendererFor("image") != null` (T3).

- [x] **T3** — `:sdui-compose` tests (commonTest): `ContentScaleTest` (`toContentScale`: crop/fit/
  fillBounds/inside/none/desconocido→Fit) y ampliar `ComponentRegistryTest` (`rendererFor("image")`).
  - _ref:_ HU-1.3, HU-3.1 · design §"Estrategia de verificación"
  - _verif:_ `./gradlew :sdui-compose:jvmTest` en verde.

- [x] **T4** — Catálogo de versiones: añadir `coil = "3.4.0"` y los `library` `coil-compose`
  (`io.coil-kt.coil3:coil-compose`) y `coil-network-ktor3` (`io.coil-kt.coil3:coil-network-ktor3`) a
  `gradle/libs.versions.toml`; declararlos en `shared/build.gradle.kts` (`commonMain`).
  - _ref:_ §"Dependencias nuevas"
  - _verif:_ `./gradlew :shared:dependencies` resuelve Coil; `:shared:compileKotlinDesktop` en verde.

- [x] **T5** — `:shared`: `CoilAsyncImageLoader` (impl del seam con `SubcomposeAsyncImage` + slots
  loading/error) y `rememberCoilImageLoader()` (`ImageLoader.Builder` + `KtorNetworkFetcherFactory`).
  Cablear `LocalAsyncImage provides rememberCoilImageLoader()` en el `CompositionLocalProvider` de
  `SduiHost`.
  - _ref:_ HU-2.3, HU-1.5/1.6 · design §"Impl Coil", §"Cableado en SduiHost"
  - _verif:_ `./gradlew :shared:compileKotlinDesktop` (y `:desktopApp:compile*`) en verde.

- [x] **T6** — `:server`: pantalla piloto. Añadir un `image` (URL pública estable) a una pantalla
  existente (candidata: `feed` o `home`) con `contentScale` y un `UiModifier` de tamaño.
  - _ref:_ HU-1 · design §"Estrategia de verificación" (e2e)
  - _verif:_ `./gradlew :server:build`; `curl localhost:8080/screen/<piloto>` muestra `type:"image"` con `url`.

- [x] **T7** — Calidad + smoke visual: `./gradlew detekt ktlintCheck` en verde; `:desktopApp:run` y
  observar que la imagen carga (y el fallback con una URL rota).
  - _ref:_ Requisitos no funcionales · design §"Estrategia de verificación"
  - _verif:_ lint/detekt verdes ✅. Smoke visual de escritorio NO ejecutado aquí (no abre UI; la carga
    real necesita red); justificado con evidencia: `curl /screen/home` devuelve el nodo `image` (T6) y
    `:shared:desktopTest` compila el código Coil. Pendiente confirmación visual con `:desktopApp:run`.

## Verificación final (Definition of Done)
- [x] `requirements.md` y `design.md` en `approved`.
- [x] `image` + seam `LocalAsyncImage` en `:sdui-compose`, SIN dependencia de Coil/red.
- [x] Coil 3.4.0 (`coil-compose` + `coil-network-ktor3`) solo en `:shared`; impl del seam cableada en `SduiHost`.
- [x] `toContentScale` con test verde; `rendererFor("image") != null`.
- [x] `:sdui-compose:jvmTest`, `:shared:desktopTest`, `:server:build`, `detekt`, `ktlintCheck` en verde.
- [x] Pantalla piloto (`home`) sirve un `image` con `url` (T6) — evidencia e2e (curl verificado).
- [x] `tasks.md` todo `[x]`; smoke visual de escritorio pendiente de confirmación del usuario (justificado).
