# Diseño — Extraer el motor a un módulo Gradle `:sdui-compose`

> Spec ID: 006 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Crear un nuevo módulo Gradle `:sdui-compose` (lib KMP + Compose, espejo de targets de `:shared`) y
**mover ahí, sin tocar la lógica**, los 6 archivos del motor que hoy viven en
`shared/src/commonMain/kotlin/dev/kuisd/sdui/` (más el test del motor). El paquete `dev.kuisd.sdui`
se **conserva** (solo cambia de módulo). `:sdui-compose` declara `api(project(":sdui-core"))` + Compose;
`:shared` pasa a depender de `:sdui-compose`. Con eso, la frontera de la spec 003 (motor depende solo de
core + Compose; PROHIBIDO Ktor y app) deja de ser una convención de paquetes y pasa a estar **forzada por
el compilador**: el motor ya no tiene `dev.kuisd.app` ni `io.ktor.*` en su classpath. La única fricción es
la **visibilidad**: `sduiLog` es `internal` y la app lo usa hoy a través de la frontera de paquetes; al
cruzar a otro módulo hay que resolverlo. **Decisión del usuario:** `sduiLog` se **queda `internal`** en
`:sdui-compose` (lo usa el motor, p.ej. el decode fallido de `ComponentRegistry`); la app **no** lo usa —
se crea un logger propio de la app `internal fun appLog(message: String)` en `dev.kuisd.app`
(`dev/kuisd/app/Logging.kt`) y `NavActionHandler` pasa a usar `appLog`. Es un refactor mecánico: HU-3
exige render/navegación idénticos.

## Arquitectura

### Grafo de módulos (después)
```
:sdui-core   (dev.kuisd.sdui.core)   contrato @Serializable — sin deps de cliente · publicable
      ▲ api
:sdui-compose (dev.kuisd.sdui)       MOTOR Compose — api(:sdui-core) + Compose · publicable  ← NUEVO
      ▲ implementation
:shared      (dev.kuisd.app*, presentation/)   APP — depende de :sdui-compose (+ :sdui-core, Ktor)
      ▲ implementation
:androidApp / :desktopApp            consumen :shared (sin cambios)

:server  → implementation(:sdui-core)   (sin cambios; no ve el motor Compose)
```
No hay ciclos: `:shared` → `:sdui-compose` → `:sdui-core`. El motor **no** puede ver `:shared`.

### Estructura del nuevo módulo `:sdui-compose/`
```
sdui-compose/
├── build.gradle.kts                 (ver §Build files previstos)
└── src/
    ├── commonMain/kotlin/dev/kuisd/sdui/
    │   ├── RenderNode.kt            ← movido desde shared (public RenderNode + internal UnknownNode)
    │   ├── RenderScope.kt           ← movido  (public RenderScope, ctor internal)
    │   ├── ComponentRegistry.kt     ← movido  (RegisteredComponent, ComponentRegistry, builder, DSL, LocalComponentRegistry)
    │   ├── CorePack.kt              ← movido  (ColumnProps/RowProps/TextProps/ButtonProps + CorePack)
    │   ├── SduiActionHandler.kt     ← movido  (fun interface + LocalSduiActionHandler — SEAM)
    │   └── Logging.kt               ← movido  (sduiLog; ver §Visibilidad — se queda internal)
    └── commonTest/kotlin/dev/kuisd/sdui/
        └── ComponentRegistryTest.kt ← movido desde shared/commonTest (mismo paquete, accede a internals del motor)
```
No hay `androidMain`/`iosMain`/`desktopMain` propios en el motor: todo su código es `commonMain` +
Compose multiplatform (no usa `expect/actual`). El `androidLibrary { namespace = "dev.kuisd.sdui" }`
genera el target Android.

### Qué se MUEVE vs. qué se QUEDA
**Se mueve a `:sdui-compose` (de `shared/src/commonMain/.../dev/kuisd/sdui/`):**
`RenderNode.kt`, `RenderScope.kt`, `ComponentRegistry.kt`, `CorePack.kt`, `SduiActionHandler.kt`,
`Logging.kt` + el test `commonTest/.../sdui/ComponentRegistryTest.kt`.

**Se queda en `:shared` (todo `dev.kuisd.app*`, `presentation/`, `MainViewController`):**
- `dev.kuisd.app`: `SduiHost.kt`, `SduiScreen.kt`, `ScreenUiState.kt`
- `dev.kuisd.app.components`: `AppComponents.kt`, `BadgeComponent.kt` (el `badge` de la spec 004 — es
  catálogo de la **app**, no del motor; registra contra `ComponentRegistry`/`CorePack` de `:sdui-compose`)
- `dev.kuisd.app.data`: `ScreenSource`, `KtorScreenSource`, `SduiClient`, `SduiHttp(.kt + actuals)`,
  `HttpErrorMapper`
- `dev.kuisd.app`: `Logging.kt` (`internal fun appLog` — logger propio de la app, ver §Visibilidad)
- `dev.kuisd.app.nav`: `NavBackStack`, `NavActionHandler` (consume `SduiActionHandler` + `appLog`)
- `dev.kuisd.presentation.PlaceholderApp`, `dev.kuisd.Placeholder`, `iosMain/MainViewController.kt`
- Tests de app/nav: `ScreenUiStateTest`, `NavActionHandlerTest`, `NavBackStackTest` (se quedan).

El paquete `dev.kuisd.sdui` **desaparece de `:shared`** (queda vacío y se borra); los `import
dev.kuisd.sdui.*` de la app **no cambian** porque el paquete es idéntico, solo proviene de otro módulo.

## Build files previstos

### `sdui-compose/build.gradle.kts` (nuevo) — HU-1.3, HU-1.4, HU-4
Espejo de `:shared` pero **sin** `binaries.framework` y con deps de motor (`api(:sdui-core)` + Compose):
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)   // CorePack props @Serializable
}

kotlin {
    jvmToolchain(25)

    androidLibrary {
        namespace = "dev.kuisd.sdui"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        withHostTestBuilder {}
    }

    jvm()   // consumido por shared(desktop) — resolución KMP por atributo de plataforma (jvm)

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { /* lib intermedia: SIN binaries.framework propio (como :sdui-core) */ }

    sourceSets {
        commonMain.dependencies {
            api(project(":sdui-core"))            // re-expone contrato + kotlinx-serialization (HU-1.3)
            api(libs.runtime)                     // los renderers públicos exponen tipos Compose (@Composable)
            api(libs.foundation)
            api(libs.material3)
            api(libs.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```
- **`api(:sdui-core)`**: el motor expone tipos de core en su superficie pública (`SduiNode`,
  `SduiComponent<P>`, `DefaultSduiJson`), igual que hoy en `:shared`. Debe ser transitivo.
- **`api` para Compose**: la API pública del motor (`RenderNode`, `RenderScope.renderChildren`,
  `RegisteredComponent.renderer: @Composable RenderScope.(P) -> Unit`, los `Local*`) está marcada
  `@Composable` y referencia tipos Compose. Para que `:shared` pueda escribir renderers (`AppComponents`)
  y proveer los `Local*`, necesita esos tipos en su API → `api`, no `implementation`. (`:sdui-core`
  usa el mismo criterio con kotlinx-serialization.)
- **`jvm()`** (sin nombre, como `:sdui-core`): `:shared` tiene `jvm("desktop")`; la resolución KMP es
  por atributo de plataforma (jvm), no por nombre de target, así que `jvm()` de `:sdui-compose` cubre
  `desktopMain` de `:shared`.

### `settings.gradle.kts` — HU-1.1
Añadir `include(":sdui-compose")` junto a los demás `include`. (El módulo hereda la convención de
publicación por el prefijo `sdui-`; no se toca el `build.gradle.kts` raíz.)

### `shared/build.gradle.kts` — HU-3.1, decisión api/implementation
En `commonMain.dependencies`, **sustituir** `implementation(project(":sdui-core"))` por
`implementation(project(":sdui-compose"))` y **quitar** las 4 deps directas de Compose
(`runtime`/`foundation`/`material3`/`ui`), que ahora llegan transitivamente vía `api` de
`:sdui-compose`. `:sdui-core` también llega transitivo (vía `api` del motor), pero **se mantiene
`implementation(project(":sdui-core"))` explícito** en `:shared` porque la app (`data`, `nav`) usa
tipos de core directamente (`SduiEnvelope`, `UiAction`, `Navigate`…) — dependencia directa = declarada.
Se conservan las deps de Ktor (`ktor-client-*`) y `kotlinx-coroutines-core`. El plugin
`kotlin-serialization` puede **mantenerse** en `:shared` (lo usa el `badge`/004 si tiene props
`@Serializable`) o evaluarse quitar; por seguridad se mantiene (no rompe nada).

```kotlin
// shared/build.gradle.kts — commonMain (resultante)
commonMain.dependencies {
    implementation(project(":sdui-compose"))    // motor (trae core + Compose transitivos por api)
    implementation(project(":sdui-core"))       // la app usa tipos de core directamente
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)
}
```
- **Decisión `implementation` vs `api` de `:sdui-compose` en `:shared`:** **`implementation`**.
  `:androidApp`/`:desktopApp` consumen `:shared` para **arrancar la app** (montan `App()` /
  `PlaceholderApp`), no para escribir componentes SDUI ni llamar a `RenderNode` directamente; no
  necesitan los tipos del motor en su classpath de compilación. Si más adelante una app host
  necesitara registrar componentes propios fuera de `:shared`, se promovería a `api` entonces (YAGNI).

`:androidApp`/`:desktopApp`/`:server` **no cambian** (siguen con `project(":shared")` /
`project(":sdui-core")` respectivamente).

## Visibilidad (frontera de módulo) — HU-3.3
`internal` es **a nivel de módulo Gradle**. Al mover el motor, lo `internal` del motor deja de verse
desde `:shared`. Auditoría de los símbolos `internal` del motor y de quién los usa:

| Símbolo (motor) | Visib. hoy | ¿Usado por la app? | Acción en 006 |
|---|---|---|---|
| `sduiLog(message)` (`Logging.kt`) | `internal` | El motor sí (decode fallido en `ComponentRegistry`); la app NO | **Se queda `internal`** en `:sdui-compose`. La app usa su propio `internal fun appLog` (`dev.kuisd.app.Logging.kt`) y `NavActionHandler` pasa a `appLog`. Decisión del usuario. |
| `UnknownNode(type)` (`RenderNode.kt`) | `internal` | No (solo lo usa el motor) | Sigue `internal` (ahora interno al módulo motor). OK. |
| `RenderScope` ctor (`internal constructor`) | `internal` | No (la app no instancia `RenderScope`; solo lo recibe como receiver) | Sigue `internal`. OK. |
| `ComponentRegistry` ctor (`internal constructor`) | `internal` | No (la app crea registros vía `componentRegistry { }` y `plus`, ambos públicos) | Sigue `internal`. OK. |
| `ComponentRegistryBuilder.entries` (`@PublishedApi internal`) | publishedApi | No directamente | Sigue igual. OK. |

**Único cambio necesario (decisión del usuario):** `sduiLog` **se queda `internal`** en `:sdui-compose`
(lo sigue usando el motor en el decode fallido de `ComponentRegistry`). La app deja de depender de él: se
añade `internal fun appLog(message: String)` en `dev.kuisd.app` (`dev/kuisd/app/Logging.kt`) y
`NavActionHandler` (y cualquier otro uso de `sduiLog` en `dev.kuisd.app`) pasa a `appLog`. Así el motor no
expone API de log público y cada capa tiene su propio seam de log; cuando llegue el logger estructurado se
sustituyen ambos. Marcarlo en `tasks` como el **único delta de visibilidad/wiring de log**.

El **seam del motor ya es público** por diseño (003/004): `RenderNode`, `RenderScope`,
`ComponentRegistry`/`RegisteredComponent`/`componentRegistry`, `CorePack`, `LocalComponentRegistry`,
`SduiActionHandler`/`LocalSduiActionHandler`, las props de `CorePack`. No requieren cambios.

## Publicación — HU-4
La convención `subprojects { if (!name.startsWith("sdui-")) return@subprojects … }` del
`build.gradle.kts` raíz aplica **automáticamente** a `:sdui-compose` (prefijo `sdui-`): le pone
`group = "dev.kuisd"`, `version = -Pkuisd.version ?: "0.1.0"`, aplica `maven-publish` y el POM común.
Como es un módulo `kotlin.multiplatform`, **las publicaciones por target se generan solas** (no hace
falta el bloque `withId("org.jetbrains.kotlin.jvm")`, que es solo para JVM puro como el futuro
`:sdui-ktor`). `publishToMavenLocal` produce:
- `dev.kuisd:sdui-compose:<v>` — publicación raíz `kotlinMultiplatform` (módulo Gradle metadata).
- `dev.kuisd:sdui-compose-android:<v>`
- `dev.kuisd:sdui-compose-jvm:<v>`
- `dev.kuisd:sdui-compose-iosarm64:<v>` y `-iossimulatorarm64:<v>`

**Nota de re-publicación:** publicar `:sdui-compose` no obliga a re-publicar `:sdui-core`, pero como
`:sdui-compose` lo declara `api`, su POM lista `dev.kuisd:sdui-core` como dependencia transitiva — un
consumidor externo necesita ambos artefactos en su repo. Para un release coordinado se publican juntos
(`./gradlew :sdui-core:publishToMavenLocal :sdui-compose:publishToMavenLocal`).

## Riesgos y mitigaciones
- **Ciclo de dependencias** (`:shared` ↔ `:sdui-compose`) → no ocurre: el motor no importa nada de la
  app; el grafo es estrictamente `shared → sdui-compose → sdui-core`. Verificar con `grep` (HU-2.3).
- **Visibilidad `internal` rota** (`sduiLog`) → detectada arriba; `sduiLog` se queda `internal` y la app
  usa su propio `appLog` (único delta de log).
  Riesgo de que aparezca otro `internal` no listado → la compilación de `:shared` lo delatará
  inmediatamente (red de seguridad del propio refactor).
- **Compose Multiplatform en un módulo lib** → soportado (plugins `compose.multiplatform` +
  `compose.compiler`); `:sdui-core` ya valida el patrón KMP lib sin framework, y `:shared` ya valida
  Compose en lib. `:sdui-compose` combina ambos. Sin `binaries.framework` (lib intermedia).
- **`expect/actual`** → **no aplica**: el motor es 100 % `commonMain` + Compose (los `expect/actual` de
  HTTP viven en `dev.kuisd.app.data`, que se queda en `:shared`).
- **Targets desalineados** (resolución KMP) → `:sdui-compose` declara exactamente los 4 targets de
  `:shared` (android, jvm, iosArm64, iosSimulatorArm64); `jvm()` sin nombre cubre `jvm("desktop")` por
  atributo de plataforma.
- **Tiempos de build** → un módulo más = un poco más de configuración/grafo, pero **mejor
  incrementalidad**: tocar la app no recompila el motor y viceversa. Neto previsto: neutro o mejor.
- **Cambio de comportamiento** → el riesgo principal del refactor. Mitigación: mover archivos
  **byte-idénticos** (salvo el `internal`→`public` de `sduiLog`), los tests del motor migran y siguen
  verdes, y el smoke de las apps confirma render/navegación idénticos (HU-3.2).
- **Choque de merge con spec 005** (paralela) → ambas tocan `settings.gradle.kts` /
  `shared/build.gradle.kts`. Mitigación: cambios mínimos y localizados; resolver en el merge.

## Estrategia de verificación
- **Compilación del módulo motor:**
  `./gradlew :sdui-compose:compileKotlinMetadata` y `:sdui-compose:assemble` (compila common + todos
  los targets).
- **Tests del motor migrados:** `./gradlew :sdui-compose:check` (el `ComponentRegistryTest` pasa en el
  nuevo módulo).
- **App + resto compilan:** `./gradlew :shared:assemble :server:build :androidApp:assembleDebug`
  (y `:desktopApp` compila).
- **Regla de dependencias forzada (HU-2):**
  - `grep -R "dev\.kuisd\.app" sdui-compose/src` → **sin resultados**.
  - `grep -R "io\.ktor\." sdui-compose/src` → **sin resultados**.
  - (Prueba negativa opcional, no se commitea: añadir un `import dev.kuisd.app.X` temporal en el motor
    debe **fallar** `:sdui-compose:compileKotlinMetadata`.)
- **Publicación (HU-4):** `./gradlew :sdui-compose:publishToMavenLocal` y comprobar en
  `~/.m2/repository/dev/kuisd/` los artefactos `sdui-compose`, `sdui-compose-android`,
  `sdui-compose-jvm`, `sdui-compose-iosarm64`, `sdui-compose-iossimulatorarm64`.
- **Smoke (sin cambio de comportamiento):** `:server:run` + `:desktopApp:run` → la pantalla `home`
  renderiza igual que antes; la navegación de la spec 003 funciona; el `badge` de la spec 004 se ve en
  `details`. (Requiere pantalla; el data-path ya está cubierto por los tests de `:server`.)
- **Calidad:**
  `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck`
  en verde.
