# Diseño — Builder de blueprints SDUI (desktop)

> Spec ID: 014 · Estado: draft · Trazabilidad: ./requirements.md

## Enfoque
Una app **Compose Desktop** en un módulo nuevo `:builder` que mantiene el documento como un `SduiNode`
mutable en estado, lo muta desde la **paleta**/**inspector** y lo renderiza con el **motor real**
(`RenderNode`) bajo los seams de `:shared`. La lógica no-UI (operaciones de árbol, catálogo, export) se
extrae a funciones puras testeables sin Compose. Toda mutación produce un `SduiNode` válido y
serializable; exportar = `SduiEnvelope` JSON con `DefaultSduiJson`.

**Exposición mínima en `:shared` (justificada):** los seams de la app (`appRegistry`, `rememberAppTheme`,
iconos, imágenes) son `internal`. Se añade **una** función pública `SduiPreviewEnvironment(...)` que
provee esos `CompositionLocals` con un `SduiActionHandler` inyectable. Es la única adición a `:shared`;
no cambia comportamiento del runtime (y puede DRY-ear `SduiHost` en el futuro).

## Arquitectura
- **Módulo nuevo `:builder`** (JVM, Compose Desktop `application{}`), molde = `:desktopApp`. Depende de
  `:sdui-core`, `:sdui-compose`, `:shared`, `compose.desktop.currentOs`.
  - `model/` — `BuilderDocument` (estado) + operaciones puras de árbol (`TreeOps`).
  - `catalog/` — `BuilderCatalog` (descriptores de paleta + `FieldSpec`).
  - `export/` — serialización a `SduiEnvelope` JSON.
  - `ui/` — `BuilderApp` (split layout), `PreviewPane`, `PalettePane`, `OutlinePane`, `InspectorPane`.
  - `Main.kt` — `application { Window { BuilderApp() } }`.
- **`:shared`** (editar, mínimo): `SduiPreviewEnvironment.kt` (público).
- **`:sdui-compose`/`:sdui-core`**: SIN cambios.
- settings: `include(":builder")`.

```
BuilderApp (Row)
 ├─ PreviewPane    : SduiPreviewEnvironment(LoggingHandler) { RenderNode(doc.root) }
 └─ Column (derecha)
     ├─ PalettePane   : categorías → PaletteEntry (click → TreeOps.insert)
     ├─ OutlinePane   : árbol de nodos (click → select; borrar)
     ├─ InspectorPane : FieldSpec del type seleccionado → edita props/modifier
     └─ Export        : DefaultSduiJson → JSON (panel/portapapeles)
```

## Componentes y contratos

### Exposición en `:shared` — `SduiPreviewEnvironment` (público)
```kotlin
/** Provee los seams de la app (registry/tema/iconos/imágenes/variables) con un handler inyectable. */
@Composable
fun SduiPreviewEnvironment(
    actionHandler: SduiActionHandler,
    variables: VariableScope = VariableScope { null },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComponentRegistry provides appRegistry,
        LocalKuisdTheme provides rememberAppTheme(),
        LocalIconRegistry provides remember { DefaultIconRegistry + appIconsOverride() },
        LocalImageRegistry provides remember { appImageRegistry() },
        LocalVariables provides variables,
        LocalSduiActionHandler provides actionHandler,
        content = content,
    )
}
```

### Estado del documento (`:builder/model`)
```kotlin
@Stable
class BuilderDocument {
    var root: SduiNode by mutableStateOf(SduiNode(type = "column", id = "root"))
        private set
    var selectedId: String? by mutableStateOf("root")
        private set

    fun select(id: String?) { selectedId = id }
    fun insert(template: SduiNode) { root = TreeOps.insert(root, selectedId, template) }
    fun delete(id: String) { root = TreeOps.delete(root, id); if (selectedId == id) selectedId = "root" }
    fun updateProps(id: String, props: JsonObject) { root = TreeOps.updateProps(root, id, props) }
    fun updateModifier(id: String, modifier: UiModifier) { root = TreeOps.updateModifier(root, id, modifier) }
}
```

### Operaciones de árbol puras (`TreeOps`, testeables sin UI)
```kotlin
internal object TreeOps {
    /** Inserta [template] (con id único) como hijo de [parentId] si admite hijos; si no, a la raíz. */
    fun insert(root: SduiNode, parentId: String?, template: SduiNode): SduiNode
    fun delete(root: SduiNode, id: String): SduiNode               // no borra la raíz
    fun updateProps(root: SduiNode, id: String, props: JsonObject): SduiNode
    fun updateModifier(root: SduiNode, id: String, modifier: UiModifier): SduiNode
    fun findById(root: SduiNode, id: String): SduiNode?
    fun ensureId(node: SduiNode, existing: Set<String>): SduiNode  // asigna id único si falta (determinista)
}
```
- **Identidad:** cada inserción asigna un `id` único (`"${type}-${n}"`, `n` = primer entero libre); sin
  `Random` → determinista y serializable. Selección/edición por `id`.
- **Contenedor:** el `PaletteEntry.acceptsChildren` marca si el `type` admite hijos (`column`/`row`/
  `card`/`surface`/`scaffold`); insertar con un `parentId` no-contenedor cae a la raíz (HU-2.4).

### Catálogo (`BuilderCatalog`, `:builder/catalog`)
```kotlin
enum class Category { Texto, Contenedores, Estructura, Media }

enum class FieldEditor { Text, Bool, Enum }

enum class FieldTarget { Prop, Modifier }

data class FieldSpec(
    val key: String,                    // clave en props (o nombre de campo de UiModifier)
    val label: String,
    val editor: FieldEditor,
    val options: List<String> = emptyList(),   // para Enum (p.ej. contentScale, tokens de estilo)
    val target: FieldTarget = FieldTarget.Prop,
)

data class PaletteEntry(
    val type: String,
    val category: Category,
    val label: String,
    val acceptsChildren: Boolean,
    val template: SduiNode,             // nodo plantilla con props por defecto válidas
    val fields: List<FieldSpec>,
)

val builderCatalog: List<PaletteEntry>   // subconjunto curado de CorePack
```
- **Decisión:** catálogo **hecho a mano** (no introspección del `ComponentRegistry`): los serializers no
  exponen el esquema de campos de forma práctica; un descriptor explícito da control sobre editores,
  opciones (tokens) y plantillas válidas.

### Preview, handler y export
```kotlin
/** Handler del preview: no navega ni hace red; acumula las acciones en un log observable (HU-5.2). */
class LoggingActionHandler : SduiActionHandler { val log: SnapshotStateList<String>; /* handle → log */ }

/** Export (HU-6): envelope JSON idéntico al del BFF; para mostrar se usa un Json "pretty" aparte. */
internal fun exportEnvelope(root: SduiNode): String =
    DefaultSduiJson.encodeToString(
        SduiEnvelope.serializer(),
        SduiEnvelope(schemaVersion = 1, screenId = "builder", root = root),
    )
```

## Modelo de datos y estados
- Estado en `:builder` (no en el motor): `BuilderDocument` (`root` + `selectedId`), `LoggingActionHandler`.
- El documento es siempre un `SduiNode` serializable (invariante mantenido por `TreeOps`).
- Inspector: edita `props` (JsonObject) y un subconjunto de `UiModifier` (p.ej. `fillMaxWidth`); valores
  inválidos se normalizan/ignoran (HU-4.4).

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| `compose.desktop.currentOs` | (del plugin compose) | `:builder` | app Compose Desktop |
> Reutiliza `:sdui-compose`/`:shared`/`:sdui-core` y libs ya presentes (runtime/foundation/material3).
> No se añaden librerías al catálogo de versiones.

## Riesgos y mitigaciones
- **Tocar `:shared`** → solo se AÑADE `SduiPreviewEnvironment` público (no cambia el runtime); cubierto
  por que `:shared:desktopTest`/compilación sigue verde y `SduiHost` no se altera.
- **Componentes que requieren host/estado** (textField two-way, bottomBar selección) → se renderizan
  como vista de diseño; su interacción dinámica no se ejercita (documentado, HU-5).
- **Tipo no registrado / plantilla inválida** → `UnknownNode` (004) en preview; test de catálogo asegura
  que toda plantilla decodifica y su `type` está en `appRegistry`.
- **`application{}` + toolchain javaHome** → se replica el patrón de `:desktopApp` (provider lazy del
  launcher) para evitar `UnsupportedClassVersionError`.
- **Preview de un árbol en construcción** (contenedor vacío) → Material/engine lo toleran; sin crash.

## Estrategia de verificación
- **Unit `:builder` (test JVM):**
  - `TreeOpsTest` — insert (en contenedor seleccionado vs raíz; no-contenedor→raíz), delete (no borra
    raíz; limpia selección), updateProps/updateModifier, findById, ensureId (ids únicos/deterministas).
  - `BuilderCatalogTest` — cada `PaletteEntry.template` (de)serializa por `DefaultSduiJson` y su `type`
    está en `appRegistry`; categorías no vacías.
  - `ExportTest` — `exportEnvelope(root)` re-parsea a un `SduiEnvelope` equivalente (round-trip HU-6.3).
- **Compilación:** `:builder:compileKotlin` (UI Compose Desktop) en verde; `:shared` compila con la
  nueva API pública.
- **Smoke manual:** `./gradlew :builder:run` — añadir componentes desde la paleta, seleccionar en el
  outline, editar una prop (p.ej. `text`), ver el preview cambiar, exportar JSON y comprobar que parsea.
- **Calidad:** `:builder:test`, `:shared:desktopTest`, `detekt`, `ktlintCheck`.
