# Tareas — Contenedores principales en el builder (Scaffold)

> Spec ID: 016 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.
Orden pensado para mantener el módulo compilando: primero lo puro/declarativo, luego documento, luego UI.

- [x] **T1** — Tabla declarativa `MainContainers`: `SlotSpec`, `MainContainerSpec` (con `contentSlot`,
  `reservedChildTypes`, `slotForChildType`), `mainContainers` (solo `scaffold`) e `isMainContainer()`.
  - _ref:_ HU-1 · design §MainContainers
  - _verif:_ `MainContainersTest`: `isMainContainer("scaffold")` true / otros false; `slotForChildType` mapea
    topAppBar→topBar, bottomBar→bottomBar, resto→content; `reservedChildTypes == {topAppBar, bottomBar}`. ✅

- [x] **T2** — `SlotOps` puro: `project`, `setSingleSlot`, `normalize`, `containsMainContainer`.
  - _ref:_ HU-1 · HU-3.6 · HU-4.4 · design §SlotOps
  - _verif:_ `SlotOpsTest`: `project` parte como el motor (1.º gana, resto a content en orden);
    `setSingleSlot` reemplaza el bar sin duplicar y normaliza; `normalize` deja ≤1 bar por slot (extras→content);
    `containsMainContainer` true con scaffold anidado, false sin él. ✅

- [x] **T3** — Catálogo: campo `contentDirection` (Enum column/row) en `scaffold`; entrada nueva `bottomBar`
  (categoría Estructura). `bottomBarItem` queda fuera (no registrado en CorePack; poblar ítems fuera de alcance).
  - _ref:_ HU-3.1 · HU-3.4 · design §BuilderCatalog
  - _verif:_ `BuilderCatalogTest` (amplía): `catalogByType["bottomBar"]` existe y su plantilla decodifica;
    `scaffold` expone el campo `contentDirection`. ✅

- [x] **T4** — `BuilderDocument`: `emptyRoot` = `scaffold "root"`; `selectedSlotId` + `selectSlot`;
  `insert` ruteado (rechaza contenedor principal; reservados→slot único con reemplazo; resto→content);
  `lastError` observable; `loadWrapped`; `load` aplica `SlotOps.normalize` a raíz contenedor principal.
  - _ref:_ HU-2.2 · HU-2.5 · HU-3.3 · HU-3.4 · HU-3.5 · HU-4.1 · HU-4.3 · design §BuilderDocument
  - _verif:_ `BuilderDocumentTest` (amplía): nuevo doc raíz scaffold "root"; `insert(topAppBar)`→topBar y
    2.º reemplaza; `insert(bottomBar)`→bottomBar; `insert(text)`→content; `insert(scaffold)` rechazado
    (árbol intacto + `lastError`); slot único seleccionado + tipo no acorde → rechazo; `loadWrapped` produce
    `scaffold(children=[árbol])`; `load` de raíz scaffold es round-trip fiel; `delete` no borra la raíz.

- [x] **T5** — Actualizar tests de 015 al nuevo default (raíz scaffold) sin romper su contrato de export.
  - _ref:_ HU-2.2 · design §Riesgos
  - _verif:_ `BuilderDocumentTest`/`EnvelopeCodecTest` de 015 en verde con raíz `scaffold` (export sigue
    `schemaVersion=1`/`screenId="builder"`).

- [x] **T6** — `OutlinePane`: raíz contenedor-principal con 3 slots explícitos seleccionables (Top bar/
  Content/Bottom bar) sobre `project`; resalte de `selectedSlotId`; fallback defensivo a árbol plano.
  - _ref:_ HU-3.2 · design §OutlinePane
  - _verif:_ compila ✅; smoke `:builder:run` muestra los 3 slots y resalta el seleccionado (pendiente smoke).

- [x] **T7** — `BuilderApp`: `ErrorMsg(text, fix?)` (generaliza `errorMessage`); `ErrorDialog` con botón
  "Arreglar" opcional; `openFlow` valida raíz (load / error+fix / error sin fix por scaffold anidado);
  selección de slot cableada; `lastError` de inserción rechazada enrutado al diálogo; paleta oculta
  contenedores principales.
  - _ref:_ HU-2.5 · HU-3.2 · HU-4.1 · HU-4.2 · HU-4.3 · HU-4.4 · HU-4.5 · design §BuilderApp · §PalettePane
  - _verif:_ compila + lint; smoke: añadir Top bar/Content/Bottom bar; Scaffold no aparece en paleta;
    Abrir raíz `column` → error con "Arreglar" → queda envuelto; raíz no-principal con scaffold anidado →
    error sin arreglo.

## Verificación final (Definition of Done)
- [x] `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- [x] `:sdui-core`/`:sdui-compose`/`:shared` sin cambios (solo `:builder`).
- [ ] Smoke `:builder:run`: Nuevo → raíz Scaffold con 3 slots; insertar en cada slot funciona; topBar/bottomBar
  únicos (2.º reemplaza); contenedor principal no insertable salvo raíz; Abrir con raíz inválida → error +
  arreglo (o error sin arreglo si hay scaffold anidado); la app no se cierra ante errores. **(pendiente)**
- [x] Añadir un contenedor principal nuevo solo requeriría una entrada en `mainContainers` (escalabilidad HU-1.3).
