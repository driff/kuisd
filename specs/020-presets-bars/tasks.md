# Tareas — Presets de barras + FAB + top bar centrado

> Spec ID: 020 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.
Orden: motor (T1-T2), luego builder (T3-T5).

- [x] **T1** — Motor FAB: `ScaffoldSlots.fab` + partición `"fab"`; `ScaffoldProps.fabPosition`;
  `ScaffoldRenderer` monta `floatingActionButton` + `floatingActionButtonPosition`; componente `fab`
  (`FabProps(icon)` + onClick) registrado en CorePack.
  - _ref:_ HU-1 · design §ScaffoldSlots/Renderer/fab
  - _verif:_ `:sdui-compose` jvmTest: `ScaffoldSlotsTest` (child fab → slot; primero gana; sin fab ⇒ null);
    `fab` registrado y decodifica; no-regresión verde.

- [x] **T2** — Motor centrado: `TopAppBarProps.centered` → `CenterAlignedTopAppBar` vs `TopAppBar`
  (slots compartidos en lambdas), resto del comportamiento igual.
  - _ref:_ HU-2 · design §TopAppBar centrado
  - _verif:_ `:sdui-compose` compila + jvmTest verde; `TopAppBarProps` decodifica con `centered` default false.

- [x] **T3** — Builder identidad de paleta: `PaletteEntry.key` (default `=type`) + `isPreset`;
  `catalogByType = filterNot { isPreset }.associateBy { type }`; `PalettePane` usa `key` como key de `items`.
  - _ref:_ HU-4.3 · design §PaletteEntry
  - _verif:_ `:builder` compila; `BuilderCatalogTest`: `catalogByType` 1 entrada por type; `key`s únicos.

- [x] **T4** — Builder slot FAB: `MainContainerSpec` del scaffold añade `SlotSpec("fab","FAB",false,{"fab"})`.
  - _ref:_ HU-3.1 · design §MainContainers
  - _verif:_ `MainContainersTest`: scaffold con 4 slots; `reservedChildTypes` contiene `fab`;
    `slotForChildType("fab").id == "fab"`.

- [x] **T5** — Builder catálogo + presets: base `fab`; `fabPosition` (scaffold) y `centered` (topAppBar);
  `Category.Presets`; 6 presets (`isPreset=true`, `key` único): Top Bar Navegación/Acciones/Centrado,
  Bottom Menú 3/5, FAB.
  - _ref:_ HU-3.2/3.3 · HU-4 · design §Catálogo
  - _verif:_ `BuilderCatalogTest` (amplía): cada plantilla round-trip; `fab`/presets presentes; `Category.Presets`
    presente. `BuilderDocumentTest`: `insert(fab)`→slot FAB; `insert(preset topAppBar)` reemplaza topBar con su
    subárbol (ids únicos). Smoke `:builder:run`.

## Verificación final (Definition of Done)
- [x] `./gradlew :sdui-compose:jvmTest :sdui-compose:detekt :sdui-compose:ktlintCheck :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- [x] `:sdui-core`/`:shared` sin cambios.
- [x] **Sin regresión** del motor (scaffold/topAppBar sin fab/centered renderizan igual).
- [ ] Smoke `:builder:run`: presets insertan en su slot; FAB visible (end/center); top bar centrado; varios
  presets de topAppBar conviven en la paleta sin colisión.
