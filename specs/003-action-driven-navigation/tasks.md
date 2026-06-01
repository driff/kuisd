# Tareas — Navegación dirigida por acciones (Navigate)

> Spec ID: 003 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Contrato `:sdui-core`: añadir `data object NavigateBack : UiAction`
  (`@SerialName("navigateBack")`) en `UiAction.kt`.
  - _ref:_ HU-2.1, HU-3.4 · design §Contrato
  - _verif:_ `:sdui-core:check`; test de round-trip de `NavigateBack`; un `type` desconocido sigue → `NoOpAction`.

- [x] **T2** — `NavBackStack.kt` (commonMain): `NavEntry(route, args)` + `NavBackStack`
  (`entries` snapshot-state, `current`, `canGoBack`, `push`, `pop` que no desapila la raíz).
  - _ref:_ HU-2, HU-4.1 · design §NavBackStack
  - _verif:_ tests: `push` cambia `current`; `pop` en pila de 1 → `false` y no vacía; `canGoBack == (size>1)`.

- [x] **T3** — `ActionDispatcher.kt` (commonMain): `fun interface ActionDispatcher`,
  `NavigationActionDispatcher` (`Navigate`→push, `NavigateBack`→pop, resto no-op+log), y
  `LocalActionDispatcher` (`staticCompositionLocalOf` con default no-op).
  - _ref:_ HU-1.1, HU-2.1, HU-3.1 · design §ActionDispatcher
  - _verif:_ tests: `Navigate` apila; `NavigateBack` desapila (y no-op en raíz); acción no soportada y `emptyList()` no alteran la pila.

- [x] **T4** — `SduiScreen.kt` (commonMain): recibir `client: SduiClient` (provisto por el host) y
  `modifier: Modifier = Modifier`; **quitar** la creación/cierre propio del `SduiClient`
  (`DisposableEffect.onDispose { client.close() }`). Lógica de estados sin cambios.
  - _ref:_ HU-4.2 · design §SduiScreen
  - _verif:_ `:shared:assemble`; el cliente compartido no se cierra al navegar.

- [x] **T5** — `SduiHost.kt` (commonMain): `@Composable SduiHost(startRoute, modifier)` que posee
  `NavBackStack`, `NavigationActionDispatcher` y **un `SduiClient` único** (creado + cerrado vía
  `DisposableEffect`); `Scaffold` + `TopAppBar` con `TextButton("‹ Atrás")` cuando `canGoBack`;
  `CompositionLocalProvider(LocalActionDispatcher)` y `key(current.route) { SduiScreen(client=...) }`.
  - _ref:_ HU-1.3, HU-2.2, HU-2.3, HU-4.2 · design §SduiHost
  - _verif:_ `:shared:assemble`; cubierto end-to-end por el smoke (T8).

- [x] **T6** — `RenderNode.kt` (commonMain): la rama `button` lee `LocalActionDispatcher.current` y en
  `onClick` despacha `node.actions["onClick"].orEmpty()` (quita el `sduiLog`). Resto sin cambios.
  - _ref:_ HU-1.2, HU-3.2 · design §RenderNode
  - _verif:_ `:shared:assemble`; `onClick` sin acciones es no-op.

- [x] **T7** — `presentation/PlaceholderApp.kt` (commonMain): montar `SduiHost(startRoute = "home")`
  en vez de `SduiScreen("home")`.
  - _ref:_ HU-1 · design §Arquitectura
  - _verif:_ `:androidApp:assembleDebug` y `:desktopApp` compilan.

- [x] **T8** — Server: `screens/DetailsScreen.kt` (botón "Ver más"→`Navigate("more")`, "Atrás"→`NavigateBack`)
  y `screens/MoreScreen.kt` (botón "Atrás"→`NavigateBack`); registrar `"details"` y `"more"` en
  `defaultScreenRegistry()`.
  - _ref:_ HU-1, HU-2 · design §Server
  - _verif:_ test `:server`: `GET /screen/details` y `/screen/more` → 200 con su `screenId`; `/screen/home` sigue 200.

- [ ] **T9** — Smoke end-to-end de navegación: `:server:run` + `:desktopApp:run`; "Empezar"→`details`,
  "Ver más"→`more`, "Atrás" sube en la pila hasta `home`, la barra desaparece en la raíz.
  - _ref:_ HU-1, HU-2, HU-4 · design §Verificación
  - _verif:_ observación manual de la ventana desktop con el server arriba.
  - _NOTA:_ pendiente verificación manual (no verificable sin pantalla). El data-path
    (`/screen/details` y `/screen/more` vía curl → `SduiEnvelope` con `Navigate`/`NavigateBack`)
    SÍ queda validado automáticamente.

- [x] **T10** — Calidad.
  - _ref:_ RNF · design §Verificación
  - _verif:_ `./gradlew :sdui-core:check :shared:assemble :server:build :androidApp:assembleDebug detekt ktlintCheck` sin fallos.

## Verificación final (Definition of Done)
- [ ] Pulsar "Empezar" carga `details` del BFF; "Ver más" carga `more` (cadena de 3).
- [ ] `NavigateBack` (y la barra "atrás") vuelven a la pantalla anterior; en la raíz son no-op y la barra se oculta.
- [ ] `NavigateBack` existe en el contrato y degrada a `NoOpAction` en clientes antiguos (forward-compat).
- [ ] Una acción no soportada, un `onClick` vacío y una ruta inexistente no provocan crash.
- [ ] El server sirve `/screen/details` y `/screen/more` (200) además de `/screen/home`.
- [ ] Un único `SduiClient` por sesión de host (no se abre/cierra el engine por navegación).
- [ ] `:sdui-core:check`, `:shared:assemble`, `:server:build`, `:androidApp:assembleDebug`, `detekt`, `ktlintCheck` en verde.
- [ ] `tasks.md` con todas las tareas marcadas `[x]`.
