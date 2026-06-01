# Tareas — Inputs two-way (`textField`, API state-based)

> Spec ID: 007 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T0** — Aprobar `requirements.md` y `design.md` (cabecera `Estado: approved`); decisiones
  cerradas: (a) patrón state-based (`rememberTextFieldState` + `snapshotFlow`); (b) demo incluye
  saludo `$name` además del campo `count`; (c) `bind = ""` se acepta como uncontrolled.
  - _verif:_ ambos archivos en `Estado: approved`.

- [x] **T1** — APP `:shared` · `VariableStore.seed` se vuelve **idempotente** (merge no-overwrite).
  Reemplazar `vars = initial` por un merge que respete las claves ya presentes.
  - _ref:_ HU-6.5 · design §APP·VariableStore.seed
  - _verif:_ `./gradlew :shared:check` compila; los tests existentes se ajustan (ver T2).

- [x] **T2** — APP `:shared` · `VariableStoreTest` actualizado: ajustar el test
  `seed_replaces_the_whole_map` a la nueva semántica (renombrado a `seed_preserves_existing_values`)
  y añadir `seed_adds_missing_keys`. Primera siembra desde mapa vacío sigue comportándose igual
  (cubierto por los demás tests al usar `seed(...)`).
  - _ref:_ HU-6.5 · design §Verificación
  - _verif:_ `./gradlew :shared:check` verde.

- [x] **T3** — MOTOR `:sdui-compose` · `CorePack.kt`: añadir
  `@Serializable data class TextFieldProps(val bind: String = "", val placeholder: String = "",
  val label: String? = null)` como tipo público del paquete `dev.kuisd.sdui`.
  - _ref:_ HU-1.1 · design §MOTOR·TextFieldProps
  - _verif:_ `:sdui-compose:check` compila; `import dev.kuisd.sdui.TextFieldProps` resuelve.

- [x] **T4** — MOTOR `:sdui-compose` · `CorePack.kt`: registrar
  `register(sduiComponent<TextFieldProps>("textField")) { p -> ... }`. Patrón state-based:
  1. Calcular `initial = LocalVariables.current.get(p.bind)?.asDisplayString().orEmpty()`.
  2. `val state = rememberTextFieldState(initial)`.
  3. Si `p.bind` no es vacío, montar un `LaunchedEffect(state, p.bind)` que colecta
     `snapshotFlow { state.text.toString() }.drop(1)` y, para cada nuevo valor, emite
     `handler.handle(listOf(SetVar(p.bind, JsonPrimitive(newText))))`.
  4. Renderizar `OutlinedTextField(state = state, placeholder = { Text(p.placeholder) },
     label = p.label?.let { { Text(bind(it)) } }, modifier = Modifier.fillMaxWidth())` con
     `@OptIn(ExperimentalMaterial3Api::class)`.
  - _ref:_ HU-2.1, HU-2.2, HU-3.1–HU-3.4, HU-4.1–HU-4.3, HU-6.1–HU-6.4 · design §MOTOR·renderer textField
  - _verif:_ `:sdui-compose:check` verde; `grep -rn "dev.kuisd.app\|io.ktor\." sdui-compose/src/`
    sigue vacío.

- [x] **T5** — MOTOR · Test unitario de decodificación de props (sin UI). En
  `sdui-compose/src/commonTest/.../TextFieldPropsTest.kt`:
  `{"bind":"count","placeholder":"x","label":"L"}` → `TextFieldProps("count","x","L")`;
  `{}` → `TextFieldProps("","",null)`. (El camino "props inválidas → UnknownNode" ya está cubierto
  por `RegisteredComponent.Render` y `ComponentRegistryTest`.)
  - _ref:_ HU-1.4, HU-1.1 · design §Verificación
  - _verif:_ `./gradlew :sdui-compose:check` verde con el nuevo test.

- [x] **T6** — APP · Test unitario de flujo two-way (sin UI). En
  `shared/src/commonTest/.../variables/TextFieldTwoWayFlowTest.kt`: construir
  `VariableStore`, sembrar `count = 0`, ejecutar
  `AppActionHandler(listOf(VariableActionHandler(store))).handle(listOf(SetVar("count",
  JsonPrimitive("7"))))` y comprobar `store.vars["count"] == JsonPrimitive("7")`. Cubrir también:
  `SetVar("name", "Ana")` sobre store vacío crea la variable; tras `SetVar("count","7")`,
  `Increment("count", by=1)` produce `JsonPrimitive(8)` (coerción `intOrNull` de 005). (El test
  de "no-overwrite por re-seed" vive en `VariableStoreTest`, T2.)
  - _ref:_ HU-3.4, HU-5.2, HU-6.1 · design §APP, §Verificación
  - _verif:_ `./gradlew :shared:check` verde con el nuevo test.

- [x] **T7** — SERVER · `screens/CounterScreen.kt`: añadir variable `"name" to JsonPrimitive("")`;
  insertar dos nodos `textField` (uno con `bind="count"` y placeholder `"Escribe un número"`; otro
  con `bind="name"` y placeholder `"Tu nombre"`) y un `row` saludo con `text "Hola, "` + `text
  "$name"`. Helper privado `textFieldNode(id, bind, placeholder)`.
  - _ref:_ HU-7.1, HU-7.2 · design §SERVER
  - _verif:_ test `:server` actualizado (T8); `curl /screen/counter | jq` muestra los nodos.

- [x] **T8** — SERVER · `ApplicationTest`: actualizar
  `screen_counter_serves_an_envelope_with_initial_variables` para validar también
  `variables.name == ""`, un `textField(bind="count")`, un `textField(bind="name")` y un `text`
  con `props.text == "$name"`.
  - _ref:_ HU-7.4 · design §Verificación
  - _verif:_ `./gradlew :server:build` verde.

- [x] **T9** — Regla de dependencias y API mínima del motor: `grep -rn "dev.kuisd.app\|io.ktor\."
  sdui-compose/src/` vacío; `grep -rn "TextFieldProps\|\"textField\"" sdui-compose/src/` solo en
  `CorePack.kt` y `TextFieldPropsTest.kt`. El resto del motor no se toca.
  - _ref:_ HU-1.2, HU-1.3 · design §Arquitectura, §public/internal
  - _verif:_ `git diff` (sin commit en esta spec) muestra cambios solo en
    `sdui-compose/.../CorePack.kt` + el test motor, `shared/.../VariableStore.kt` + tests,
    `server/.../CounterScreen.kt` + test.

- [ ] **T10** — Smoke manual `:server:run` + `:desktopApp:run` en `counter`:
  1. Teclear `7` en `count-input` → el `text` `$count` muestra `7`.
  2. Pulsar `+1` → display `8`; el campo `count-input` **sigue mostrando `7`** (HU-2.2 ajustada).
  3. Borrar `7` y teclear `abc` → texto del campo es `abc`, display muestra `abc`; `+1` → display
     `1` (coerción `intOrNull?:0`).
  4. Teclear `Ana` en `name-input` → display de `$name` y row "Hola, Ana".
  5. Atrás → Adelante a `counter`: nueva entrada de back stack ⇒ store nuevo; campos vuelven a
     `""`/`0`.
  - _ref:_ HU-2.1, HU-2.2, HU-3.4, HU-7.2, HU-7.3 · design §Verificación
  - _verif:_ observación manual (si el harness no abre UI, documentar como smoke no verificado).

- [x] **T11** — Calidad: `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble
  :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` (tras
  `ktlintFormat`) — todos verdes.
  - _ref:_ RNF · design §Verificación
  - _verif:_ salida de Gradle en verde.

## Verificación final (Definition of Done)
- [x] El motor (`:sdui-compose`) gana **solo** `TextFieldProps` + el `register("textField", ...)`
  dentro de `CorePack.kt`; ningún otro archivo del motor cambia.
- [x] El motor sigue sin importar `dev.kuisd.app`/`io.ktor.*` y sin estado mutable propio de
  variables; el `rememberTextFieldState` del renderer es estado de UI local de un Composable
  (no estado de variables del motor).
- [x] Un `textField(bind="count")` arranca con el valor actual de `count`; al teclear, emite
  `SetVar("count", JsonPrimitive(<texto>))` por keystroke vía `snapshotFlow + drop(1)`.
- [x] La app gana **solo** el merge idempotente en `VariableStore.seed` (HU-6.5); `AppActionHandler`
  + `VariableActionHandler` + el resto del store quedan intactos.
- [x] El campo es la fuente del texto tras la 1ª composición: `Increment` desde un botón actualiza
  el display `$count` pero no reescribe el campo (cero cursor-jump).
- [x] Coerción string-only documentada y verificada: tras `SetVar("count","7")`, `Increment`
  produce `8` (clamp a `[0,10]`); tras `SetVar("count","abc")`, `Increment` produce `1`.
- [x] Resiliencia: `bind` vacío = uncontrolled sin emitir; `bind` a variable ausente crea la
  variable al primer `SetVar`; props inválidas degradan a `UnknownNode` (regla 004); re-seed del
  envelope no pisa valores ya escritos (HU-6.5).
- [x] El server sirve `/screen/counter` (200) con un `textField(bind="count")`, un
  `textField(bind="name")`, `variables.name == ""` y un `text "$name"` + literal `"Hola, "`.
- [x] `:sdui-core:check`, `:sdui-compose:check`, `:shared:assemble`, `:shared:check`,
  `:server:build`, `:androidApp:assembleDebug`, `detekt`, `ktlintCheck` — todos verdes.
- [x] `tasks.md` todo `[x]` salvo el smoke manual si el harness no abre UI (justificado).
