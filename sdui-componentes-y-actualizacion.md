# Catálogo de Componentes SDUI + Modelo de Actualización

Profundización sobre los componentes visuales del SDUI (Ktor BFF + KMP). Tres partes:

1. **Añadidos al contrato** que son prerrequisito (IDs de nodo + variables/binding).
2. **Catálogo completo** de componentes, agrupados, con el JSON de cada uno.
3. **Actualización de datos dentro de un componente**: estrategias y casos trabajados.

> Principio que se mantiene: construimos el *mecanismo* de layout libre, pero **gobernamos** con catálogo curado, estilo solo por tokens y composición/acciones del lado server.

---

## Parte 1 — Añadidos al contrato (prerrequisito)

Para poder "actualizar info dentro de un componente" necesitas dos cosas que el blueprint inicial no tenía: **identidad de nodo** y un **sistema de variables/binding**.

### 1.1 Todo nodo lleva `id`

```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed interface UiNode {
    val id: String?     // estable; requerido para patches y para binding de inputs
}
// cada data class: override val id: String? = null
```

El `id` es la dirección para patches dirigidos (`replace`, `update`, `remove`) y para enlazar inputs a variables. Para listas dinámicas el `id` además sirve de `key` de Compose (mejor diffing/scroll).

### 1.2 Variables y binding

El envelope transporta el **estado inicial** de variables; el cliente las guarda en un store reactivo.

```kotlin
@Serializable
data class SduiEnvelope(
    val schemaVersion: Int,
    val screenId: String,
    val root: UiNode,
    val variables: Map<String, JsonElement> = emptyMap(),  // ← estado inicial
    val meta: Map<String, String> = emptyMap()
)
```

Dos formas de binding (las dos conviven):

- **Display (one-way):** interpolación `@{nombreVar}` dentro de campos string. Ej. `"text": "Total: @{cartTotal}"`. El cliente resuelve contra el store y **recompone** cuando la variable cambia.
- **Input (two-way):** los componentes de entrada llevan `bind: "nombreVar"`. Leen el valor actual del store y lo escriben de vuelta en `onChange`.

```kotlin
// store reactivo en el cliente (commonMain)
class VariableStore(initial: Map<String, JsonElement>) {
    private val _vars = MutableStateFlow(initial)
    val vars: StateFlow<Map<String, JsonElement>> = _vars
    fun set(name: String, value: JsonElement) { _vars.update { it + (name to value) } }
    fun resolve(template: String): String =                 // "@{x}" -> valor
        Regex("""@\{(\w+)\}""").replace(template) { m ->
            _vars.value[m.groupValues[1]]?.let { (it as? JsonPrimitive)?.content } ?: ""
        }
}
```

### 1.3 Acciones nuevas (mutar estado sin server)

```kotlin
@Serializable @SerialName("setVar")    data class SetVar(val name: String, val value: JsonElement) : UiAction
@Serializable @SerialName("increment") data class Increment(val name: String, val by: Int = 1, val min: Int? = null, val max: Int? = null) : UiAction
@Serializable @SerialName("toggle")    data class Toggle(val name: String) : UiAction
@Serializable @SerialName("network")   data class FireEndpoint(val endpoint: String, val payloadVars: List<String> = emptyList()) : UiAction
```

`network`/`submit` mandan al server y reciben un **patch** (ver Parte 3).

---

## Parte 2 — Catálogo de componentes

Notación: muestro props clave + JSON. Todos aceptan `id` y `modifier` (omitidos cuando no aportan). Marco con 🔁 los que participan en actualización de datos.

### A. Contenedores / layout

**`column` / `row` / `box`** — apilado vertical / horizontal / superpuesto.
```json
{ "type": "column", "id": "main", "spacing": 12,
  "modifier": { "fillMaxWidth": true, "padding": {"l":16,"t":16,"r":16,"b":16} },
  "children": [ /* ... */ ] }
```

**`lazyColumn` / `lazyRow`** — listas perezosas para colecciones largas.
```json
{ "type": "lazyColumn", "id": "feed", "itemSpacing": 8,
  "children": [ {"type":"text","text":"Item 1"}, {"type":"text","text":"Item 2"} ] }
```

**`lazyGrid`** — grilla perezosa.
```json
{ "type": "lazyGrid", "columns": 2, "itemSpacing": 8, "children": [ /* ... */ ] }
```

**`card`** — contenedor con superficie/elevación/esquinas (token).
```json
{ "type": "card", "id": "promo", "elevation": 2,
  "modifier": { "fillMaxWidth": true, "cornerRadius": 16, "background": "color.surface" },
  "children": [ {"type":"text","text":"Oferta","style":"type.title"} ] }
```

**`spacer`** — espacio fijo. **`divider`** — separador.
```json
{ "type": "spacer", "size": 16 }
{ "type": "divider", "thickness": 1, "color": "color.outline" }
```

**`tabs`** 🔁 — pestañas; la pestaña activa es una variable.
```json
{ "type": "tabs", "id": "tabs", "selected": "tabIndex",
  "tabs": [
    { "label": "Resumen", "content": {"type":"text","text":"..."} },
    { "label": "Detalle", "content": {"type":"text","text":"..."} }
  ] }
```

**`pager` / `carousel`** — páginas deslizables (hero, onboarding).
```json
{ "type": "pager", "id": "hero", "pages": [ {"type":"image","url":"..."}, {"type":"image","url":"..."} ] }
```

**`expandable` / `accordion`** 🔁 — colapsable; estado en variable.
```json
{ "type": "expandable", "id": "faq1", "expandedVar": "faq1Open",
  "header": {"type":"text","text":"¿Cómo funciona?","style":"type.body"},
  "content": {"type":"text","text":"Respuesta larga..."} }
```

**`state` / `switch`** 🔁 — nodo con estados nombrados, conmutados por variable (loading / loaded / error, sin round-trip).
```json
{ "type": "state", "id": "list", "stateVar": "listStatus", "default": "loading",
  "states": {
    "loading": {"type":"skeleton","lines":5},
    "loaded":  {"type":"lazyColumn","children":[]},
    "error":   {"type":"banner","style":"error","text":"Falló la carga"}
  } }
```

### B. Display / contenido

**`text`** 🔁 — texto; soporta interpolación `@{var}`.
```json
{ "type": "text", "id": "greet", "text": "Hola, @{userName}",
  "style": "type.title", "color": "color.onSurface", "maxLines": 2 }
```

**`richText`** — texto con tramos de estilo distintos.
```json
{ "type": "richText", "spans": [
    {"text":"Total: ","style":"type.body"},
    {"text":"@{cartTotal}","style":"type.title","color":"color.primary"} ] }
```

**`image`** — remota, con escala y placeholder.
```json
{ "type": "image", "url": "https://cdn/x.png", "contentDescription": "Banner",
  "scale": "crop", "modifier": { "fillMaxWidth": true, "height": 180, "cornerRadius": 12 } }
```

**`icon`** — icono del set (nombre de token, no recurso crudo).
```json
{ "type": "icon", "name": "icon.cart", "color": "color.primary", "size": 24 }
```

**`avatar`** — imagen circular / iniciales.
```json
{ "type": "avatar", "url": "https://cdn/u.png", "fallbackInitials": "JC", "size": 40 }
```

**`badge`** 🔁 — contador/indicador (típico sobre icono).
```json
{ "type": "badge", "id": "cartBadge", "count": "@{cartCount}", "color": "color.error" }
```

**`chip`** 🔁 — etiqueta seleccionable (filtros).
```json
{ "type": "chip", "id": "f-new", "label": "Nuevos", "selectedVar": "filterNew",
  "onClick": [ {"type":"toggle","name":"filterNew"} ] }
```

**`rating`** — estrellas (display o input según `readOnly`).
```json
{ "type": "rating", "value": "@{stars}", "max": 5, "readOnly": true }
```

**`progress`** 🔁 — lineal o circular, determinado o indeterminado.
```json
{ "type": "progress", "style": "linear", "value": "@{uploadPct}", "indeterminate": false }
```

**`skeleton`** — placeholder de carga (shimmer).
```json
{ "type": "skeleton", "lines": 3, "modifier": { "fillMaxWidth": true } }
```

### C. Inputs / interacción (los que actualizan datos) 🔁

**`button`** — dispara una lista de acciones en orden.
```json
{ "type": "button", "id": "checkout", "label": "Pagar",
  "variant": "primary", "enabledVar": "canCheckout",
  "onClick": [ {"type":"network","endpoint":"/cart/checkout","payloadVars":["cartId"]} ] }
```

**`iconButton`** — botón solo-icono.
```json
{ "type": "iconButton", "icon": "icon.favorite", "onClick": [ {"type":"toggle","name":"isFav"} ] }
```

**`textField`** — entrada de texto, two-way con `bind`.
```json
{ "type": "textField", "id": "email", "bind": "emailVar",
  "label": "Correo", "placeholder": "tu@correo.com",
  "keyboard": "email", "errorVar": "emailError",
  "onChange": [ {"type":"setVar","name":"emailVar","value":"$value"} ] }
```
> Convención: `"$value"` es el valor actual del input, inyectado por el cliente al despachar la acción.

**`checkbox`** / **`switch`** — booleano.
```json
{ "type": "switch", "id": "notif", "bind": "notifVar", "label": "Notificaciones",
  "onChange": [ {"type":"setVar","name":"notifVar","value":"$value"} ] }
```

**`radioGroup`** — selección única.
```json
{ "type": "radioGroup", "id": "ship", "bind": "shipMethod",
  "options": [ {"value":"std","label":"Estándar"}, {"value":"exp","label":"Express"} ] }
```

**`slider`** — valor numérico en rango.
```json
{ "type": "slider", "id": "budget", "bind": "budgetVar", "min": 0, "max": 100, "step": 5 }
```

**`stepper`** — selector de cantidad (+/−).
```json
{ "type": "stepper", "id": "qty", "bind": "qtyVar", "min": 1, "max": 99,
  "onChange": [ {"type":"network","endpoint":"/cart/update","payloadVars":["qtyVar"]} ] }
```

**`dropdown` / `select`** — selección de lista desplegable.
```json
{ "type": "dropdown", "id": "country", "bind": "countryVar", "label": "País",
  "options": [ {"value":"cl","label":"Chile"}, {"value":"ar","label":"Argentina"} ] }
```

**`datePicker`** — fecha.
```json
{ "type": "datePicker", "id": "date", "bind": "dateVar", "min": "2026-01-01" }
```

**`searchBar`** 🔁 — búsqueda con submit con debounce.
```json
{ "type": "searchBar", "id": "q", "bind": "queryVar", "debounceMs": 300,
  "onChange": [ {"type":"network","endpoint":"/search","payloadVars":["queryVar"]} ] }
```

### D. Secciones / feedback / overlay

**`listItem`** — fila compuesta (avatar + título + subtítulo + trailing).
```json
{ "type": "listItem", "id": "row-1",
  "leading": {"type":"avatar","fallbackInitials":"AB"},
  "title": "Ana Bravo", "subtitle": "ana@x.com",
  "trailing": {"type":"icon","name":"icon.chevron"},
  "onClick": [ {"type":"navigate","route":"profile","args":{"id":"1"}} ] }
```

**`appBar` / `header`** — barra superior.
```json
{ "type": "appBar", "title": "Inicio",
  "actions": [ {"type":"iconButton","icon":"icon.search","onClick":[{"type":"navigate","route":"search"}]} ] }
```

**`banner` / `alert`** — mensaje contextual (info/success/warning/error).
```json
{ "type": "banner", "style": "warning", "text": "Stock limitado",
  "action": {"label":"Ver","onClick":[{"type":"navigate","route":"stock"}]} }
```

**`dialog`** 🔁 — modal; visibilidad por variable.
```json
{ "type": "dialog", "id": "confirm", "visibleVar": "showConfirm",
  "title": "¿Confirmar?", "body": {"type":"text","text":"Esta acción es definitiva."},
  "confirm": {"label":"Sí","onClick":[{"type":"network","endpoint":"/confirm"}]},
  "dismiss": {"label":"No","onClick":[{"type":"setVar","name":"showConfirm","value":false}]} }
```

**`bottomSheet`** 🔁 — hoja inferior; visibilidad por variable.
```json
{ "type": "bottomSheet", "id": "filters", "visibleVar": "showFilters",
  "content": {"type":"column","children":[ /* chips de filtro */ ]} }
```

**`snackbar`** — feedback efímero (normalmente lo emite un patch, ver Parte 3).
```json
{ "type": "snackbar", "message": "Guardado", "durationMs": 2000 }
```

---

## Parte 3 — Actualización de datos dentro de un componente

La pregunta clave. Hay **cinco estrategias**, de menor a mayor granularidad. Elige por tabla al final; lo normal es combinar 3 + 4 + 2.

### Estrategia 1 — Recarga total de pantalla

El cliente vuelve a pedir `GET /screen/{id}` y reemplaza todo el árbol. Simple y robusta; úsala en navegación, pull-to-refresh o cambios estructurales grandes. Costo: re-render completo y un round-trip pesado.

### Estrategia 2 — Patch dirigido (server es dueño de la verdad)

Una acción `network`/`submit` viaja al server, que responde un **patch** con operaciones por `id`. El cliente lo aplica sobre el árbol que tiene en estado y solo recompone los subárboles tocados. Para precio recalculado, stock, validación server, resultado de checkout.

Contrato del patch:
```kotlin
@Serializable
data class SduiPatch(val schemaVersion: Int, val changes: List<PatchOp>)

@Serializable @JsonClassDiscriminator("op")
sealed interface PatchOp
@Serializable @SerialName("replace")    data class Replace(val targetId: String, val node: UiNode) : PatchOp
@Serializable @SerialName("updateProps") data class UpdateProps(val targetId: String, val props: JsonObject) : PatchOp
@Serializable @SerialName("insert")     data class Insert(val parentId: String, val index: Int, val node: UiNode) : PatchOp
@Serializable @SerialName("remove")     data class Remove(val targetId: String) : PatchOp
@Serializable @SerialName("setVars")    data class SetVars(val values: Map<String, JsonElement>) : PatchOp
```

Ejemplo de respuesta a "actualicé la cantidad del carrito":
```json
{ "schemaVersion": 1, "changes": [
  { "op": "replace", "targetId": "lineTotal-42",
    "node": {"type":"text","id":"lineTotal-42","text":"$ 12.990","style":"type.title"} },
  { "op": "setVars", "values": { "cartCount": 3, "cartTotal": "$ 38.970" } },
  { "op": "insert", "parentId": "main", "index": 0,
    "node": {"type":"snackbar","message":"Carrito actualizado"} }
] }
```

Aplicación en el cliente (recorrido por `id`):
```kotlin
fun UiNode.applyPatch(p: SduiPatch): UiNode =
    p.changes.fold(this) { tree, op -> tree.apply(op) }

private fun UiNode.apply(op: PatchOp): UiNode = when (op) {
    is Replace     -> mapTree { if (it.id == op.targetId) op.node else it }
    is UpdateProps -> mapTree { if (it.id == op.targetId) it.mergeProps(op.props) else it }
    is Remove      -> filterTree { it.id != op.targetId }
    is Insert      -> insertInto(op.parentId, op.index, op.node)
    is SetVars     -> this.also { /* aplicar al VariableStore */ }
}
// mapTree/filterTree: recorren children recursivamente reconstruyendo el árbol inmutable
```

### Estrategia 3 — Variables + binding (cliente, sin round-trip)

El server manda `variables` iniciales; las acciones `setVar`/`increment`/`toggle` mutan el `VariableStore` local; los nodos con `@{var}`, `bind`, `selectedVar`, `visibleVar`, etc. **recomponen al instante**. Para contadores, toggles, selección de filtros/chips, totales calculables en cliente, abrir/cerrar diálogos y sheets, conmutar `state`.

Ejemplo — stepper que actualiza un total y un badge **sin tocar el server**:
```json
{ "type": "row", "id": "qtyrow", "children": [
  { "type": "iconButton", "icon": "icon.minus",
    "onClick": [ {"type":"increment","name":"qty","by":-1,"min":1} ] },
  { "type": "text", "text": "@{qty}", "style": "type.title" },
  { "type": "iconButton", "icon": "icon.plus",
    "onClick": [ {"type":"increment","name":"qty","by":1,"max":99} ] },
  { "type": "text", "id":"total", "text": "Subtotal: @{qty} × $4.990" }
] }
```
El badge del carrito (`{"type":"badge","count":"@{qty}"}`) en otra parte del árbol se actualiza solo porque lee la misma variable.

### Estrategia 4 — Estado local de input + submit con debounce

El input mantiene su valor en una variable vía `bind`; `onChange` actualiza la variable de inmediato (UX fluida) y, si corresponde, dispara un `network` con debounce para que el server valide o reaccione (búsqueda, validación de formulario). El server responde con patch (estrategia 2) para, p. ej., poblar resultados o setear `emailError`.

```json
{ "type": "searchBar", "id":"q", "bind": "queryVar", "debounceMs": 300,
  "onChange": [ {"type":"network","endpoint":"/search","payloadVars":["queryVar"]} ] }
```
Respuesta (patch): `replace` del nodo `resultsList` con los nuevos hijos.

### Estrategia 5 — Optimista + confirmación

Para like/favorito/guardar: mutas la variable local **ya** (la UI cambia al toque) y disparas `network` en paralelo. Si el server confirma, no hace nada o manda patch idempotente; si falla, el patch revierte la variable.
```json
{ "type": "iconButton", "id":"fav", "icon": "icon.favorite", "tintVar": "isFav",
  "onClick": [
    {"type":"toggle","name":"isFav"},
    {"type":"network","endpoint":"/items/42/favorite","payloadVars":["isFav"]}
  ] }
```

### El dispatcher orquesta todo

Un único punto resuelve cada acción contra el store, el patcher y la red:
```kotlin
class SduiDispatcher(
    private val store: VariableStore,
    private val tree: MutableStateFlow<UiNode>,
    private val client: SduiClient,
    private val nav: Navigator,
) : ActionDispatcher {
    override fun dispatch(action: UiAction) { when (action) {
        is SetVar    -> store.set(action.name, action.value)
        is Toggle    -> store.toggle(action.name)
        is Increment -> store.increment(action.name, action.by, action.min, action.max)
        is Navigate  -> nav.go(action.route, action.args)
        is FireEndpoint -> scope.launch {
            val patch = client.fire(action.endpoint, store.collect(action.payloadVars))
            tree.update { it.applyPatch(patch) }
            patch.changes.filterIsInstance<SetVars>().forEach { store.applyAll(it.values) }
        }
        is Track, NoOpAction -> { /* analytics / nada */ }
    } }
}
```

### Tabla de decisión

| Caso | Estrategia | Round-trip |
|---|---|---|
| Navegar / pull-to-refresh / cambio estructural | 1 Recarga total | Sí (pesado) |
| Precio/stock/validación que el server decide | 2 Patch dirigido | Sí (ligero) |
| Contador, toggle, filtro, total calculable, abrir modal/sheet, tab | 3 Variables/binding | No |
| Escribir en input, búsqueda, validación con debounce | 4 Local + submit | Sí (diferido) |
| Like / favorito / guardado rápido | 5 Optimista + confirma | Sí (en background) |

**Regla práctica:** si el cliente puede calcular el cambio sin la verdad del server → variables (3). Si el server es dueño del dato → patch (2). Reserva recarga total (1) para cuando cambia la estructura, no un valor.

---

## Implicaciones en el contrato (resumen de lo que hay que añadir)

1. `id: String?` en `sealed interface UiNode` (override en cada nodo).
2. `variables: Map<String, JsonElement>` en `SduiEnvelope`.
3. Acciones `SetVar`, `Increment`, `Toggle`, `FireEndpoint` (y `Submit`).
4. Jerarquía `SduiPatch` + `PatchOp` (`replace`/`updateProps`/`insert`/`remove`/`setVars`).
5. Cliente: `VariableStore` reactivo, `applyPatch` recursivo, dispatcher que orquesta store + patcher + red.
6. Convenciones: interpolación `@{var}` en strings; `bind` en inputs; `"$value"` como valor actual del input.

Con esto el catálogo y el modelo de actualización quedan cerrados. Siguiente paso natural: extender `:sdui-contract` con estos tipos y meter 6–8 componentes del catálogo (column, row, text, image, button, textField, stepper, lazyColumn) más las 3 estrategias de actualización core (2, 3, 4), y validar el flujo completo en Android + iOS antes de ampliar.
