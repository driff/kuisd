# Tareas — Extraer el motor a un módulo Gradle `:sdui-compose`

> Spec ID: 006 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.
Refactor mecánico: los archivos del motor se mueven **byte-idénticos** salvo el único delta de
visibilidad (T4, `sduiLog`).

- [x] **T1** — Crear el módulo `:sdui-compose`: registrar `include(":sdui-compose")` en
  `settings.gradle.kts` y crear `sdui-compose/build.gradle.kts` (plugins kotlin-multiplatform +
  android.kotlin.multiplatform.library + compose.multiplatform + compose.compiler +
  kotlin-serialization; `androidLibrary { namespace = "dev.kuisd.sdui" }`; targets `jvm()`,
  `iosArm64()`, `iosSimulatorArm64()` **sin** `binaries.framework`; deps `api(:sdui-core)` +
  `api` de runtime/foundation/material3/ui; `commonTest` con `kotlin("test")`).
  - _ref:_ HU-1.1, HU-1.3, HU-1.4 · design §Build files previstos, §Estructura del nuevo módulo
  - _verif:_ `./gradlew :sdui-compose:tasks` lista el módulo (configura sin error).

- [x] **T2** — Mover los 6 archivos del motor de
  `shared/src/commonMain/kotlin/dev/kuisd/sdui/` a
  `sdui-compose/src/commonMain/kotlin/dev/kuisd/sdui/`: `RenderNode.kt`, `RenderScope.kt`,
  `ComponentRegistry.kt`, `CorePack.kt`, `SduiActionHandler.kt`, `Logging.kt` (paquete
  `dev.kuisd.sdui` sin cambios). Borrar el paquete `sdui/` ya vacío en `:shared`.
  - _ref:_ HU-1.1, HU-1.2 · design §Qué se MUEVE vs. qué se QUEDA
  - _verif:_ `./gradlew :sdui-compose:compileKotlinMetadata` compila el motor en su nuevo módulo.

- [x] **T3** — Mover el test del motor `ComponentRegistryTest.kt` de
  `shared/src/commonTest/.../sdui/` a `sdui-compose/src/commonTest/.../sdui/` (mismo paquete; accede
  a internals del motor). Los tests de app/nav (`ScreenUiStateTest`, `NavActionHandlerTest`,
  `NavBackStackTest`) **se quedan** en `:shared`.
  - _ref:_ HU-3.4 · design §Estructura del nuevo módulo
  - _verif:_ `./gradlew :sdui-compose:check` ejecuta `ComponentRegistryTest` en verde.

- [x] **T4** — Visibilidad (decisión del usuario): `sduiLog` **se queda `internal`** en
  `sdui-compose/.../Logging.kt` (lo usa el motor en el decode fallido de `ComponentRegistry`). La app
  deja de usarlo: se añade `internal fun appLog` en `dev.kuisd.app` (`dev/kuisd/app/Logging.kt`) y
  `NavActionHandler` (único uso de `sduiLog` en la app) pasa a `appLog`. Verificado que ningún otro
  `internal` del motor lo usa la app.
  - _ref:_ HU-3.3 · design §Visibilidad
  - _verif:_ `:shared:assemble` compila sin errores de visibilidad; `grep "sduiLog"` no aparece en
    `dev/kuisd/app/` (usa `appLog`).

- [x] **T5** — Wiring de `shared/build.gradle.kts`: en `commonMain` sustituir las deps de Compose
  directas + `:sdui-core` por `implementation(project(":sdui-compose"))`; mantener
  `implementation(project(":sdui-core"))` (la app usa core directamente) y las deps de Ktor +
  coroutines. **`implementation`** (no `api`) de `:sdui-compose`.
  - _ref:_ HU-3.1, decisión api/implementation · design §`shared/build.gradle.kts`
  - _verif:_ `./gradlew :shared:assemble` compila; los `import dev.kuisd.sdui.*` de la app resuelven
    contra `:sdui-compose` sin cambiar.

- [x] **T6** — Regla de dependencias forzada por Gradle (HU-2): confirmar que el grafo es
  `shared → sdui-compose → sdui-core` sin ciclo, y que el motor no referencia app ni Ktor.
  - _ref:_ HU-2.1, HU-2.2, HU-2.3 · design §Riesgos, §Estrategia de verificación
  - _verif:_ `grep -R "dev\.kuisd\.app" sdui-compose/src` y `grep -R "io\.ktor\." sdui-compose/src`
    → **sin resultados**. (Opcional: un import temporal de `dev.kuisd.app` en el motor **falla** la
    compilación — no se commitea.)

- [x] **T7** — Apps y server siguen compilando sin cambios de build propios: `:androidApp` y
  `:desktopApp` (vía `:shared`), `:server` (vía `:sdui-core`).
  - _ref:_ HU-3.1 · design §Arquitectura
  - _verif:_ `./gradlew :androidApp:assembleDebug :server:build` en verde (y `:desktopApp` compila).

- [x] **T8** — Publicación (HU-4): la convención `sdui-*` aplica a `:sdui-compose` →
  `dev.kuisd:sdui-compose`. Publicar a Maven local y comprobar artefactos por target.
  - _ref:_ HU-4.1, HU-4.2 · design §Publicación
  - _verif:_ `./gradlew :sdui-compose:publishToMavenLocal`; en `~/.m2/repository/dev/kuisd/` aparecen
    `sdui-compose`, `-android`, `-jvm`, `-iosarm64`, `-iossimulatorarm64`.

- [x] **T9** — Smoke del data-path sin cambio de comportamiento: `:server:run` + `curl` de
  `/screen/home|details|more` → envelopes idénticos (home con `navigate` a details; details con el
  `badge` de 004 y `navigate`/`navigateBack`; more con `navigateBack`). El smoke **visual** de la
  ventana desktop NO aplica a un refactor de empaquetado (el comportamiento no cambia) y requiere
  pantalla; el data-path queda cubierto por el server + los tests del motor en verde.
  - _ref:_ HU-3.2 · design §Estrategia de verificación
  - _verif:_ `curl http://localhost:8080/screen/{home,details,more}` devuelve los envelopes esperados.

- [x] **T10** — Calidad.
  - _ref:_ RNF · design §Estrategia de verificación
  - _verif:_ `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck`
    sin fallos.

## Verificación final (Definition of Done)
- [x] El motor (`dev.kuisd.sdui`: RenderNode/RenderScope/ComponentRegistry/CorePack/SduiActionHandler/
  Logging + su test) vive en `:sdui-compose`; el paquete `sdui/` ya no existe en `:shared`.
- [x] `:sdui-compose` depende solo de `:sdui-core` + Compose; `:shared` depende de `:sdui-compose`
  (`implementation`); sin ciclos. `grep` confirma que el motor no referencia `dev.kuisd.app` ni `io.ktor.`.
- [x] Único delta de visibilidad/log: `sduiLog` **se queda `internal`** en el motor; la app usa su
  propio `internal fun appLog` y `NavActionHandler` pasa a `appLog`; sin otros cambios de código del motor.
- [x] `:sdui-compose` es publicable como `dev.kuisd:sdui-compose` (artefactos por target en `~/.m2`).
- [x] Sin cambio de comportamiento: envelopes de `home`/`details`/`more` idénticos (data-path vía server),
  `badge` 004 presente en `details`; tests del motor y de la app en verde.
- [x] Todos los `:check`/`assemble`/`build`/`assembleDebug`/`detekt`/`ktlintCheck` en verde; `tasks.md`
  todo `[x]` (T9 visual N/A para refactor de empaquetado, ver nota en T9).
