# Diseño — Contenedores principales en el builder (Scaffold)

> Spec ID: 016 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Introducir en el `:builder` el concepto de **contenedor principal** como una **tabla declarativa** (única
fuente de verdad) que la paleta, el outline y las operaciones de árbol consultan. Un contenedor principal
declara sus **slots**; cada slot dice qué `type`s de child captura (igual criterio que el motor:
`topAppBar`→topBar, `bottomBar`→bottomBar, resto→content). El builder sigue emitiendo `children` **planos**
—sin tocar `SduiNode` ni el motor—; los "slots explícitos" del outline son una **vista** pura sobre esos
children. La regla de raíz (solo un contenedor principal puede ser raíz; nada de anidarlos) se centraliza en
`isMainContainer(type)`, de modo que añadir un contenedor principal nuevo es **una entrada en la tabla**, sin
cambiar `TreeOps`/`BuilderApp`/`OutlinePane`.

## Arquitectura
Módulo afectado: **solo `:builder`**. `:sdui-core`/`:sdui-compose`/`:shared` SIN cambios.

```
catalog/MainContainers.kt   ── SlotSpec, MainContainerSpec, mainContainers (tabla), isMainContainer()  [NUEVO]
catalog/BuilderCatalog.kt   ── + entradas bottomBar / bottomBarItem; campo contentDirection del scaffold
model/SlotOps.kt            ── proyección children↔slots + setSingleSlot + normalize + hasMainContainer  [NUEVO, puro]
model/BuilderDocument.kt    ── emptyRoot = scaffold; selectedSlotId; insert ruteado por slot; wrapInScaffold
ui/OutlinePane.kt           ── raíz contenedor-principal con 3 slots explícitos (Top bar/Content/Bottom bar)
ui/BuilderApp.kt            ── selección de slot; diálogo de error CON acción opcional ("Arreglar"); openFlow valida raíz
```

Flujo de inserción (HU-3): `paleta → BuilderDocument.insert(template)` → según `type` y slot seleccionado se
rutea a `setSingleSlot` (topBar/bottomBar) o a `TreeOps.insert` en content. Flujo de carga (HU-4):
`read → si raíz es contenedor principal → load; si no → error + acción Arreglar → (sin scaffold anidado) wrap`.

## Componentes y contratos

### MainContainers — tabla declarativa (HU-1)
- **Ubicación:** `catalog/MainContainers.kt` (nuevo).
- **Responsabilidad:** declarar qué tipos son contenedor principal y sus slots; única fuente de verdad.
```kotlin
/** Un slot de un contenedor principal, como vista sobre los children planos del nodo. */
data class SlotSpec(
    val id: String,            // "topBar" | "content" | "bottomBar"
    val label: String,         // "Top bar" | "Content" | "Bottom bar"
    val multiple: Boolean,     // content = true; topBar/bottomBar = false
    val childTypes: Set<String>, // types que el motor extrae a este slot; vacío ⇒ "el resto" (content)
)

/** Declaración de un contenedor principal (solo-raíz) y sus slots, en orden de render. */
data class MainContainerSpec(val type: String, val slots: List<SlotSpec>) {
    val contentSlot: SlotSpec get() = slots.first { it.childTypes.isEmpty() }
    val reservedChildTypes: Set<String> get() = slots.flatMapTo(mutableSetOf()) { it.childTypes }
    fun slotForChildType(type: String): SlotSpec = slots.firstOrNull { type in it.childTypes } ?: contentSlot
}

/** Única fuente de verdad. v1: solo scaffold (orden = topBar, content, bottomBar, como el motor). */
val mainContainers: Map<String, MainContainerSpec> = mapOf(
    "scaffold" to MainContainerSpec(
        type = "scaffold",
        slots = listOf(
            SlotSpec("topBar", "Top bar", multiple = false, childTypes = setOf("topAppBar")),
            SlotSpec("content", "Content", multiple = true, childTypes = emptySet()),
            SlotSpec("bottomBar", "Bottom bar", multiple = false, childTypes = setOf("bottomBar")),
        ),
    ),
)

fun isMainContainer(type: String): Boolean = type in mainContainers
```
- **Decisión:** los `childTypes` espejan exactamente `partitionScaffoldSlots` del motor (topAppBar/bottomBar/
  resto). Si mañana se añade otro contenedor principal, basta otra entrada en `mainContainers`.

### SlotOps — proyección y mutación de slots (puro, testeable)
- **Ubicación:** `model/SlotOps.kt` (nuevo). Sin Compose.
- **Responsabilidad:** ver los children planos como slots y mutar el slot único; normalizar al cargar.
```kotlin
internal object SlotOps {
    /** Proyecta los children de [node] sobre los slots de [spec] (1.º gana en slots únicos; resto = content). */
    fun project(node: SduiNode, spec: MainContainerSpec): Map<String, List<SduiNode>>

    /** Reemplaza/inserta el child único de un slot mono-ocupante (topBar/bottomBar) y normaliza el orden. */
    fun setSingleSlot(root: SduiNode, spec: MainContainerSpec, slot: SlotSpec, child: SduiNode): SduiNode

    /** Reordena children a [topBar?, content…, bottomBar?] dejando ≤1 nodo por slot único (extras → content). */
    fun normalize(root: SduiNode, spec: MainContainerSpec): SduiNode

    /** ¿Hay algún contenedor principal en el subárbol de [node] (incluido [node])? (HU-4.4) */
    fun containsMainContainer(node: SduiNode): Boolean
}
```
- **Decisiones:**
  - `project` es la **única** función de partición (por identidad: 1.º de cada slot único gana; lo no tomado
    —incluidos duplicados de un type reservado— cae a content, **visible/borrable**, no fantasma).
    `normalize` se define **sobre** `project` (aplana en orden de slots) → no hay dos criterios que puedan
    divergir. Se aplica en `load` para JSON determinista en round-trip.
  - El content reutiliza `TreeOps` existente (insert anidado, delete, updateProps/Modifier) operando sobre
    `root.children`; `SlotOps` solo añade lo específico de slots únicos.

### BuilderCatalog — entradas y campos (HU-3.1)
- **Ubicación:** `catalog/BuilderCatalog.kt` (amplía).
- **Cambios:**
  - `scaffold`: añadir campo editable `contentDirection` (Enum, prop, opciones `["column", "row"]`).
  - Nueva entrada **`bottomBar`** (categoría Estructura, `acceptsChildren = true`, plantilla
    `SduiNode(type = "bottomBar")`, campo opcional `selectedBind` Text).
  - **`bottomBarItem` NO entra en la paleta**: el motor no lo registra como componente standalone (se
    decodifica dentro del renderer de `bottomBar`), y el invariante `BuilderCatalogTest`·"cada type registrado
    en CorePack" lo rechazaría. Poblar ítems del bottomBar queda fuera de alcance (ya en requirements).
- **Decisión:** `topAppBar` ya existe; con `bottomBar` quedan cubiertos los dos slots únicos. El builder NO
  añade `scaffold` como inserción libre de la paleta (se filtra: es contenedor principal, solo-raíz; ver
  PalettePane).

### BuilderDocument — raíz scaffold, selección de slot, inserción ruteada (HU-2/HU-3)
- **Ubicación:** `model/BuilderDocument.kt` (amplía 015).
- **Cambios de estado:**
```kotlin
private val emptyRoot: SduiNode get() = SduiNode(type = "scaffold", id = "root") // HU-2.2 (antes: column)
var selectedSlotId: String? by mutableStateOf(null); private set                  // slot del root seleccionado
private val rootSpec: MainContainerSpec get() = mainContainers.getValue(root.type) // root siempre es principal
```
- **Selección:**
```kotlin
fun select(id: String?) { selectedId = id; selectedSlotId = null }
fun selectSlot(slotId: String) { selectedSlotId = slotId; selectedId = null }
```
- **Inserción ruteada (HU-2.5/HU-3.3-3.5):**
```kotlin
fun insert(template: SduiNode) {
    val type = template.type
    when {
        isMainContainer(type) -> return signalRejected("Un $type solo puede ir en la raíz.")   // HU-2.5
        selectedSingleSlot() != null -> {                                                       // slot único seleccionado
            val slot = selectedSingleSlot()!!
            if (type in slot.childTypes) root = SlotOps.setSingleSlot(root, rootSpec, slot, ensureUnique(template))
            else return signalRejected("En «${slot.label}» solo va ${slot.childTypes.joinToString("/")}.")
        }
        type in rootSpec.reservedChildTypes ->                                                  // auto-rutea a su slot
            root = SlotOps.setSingleSlot(root, rootSpec, rootSpec.slotForChildType(type), ensureUnique(template))
        else -> root = TreeOps.insert(root, contentParentId(), template, containerTypes)         // content (HU-3.5)
    }
    isModified = true
}
```
  - `selectedSingleSlot()` = el `SlotSpec` no-múltiple cuyo id == `selectedSlotId`, si lo hay.
  - `contentParentId()` = `selectedId` si apunta a un nodo de content contenedor (anida), si no `null` (raíz
    de content → `root.children`).
  - `signalRejected(msg)` deja un `lastError: String?` que la UI muestra (no muta el árbol). Alternativa:
    exponer `Result`. Se elige `lastError` observable para encajar con el `errorMessage` de BuilderApp.
- **`delete` (HU-2.4):** sin cambios respecto a 015 (`TreeOps.delete` nunca borra la raíz; fallback a
  `root.id`). Los nodos de slot tienen id y se borran como cualquier child.
- **`newDocument`/`load`:** root scaffold por defecto; `load` aplica `SlotOps.normalize(root, spec)` cuando la
  raíz cargada es contenedor principal (HU-4.1) tras `ensureUniqueTree`.
- **Wrap (HU-4.3):**
```kotlin
/** Envuelve [tree] como único content de un scaffold nuevo (id "root"); precondición: sin main containers dentro. */
fun loadWrapped(envelope: SduiEnvelope, file: File?)  // root = scaffold(children = [tree]); isModified = true
```

### BuilderApp — selección de slot, carga validada, diálogo con acción (HU-3.2/HU-4)
- **Ubicación:** `ui/BuilderApp.kt` (amplía 015).
- **openFlow (HU-4.1-4.5):**
```kotlin
fun openFlow() {
    val file = openFileDialog() ?: return
    BuilderFileStore.read(file)
        .onSuccess { env ->
            when {
                isMainContainer(env.root.type) -> document.load(env, file)                 // 4.1
                SlotOps.containsMainContainer(env.root) ->                                  // 4.4
                    errorMessage = ErrorMsg("La raíz no es un contenedor principal y contiene un scaffold anidado: no se puede arreglar.")
                else -> errorMessage = ErrorMsg(                                            // 4.2 + acción 4.3
                    text = "La raíz debe ser un contenedor principal.",
                    fix = "Arreglar (envolver en scaffold)" to { document.loadWrapped(env, file) },
                )
            }
        }
        .onFailure { errorMessage = ioErrorMessage("abrir", file, it) }   // sin cambios (string simple)
}
```
- **Estado de error con acción opcional:**
```kotlin
data class ErrorMsg(val text: String, val fix: Pair<String, () -> Unit>? = null)
var errorMessage: ErrorMsg? // el ErrorDialog muestra "Aceptar" y, si fix != null, un botón con fix.first
```
  (Se generaliza el `errorMessage: String?` de 015 a `ErrorMsg`; `ioErrorMessage` devuelve `ErrorMsg(text)`.)
- **Selección de slot e inserción rechazada:** `RightColumn`/`Toolbar` pasan `document.selectedSlotId`,
  `document::selectSlot`; el `lastError` de inserción rechazada se enruta al mismo `ErrorMsg`.
- **PalettePane:** filtra los `type` que son contenedor principal (no se insertan desde la paleta; solo
  existen como raíz). Firma: `PalettePane(onAdd, modifier)` sin cambios; el filtro va en `catalogByCategory`
  consumido o en un `paletteEntries = builderCatalog.filterNot { isMainContainer(it.type) }`.

### OutlinePane — slots explícitos sobre la raíz (HU-3.2)
- **Ubicación:** `ui/OutlinePane.kt` (reestructura).
- **Contrato:**
```kotlin
internal fun OutlinePane(
    root: SduiNode,
    selectedId: String?,
    selectedSlotId: String?,
    onSelect: (String?) -> Unit,
    onSelectSlot: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```
- **Render:** si `isMainContainer(root.type)`: fila de la raíz (seleccionable, no borrable) y, por cada
  `SlotSpec` de su spec, una **cabecera de slot** seleccionable (resalta si `selectedSlotId == slot.id`)
  seguida de los nodos del slot (vía `project`) renderizados con el `OutlineNode` actual (selección/borrado
  por id, raíz protegida por `root.id` — fix de 015). Si la raíz no fuera contenedor principal (no debería
  pasar tras la validación de carga), cae al árbol plano actual (defensivo).

## Modelo de datos y estados
- Sin tipos nuevos de dominio (`SduiNode`/`SduiEnvelope` intactos). Los slots son una **vista**.
- Estado del documento: + `selectedSlotId: String?` y `lastError`/`ErrorMsg` (acción opcional de arreglo).
- La estructura en disco sigue siendo `children` planos; `normalize` garantiza orden estable
  `[topBar?, content…, bottomBar?]`.

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| _(ninguna)_ | — | — | Solo lógica/UI en `:builder`; reutiliza catálogo, `TreeOps`, `BuilderDocument`, render de preview. |

## Riesgos y mitigaciones
- **Raíz por defecto pasa de `column` a `scaffold`** → rompe el JSON por defecto de 015 y sus tests. Mitigación:
  actualizar `BuilderDocumentTest` (defaults, newDocument) y cualquier aserción de tipo de raíz; el contrato de
  export no cambia (sigue `schemaVersion=1`/`screenId="builder"`).
- **`topAppBar`/`bottomBar` insertados como content** (comportamiento 014) → ahora se autorrutean a su slot
  único; un segundo bar reemplaza al anterior (HU-3.3/3.4). Cubierto por test de `setSingleSlot`.
- **Archivo con 2+ topAppBar** → `normalize` deja el 1.º como slot y degrada extras a content (visible, no
  fantasma), igual que el motor descarta duplicados al renderizar. Test de `normalize`.
- **Wrap con scaffold anidado** (HU-4.4) → `containsMainContainer` bloquea el arreglo con error; test.
- **Inserción de un contenedor principal en cualquier punto** (HU-2.5) → `insert` la rechaza siempre; la
  paleta además los oculta. Test.
- **Slot único seleccionado + componente que no corresponde** → rechazo con mensaje, sin mutar (HU-3.3/3.4).

## Estrategia de verificación
- **`MainContainersTest`** (puro): `isMainContainer("scaffold")` true / otros false; `slotForChildType` mapea
  `topAppBar`→topBar, `bottomBar`→bottomBar, resto→content; `reservedChildTypes == {topAppBar, bottomBar}`.
- **`SlotOpsTest`** (puro): `project` parte como el motor (1.º gana en slots únicos; resto en orden a content);
  `setSingleSlot` inserta y **reemplaza** el bar existente sin duplicar y normaliza orden; `normalize` deja
  ≤1 bar por slot y degrada extras a content; `containsMainContainer` detecta scaffold anidado y lo niega en
  árboles sin él.
- **`BuilderDocumentTest`** (amplía): documento nuevo tiene raíz `scaffold` "root"; `insert(topAppBar)` cae en
  topBar y un segundo lo reemplaza; `insert(bottomBar)` cae en bottomBar; `insert(text)` cae en content;
  `insert(scaffold)` se rechaza (árbol intacto, `lastError` set); con slot único seleccionado e inserción no
  acorde → rechazo; `loadWrapped` produce `scaffold(children=[árbol])`; `load` de raíz contenedor principal es
  fiel (round-trip); `delete` no borra la raíz.
- **`BuilderDocumentTest`/`EnvelopeCodecTest` de 015**: actualizados al nuevo default (raíz scaffold).
- **Compilación/lint:** `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- **Smoke manual (`:builder:run`):** Nuevo → raíz Scaffold con 3 slots; añadir Barra superior → aparece en Top
  bar; añadir Texto en Content → se apila; añadir Barra inferior → Bottom bar; intentar Scaffold desde paleta
  → no aparece/!rechazo; Abrir archivo con raíz `column` → error con botón "Arreglar" → al pulsarlo queda
  envuelto en scaffold; abrir uno con scaffold anidado bajo raíz no-principal → error sin arreglo.
