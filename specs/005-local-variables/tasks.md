# Tareas — Variables locales reactivas (estrategia "variables locales")

> Spec ID: 005 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T0** — Aprobar `requirements.md` y `design.md` (cabecera `Estado: approved`).

- [x] **T1** — MOTOR `dev.kuisd.sdui/Variables.kt`: `fun interface VariableScope { fun get(name): JsonElement? }`
  + `LocalVariables` (`staticCompositionLocalOf`, default `VariableScope { null }`).
  - _ref:_ HU-1.1, HU-1.2 · design §MOTOR·VariableScope
  - _verif:_ `:sdui-compose:check` verde; `grep` no encuentra `io.ktor.`/`dev.kuisd.app`/`mutableStateOf` en `:sdui-compose`.

- [x] **T2** — MOTOR `RenderScope.kt`: añadir `@Composable fun bind(raw: String?): String` con la regla
  (null→""; `$name`→variable o ""; literal→literal; `$$`→`$`) leyendo `LocalVariables.current`. Extraída a
  función pura `resolveBinding(raw, lookup)` para testear sin UI; `asDisplayString` internal.
  - _ref:_ HU-2.1, HU-2.2, HU-5.1, HU-5.2 · design §MOTOR·RenderScope.bind
  - _verif:_ `ResolveBindingTest` cubre literal→literal, `"$x"`→valor, ausente→"", `"$$x"`→`"$x"`, objeto→toString.

- [x] **T3** — MOTOR `CorePack.kt`: el renderer de `text` pasa a `Text(bind(p.text))` **y** el `button` a
  `Text(bind(p.label))` (decisión del usuario: extender la convención al label). column/row sin cambios.
  - _ref:_ HU-2.2 · design §MOTOR·CorePack
  - _verif:_ `:sdui-compose:check` verde; sin imports de `io.ktor.`/`dev.kuisd.app`.

- [x] **T4** — APP `dev.kuisd.app.variables/VariableStore.kt` (`@Stable`): `vars` en snapshot-state;
  `scope: VariableScope`; `set`, `seed`, `toggle` (coerción `false`), `increment` (coerción `0` + clamp `[min,max]`).
  - _ref:_ HU-3, HU-1.3, HU-5.3 · design §APP·VariableStore
  - _verif:_ `VariableStoreTest` cubre `set`/`seed`; `toggle` ausente→true, true→false, no-booleano→true;
    `increment` suma, clamp a `max`/`min`, ausente/no-entero parte de 0.

- [x] **T5** — APP `dev.kuisd.app.variables/VariableActionHandler.kt` (internal, `SubHandler`):
  `SetVar`→set, `Toggle`→toggle, `Increment`→increment; ignora en silencio las demás (sin log propio).
  - _ref:_ HU-3.2/3.3/3.4 · design §APP·VariableActionHandler
  - _verif:_ `VariableActionHandlerTest` valida cada acción muta el store; `Navigate` ajena no lo altera;
    `supports` solo verdadero para las tres acciones.

- [x] **T6** — APP `dev.kuisd.app/AppActionHandler.kt` (internal): handler compuesto que, por acción, busca
  el primer `SubHandler` que la soporte; si ninguno la soporta, hace **un único log** (sin doble-log).
  Define la interfaz interna `SubHandler : SduiActionHandler { fun supports(action): Boolean }`. El contrato
  del motor `SduiActionHandler` no se toca.
  - _ref:_ HU-4.1, HU-4.2, HU-4.3 · design §APP·AppActionHandler
  - _verif:_ `AppActionHandlerTest` valida que `[Increment, Navigate]` incrementa el store **y** apila;
    preserva orden; `CustomAction` ajena → no-op sin crash.

- [x] **T7** — APP `SduiScreen.kt`: recibe `store: VariableStore`; al pasar a `Content` lo siembra
  (`LaunchedEffect(envelope) { store.seed(envelope.variables) }`). El `LocalVariables` lo provee el host
  (Opción A elegida por el usuario).
  - _ref:_ HU-3.1, HU-1.3 · design §APP·SduiScreen
  - _verif:_ `:shared:assemble` y `:shared:check` verdes; comportamiento Loading/Error/Content intacto.

- [x] **T8** — APP `SduiHost.kt`: `remember(current.id) { VariableStore() }`; `handler =
  AppActionHandler(listOf(NavActionHandler(backStack), VariableActionHandler(store)))`; provee
  `LocalSduiActionHandler`, `LocalComponentRegistry` (004) y `LocalVariables provides store.scope`; pasa
  `store` a `SduiScreen` dentro de `key(current.id)`.
  - _ref:_ HU-4.1, HU-1.1 · design §APP·SduiHost
  - _verif:_ `:shared:assemble`, `:androidApp:assembleDebug`, `:shared:check` verdes.

- [x] **T9** — SERVER `screens/CounterScreen.kt`: `variables {count:0, flag:false}`; `text` literal +
  `text` con `"$count"` + `text` con `"$flag"`; botones `Increment(count, by=±1, min=0, max=10)`,
  `Toggle(flag)`, `Atrás → NavigateBack`. Registrar `"counter"` en `defaultScreenRegistry()`. Enlace desde
  `home` con un botón "Contador" → `Navigate("counter")` (decisión del usuario).
  - _ref:_ HU-6 · design §SERVER·CounterScreen
  - _verif:_ test `:server`: `/screen/counter` → 200; `variables["count"]==0`, `variables["flag"]==false`;
    nodo `text` con `props.text=="$count"`. `curl` confirma el envelope.

- [x] **T10** — Regla de dependencias / API mínima: `dev/kuisd/sdui/` (módulo `:sdui-compose`) sin
  `dev.kuisd.app`/`io.ktor.` y sin `mutableStateOf`/`MutableStateFlow`; public/internal según el mapa.
  - _ref:_ RNF, HU-1.2, HU-1.4 · design §public/internal, §Capas
  - _verif:_ `grep -rn "dev.kuisd.app" sdui-compose/src/` → vacío. `grep -rn
    "mutableStateOf\|MutableStateFlow\|io.ktor" sdui-compose/src/commonMain/` → vacío.

- [ ] **T11** — Smoke end-to-end UI: `:server:run` + `:desktopApp:run`; en `counter`, "+1" sube el número
  sin red y se detiene en 10 (clamp); "Toggle" no crashea; navegación 003 y `badge` 004 intactos.
  - _ref:_ HU-2.3, HU-6.2 · design §Verificación
  - _verif:_ observación manual de la ventana desktop. **NO verificado en este ciclo** (el harness no abre
    pantalla). El data-path queda validado por `curl /screen/counter` y por los tests unitarios.

- [x] **T12** — Calidad: `:sdui-core:check`, `:sdui-compose:check`, `:shared:assemble`, `:shared:check`,
  `:server:build`, `:androidApp:assembleDebug`, `detekt`, `ktlintCheck` (tras `ktlintFormat`) — todos verdes.
  - _ref:_ RNF · design §Verificación

## Verificación final (Definition of Done)
- [x] El motor lee variables por `VariableScope`/`LocalVariables` (seam de **solo lectura**) y **no posee
  estado mutable**; sigue sin importar Ktor ni `dev.kuisd.app`.
- [x] Un `text` con `"$count"` muestra el valor de la variable; literal sin `$` se pinta tal cual; `$$`
  escapa (retrocompat 004). Mismo binding extendido al `label` del button.
- [x] `VariableStore` (app) sembrado por `envelope.variables`; `SetVar`/`Toggle`/`Increment` mutan y la UI
  recompone **sin** recargar la pantalla (snapshot-state); `Increment` respeta `min`/`max` (clamp).
- [x] `AppActionHandler` compone navegación (003) + variables sin romper SRP; **sin doble log**: los
  sub-handlers ignoran en silencio; el compuesto loguea solo las acciones que ningún sub-handler soportó.
- [x] Binding a variable ausente / tipo raro y acciones sobre tipos incompatibles **no crashean**
  (fallback/coerción).
- [x] El server sirve `counter` (200) con `variables` + binding + acciones; navegación 003 y `badge` 004
  intactos.
- [x] Todos los `:check`/`build`/`assembleDebug`/`detekt`/`ktlintCheck` en verde; `tasks.md` todo `[x]`
  salvo el smoke manual (T11) — no observable en este harness.
