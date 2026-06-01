# Tareas — Navegación por acciones + frontera Clean Architecture (motor puro / app)

> Spec ID: 003 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [ ] **T1** — Contrato: `data object NavigateBack : UiAction` (`@SerialName("navigateBack")`) en
  `:sdui-core/UiAction.kt`.
  - _ref:_ HU-5.1 · design §Contrato
  - _verif:_ `:sdui-core:check`; round-trip de `NavigateBack`; unknown type sigue → `NoOpAction`.

- [ ] **T2** — MOTOR: `dev.kuisd.sdui/SduiActionHandler.kt` (`fun interface SduiActionHandler` +
  `LocalSduiActionHandler` default no-op).
  - _ref:_ HU-1.2 · design §MOTOR·SduiActionHandler
  - _verif:_ `:shared:assemble`.

- [ ] **T3** — MOTOR: `RenderNode.kt` delega `node.actions["onClick"].orEmpty()` al
  `LocalSduiActionHandler` (quita el `sduiLog` del botón); `NodeProps`/`sduiLog` → `internal`. El motor
  no importa Ktor ni `dev.kuisd.app`.
  - _ref:_ HU-1.1, HU-1.4, HU-1.5 · design §MOTOR·RenderNode
  - _verif:_ `:shared:assemble`; `grep` no encuentra `io.ktor.`/`dev.kuisd.app` en `dev/kuisd/sdui/`.

- [ ] **T4** — DATA: crear `dev.kuisd.app.data` y **mover** `SduiClient.kt` y `SduiHttp.kt`(+actuals)
  ahí como `internal`. Añadir `ScreenSource` (interface, public) + `KtorScreenSource` (internal) +
  `HttpErrorMapper` (internal; mueve `toUserMessage`/`problemMessage` con los imports de Ktor).
  - _ref:_ HU-2 · design §DATA
  - _verif:_ `:shared:assemble` en los 3 targets (expect/actual ok).

- [ ] **T5** — APP: `dev.kuisd.app/ScreenUiState.kt` (sealed) + `SduiScreen.kt` (recibe `ScreenSource`,
  `produceState`, mapea error vía `HttpErrorMapper`, renderiza con `RenderNode`; **sin** Ktor ni
  `client.close()`). Eliminar el viejo `dev.kuisd.sdui/SduiScreen.kt`/`SduiUiState`.
  - _ref:_ HU-3 · design §APP·ScreenUiState/SduiScreen
  - _verif:_ `:shared:assemble`; la presentación no importa `io.ktor.`.

- [ ] **T6** — APP/nav: `dev.kuisd.app.nav/NavBackStack.kt` (`NavEntry` con `id`, snapshot-state, push/pop
  que respeta raíz) + `NavActionHandler.kt` (`internal`, implementa `SduiActionHandler`:
  Navigate→push, NavigateBack→pop, resto no-op+log) + tests.
  - _ref:_ HU-4.1 · design §APP·NavBackStack/NavActionHandler
  - _verif:_ tests: push/pop/raíz, ids distintos para misma route; Navigate apila, NavigateBack desapila,
    raíz no-op, acción no soportada / lista vacía no alteran la pila.

- [ ] **T7** — APP: `dev.kuisd.app/SduiHost.kt` (`@Composable`): posee `NavBackStack` + `KtorScreenSource`
  (ciclo de vida con `DisposableEffect`) + `NavActionHandler`; `Scaffold`+`TopAppBar` con
  `TextButton("‹ Atrás")` si `canGoBack`; `CompositionLocalProvider(LocalSduiActionHandler)` y
  `key(current.id) { SduiScreen(current.route, source, ...) }`.
  - _ref:_ HU-4.2 · design §APP·SduiHost
  - _verif:_ `:shared:assemble`; cubierto end-to-end por el smoke (T11).

- [ ] **T8** — APP: `presentation/PlaceholderApp.kt` monta `SduiHost("home")`.
  - _ref:_ HU-4 · design §Mapa de paquetes
  - _verif:_ `:androidApp:assembleDebug` y `:desktopApp` compilan.

- [ ] **T9** — SERVER: `screens/DetailsScreen.kt` ("Ver más"→`Navigate("more")`, "Atrás"→`NavigateBack`)
  y `screens/MoreScreen.kt` ("Atrás"→`NavigateBack`); registrar `details`/`more` en `defaultScreenRegistry()`.
  - _ref:_ HU-5.2 · design §SERVER
  - _verif:_ test `:server`: `/screen/details` y `/more` → 200 con su `screenId`; `/home` sigue 200.

- [ ] **T10** — `public`/`internal` según el mapa de diseño; comprobar regla de dependencias.
  - _ref:_ HU-1.3, HU-1.4, RNF · design §public/internal, §Capas
  - _verif:_ `grep` sin `io.ktor.` en `dev/kuisd/sdui/`; sin `androidx.compose.` en `dev/kuisd/app/data/`.

- [ ] **T11** — Smoke end-to-end de navegación: `:server:run` + `:desktopApp:run`; "Empezar"→`details`,
  "Ver más"→`more`, "‹ Atrás" sube hasta `home`, la barra desaparece en la raíz.
  - _ref:_ HU-3, HU-4 · design §Verificación
  - _verif:_ observación manual de la ventana desktop con el server arriba.

- [ ] **T12** — Calidad.
  - _ref:_ RNF · design §Verificación
  - _verif:_ `./gradlew :sdui-core:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` sin fallos.

## Verificación final (Definition of Done)
- [ ] Motor `dev.kuisd.sdui` puro: solo render + delega vía `SduiActionHandler`; **no importa Ktor ni `dev.kuisd.app`**.
- [ ] App `dev.kuisd.app` posee fetch (tras `ScreenSource`), estado y navegación (`NavBackStack`/`NavActionHandler`/`SduiHost`).
- [ ] Cadena `home→details→more` con "‹ Atrás" (y `NavigateBack` del server) funcional; raíz sin barra.
- [ ] `NavigateBack` en el contrato, forward-compatible (→`NoOpAction`).
- [ ] Acción no soportada / `onClick` vacío / ruta inexistente no crashean.
- [ ] API pública mínima (public/internal aplicado).
- [ ] Todos los `:check`/`build`/`assembleDebug`/`detekt`/`ktlintCheck` en verde; `tasks.md` todo `[x]`.
