# Tareas — Builder de blueprints SDUI (desktop)

> Spec ID: 014 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Andamiaje del módulo + exposición en `:shared`. Crear `:builder` (Compose Desktop,
  molde `:desktopApp`: plugins, `compose.desktop application{}`, javaHome lazy), `include(":builder")` en
  settings, `Main.kt` mínimo (`Window { Text("builder") }`). En `:shared`, `SduiPreviewEnvironment.kt`
  público (provee los seams con `SduiActionHandler` inyectable).
  - _ref:_ HU-1.1/1.3, HU-5.1 · design §"Arquitectura", §"SduiPreviewEnvironment"
  - _verif:_ `./gradlew :builder:compileKotlin :shared:desktopTest` en verde; `:builder:run` abre ventana.

- [ ] **T2** — `model/`: `TreeOps` (insert/delete/updateProps/updateModifier/findById/ensureId, puro) y
  `BuilderDocument` (estado `root`/`selectedId` + métodos que delegan en `TreeOps`). Test `TreeOpsTest`.
  - _ref:_ HU-2.3/2.4, HU-3.2/3.3, HU-4.2 · design §"Estado del documento", §"TreeOps"
  - _verif:_ `./gradlew :builder:test` (incluye `TreeOpsTest`: insert en contenedor/raíz, delete no-raíz,
    update, ensureId único/determinista).

- [ ] **T3** — `catalog/`: `Category`/`FieldEditor`/`FieldTarget`/`FieldSpec`/`PaletteEntry` y
  `builderCatalog` (subconjunto curado: `text`, `button`, `column`, `row`, `card`, `surface`, `divider`,
  `spacer`, `image`, `icon`, `scaffold`/`topAppBar`) con plantillas y campos editables. Test
  `BuilderCatalogTest`.
  - _ref:_ HU-2.1/2.2, HU-4.1/4.3 · design §"Catálogo"
  - _verif:_ `./gradlew :builder:test` (`BuilderCatalogTest`: cada `template` (de)serializa por
    `DefaultSduiJson` y su `type` ∈ `appRegistry`; categorías no vacías).

- [ ] **T4** — `export/`: `exportEnvelope(root)` → `SduiEnvelope` JSON con `DefaultSduiJson`. Test
  `ExportTest` (round-trip: serializa → re-parsea → equivalente).
  - _ref:_ HU-6.1/6.3 · design §"Preview, handler y export"
  - _verif:_ `./gradlew :builder:test` (`ExportTest` verde).

- [ ] **T5** — `ui/`: `BuilderApp` (Row: preview izq / columna derecha), `PreviewPane`
  (`SduiPreviewEnvironment(LoggingActionHandler) { RenderNode(doc.root) }`), `PalettePane` (categorías →
  insertar), `OutlinePane` (árbol → seleccionar/borrar, resalta selección), `InspectorPane` (campos del
  descriptor → edita props/modifier en vivo) y `LoggingActionHandler`.
  - _ref:_ HU-1.2, HU-2, HU-3, HU-4, HU-5 · design §"Arquitectura" (diagrama)
  - _verif:_ `./gradlew :builder:compileKotlin` en verde.

- [ ] **T6** — `Main.kt`: `application { Window(title="kuisd builder") { BuilderApp() } }` + botón/zona
  de **Exportar** que muestra el JSON. Smoke.
  - _ref:_ HU-1.1, HU-6.2 · design §"Estrategia de verificación"
  - _verif:_ `./gradlew :builder:run` — añadir componentes, seleccionar en outline, editar `text`, ver
    el preview cambiar, exportar y comprobar que el JSON parsea.

- [ ] **T7** — Calidad: `./gradlew detekt ktlintCheck` en verde para `:builder` (y resto).
  - _ref:_ Requisitos no funcionales · design §"Estrategia de verificación"
  - _verif:_ lint/detekt verdes; smoke manual de T6 (si el harness no abre UI, se justifica con los tests
    de árbol/catálogo/export como evidencia).

## Verificación final (Definition of Done)
- [ ] `requirements.md` y `design.md` en `approved`.
- [ ] Módulo `:builder` (Compose Desktop) ejecutable con `:builder:run`; `:sdui-compose`/`:sdui-core` SIN cambios.
- [ ] `:shared` solo añade `SduiPreviewEnvironment` público; `SduiHost`/runtime intacto.
- [ ] `TreeOps`/`BuilderCatalog`/`exportEnvelope` con tests verdes (árbol, round-trip, catálogo válido).
- [ ] UI split: preview real (motor) + paleta por categoría + outline (selección/borrado) + inspector de props.
- [ ] Exportar produce un `SduiEnvelope` JSON que re-parsea (round-trip).
- [ ] `:builder:test`, `:builder:compileKotlin`, `:shared:desktopTest`, `detekt`, `ktlintCheck` en verde.
- [ ] `tasks.md` todo `[x]` salvo smoke visual manual si el harness no abre UI (justificado).
