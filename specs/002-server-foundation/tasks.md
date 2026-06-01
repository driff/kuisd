# Tareas — Endurecimiento de la fundación del servidor

> Spec ID: 002 · Trazabilidad: ./requirements.md · ./design.md
> Orden de implementación: antes de la spec 001.

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Añadir al catálogo (`libs.versions.toml`) los artefactos
  `ktor-server-call-id`, `ktor-server-compression`, `ktor-server-default-headers`; referenciarlos
  en `server/build.gradle.kts`.
  - _ref:_ HU-6 · design §Dependencias
  - _verif:_ `./gradlew :server:dependencies` resuelve; `:server` compila.

- [x] **T2** — `ProblemDetail` en `:sdui-core` (`ProblemDetail.kt`) + constante
  `KUISD_VERSION_HEADER` (`"X-Kuisd-Version"`) junto al contrato. Test de round-trip de `ProblemDetail`.
  - _ref:_ HU-2.1, HU-5.1 · design §ProblemDetail
  - _verif:_ `:sdui-core:check` verde (incluye nuevo test).

- [x] **T3** — Quitar `SampleScreens.kt` de `:sdui-core`; actualizar `SduiContractTest` para
  construir su `SduiEnvelope` inline; revertir `:shared` `PlaceholderApp.kt` al placeholder simple
  (sin `SampleScreens`).
  - _ref:_ HU-1 · design §:sdui-core, §:shared
  - _verif:_ `:sdui-core:check` y `:shared:assemble` verdes; `grep` no encuentra `SampleScreens` en `:sdui-core`/`:shared`.

- [x] **T4** — `screens/`: `ScreenContext.kt` (`ClientCapability`, `ScreenContext`),
  `ScreenRegistry.kt` (`ScreenBuilder`, `ScreenRegistry`, `defaultScreenRegistry()`),
  `HomeScreen.kt` (árbol migrado de `SampleScreens.home()`).
  - _ref:_ HU-1.1, HU-4.2, HU-5 · design §ScreenRegistry, §HomeScreen
  - _verif:_ `:server` compila; `home` produce el mismo árbol (column/text/button).

- [x] **T5** — `plugins/`: `Serialization.kt`, `Monitoring.kt` (CallLogging+CallId),
  `Http.kt` (CORS restringido + Compression + DefaultHeaders), `ErrorHandling.kt`
  (StatusPages → `ProblemDetail`, sin fuga, log con callId).
  - _ref:_ HU-2, HU-3, HU-6 · design §Http, §Monitoring, §ErrorHandling
  - _verif:_ `:server` compila.

- [x] **T6** — `routing/`: `HealthRoutes.kt`, `ScreenRoutes.kt` (lee `KUISD_VERSION_HEADER`,
  usa `ScreenRegistry`, 404 vía `ScreenNotFoundException`). Reescribir `Application.kt` para
  solo cablear plugins + routing.
  - _ref:_ HU-4.1, HU-4.3, HU-5.2 · design §:server estructura, §ScreenRoutes
  - _verif:_ `:server` compila; estructura en `plugins/`/`routing/`/`screens/`.

- [x] **T7** — Actualizar `ApplicationTest`: health 200; `/screen/home` 200 decodifica envelope;
  `/screen/missing` 404 con `ProblemDetail` (`application/problem+json`); presencia de header de
  correlación; `X-Kuisd-Version: 1` no rompe.
  - _ref:_ HU-2, HU-5, HU-6 · design §Verificación
  - _verif:_ `:server:build` verde (todos los tests).

- [x] **T8** — Calidad global.
  - _ref:_ RNF · design §Verificación
  - _verif:_ `./gradlew :sdui-core:check :server:build :shared:assemble detekt ktlintCheck` verde.

## Verificación final (Definition of Done)
- [x] `:sdui-core` ya no contiene `SampleScreens`; sus tests pasan con envelope inline.
- [x] Errores responden `ProblemDetail` JSON sin filtrar `cause.message` (salvo `KUISD_DEV=true`).
- [x] `:server` estructurado en `plugins/`/`routing/`/`screens/` con `ScreenRegistry`.
- [x] Header `X-Kuisd-Version` se lee en `/screen/{id}`.
- [x] Compression + DefaultHeaders + CallId activos.
- [x] `detekt`, `ktlintCheck` y todos los `:check`/`build` verdes; `tasks.md` con todo en `[x]`.
