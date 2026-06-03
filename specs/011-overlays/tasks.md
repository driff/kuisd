# Tareas — Overlays (`dialog` · `bottomSheet` · `snackbar`)

> Spec ID: 011 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — `:sdui-core`: añadir al `sealed interface UiAction` (`UiAction.kt`) las acciones
  `ShowDialog`, `ShowBottomSheet(content: List<SduiNode>, onDismiss)`, `ShowSnackbar(message,
  messageVar?, actionLabel?, onAction, duration="short")` y `DismissOverlay` (`data object`), con sus
  `@SerialName`. Test `OverlayActionsWireTest` (round-trip JSON por el polimorfismo, incl. `content`
  anidado y el `data object`).
  - _ref:_ HU-1.1, HU-2.1, HU-3.1, HU-4.2 · design §"Acciones"
  - _verif:_ `./gradlew :sdui-core:check` en verde (incluye el wire-test).

- [ ] **T2** — `:shared`: `OverlayController.kt` (estado `dialog`/`sheet` + `showDialog`/`showSheet`/
  `dismissAll`) y los helpers puros `resolveSnackbarMessage`, `JsonElement.asPlainString`,
  `String.toSnackbarDuration`. Más `OverlayActionHandler` (`SubHandler`: `supports` + `handle` que muta
  el controller y lanza el snackbar en `scope`, con `dispatch` para `onAction`).
  - _ref:_ HU-1.2, HU-2.2, HU-3.3/3.4, HU-4.1/4.2 · design §"OverlayController", §"Resolución pura", §"OverlayActionHandler"
  - _verif:_ `./gradlew :shared:compileKotlinJvm`; cubierto por los tests de T4.

- [ ] **T3** — `:shared`: `OverlayHost` (`@OptIn` acotado: `AlertDialog` + `ModalBottomSheet` con
  `RenderNode(content)` + `SnackbarHost`) y cableado en `SduiHost.kt` (crear `overlay`/`snackbarHostState`/
  `overlayHandler` dentro de `key(current.id)`, añadir el 5º sub-handler al `AppActionHandler`, fijar
  `overlayHandler.dispatch = handler::handle`, envolver `SduiScreen` + `OverlayHost` en un `Box` que
  recibe el `padding`).
  - _ref:_ HU-1.2/1.3/1.4, HU-2.2/2.3, HU-3.2/3.5, HU-4.1/4.3 · design §"OverlayHost", §"Cableado en SduiHost"
  - _verif:_ `./gradlew :shared:compileKotlinJvm` (y `:desktopApp:compile*` si aplica) en verde.

- [ ] **T4** — `:shared` tests (commonTest): `OverlayControllerTest` (showDialog limpia sheet y
  viceversa; dismissAll), `OverlaySnackbarTest` (`resolveSnackbarMessage` con messageVar/literal/
  no-primitivo; `toSnackbarDuration`), `OverlayActionHandlerTest` (`supports` de las 4; mutaciones del
  controller), y ampliar `AppActionHandlerTest` (una acción overlay NO cae a no-op).
  - _ref:_ HU-1.6/2.5, HU-3.4, HU-4.1 · design §"Estrategia de verificación"
  - _verif:_ `./gradlew :shared:jvmTest` en verde (incluye los nuevos tests).

- [ ] **T5** — `:server`: pantalla piloto. En una pantalla existente (candidata: `form` o `home`) añadir
  un botón que despache `ShowDialog` cuyo `onConfirm` lance `ShowSnackbar`, y otro botón que despache
  `ShowBottomSheet` con un pequeño `content` (p.ej. `column` con `text`).
  - _ref:_ HU-1, HU-2, HU-3 · design §"Estrategia de verificación" (e2e)
  - _verif:_ `./gradlew :server:build`; `curl localhost:8080/screen/<piloto>` muestra los nodos con las
    acciones `showDialog`/`showBottomSheet`/`showSnackbar` en el JSON.

- [ ] **T6** — Calidad + smoke visual: `./gradlew detekt ktlintCheck` en verde; `:desktopApp:run` y
  observar: abrir diálogo → confirmar → snackbar; abrir hoja → descartar.
  - _ref:_ Requisitos no funcionales · design §"Estrategia de verificación"
  - _verif:_ lint/detekt verdes; smoke manual (si el harness no abre UI, se justifica con el árbol del
    piloto T5 + los tests como evidencia).

## Verificación final (Definition of Done)
- [ ] `requirements.md` y `design.md` en `approved`.
- [ ] 4 acciones nuevas en `:sdui-core` con wire-test verde; `:sdui-compose` SIN cambios.
- [ ] `OverlayController`/`OverlayActionHandler`/helpers con tests verdes; `AppActionHandler` compone 5
  sub-handlers sin romper acciones existentes.
- [ ] `SduiHost` renderiza overlays + `SnackbarHost`; overlays se descartan al navegar.
- [ ] `:sdui-core:check`, `:shared:jvmTest`, `:server:build`, `detekt`, `ktlintCheck` en verde.
- [ ] Pantalla piloto sirve las acciones de overlay (T5) — evidencia e2e.
- [ ] `tasks.md` todo `[x]` salvo smoke visual manual si el harness no abre UI (justificado).
