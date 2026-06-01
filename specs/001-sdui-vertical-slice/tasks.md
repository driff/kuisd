# Tareas — Slice vertical SDUI (cliente ↔ servidor)

> Spec ID: 001 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Añadir dependencias de Ktor client a `shared/build.gradle.kts`
  (`ktor-client-core` + `kotlinx-coroutines-core` en commonMain; `okhttp`/`darwin`/`cio`
  en android/ios/desktopMain). Crear los source sets `desktopMain`/`iosMain` deps.
  - _ref:_ HU-4 · design §Dependencias
  - _verif:_ `./gradlew :shared:assemble` resuelve dependencias (compila).

- [x] **T2** — `SduiHttp.kt` (commonMain): `expect fun sduiHttpClient(): HttpClient`,
  `expect val defaultBaseUrl: String` y config compartida `sduiConfig()` con `HttpTimeout` (R9).
  Implementar los tres `actual` (`SduiHttp.android.kt` OkHttp+10.0.2.2, `SduiHttp.ios.kt`
  Darwin+localhost, `SduiHttp.desktop.kt` CIO+localhost).
  - _ref:_ HU-4.1, HU-4.2, HU-1.3 (R9) · design §SduiHttp
  - _verif:_ compila en los tres targets (`:shared:assemble`).

- [x] **T3** — `NodeProps.kt` (commonMain): helper `SduiNode.stringProp(key)`.
  - _ref:_ HU-2.2 · design §NodeProps
  - _verif:_ test unitario `stringProp` devuelve el valor para prop string y `null` si falta.

- [x] **T4** — `SduiClient.kt` (commonMain): `fetchScreen(screenId)` con `bodyAsText()` +
  `DefaultSduiJson.decodeFromString`.
  - _ref:_ HU-1.1, HU-1.2 · design §SduiClient
  - _verif:_ compila; cubierto end-to-end por el smoke (T8).

- [x] **T5** — Estado `SduiUiState` (sealed) + `SduiScreen.kt` (commonMain) con
  `produceState` y ramas Loading/Error/Content.
  - _ref:_ HU-1.3, HU-3 · design §Estado de UI, §SduiScreen
  - _verif:_ compila; test del modelo de estado; smoke muestra spinner→contenido.

- [x] **T6** — `RenderNode.kt` (commonMain): mapeo `column/row/text/button` + `UnknownNode`
  para tipos desconocidos; reconocer (log) `actions["onClick"]` en button.
  - _ref:_ HU-2.1, HU-2.3, HU-2.4 · design §RenderNode
  - _verif:_ compila; smoke renderiza el árbol de `home`; un type inventado muestra el marcador.

- [x] **T7** — Reescribir `presentation/PlaceholderApp.kt` para montar `SduiScreen("home")`
  (tras la 002, `PlaceholderApp` es el placeholder simple). Android: habilitar
  `usesCleartextTraffic` **solo en debug** (manifest de debug o `networkSecurityConfig` acotado a
  `10.0.2.2`), nunca global en release.
  - _ref:_ HU-2, HU-4.3 · design §Arquitectura, §Riesgos
  - _verif:_ `:androidApp:assembleDebug` y `:desktopApp` compilan.

- [ ] **T8** — Smoke end-to-end: `:server:run` + `:desktopApp:run` muestra "Bienvenido a kuisd"
  y el botón "Empezar" servidos por el BFF.
  - _ref:_ HU-1, HU-2, HU-3 · design §Verificación
  - _verif:_ observación manual de la ventana desktop con el server arriba.
  - _nota:_ pendiente verificación manual (requiere pantalla; no verificable en este entorno).
    El data-path SÍ está validado: con `:server:run` arriba, `curl localhost:8080/screen/home`
    devuelve el `SduiEnvelope` con `screenId=home`, el texto "Bienvenido a kuisd" y el botón
    "Empezar" que `RenderNode` mapea.

- [x] **T9** — Calidad: `detekt` + `ktlintCheck` (o `ktlintFormat`) en verde para `:shared`.
  - _ref:_ RNF · design §Verificación
  - _verif:_ `./gradlew :shared:assemble detekt ktlintCheck` sin fallos.

## Verificación final (Definition of Done)
- [ ] El cliente desktop renderiza la pantalla `home` obtenida del `:server` (no hardcode).
  _Pendiente verificación visual manual (T8); el data-path está validado vía curl._
- [x] Un nodo de tipo desconocido degrada a marcador sin crash. _(`RenderNode` rama `else → UnknownNode`)_
- [x] `:shared:assemble`, `:androidApp:assembleDebug`, `detekt` y `ktlintCheck` en verde.
- [ ] `tasks.md` con todas las tareas marcadas `[x]`. _T8 queda `[ ]` (verificación manual pendiente)._
