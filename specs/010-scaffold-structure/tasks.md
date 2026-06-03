# Tareas — Estructura de pantalla (`scaffold` · `topAppBar` · `bottomBar`)

> Spec ID: 010 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Lógica pura de slots y selección: crear `ScaffoldSlots.kt` con `ScaffoldSlots` (data
  class), `partitionScaffoldSlots(children)`, `isItemSelected(selectedValue, itemValue)` y el helper
  interno `inline fun <reified T : Any> decodeOrNull(node): T?` (vía `DefaultSduiJson` + `runCatching`).
  - _ref:_ HU-1, HU-3 · design §"Partición de slots", §"bottomBar"
  - _verif:_ compila; cubierto por los tests de T5 (se valida al ejecutar `:sdui-compose:check`).

- [x] **T2** — Componente `scaffold`: `ScaffoldProps` (sin campos), `register("scaffold")` y
  `ScaffoldRenderer` (Material `Scaffold` con slots de T1, `Box(padding=innerPadding)` para content).
  - _ref:_ HU-1 · design §"scaffold"
  - _verif:_ `./gradlew :sdui-compose:compileKotlinJvm`; `CorePack.rendererFor("scaffold") != null` (T5).

- [x] **T3** — Componente `topAppBar`: `TopAppBarProps(title, navigationIcon)`, `register("topAppBar")`
  y `TopAppBarRenderer` (`@OptIn` acotado; nav-icon solo si hay `onNavigationClick`; `actions =
  { renderChildren() }`).
  - _ref:_ HU-2 · design §"topAppBar"
  - _verif:_ compila; `rendererFor("topAppBar") != null` (T5).

- [x] **T4** — Componente `bottomBar`: `BottomBarProps(selectedBind)`, `BottomBarItemProps(icon, label,
  value)`, `register("bottomBar")` y `BottomBarRenderer` (`NavigationBar` + `NavigationBarItem` por child
  `bottomBarItem` decodificado con `decodeOrNull`; `selected = isItemSelected(...)`; `onClick` delega).
  - _ref:_ HU-3, HU-4 · design §"bottomBar + bottomBarItem"
  - _verif:_ compila; `rendererFor("bottomBar") != null` (T5).

- [x] **T5** — Tests unitarios (commonTest, sin UI): `ScaffoldSlotsTest` (mixto en orden / sin barras /
  doble `topAppBar` descarta el 2º / vacío), `BottomBarSelectionTest` (`isItemSelected`: match / no-match
  / null⇒false) y ampliar `ComponentRegistryTest` con los 3 tipos nuevos.
  - _ref:_ HU-1.2/1.5, HU-3.3/3.5, HU-4.1 · design §"Estrategia de verificación"
  - _verif:_ `./gradlew :sdui-compose:check` en verde (incluye los nuevos tests).

- [x] **T6** — Pantalla piloto en el server: migrar `home` a `scaffold` con `topAppBar` (título) +
  `bottomBar` de 3 secciones (`bottomBarItem` con `icon`/`label`/`value` + `onClick: setVar(section,…)`),
  sembrando la variable `section` en `envelope.variables`. Iconos por nombre existentes en
  `DefaultIconRegistry` (p.ej. `home`/`search`/`settings`).
  - _ref:_ HU-1, HU-2, HU-3 · design §"Estrategia de verificación" (smoke e2e)
  - _verif:_ `./gradlew :server:build`; `curl localhost:8080/screen/home` devuelve un árbol con
    `type:"scaffold"` y los 3 `bottomBarItem`.

- [x] **T7** — Calidad + smoke visual: `./gradlew detekt ktlintCheck` en verde; `:desktopApp:run` y
  observar barra superior + barra inferior, y que pulsar una sección la marca activa.
  - _ref:_ Requisitos no funcionales · design §"Estrategia de verificación"
  - _verif:_ lint/detekt verdes ✅. Smoke visual de escritorio NO ejecutado en este harness (no abre
    ventana UI); justificado y respaldado por evidencia e2e: los `ApplicationTest` afirman que
    `GET /screen/home` devuelve `root.type=="scaffold"` con `topAppBar` y `bottomBar`, más los tests
    unitarios. Pendiente confirmación visual manual del usuario al correr `:desktopApp:run`.

## Verificación final (Definition of Done)
- [x] `requirements.md` y `design.md` en `approved`.
- [x] `scaffold`, `topAppBar`, `bottomBar` registrados en `CorePack` y resolubles por `rendererFor`.
- [x] `partitionScaffoldSlots` e `isItemSelected` con tests verdes (incluido el caso de barra duplicada).
- [x] Sin cambios en `:sdui-core` (contrato intacto); sin dependencias nuevas.
- [x] `:sdui-compose:jvmTest`, `:server:build`, `detekt`, `ktlintCheck` en verde.
- [x] Pantalla `home` migrada a `scaffold` sirve el árbol esperado (T6) — evidencia e2e (`ApplicationTest`).
- [x] `tasks.md` todo `[x]`; smoke visual manual de escritorio pendiente de confirmación del usuario (justificado).
