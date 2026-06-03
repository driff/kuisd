# Diseño — Imágenes locales (`ImageRegistry`)

> Spec ID: 013 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Un `ImageRegistry` en `:sdui-compose` (catálogo abierto `nombre → @Composable () -> Painter`, **misma
mecánica que `IconRegistry`**, 008) provisto por `LocalImageRegistry`. El componente `image` (012) gana
una prop `name`: el `ImageRenderer` decide local (registry → `foundation.Image(painter)`) vs remoto
(seam `LocalAsyncImageLoader`). El motor sigue **agnóstico**: no empaqueta assets; los `Painter` los
aporta la app vía Compose Multiplatform Resources. El valor del registry es un **factory `@Composable`**
porque `painterResource` es `@Composable`.

## Arquitectura
- **`:sdui-compose`** (`commonMain`):
  - `ImageRegistry.kt` (nuevo) — `ImageRegistry` (`Empty`/`plus`/`get`) + DSL `imageRegistry{}` +
    `LocalImageRegistry`. Espejo de `IconRegistry`.
  - `CorePack.kt` (editar) — `ImageProps` gana `name`; `ImageRenderer` ramifica local/remoto.
- **`:shared`** (`commonMain`):
  - Compose Resources: asset de demo en `src/commonMain/composeResources/drawable/` + dep
    `compose.components.resources`.
  - `image/AppImages.kt` (nuevo) — `appImageRegistry()` = `imageRegistry { register("kuisd_logo")
    { painterResource(Res.drawable.kuisd_logo) } }`.
  - `SduiHost.kt` (editar) — provee `LocalImageRegistry provides remember { appImageRegistry() }`.
- **`:server`** (editar, demo) — un `image(name="kuisd_logo")` en una pantalla piloto.

```
image node
 ├─ props.name no vacío ─► LocalImageRegistry.get(name)?.invoke() ─► foundation.Image(painter)   [local]
 │                          └─ name desconocido ─► Box(modifier) neutro (HU-1.5)
 └─ props.name vacío    ─► LocalAsyncImageLoader.current.Image(url, …)                            [remoto, 012]
```

## Componentes y contratos

### `ImageRegistry` (`:sdui-compose/ImageRegistry.kt`)
```kotlin
/** Catálogo abierto de imágenes locales (spec 013): nombre → factory `@Composable` de `Painter`. */
class ImageRegistry internal constructor(
    internal val byName: Map<String, @Composable () -> Painter>,
) {
    fun get(name: String): (@Composable () -> Painter)? = byName[name]
    operator fun plus(other: ImageRegistry): ImageRegistry = ImageRegistry(byName + other.byName)
    companion object { val Empty = ImageRegistry(emptyMap()) }
}

class ImageRegistryBuilder @PublishedApi internal constructor() {
    @PublishedApi internal val byName = mutableMapOf<String, @Composable () -> Painter>()
    fun register(name: String, painter: @Composable () -> Painter) { byName[name] = painter }
}

fun imageRegistry(block: ImageRegistryBuilder.() -> Unit): ImageRegistry =
    ImageRegistry(ImageRegistryBuilder().apply(block).byName.toMap())

/** Seam de SOLO LECTURA; la app provee el suyo por `SduiHost`. Default vacío (motor sin assets). */
val LocalImageRegistry: ProvidableCompositionLocal<ImageRegistry> =
    staticCompositionLocalOf { ImageRegistry.Empty }
```
- **Decisión:** valores `@Composable () -> Painter` (no `Painter` directo) → `painterResource` encaja sin
  crear painters fuera de composición. El builder solo **almacena** las lambdas (no es `@Composable`).

### `image` extendido (`:sdui-compose/CorePack.kt`)
```kotlin
@Serializable
data class ImageProps(
    val url: String = "",
    val name: String = "",            // imagen local en el ImageRegistry (013); tiene prioridad sobre url
    val contentScale: String = "fit",
    val contentDescription: String? = null,
)

@Composable
private fun RenderScope.ImageRenderer(p: ImageProps, baseModifier: Modifier) {
    val description = p.contentDescription?.let { bind(it) }?.ifEmpty { null }
    val scale = p.contentScale.toContentScale()
    val name = bind(p.name)
    if (name.isNotEmpty()) {                                   // local (HU-1.2/1.4)
        val painter = LocalImageRegistry.current.get(name)
        if (painter != null) {
            Image(painter = painter(), contentDescription = description, contentScale = scale, modifier = baseModifier)
        } else {
            Box(baseModifier)                                  // nombre desconocido → fallback neutro (HU-1.5)
        }
    } else {                                                   // remoto (012, HU-1.3)
        LocalAsyncImageLoader.current.Image(bind(p.url), description, scale, baseModifier)
    }
}
```
- **Decisión:** `name` (bindeado) gana sobre `url` (HU-1.4). Imports nuevos en `CorePack`:
  `androidx.compose.foundation.Image`, `androidx.compose.foundation.layout.Box`.

### Impl app (`:shared`)
```kotlin
// shared/src/commonMain/composeResources/drawable/kuisd_logo.xml  (vector drawable, asset de demo)
// → genera Res.drawable.kuisd_logo (paquete generado por el plugin compose, p.ej. kuisd.shared.generated.resources)

internal fun appImageRegistry(): ImageRegistry = imageRegistry {
    register("kuisd_logo") { painterResource(Res.drawable.kuisd_logo) }
}

// SduiHost (host-level), junto a `asyncImage`/`icons`:
val images = remember { appImageRegistry() }
CompositionLocalProvider(/* … */, LocalImageRegistry provides images) { … }
```
- **Decisión:** el registry se construye una vez (`remember`, no `@Composable`); el `painterResource`
  solo se evalúa cuando el renderer invoca el factory dentro de composición.

## Modelo de datos y estados
- Sin estado nuevo del motor. `ImageRegistry` inmutable (como `IconRegistry`).
- `ImageProps.name` reactivo vía `bind` (HU-3.3).

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| `compose.components.resources` | (del plugin compose) | `:shared` commonMain | `painterResource` + `Res` para el asset de demo |
> Solo en `:shared`. `:sdui-compose` NO añade dependencias (sigue agnóstico; `Painter`/`Image` son de
> Compose foundation, ya presente).

## Riesgos y mitigaciones
- **Setup de Compose Resources / paquete `Res` generado** → confirmar el paquete generado al compilar
  (`./gradlew :shared:generateComposeResClass` / build) y ajustar el import. Riesgo acotado a `:shared`.
- **Valores `@Composable` en un `Map`** → válido (igual que `ComponentRegistry` guarda
  `@Composable RenderScope.(P) -> Unit`); el builder no es `@Composable`, solo almacena las lambdas.
- **`name` desconocido / sin registry** → `Box(modifier)` neutro, sin crash (HU-1.5/3.2); cubierto por test.
- **Asset multiplataforma** → un vector drawable XML lo soportan android/ios/desktop vía Compose
  Resources; evita problemas de densidad de PNG.
- **Retrocompat 012** → `name` default `""` ⇒ `image` solo-`url` se comporta igual que antes (test).

## Estrategia de verificación
- **Unit `:sdui-compose` (commonTest):** `ImageRegistryTest` — `Empty.get` → null; `register` hace
  `get` no-null; `plus` combina y el `other` gana (override); el builder no invoca el factory (se asierta
  presencia, no el `Painter`). Espejo de `IconRegistryTest`.
- **Compilación:** `:sdui-compose:jvmTest` (registry + image extendido); `:shared:compileKotlinDesktop`
  (Compose Resources + `appImageRegistry` resuelven).
- **e2e `:server`:** `image(name="kuisd_logo")` en una pantalla piloto; `:server:build` + `curl` que el
  árbol emite `type:"image"` con `name`.
- **Smoke manual:** `:desktopApp:run` — el logo local se ve sin red; un `name` inexistente → hueco neutro.
- **Calidad:** `:sdui-compose:jvmTest`, `:shared:desktopTest`, `:server:build`, `detekt`, `ktlintCheck`.
