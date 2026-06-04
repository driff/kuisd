# Tareas — Preview multi-dispositivo

> Spec ID: 018 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — `ui/DevicePreset.kt`: enum `DevicePreset(label, width, height)` (Phone 360×800, Tablet 800×1280,
  Desktop 1280×800) + `DefaultDevice = Phone`.
  - _ref:_ HU-1 · design §DevicePreset
  - _verif:_ `DevicePresetTest`: los 3 presets con dimensiones > 0; `DefaultDevice == Phone`.

- [x] **T2** — `ui/PreviewPane.kt`: selector de dispositivo + lienzo `Surface` a tamaño real (borde+elevación)
  envuelto en `horizontalScroll`+`verticalScroll`, con `SduiPreviewEnvironment(handler) { RenderNode(root) }`
  dentro; estado `device` efímero (`remember`).
  - _ref:_ HU-1 · HU-2 · design §PreviewPane
  - _verif:_ compila; smoke (selector cambia el tamaño; Phone entra; Tablet/Desktop con scroll; árbol idéntico).

- [x] **T3** — `ui/BuilderApp.kt`: reemplazar el `Box` del preview por `PreviewPane(document, handler, …)`.
  - _ref:_ HU-2 · design §BuilderApp
  - _verif:_ compila; el preview se renderiza dentro del lienzo del dispositivo.

## Verificación final (Definition of Done)
- [x] `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- [x] `:sdui-core`/`:sdui-compose`/`:shared` sin cambios (solo `:builder`).
- [ ] Smoke `:builder:run`: selector Phone/Tablet/Desktop cambia el lienzo; Phone cabe, Tablet/Desktop hacen
  scroll a tamaño real; `fillMaxWidth` ocupa el ancho del dispositivo (no del panel); el dispositivo no se
  persiste en el documento.
