# Diseño — Estructura de pantalla (`scaffold` · `topAppBar` · `bottomBar`)

> Spec ID: 010 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Tres componentes nuevos en `CorePack` (`:sdui-compose`), siguiendo el mismo molde que 008: un
`@Serializable …Props` por componente, un `register(sduiComponent<…>("type")) { … }` y un renderer
privado `@Composable RenderScope.(…)` que aplica el `modifier` resuelto (008) a su composable raíz y
delega acciones vía `LocalSduiActionHandler`. **No se toca el contrato `:sdui-core`**: los slots del
`scaffold` se infieren del `type` de cada child (decisión de requisitos). El estado de selección del
`bottomBar` no vive en el motor: es una **variable local** (005) que el server muta con `setVar` desde
el `onClick` del ítem; el motor solo la **lee reactivamente** para pintar el ítem activo. La lógica no
visual (partición de slots, ítem seleccionado) se extrae a **funciones puras** testeables sin UI.

## Arquitectura
- Módulo/source set único: `sdui-compose/src/commonMain/kotlin/dev/kuisd/sdui/`.
  - `CorePack.kt` — registra `scaffold`, `topAppBar`, `bottomBar`; añade los `…Props`; renderers privados.
  - `ScaffoldSlots.kt` (nuevo) — función pura `partitionScaffoldSlots(children)` + helper de selección.
- Sin cambios en `:sdui-core`, `:server` (salvo pantalla piloto de demo en §verificación), ni en el host.
- Flujo de datos (bottomBar): `NavigationBarItem.onClick` → `handler.handle(item.onClick)` →
  `VariableActionHandler.setVar(selected, value)` (005) → recomposición → `selectedBind` lee el nuevo
  valor → el ítem cuyo `value` coincide se pinta `selected`. El motor nunca guarda el índice/seleccción.

```
scaffold (Scaffold)
 ├─ topAppBar      → slot topBar     (primero de su type; resto ignorado)
 ├─ bottomBar      → slot bottomBar  (primero de su type; resto ignorado)
 └─ <resto>        → slot content    (en orden, dentro de Box(padding = innerPadding))
```

## Componentes y contratos

### Partición de slots (puro)
- **Ubicación:** `sdui-compose/.../ScaffoldSlots.kt`
- **Responsabilidad:** separar `children` en (topBar?, bottomBar?, content[]) por `type`, sin UI.
- **API (firmas):**
```kotlin
internal data class ScaffoldSlots(
    val topBar: SduiNode?,
    val bottomBar: SduiNode?,
    val content: List<SduiNode>,
)

/** topBar/bottomBar = PRIMER child de ese type; content = el resto en orden. Determinista (HU-1.2/1.5). */
internal fun partitionScaffoldSlots(children: List<SduiNode>): ScaffoldSlots
```
- **Decisiones:** primero-gana por type; los duplicados de barra se descartan (no caen a content) para
  evitar pintar dos barras apiladas. Función pura → test directo sin Compose.

### `scaffold`
- **Ubicación:** `CorePack.kt`
- **Responsabilidad:** montar `material3.Scaffold` con los slots resueltos y propagar `innerPadding` al
  contenido (HU-1.1/1.3/1.4/1.6).
- **API (firmas):**
```kotlin
@Serializable
class ScaffoldProps   // sin campos propios: todo viene de children + UiModifier del nodo

register(sduiComponent<ScaffoldProps>("scaffold")) { ScaffoldRenderer(modifier) }

@Composable
private fun RenderScope.ScaffoldRenderer(baseModifier: Modifier) {
    val slots = remember(node.children) { partitionScaffoldSlots(node.children) }
    Scaffold(
        modifier = baseModifier,
        topBar = { slots.topBar?.let { RenderNode(it) } },
        bottomBar = { slots.bottomBar?.let { RenderNode(it) } },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            slots.content.forEach { RenderNode(it) }
        }
    }
}
```
- **Decisiones:** el `padding(innerPadding)` se aplica en un `Box` envoltorio del content (RenderNode no
  acepta modifier); cada child de content conserva su propio `UiModifier`. `topBar`/`bottomBar` ausentes
  ⇒ lambda vacía (slot omitido por Material).

### `topAppBar`
- **Ubicación:** `CorePack.kt`
- **Responsabilidad:** `material3.TopAppBar` con título bindable, icono de navegación opcional y children
  como acciones (HU-2).
- **API (firmas):**
```kotlin
@Serializable
data class TopAppBarProps(
    val title: String = "",
    val navigationIcon: String? = null,   // nombre resoluble por IconRegistry (008)
)

register(sduiComponent<TopAppBarProps>("topAppBar")) { p -> TopAppBarRenderer(p, modifier) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderScope.TopAppBarRenderer(p: TopAppBarProps, baseModifier: Modifier) {
    val handler = LocalSduiActionHandler.current
    val navActions = node.actions["onNavigationClick"].orEmpty()
    TopAppBar(
        modifier = baseModifier,
        title = { Text(bind(p.title)) },
        navigationIcon = {
            if (navActions.isNotEmpty()) {
                val vector = LocalIconRegistry.current.get(p.navigationIcon.orEmpty())
                    ?: Icons.AutoMirrored.Outlined.HelpOutline
                IconButton(onClick = { handler.handle(navActions) }) {
                    Icon(vector, contentDescription = null)
                }
            }
        },
        actions = { renderChildren() },   // children → iconButton(s) a la derecha (RowScope)
    )
}
```
- **Decisiones:** el icono de navegación se muestra **solo si** hay `onNavigationClick` (HU-2.3); las
  acciones de la derecha reutilizan `renderChildren()` (cada `iconButton` ya delega su `onClick`, 008).
  `@OptIn` acotado al renderer (patrón 007).

### `bottomBar` + `bottomBarItem`
- **Ubicación:** `CorePack.kt`
- **Responsabilidad:** `material3.NavigationBar` con un `NavigationBarItem` por child de type
  `bottomBarItem`; marca seleccionado el que casa con la variable `selectedBind` (HU-3).
- **API (firmas):**
```kotlin
@Serializable
data class BottomBarProps(
    val selectedBind: String? = null,   // nombre DIRECTO de la variable (como textField.bind, 007)
)

@Serializable
data class BottomBarItemProps(
    val icon: String = "",     // nombre IconRegistry (008)
    val label: String = "",    // bindable (005)
    val value: String = "",    // identidad del ítem; se compara con el valor de selectedBind
)

register(sduiComponent<BottomBarProps>("bottomBar")) { p -> BottomBarRenderer(p, modifier) }

@Composable
private fun RenderScope.BottomBarRenderer(p: BottomBarProps, baseModifier: Modifier) {
    val handler = LocalSduiActionHandler.current
    val vars = LocalVariables.current
    // Lectura reactiva: si selectedBind != null, esta get() registra dependencia de snapshot (005).
    val selectedValue = p.selectedBind?.let { vars.get(it)?.asDisplayString() }
    NavigationBar(modifier = baseModifier) {
        node.children.forEach { child ->
            val item = decodeOrNull<BottomBarItemProps>(child) ?: return@forEach  // no-item → se ignora
            NavigationBarItem(
                selected = isItemSelected(selectedValue, item.value),
                onClick = { handler.handle(child.actions["onClick"].orEmpty()) },
                icon = {
                    val v = LocalIconRegistry.current.get(item.icon) ?: Icons.AutoMirrored.Outlined.HelpOutline
                    Icon(v, contentDescription = null)
                },
                label = { Text(bind(item.label)) },
            )
        }
    }
}

/** Selección pura (HU-3.3/3.5): null/no-resuelto ⇒ false. */
internal fun isItemSelected(selectedValue: String?, itemValue: String): Boolean =
    selectedValue != null && selectedValue == itemValue
```
- **Decisiones:**
  - `bottomBarItem` **no se registra** como componente standalone: solo tiene sentido dentro de
    `bottomBar`, que decodifica sus props directamente (`decodeOrNull`) y construye el `NavigationBarItem`
    (Material exige que el item sea hijo directo del `RowScope` de `NavigationBar`). Un `bottomBarItem`
    suelto en otro contenedor cae a `UnknownNode` (resiliencia 004, HU-4.2).
  - `selectedBind` es el **nombre directo** de la variable (sin `$`), consistente con `textField.bind`
    (007). Reconcilia HU-3.3: el motor lo lee reactivamente; el `$` no es necesario porque no es un
    string de display sino un identificador de variable.
  - `decodeOrNull<T>(node)` reutiliza `DefaultSduiJson.decodeFromJsonElement` con `runCatching` (mismo
    patrón que `RegisteredComponent.Render`); props inválidas ⇒ el ítem se ignora sin crash.

## Modelo de datos y estados
- **Sin estado nuevo del motor.** La selección del `bottomBar` es una variable del `VariableStore`
  (005) sembrada por el server (`envelope.variables`) y mutada por `setVar` desde el `onClick` del ítem.
- Helpers puros añadidos: `partitionScaffoldSlots`, `isItemSelected`, `decodeOrNull` (interno).
- `ScaffoldProps` sin campos (paralelo a `ColumnProps`/`LazyColumnProps`).

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| — | — | — | Ninguna. Todo usa `material3` + `material-icons` ya presentes (007/008). |

## Riesgos y mitigaciones
- **`Scaffold`/`TopAppBar` experimentales** → `@OptIn(ExperimentalMaterial3Api::class)` acotado a cada
  renderer (no contamina `CorePack`), igual que `textField` (007).
- **content con innerPadding** → el `Box(padding)` envuelve a TODO el content; si el content quiere ser
  scrollable a pantalla completa (lazyColumn), el padding del Box sigue siendo correcto (Material lo
  recomienda). Documentado; sin recorte de insets adicional en esta versión.
- **`selectedBind` con `$` por error** → no se resuelve (busca variable llamada literalmente `$x`) ⇒
  todos los ítems no seleccionados (degradación visible, sin crash). Se documenta el formato en server.
- **Duplicado de barras** → `partitionScaffoldSlots` descarta la 2ª; cubierto por test unitario.

## Estrategia de verificación
- **Unit (commonTest, sin UI):**
  - `ScaffoldSlotsTest` — `partitionScaffoldSlots`: (a) topBar+bottomBar+content mixto en orden;
    (b) sin barras ⇒ todo content; (c) dos `topAppBar` ⇒ se usa el 1º y el 2º se descarta (no a content);
    (d) lista vacía ⇒ slots vacíos.
  - `BottomBarSelectionTest` — `isItemSelected`: match, no-match, `selectedValue==null` ⇒ false.
  - `ComponentRegistryTest` (ampliar) — `CorePack.rendererFor("scaffold"/"topAppBar"/"bottomBar")` ≠ null.
- **Smoke manual / e2e:** migrar una pantalla piloto del server a `scaffold` (candidata: `home`, con
  `topAppBar` título + `bottomBar` de 3 secciones enlazadas por una variable `section`), arrancar
  `:server:run` y `:desktopApp:run`, verificar barras y que pulsar una sección la marca activa.
- **Calidad:** `./gradlew :sdui-compose:check detekt ktlintCheck` en verde.
- Comandos: `./gradlew :sdui-compose:check`, `./gradlew :server:build`, `./gradlew :desktopApp:run`.
