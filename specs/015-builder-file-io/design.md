# Diseño — Cargar/guardar a archivo en el :builder

> Spec ID: 015 · Estado: draft · Trazabilidad: ./requirements.md

## Enfoque
Añadimos persistencia a archivo al `:builder` **sin tocar el motor** (`:sdui-core`/`:sdui-compose`) ni
`:shared`. La pieza central es ampliar `BuilderDocument` para que sea un documento **con identidad de
archivo y estado de modificación** (no solo un árbol): preserva los campos del `SduiEnvelope` que el
builder no edita (`schemaVersion`, `screenId`, `variables`, `meta`), expone `currentFile`/`isModified`, y
sabe reconstruir su `SduiEnvelope`. Separamos en tres capas con responsabilidades limpias y testables:
**(de)serialización pura** (codec sobre `DefaultSduiJson`), **E/S de disco** (`Result`-based, sin UI) y
**diálogos nativos** (AWT, no testeables — smoke). Reutilizamos `DefaultSduiJson` y `TreeOps` ya
existentes; no entran dependencias nuevas.

## Arquitectura
Módulo afectado: **solo `:builder`** (JVM/Compose Desktop). `:sdui-core`, `:sdui-compose` y `:shared` SIN
cambios.

```
BuilderApp (Compose)
  ├─ Toolbar: [Nuevo] [Abrir] [Guardar] [Guardar como…]  + título(archivo + marcador "modificado")
  │     onNuevo / onAbrir  ─(si isModified)→ ConfirmDiscardDialog ─→ acción
  │     onGuardar  → currentFile ?: saveFileDialog → BuilderFileStore.write → markSaved | errorDialog
  │     onGuardarComo → saveFileDialog → write → markSaved | errorDialog
  │     onAbrir(confirmado) → openFileDialog → BuilderFileStore.read → document.load | errorDialog
  ├─ PreviewPane / PalettePane / OutlinePane / InspectorPane  (sin cambios funcionales)
  └─ Estado UI: pendingAction (confirmación), errorMessage (diálogo de error)

model/BuilderDocument  ── toEnvelope() / load() / newDocument() / markSaved() + isModified + currentFile
export/EnvelopeCodec   ── SduiEnvelope.encodeToJson() / decodeEnvelope(String)   (puro)
io/BuilderFileStore    ── read(File): Result<SduiEnvelope> / write(File, …): Result<Unit>   (sin UI)
ui/FileDialogs         ── openFileDialog() / saveFileDialog(name): File?   (AWT nativo)
```

Flujo de datos (round-trip): `archivo .json` → `BuilderFileStore.read` → `decodeEnvelope` →
`document.load` (`TreeOps.ensureUniqueTree` + metadatos) → edición → `document.toEnvelope` →
`encodeToJson` → `BuilderFileStore.write` → `archivo .json`.

## Componentes y contratos

### EnvelopeCodec (de/serialización pura)
- **Ubicación:** `builder/.../export/Export.kt` (amplía el archivo actual).
- **Responsabilidad:** convertir `SduiEnvelope` ↔ `String` con `DefaultSduiJson`. Único punto de
  (de)serialización; sin E/S ni UI.
- **API (firmas):**
```kotlin
internal fun SduiEnvelope.encodeToJson(): String =
    DefaultSduiJson.encodeToString(SduiEnvelope.serializer(), this)

internal fun decodeEnvelope(json: String): SduiEnvelope =
    DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), json)
```
- **Decisiones:** el `exportEnvelope(root)` actual se sustituye por `document.toEnvelope().encodeToJson()`
  (el panel "Exportar JSON" pasa a exportar el envelope completo del documento, no solo la raíz). Se
  actualiza `ExportTest` en consecuencia.

### BuilderFileStore (E/S de disco, testable sin UI)
- **Ubicación:** `builder/.../io/BuilderFileStore.kt` (paquete nuevo `io`).
- **Responsabilidad:** leer/escribir un `File` ↔ `SduiEnvelope`, capturando cualquier fallo en `Result`.
  No abre diálogos (recibe el `File` ya elegido).
- **API (firmas):**
```kotlin
internal object BuilderFileStore {
    fun read(file: File): Result<SduiEnvelope> =
        runCatching { decodeEnvelope(file.readText(Charsets.UTF_8)) }

    /** Garantiza extensión .json y escribe UTF-8. */
    fun write(file: File, envelope: SduiEnvelope): Result<Unit> =
        runCatching { withJsonExtension(file).writeText(envelope.encodeToJson(), Charsets.UTF_8) }
}
```
- **Decisiones:** `Result` (no excepciones propagadas) para que la UI degrade a mensaje (HU-1.5, HU-2.5).
  Lectura/escritura síncrona (archivos pequeños). `withJsonExtension` añade `.json` si falta.

### FileDialogs (diálogos nativos, no testeable → smoke)
- **Ubicación:** `builder/.../ui/FileDialogs.kt`.
- **Responsabilidad:** abrir el diálogo nativo de selección de archivo; devolver `File?` (`null` =
  cancelar, HU-1.6/HU-2.6).
- **API (firmas):**
```kotlin
internal fun openFileDialog(): File?               // FileDialog.LOAD, filtro *.json
internal fun saveFileDialog(suggestedName: String): File?  // FileDialog.SAVE, propone *.json
```
- **Decisiones:** `java.awt.FileDialog(null as Frame?, title, mode)` (AWT) en vez de `JFileChooser`:
  diálogo **nativo** del SO, sin dependencias extra, integración natural con Compose Desktop. Filtro de
  nombre `*.json`.

### BuilderDocument (estado del documento — ampliado)
- **Ubicación:** `builder/.../model/BuilderDocument.kt`.
- **Responsabilidad:** además del árbol/selección actuales, llevar identidad de archivo, estado de
  modificación y los metadatos del envelope que el builder no edita.
- **API (firmas nuevas/cambiadas):**
```kotlin
@Stable
class BuilderDocument {
    var root: SduiNode ...; private set
    var selectedId: String? ...; private set
    var currentFile: File? by mutableStateOf(null); private set
    var isModified: Boolean by mutableStateOf(false); private set

    // Metadatos preservados del envelope (no editables en v1, HU-5):
    private var schemaVersion: Int = 1
    private var screenId: String = "builder"
    private var variables: Map<String, JsonElement> = emptyMap()
    private var meta: Map<String, String> = emptyMap()

    fun toEnvelope(): SduiEnvelope =
        SduiEnvelope(schemaVersion, screenId, root, variables, meta)

    /** Reemplaza el documento por [envelope]; ids únicos en todo el árbol; selecciona la raíz; no modificado. */
    fun load(envelope: SduiEnvelope, file: File?) { … }

    /** Documento en blanco (columna raíz "root"), sin archivo, no modificado (HU-3.1). */
    fun newDocument() { … }

    /** Tras guardar OK: fija currentFile y limpia isModified (HU-4.2). */
    fun markSaved(file: File) { … }

    // insert/delete/updateProps/updateModifier: además marcan isModified = true (HU-4.1)
}
```
- **Decisiones:**
  - `load` aplica `TreeOps.ensureUniqueTree(envelope.root, mutableSetOf())` para mantener el invariante de
    ids únicos del builder (HU-2.2). Si la raíz ya trae id único, no se altera (fidelidad de round-trip).
  - **Fix necesario:** `delete` hoy hace fallback a `selectedId = "root"` literal; pasa a usar `root.id`
    (la raíz cargada puede no llamarse "root"). Es un fix puntual de robustez, no de comportamiento para
    documentos creados desde cero.
  - Defaults de documento nuevo = los del export actual (`schemaVersion=1`, `screenId="builder"`,
    `variables`/`meta` vacíos), por lo que el JSON de un documento creado desde cero no cambia respecto a
    014.

### BuilderApp / Toolbar (UI)
- **Ubicación:** `builder/.../ui/BuilderApp.kt` (+ helpers de toolbar/diálogos en el mismo paquete).
- **Responsabilidad:** botonera Nuevo/Abrir/Guardar/Guardar como…, título con nombre de archivo y
  marcador de modificado (HU-4.3), orquestación de confirmación (HU-2.7/HU-3.2) y diálogos de error
  (HU-1.5/HU-2.5).
- **Estado UI:**
```kotlin
var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) } // acción a confirmar si isModified
var errorMessage: String? by remember { mutableStateOf(null) }        // AlertDialog de error
```
- **Decisiones:** "Guardar" usa `currentFile` si existe; si no, cae a `saveFileDialog` (HU-1.4). La
  confirmación es un `AlertDialog` de Compose; los diálogos de archivo son nativos (AWT). El panel
  "Exportar JSON" de 014 se mantiene (export a panel) y convive con Guardar (export a disco).

## Modelo de datos y estados
- No hay tipos nuevos de dominio: se reutiliza `SduiEnvelope`/`SduiNode` de `:sdui-core`.
- Estado del documento: `root`, `selectedId`, `currentFile: File?`, `isModified: Boolean` +
  metadatos preservados (`schemaVersion`, `screenId`, `variables`, `meta`).
- Estado efímero de UI: `pendingAction` (confirmación de descarte) y `errorMessage` (diálogo de error).
- Resultado de E/S: `kotlin.Result<…>` (éxito/fallo) traducido a `errorMessage` legible en la UI.

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| _(ninguna)_ | — | — | AWT (`java.awt.FileDialog`) y `java.io.File` vienen con el JDK; codec reutiliza `DefaultSduiJson`. |

## Riesgos y mitigaciones
- **`java.awt.FileDialog` y EDT/Compose Desktop** → usar `FileDialog(null as Frame?, …)`; invocación
  síncrona desde el callback del botón. Smoke manual lo valida; si surge fricción de hilos, encapsulado en
  `FileDialogs` (un único punto a ajustar).
- **Raíz cargada con id ≠ "root"** rompía el fallback de `delete` → fix a `root.id` (ver arriba) + test.
- **Pérdida de `variables`/`meta`** al guardar (el builder no los edita) → se preservan en
  `BuilderDocument` y se reescriben vía `toEnvelope()`; cubierto por test de round-trip (HU-5).
- **`type` desconocido en el archivo** → ya degrada a `UnknownNode` en el preview (motor resiliente); no
  se rechaza la carga (decisión de requisitos).
- **Sobrescritura accidental con "Guardar"** → es el comportamiento pedido (HU-1.4); "Guardar como…"
  permite ruta nueva.

## Estrategia de verificación
- **`EnvelopeCodecTest`** (puro): `decodeEnvelope(encodeToJson(env))` ≡ `env` para un envelope con
  `variables`/`meta` no vacíos; `decodeEnvelope` de JSON inválido lanza (se captura en el store).
- **`BuilderFileStoreTest`** (temp files, sin UI): `write` luego `read` de un `File` temporal recupera el
  envelope; `write` añade `.json` si falta; `read` de archivo inexistente/JSON corrupto → `Result.failure`.
- **`BuilderDocumentTest`** (ampliado): `load` fija árbol/metadatos, ids únicos, selección raíz e
  `isModified=false`; una mutación pone `isModified=true`; `markSaved`/`newDocument` lo limpian;
  `toEnvelope` reproduce los metadatos cargados (round-trip de HU-5); `delete` con raíz id ≠ "root"
  resetea la selección a `root.id`.
- **Compilación/lint:** `./gradlew :builder:compileKotlin :builder:test detekt ktlintCheck` en verde.
- **Smoke manual (`:builder:run`):** crear árbol → Guardar como… (elige ruta) → cerrar/Nuevo →
  Abrir ese archivo → ver el árbol restaurado en preview/outline → marcador "modificado" aparece al
  editar y desaparece al Guardar → Abrir con cambios pide confirmación → JSON corrupto muestra error sin
  cerrar la app. (Si el harness no abre UI, se justifica con los tests de codec/store/document.)
