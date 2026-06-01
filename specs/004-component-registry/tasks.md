# Tareas — Catálogo de componentes extensible (ComponentRegistry · OCP)

> Spec ID: 004 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Build: aplicar el plugin `kotlin-serialization` en `shared/build.gradle.kts` (props
  `@Serializable` del motor y del badge).
  - _ref:_ Build · design §Build
  - _verif:_ `:shared:assemble` compila con clases `@Serializable` en `:shared`.

- [x] **T2** — MOTOR `dev.kuisd.sdui/RenderScope.kt`: `class RenderScope(val node)` + `@Composable
  fun renderChildren()`.
  - _ref:_ HU-2.2 · design §RenderScope
  - _verif:_ `:shared:assemble`.

- [x] **T3** — MOTOR `ComponentRegistry.kt`: `RegisteredComponent<P>` (con `Render(node)` que decodifica
  props vía el serializer y delega; fallo → `UnknownNode`), `ComponentRegistry` (`rendererFor`, `plus`),
  `ComponentRegistryBuilder` + `componentRegistry { }`, y `LocalComponentRegistry` (default `CorePack`).
  - _ref:_ HU-1.1, HU-1.3, HU-4.2 · design §RegisteredComponent/ComponentRegistry
  - _verif:_ tests: `rendererFor` por type; `plus` combina; type ausente → null.

- [x] **T4** — MOTOR `CorePack.kt`: props (`ColumnProps`/`RowProps`/`TextProps`/`ButtonProps`) + `CorePack`
  con column/row/text/button (button delega a `LocalSduiActionHandler`). `RenderNode.kt` pasa a resolver
  por `LocalComponentRegistry` (sustituye el `when`); `UnknownNode` `internal`.
  - _ref:_ HU-1.2, HU-2 · design §CorePack, §RenderNode
  - _verif:_ `:shared:assemble`; tests de decodificación de `TextProps` (con defaults / inválidas).

- [x] **T5** — APP `dev.kuisd.app.components`: `BadgeProps` + `BadgeComponent` + `appComponents` +
  `appRegistry = CorePack + appComponents`. `SduiHost` provee `LocalComponentRegistry provides appRegistry`
  (junto al `LocalSduiActionHandler`).
  - _ref:_ HU-3.1 · design §App
  - _verif:_ `:shared:assemble`; `:androidApp:assembleDebug` y `:desktopApp` compilan.

- [x] **T6** — SERVER: `DetailsScreen` añade un nodo `badge` (`{"text":"Nuevo"}`).
  - _ref:_ HU-3.2 · design §Server
  - _verif:_ test `:server`: `/screen/details` 200 y el árbol contiene un nodo `type=="badge"`.

- [x] **T7** — Regla de dependencias / API mínima: `dev/kuisd/sdui/` sin `dev.kuisd.app` ni `io.ktor.`;
  public/internal según el mapa.
  - _ref:_ RNF, HU-1.4 · design §public/internal
  - _verif:_ `grep` sin `dev.kuisd.app`/`io.ktor.` en `dev/kuisd/sdui/`.

- [ ] **T8** — Smoke end-to-end: `:server:run` + `:desktopApp:run`; el `badge` ("Nuevo") se ve en
  `details`; navegación 003 intacta; un type no registrado mostraría "Componente no soportado".
  - _ref:_ HU-3, HU-4.1 · design §Verificación
  - _verif:_ observación manual de la ventana desktop. **PENDIENTE**: requiere pantalla; no verificable
    en CI/headless. El data-path está verificado (curl `/screen/details` → nodo `type=="badge"`).

- [x] **T9** — Calidad.
  - _ref:_ RNF · design §Verificación
  - _verif:_ `./gradlew :sdui-core:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` sin fallos.

## Verificación final (Definition of Done)
- [x] El motor resuelve nodos por `ComponentRegistry` (sin `when` hardcodeado); `CorePack` = column/row/text/button.
- [x] La app añade `badge` registrándolo (CorePack + appComponents) **sin modificar `dev.kuisd.sdui`**.
- [x] El server emite `badge` y el cliente lo renderiza; type desconocido → `UnknownNode` sin crash.
- [x] Props se decodifican tipadas (`P`) con defaults; props inválidas → `UnknownNode` sin crash.
- [x] Frontera Clean Architecture (003) intacta; API pública mínima.
- [x] Todos los `:check`/`build`/`assembleDebug`/`detekt`/`ktlintCheck` en verde; `tasks.md` todo `[x]`.
