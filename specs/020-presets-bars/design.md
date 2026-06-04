# Diseño — Presets de barras + FAB + top bar centrado

> Spec ID: 020 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Dos bloques. **Motor (`:sdui-compose`)**: el `scaffold` gana un slot **FAB** (por `type == "fab"`, mismo
criterio de partición que topBar/bottomBar) montado en `Scaffold(floatingActionButton)`, un componente `fab`
nuevo, y `fabPosition` en `ScaffoldProps`; el `topAppBar` gana `centered` para usar `CenterAlignedTopAppBar`.
Todo aditivo con defaults ⇒ sin regresión. **Builder (`:builder`)**: el `MainContainerSpec` del scaffold
añade el slot FAB; la paleta gana el componente `fab` y **presets** (variantes ricas de topAppBar/bottomBar/
fab). Para que varios presets compartan `type` sin colisiones, se **desacopla la identidad de la entrada
(`key`) del `type` del nodo**, y el inspector usa solo las entradas **base** (no-preset) por type.

## Componentes y contratos — motor (`:sdui-compose`)

### ScaffoldSlots + partición (FAB)
```kotlin
internal data class ScaffoldSlots(
    val topBar: SduiNode?, val bottomBar: SduiNode?, val fab: SduiNode?, val content: List<SduiNode>,
)
internal fun partitionScaffoldSlots(children: List<SduiNode>): ScaffoldSlots {
    var topBar: SduiNode? = null; var bottomBar: SduiNode? = null; var fab: SduiNode? = null
    val content = mutableListOf<SduiNode>()
    children.forEach { child ->
        when (child.type) {
            "topAppBar" -> if (topBar == null) topBar = child
            "bottomBar" -> if (bottomBar == null) bottomBar = child
            "fab" -> if (fab == null) fab = child
            else -> content += child
        }
    }
    return ScaffoldSlots(topBar, bottomBar, fab, content)
}
```

### ScaffoldProps + ScaffoldRenderer
```kotlin
@Serializable data class ScaffoldProps(
    val contentDirection: String = "column",
    val fabPosition: String = "end", // "end" | "center"
)
// en ScaffoldRenderer:
floatingActionButton = { slots.fab?.let { RenderNode(it) } },
floatingActionButtonPosition = if (p.fabPosition == "center") FabPosition.Center else FabPosition.End,
```
- `fabPosition` desconocido ⇒ `End` (resiliencia).

### Componente `fab`
```kotlin
@Serializable data class FabProps(val icon: String = "")
register(sduiComponent<FabProps>("fab")) { p ->
    val handler = LocalSduiActionHandler.current
    FloatingActionButton(onClick = { handler.handle(node.actions["onClick"].orEmpty()) }, modifier = modifier) {
        Icon(resolveIcon(p.icon), contentDescription = null)
    }
}
```

### TopAppBar centrado
```kotlin
@Serializable data class TopAppBarProps(
    val title: String = "", val navigationIcon: String? = null, val centered: Boolean = false,
)
// en TopAppBarRenderer: mismos title/navigationIcon/actions; ramificar el componente:
if (p.centered) CenterAlignedTopAppBar(modifier, windowInsets, title = {...}, navigationIcon = {...}, actions = { renderChildren() })
else TopAppBar(modifier, windowInsets, title = {...}, navigationIcon = {...}, actions = { renderChildren() })
```
- Para no duplicar los slots (`title`/`navigationIcon`/`actions`), se extraen a lambdas locales reutilizadas
  por ambas ramas. `@OptIn(ExperimentalMaterial3Api::class)` ya presente.

## Componentes y contratos — builder (`:builder`)

### PaletteEntry: identidad desacoplada del type
```kotlin
data class PaletteEntry(
    val type: String,
    val category: Category,
    val label: String,
    val acceptsChildren: Boolean,
    val template: SduiNode,
    val fields: List<FieldSpec>,
    val key: String = type,          // identidad de paleta (única); base ⇒ == type
    val isPreset: Boolean = false,   // true = variante de paleta; no aporta descriptor de inspector
)
```
- **`catalogByType` (inspector)** pasa a: `builderCatalog.filterNot { it.isPreset }.associateBy { it.type }`
  → una entrada base por type (sin colisión). Los presets NO definen `fields` (irrelevantes; el inspector usa
  el descriptor base del `type` del nodo).
- **`PalettePane`**: `items(entries, key = { it.key })` (antes `{ it.type }`) → sin claves duplicadas.
- El filtro de contenedores principales (`filterNot { isMainContainer(it.type) }`) y la inserción
  (`document.insert(entry.template)`) no cambian; el ruteo por slot de la 016 coloca topAppBar/bottomBar/fab en
  su slot único (reemplazo) y asigna ids únicos vía `uniquify` (HU-4.2/4.4).

### MainContainers: slot FAB en el scaffold
```kotlin
"scaffold" to MainContainerSpec("scaffold", listOf(
    SlotSpec("topBar", "Top bar", false, setOf("topAppBar")),
    SlotSpec("content", "Content", true, emptySet()),
    SlotSpec("bottomBar", "Bottom bar", false, setOf("bottomBar")),
    SlotSpec("fab", "FAB", false, setOf("fab")),
))
```
- `SlotOps.project/normalize` ya son genéricos ⇒ el outline muestra el slot FAB y `setSingleSlot` lo gestiona.
  `reservedChildTypes` pasa a `{topAppBar, bottomBar, fab}` ⇒ insertar un `fab` se autorrutea a su slot.

### Catálogo: base + campos nuevos + presets
- **Base `fab`** (Estructura, `acceptsChildren=false`, template `fab` props `icon="add"`, fields `[icon Text]`).
- **scaffold**: + `FieldSpec("fabPosition", "Posición FAB", Enum, options=["end","center"], target=Prop)`.
- **topAppBar**: + `FieldSpec("centered", "Título centrado", Bool, target=Prop)`.
- **Presets** (`isPreset=true`, `Category.Presets`, `fields=emptyList()`):
  | key | type | label | template |
  |-----|------|-------|----------|
  | `preset-topbar-nav` | topAppBar | Top Bar · Navegación | props{title,navigationIcon="arrowBack"}, actions{onNavigationClick:[NavigateBack]} |
  | `preset-topbar-actions` | topAppBar | Top Bar · Acciones | props{title}, children=[iconButton "search", iconButton "moreVert"] |
  | `preset-topbar-centered` | topAppBar | Top Bar · Centrado | props{title, centered=true} |
  | `preset-bottom-3` | bottomBar | Bottom · Menú (3) | children = 3× bottomBarItem (home/search/settings) |
  | `preset-bottom-5` | bottomBar | Bottom · Menú (5) | children = 5× bottomBarItem (+favorite/edit) |
  | `preset-fab` | fab | FAB | props{icon="add"} |
- **Nueva categoría** `Category.Presets` (para agrupar; el test "todas las categorías presentes" la cubre con
  estas entradas).

### Inspector
- Sin cambios estructurales: `fabPosition` (Enum-prop) y `centered` (Bool-prop) usan los editores existentes
  (EnumField/BoolField con target Prop). `catalogByType` ya filtra presets.

## Modelo de datos y estados
- Sin cambios en `:sdui-core`. Props nuevas en `:sdui-compose` (ScaffoldProps.fabPosition,
  TopAppBarProps.centered, FabProps) — aditivas con default. Builder: `PaletteEntry.key`/`isPreset`.

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| _(ninguna)_ | — | — | Material3 ya provee `FloatingActionButton`/`FabPosition`/`CenterAlignedTopAppBar`. |

## Riesgos y mitigaciones
- **Colisión de paleta por type duplicado** → `key` único + `catalogByType` filtra presets + `items(key={it.key})`.
- **Regresión del scaffold/topAppBar** → props con default (`fabPosition="end"`, `centered=false`); `fab` slot
  null ⇒ sin FAB; partición ignora ausencia de `fab`. Tests de no-regresión del motor.
- **`fab` debe estar registrado en CorePack** (lo exige `BuilderCatalogTest`·"cada type en CorePack") → se
  registra en el motor (HU-1.2).
- **Acción placeholder** `NavigateBack` es serializable (`@SerialName("navigateBack")`); round-trip del
  template cubierto por `BuilderCatalogTest`.
- **`CenterAlignedTopAppBar` duplicaría slots** → lambdas locales compartidas entre ambas ramas.

## Estrategia de verificación
- **Motor `:sdui-compose`** (jvmTest):
  - `ScaffoldSlotsTest` (amplía): un child `fab` va al slot fab; primero gana; sin fab ⇒ `fab == null`.
  - No-regresión: tests existentes verdes; props con default ⇒ render idéntico.
  - `CorePackPropsTest`/registro: `fab` registrado y decodifica; `ScaffoldProps`/`TopAppBarProps` decodifican
    con los campos nuevos por defecto.
- **Builder `:builder`**:
  - `BuilderCatalogTest` (amplía): cada preset hace round-trip; `catalogByType` tiene UNA entrada por type
    (base); `key`s únicos en todo `builderCatalog`; `fab` presente y registrado; `Category.Presets` presente.
  - `MainContainersTest`: el scaffold tiene 4 slots incl. `fab`; `reservedChildTypes` contiene `fab`;
    `slotForChildType("fab").id == "fab"`.
  - `BuilderDocumentTest`: `insert(fab)` cae en el slot FAB; `insert(preset topAppBar)` reemplaza el topBar con
    el subárbol (ids únicos, children del preset presentes).
- **Lint:** `:sdui-compose` + `:builder` detekt/ktlint; tests motor + builder.
- **Smoke (`:builder:run`):** insertar cada preset y verlo en el slot correcto; FAB visible (end/center); top
  bar centrado; FAB con icono; varios presets de topAppBar conviven en la paleta sin romper.
