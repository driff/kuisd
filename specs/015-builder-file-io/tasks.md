# Tareas — Cargar/guardar a archivo en el :builder

> Spec ID: 015 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — Codec puro: `SduiEnvelope.encodeToJson()` / `decodeEnvelope(String)` sobre `DefaultSduiJson`.
  - _ref:_ HU-5 · design §EnvelopeCodec
  - _verif:_ `EnvelopeCodecTest` (round-trip con `variables`/`meta`; decode de JSON inválido lanza).
- [x] **T2** — E/S de disco `BuilderFileStore.read/write` (`Result`, sin UI) + `withJsonExtension`.
  - _ref:_ HU-1.3/HU-1.5/HU-2.5 · design §BuilderFileStore
  - _verif:_ `BuilderFileStoreTest` (write→read en temp; añade `.json`; inexistente/corrupto → failure).
- [x] **T3** — Diálogos nativos AWT `openFileDialog()` / `saveFileDialog(name)` → `File?`.
  - _ref:_ HU-1.2/HU-1.6/HU-2.1/HU-2.6 · design §FileDialogs
  - _verif:_ compila; smoke manual `:builder:run`.
- [x] **T4** — `BuilderDocument` ampliado: `currentFile`/`isModified`, metadatos preservados, `toEnvelope`/`load`/`newDocument`/`markSaved`; mutaciones marcan modificado; fix `delete` → `root.id`.
  - _ref:_ HU-2/HU-3/HU-4/HU-5 · design §BuilderDocument
  - _verif:_ `BuilderDocumentTest` (load fija árbol/metadatos/selección/ids únicos y limpia modificado; mutar marca; markSaved/newDocument limpian; round-trip; delete con raíz id≠"root").
- [x] **T5** — Toolbar Nuevo/Abrir/Guardar/Guardar como… + título (archivo + marcador modificado), confirmación de descarte y diálogo de error; export panel pasa a `toEnvelope().encodeToJson()`.
  - _ref:_ HU-1.1/HU-1.4/HU-2.7/HU-3.2/HU-4.3 · design §BuilderApp/Toolbar
  - _verif:_ compila; smoke manual.

## Verificación final (Definition of Done)
- [x] `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- [x] `:sdui-core`/`:sdui-compose`/`:shared` sin cambios (solo `:builder`).
- [ ] Smoke manual `:builder:run`: crear → Guardar como → Nuevo → Abrir → árbol restaurado; marcador modificado aparece/desaparece; Abrir con cambios pide confirmación; JSON corrupto muestra error sin cerrar.
