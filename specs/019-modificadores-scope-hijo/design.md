# Diseño — Modificadores de scope por-hijo en Row/Column

> Spec ID: 019 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
El nudo es que `weight`/`align` de un hijo solo se pueden aplicar **dentro** del `ColumnScope`/`RowScope`
del contenedor (son `ParentDataModifier`), pero el modifier del hijo se construye dentro de su propio
renderer (`RenderScope.modifier`), que no conoce el scope. La solución: **threading de un `layoutModifier`
por-hijo** desde el contenedor hasta el renderer del hijo, vía `RenderNode → RegisteredComponent.Render →
RenderScope`. El contenedor (Column/Row), que SÍ está en su scope, calcula el `Modifier.weight(...)`/
`Modifier.align(...)` de cada hijo y lo pasa; el `RenderScope` lo antepone al modifier propio del hijo. Es
**retrocompatible** (default = `Modifier`, no-op). En el `:builder`, el inspector gana campos **contextuales**
(alignment con el eje del padre + weight numérico) cuando el nodo seleccionado es hijo de un Row/Column.

## Arquitectura
```
:sdui-compose
  RenderNode.kt            ── RenderNode(node, layoutModifier = Modifier)  [param nuevo, default no-op]
  ComponentRegistry.kt     ── RegisteredComponent.Render(node, layoutModifier = Modifier)
  RenderScope.kt           ── RenderScope(node, layoutModifier); modifier = layoutModifier.then(own)
  ScopeChildren.kt (NUEVO) ── ColumnScope.childLayout(UiModifier) / RowScope.childLayout(UiModifier)
  CorePack.kt              ── Column/Row renderers iteran hijos aplicando childLayout(child.modifier)
:builder
  model/TreeOps.kt         ── findParent(root, id): SduiNode?
  model/BuilderDocument.kt ── parentType(id): String?
  catalog/ModifierFields.kt── + weight (Text↔Float) + listas de opciones de alignment por eje
  ui/InspectorPane.kt      ── sección "Layout" contextual (alignment por eje del padre + weight)
  ui/BuilderApp.kt         ── pasa parentType al InspectorPane
```
`:sdui-core` SIN cambios.

## Componentes y contratos — motor

### Threading del layoutModifier
```kotlin
// RenderNode.kt
@Composable
fun RenderNode(node: SduiNode, layoutModifier: Modifier = Modifier) {
    val entry = LocalComponentRegistry.current.rendererFor(node.type)
        ?: run { UnknownNode(node.type); return }
    entry.Render(node, layoutModifier)
}

// ComponentRegistry.kt
@Composable
fun Render(node: SduiNode, layoutModifier: Modifier = Modifier) {
    val props = remember(node) { /* …igual… */ }
    if (props == null) { UnknownNode(node.type); return }
    with(RenderScope(node, layoutModifier)) { renderer(props) }
}

// RenderScope.kt
class RenderScope internal constructor(
    val node: SduiNode,
    private val layoutModifier: Modifier = Modifier,
) {
    val modifier: Modifier
        @Composable get() {
            val theme = LocalKuisdTheme.current
            val um = node.modifier
            val own = remember(um, theme) { um.toModifier(theme) }
            return layoutModifier.then(own) // scope (weight/align) antes del propio (size/padding/bg)
        }
    // horizontalAlignmentOrNull/verticalAlignmentOrNull/renderChildren/bind sin cambios
}
```
- **Decisión:** `layoutModifier.then(own)` — el orden no afecta a `weight`/`align` (parent-data), y mantiene
  `fillMaxWidth`/`padding`/`background` del hijo intactos (HU-2.4). Default `Modifier` ⇒ todos los renderers y
  call-sites existentes (LazyColumn/Row, slots del scaffold, surface/card) siguen idénticos (sin regresión).

### ScopeChildren — modifier de scope por-hijo (nuevo)
```kotlin
// ScopeChildren.kt  (extensiones de scope; aplican weight + align del UiModifier del hijo)
internal fun ColumnScope.childLayout(um: UiModifier): Modifier {
    var m: Modifier = Modifier
    um.weight?.takeIf { it > 0f }?.let { m = m.weight(it) }
    um.alignment.toHorizontalAlignment()?.let { m = m.align(it) }
    return m
}
internal fun RowScope.childLayout(um: UiModifier): Modifier {
    var m: Modifier = Modifier
    um.weight?.takeIf { it > 0f }?.let { m = m.weight(it) }
    um.alignment.toVerticalAlignment()?.let { m = m.align(it) }
    return m
}
```

### Column/Row renderers (CorePack)
```kotlin
register(sduiComponent<ColumnProps>("column")) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignmentOrNull() ?: Alignment.Start) {
        node.children.forEach { child -> RenderNode(child, childLayout(child.modifier)) }
    }
}
register(sduiComponent<RowProps>("row")) {
    Row(modifier = modifier, verticalAlignment = verticalAlignmentOrNull() ?: Alignment.Top) {
        node.children.forEach { child -> RenderNode(child, childLayout(child.modifier)) }
    }
}
```
- **Decisión:** se sustituye `renderChildren()` por el bucle con `childLayout` en Column/Row (eager) **y en el
  content del scaffold** (que es un Column/Row eager), para que el comportamiento por-hijo sea consistente.
  `renderChildren()` se conserva para el resto (surface/card/topBar/bottomBar) sin cambios. LazyColumn/LazyRow
  quedan fuera (no son scopes con `weight`/`align` de hijo igual).

## Componentes y contratos — builder

### TreeOps.findParent + BuilderDocument.parentType
```kotlin
// TreeOps
fun findParent(root: SduiNode, id: String): SduiNode? =
    if (root.children.any { it.id == id }) root
    else root.children.firstNotNullOfOrNull { findParent(it, id) }

// BuilderDocument
fun parentType(id: String?): String? = id?.let { TreeOps.findParent(root, it)?.type }
```

### ModifierFields — weight + opciones de alignment por eje
```kotlin
internal const val KEY_WEIGHT = "weight"
internal fun modifierWeight(modifier: UiModifier): String = modifier.weight?.let { fmt(it) } ?: ""
internal fun withModifierWeight(modifier: UiModifier, text: String): UiModifier {
    val w = text.trim().toFloatOrNull()
    return modifier.copy(weight = if (w != null && w > 0f) w else null) // inválido/≤0 ⇒ limpia (HU-4.3)
}
// Opciones de alignment por eje (refs de AlignmentToken), reutilizables por inspector y catálogo:
internal val alignHorizontalRefs = listOf(Tokens.Alignment.Start.ref, Tokens.Alignment.CenterHorizontally.ref, Tokens.Alignment.End.ref)
internal val alignVerticalRefs = listOf(Tokens.Alignment.Top.ref, Tokens.Alignment.CenterVertically.ref, Tokens.Alignment.Bottom.ref)
```
- (Las listas de la 017 en `BuilderCatalog` se reescriben para reutilizar estas, evitando duplicar.)

### InspectorPane — sección "Layout en el contenedor" (contextual)
- **Firma:** `InspectorPane(node, parentType, onProps, onModifier, modifier)` (añade `parentType: String?`).
- Tras los campos del catálogo, si `node != null` y `parentType` es `"column"`/`"row"`, renderiza una sección
  con:
  - **alignment por-hijo**: un `EnumField` "sintético" (no del catálogo) con `key = KEY_ALIGNMENT`, target
    Modifier, y `options = if (parentType == "column") alignHorizontalRefs else alignVerticalRefs`. Reutiliza
    el `EnumField` target-aware de la 017 + la opción "(ninguno)".
  - **weight**: un `WeightField` (OutlinedTextField numérico) que lee `modifierWeight(node.modifier)` y escribe
    `onModifier(withModifierWeight(node.modifier, text))`.
- **Decisión:** el campo alignment por-hijo es **contextual** (depende del eje del padre), por eso NO es un
  `FieldSpec` estático del catálogo: el inspector lo construye con las opciones del eje. El `alignment` de
  contenedor de la 017 (en las entradas column/row) se mantiene; este es el del **hijo**.

### BuilderApp
- En `RightColumn`, calcular `val parentType = document.parentType(document.selectedId)` y pasarlo al
  `InspectorPane`.

## Modelo de datos y estados
- Sin tipos de dominio nuevos. `UiModifier.weight`/`alignment` (ya existen). El threading del `layoutModifier`
  es un parámetro de render, no estado persistente.

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| _(ninguna)_ | — | — | Solo Compose layout (`weight`/`align` de scope) y lógica del builder. |

## Riesgos y mitigaciones
- **Regresión por el parámetro nuevo de RenderNode**: default `Modifier` ⇒ comportamiento idéntico; el único
  cambio efectivo es en Column/Row. Cubierto por tests de no-regresión del motor.
- **weight=0 / negativo**: `takeIf { it > 0f }` lo ignora (HU-1.2); el editor limpia inválidos (HU-4.3).
- **alignment de eje cruzado** (token horizontal en Row): `toVerticalAlignment` devuelve null ⇒ no se aplica,
  cae al del contenedor (HU-2.3). El inspector ya ofrece solo el eje correcto, evitando el caso desde la UI.
- **`then` orden de modifiers**: weight/align son parent-data; anteponerlos no rompe size/padding/bg del hijo.
- **Nodo sin id como hijo**: `findParent` compara por `it.id == id`; los nodos del builder siempre tienen id
  (ensureUniqueTree), así que `parentType` funciona para la selección del inspector.

## Estrategia de verificación
- **Motor — `:sdui-compose`**:
  - Test de no-regresión: un árbol existente (sin weight/align por-hijo) produce el mismo render (los tests
    actuales de CorePack deben seguir verdes; el default Modifier garantiza identidad).
  - Test de comportamiento (si hay infra de UI test / screenshot en el módulo): un Column con dos hijos, uno
    con `weight=1`, reparte; un hijo con `alignment=End` se alinea distinto. Si no hay infra de UI test, se
    valida la pieza pura: `childLayout` no es testeable sin scope, pero `withModifierWeight`/`toH/VAlignment`
    sí (estos últimos ya cubiertos en core).
- **Builder — `:builder`**:
  - `TreeOpsTest`: `findParent` devuelve el padre correcto; null para la raíz.
  - `BuilderDocumentTest`: `parentType` de un hijo de column == "column"; de la raíz == null.
  - `ModifierFieldsTest`: `withModifierWeight("2")` fija 2f; `"0"`/`"abc"`/`""` limpian; `modifierWeight` lee.
- **Lint:** `./gradlew :sdui-compose:detekt :sdui-compose:ktlintCheck :builder:test :builder:detekt :builder:ktlintCheck` y los tests del motor (`:sdui-compose` test target) en verde.
- **Smoke (`:builder:run`):** seleccionar un hijo de una Column → aparece alignment (horizontal) + weight;
  cambiarlos mueve/redimensiona solo a ese hijo en el preview; un hijo de Row muestra alignment vertical.
