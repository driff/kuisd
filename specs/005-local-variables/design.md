# Diseño — Variables locales reactivas (estrategia "variables locales")

> Spec ID: 005 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Replicar el patrón de seams de 003/004 para el **estado de variables**: el motor (`dev.kuisd.sdui`) gana
un seam de **solo lectura** (`VariableScope` + `LocalVariables`, `CompositionLocal`, default vacío). Los
renderers del `CorePack` resuelven un campo bindable (`text`) leyendo ese seam mediante un helper del
`RenderScope` (`bind(...)`), de modo que la lectura es **observada** por Compose y la recomposición queda
**localizada** al nodo. El **estado real** y su mutación viven en la app: un `VariableStore` (snapshot-state)
sembrado con `SduiEnvelope.variables`, y un `VariableActionHandler` que interpreta `SetVar`/`Toggle`/`Increment`.
El `SduiHost` compone navegación + variables en un único `AppActionHandler : SduiActionHandler` y provee
tanto `LocalSduiActionHandler` como `LocalVariables`. El contrato (`:sdui-core`) **no cambia**. No se crean
módulos Gradle (eso es la 006); la frontera sigue siendo por paquetes.

## Capas y regla de dependencias (sin cambios respecto a 003/004)
```
:sdui-core (dev.kuisd.sdui.core)     contrato @Serializable — SetVar/Toggle/Increment + variables YA existen
        ▲
dev.kuisd.sdui   (MOTOR, Compose)    SOLO lee variables por el seam; PROHIBIDO Ktor, app y mutables de estado
        ▲
dev.kuisd.app    (APP/composición)   posee VariableStore + interpreta acciones + provee los CompositionLocal
```
El motor **lee**; la app **posee y muta**. La dirección de dependencias no se invierte: el motor define la
**interfaz** del seam (`VariableScope`) y la app aporta la **implementación** (el `VariableStore` la cumple).

## Mapa de paquetes dentro de `:shared` (commonMain salvo nota)
```
dev/kuisd/
├── sdui/                              ← MOTOR
│   ├── Variables.kt        (NUEVO) public  fun interface VariableScope + LocalVariables (SEAM de lectura)
│   ├── RenderScope.kt      (cambia) public  + fun bind(raw: String?): String (resuelve binding contra el seam)
│   ├── CorePack.kt         (cambia) public  el renderer de `text` usa bind(p.text) en vez de p.text crudo
│   ├── ComponentRegistry.kt         (sin cambios; 004)
│   ├── RenderNode.kt / SduiActionHandler.kt / Logging.kt   (sin cambios)
└── app/
    ├── vars/                          ← NUEVO: estado de variables (análogo a nav/)
    │   ├── VariableStore.kt           public   snapshot-state, seed(variables), set/toggle/increment, expone VariableScope
    │   └── VariableActionHandler.kt   internal SduiActionHandler que interpreta SetVar/Toggle/Increment
    ├── AppActionHandler.kt (NUEVO) internal handler compuesto (nav + vars) — CADENA de SduiActionHandler
    ├── SduiScreen.kt       (cambia) crea/recuerda VariableStore por pantalla, lo siembra con envelope.variables,
    │                                 provee LocalVariables; renderiza el root (igual que 004)
    └── SduiHost.kt         (cambia) compone AppActionHandler(nav + vars) y lo provee por LocalSduiActionHandler
server/.../screens/
└── CounterScreen.kt        (NUEVO) variables {count, flag} + text "$count" + botones Increment/Toggle
    ScreenRegistry.kt       (cambia) registra "counter"
```
> Decisión de propiedad del store: el `VariableStore` se crea **por pantalla** (en `SduiScreen`, vía
> `remember(envelope)`), no en el `SduiHost`. Motivo: las variables son **estado de la pantalla** y se
> siembran desde su envelope; con la recarga total por `key(current.id)` del host (003) cada entrada del
> back stack obtiene su propio store, y volver atrás re-siembra desde el envelope (sin persistencia, HU fuera
> de alcance). El handler de variables, en cambio, debe vivir junto al árbol que lo usa → ver §AppActionHandler.

## Componentes y contratos

### MOTOR · VariableScope + LocalVariables (seam de lectura) — HU-1.1, HU-1.2
```kotlin
// dev.kuisd.sdui  (public)  — Variables.kt
import kotlinx.serialization.json.JsonElement

/** Seam de SOLO LECTURA de variables. La app aporta la impl (VariableStore la cumple). */
fun interface VariableScope {
    /** Valor actual de [name], o null si no existe. Lectura observada por Compose en el impl de la app. */
    fun get(name: String): JsonElement?
}

/** Seam inyectado por el host; default vacío para usar RenderNode fuera de un host (HU-1.1). */
val LocalVariables: ProvidableCompositionLocal<VariableScope> =
    staticCompositionLocalOf { VariableScope { null } }   // default: ninguna variable
```
- El motor depende solo de `:sdui-core` (`JsonElement`) + Compose. No hay mutables aquí (HU-1.2).

### MOTOR · RenderScope.bind() (resolución del binding) — HU-2, HU-5.1, HU-5.2
```kotlin
// dev.kuisd.sdui  (public)  — RenderScope.kt  (añade bind; el resto igual que 004)
class RenderScope internal constructor(val node: SduiNode) {
    @Composable fun renderChildren() { node.children.forEach { RenderNode(it) } }

    /**
     * Resuelve un campo string bindable contra [LocalVariables]:
     *  - null            -> ""        (campo ausente)
     *  - "$name"         -> valor textual de la variable `name`, o "" si ausente (HU-5.1)
     *  - "literal"       -> "literal" (retrocompat 004; sin prefijo `$` no hay binding)
     *  - "\$literal"     -> "$literal" (escape: `$$` produce un `$` literal inicial)
     * La lectura de la variable es observada -> recomposición localizada del nodo (HU-2.3).
     */
    @Composable
    fun bind(raw: String?): String {
        if (raw == null) return ""
        if (!raw.startsWith("$")) return raw
        if (raw.startsWith("$$")) return raw.substring(1)        // "$$x" -> "$x" (literal escapado)
        val name = raw.substring(1)
        val value = LocalVariables.current.get(name) ?: return ""  // ausente -> fallback (HU-5.1)
        return value.asDisplayString()                              // tipos no textuales -> seguro (HU-5.2)
    }
}

/** Representación textual segura de un JsonElement para display (HU-5.2). */
private fun JsonElement.asDisplayString(): String = when (this) {
    is JsonPrimitive -> content                 // string/number/boolean -> su contenido
    else -> toString()                          // objeto/array -> su JSON (nunca crashea)
}
```
- **Decisión de la convención (HU-2.1):** prefijo `$nombre` en el **propio campo string** (no un campo
  `bind` separado). Razones: (1) **forward-compatible** — un cliente que no implemente binding ve el literal
  `"$count"` y lo pinta tal cual (degradación visible, no crash); (2) **mínimo** — no toca props tipadas ni
  el contrato; el server escribe `{"text":"$count"}`; (3) coincide con el espíritu del doc de visión
  (interpolación de variable en campos string), simplificado a **una sola variable por campo** y sin la
  forma `@{...}` (se reserva esa sintaxis para la interpolación multi-variable futura, fuera de alcance).
  - Alternativa descartada: campo `bind:"count"` separado en props → más explícito pero exige cambiar las
    props tipadas (`TextProps`) y no degrada de forma visible en clientes viejos; se difiere a la spec de
    inputs two-way (donde `bind` sí aporta).
  - Alternativa descartada: interpolación `@{x}` dentro del texto (varias variables, plantilla) → es
    "binding complejo", explícitamente fuera de alcance.

### MOTOR · CorePack (text y button usan bind) — HU-2.2
```kotlin
// dev.kuisd.sdui  (cambia los renderers de text y button)
register(sduiComponent<TextProps>("text")) { p -> Text(bind(p.text)) }     // antes: Text(p.text)
register(sduiComponent<ButtonProps>("button")) { p ->
    val handler = LocalSduiActionHandler.current
    Button(onClick = { handler.handle(node.actions["onClick"].orEmpty()) }) { Text(bind(p.label)) }
}
```
- `bind` viene del `RenderScope` receiver. `column`/`row` quedan igual que en 004. **Decisión del usuario
  (005):** se extiende la convención `$nombre` al `label` del botón además del `text` — coherente y útil
  para el demo.

### APP · VariableStore (estado reactivo, dueño en la app) — HU-3, HU-1.3
```kotlin
// dev.kuisd.app.vars  (public)  — VariableStore.kt
@Stable
class VariableStore(initial: Map<String, JsonElement> = emptyMap()) {
    private var vars: Map<String, JsonElement> by mutableStateOf(initial)   // snapshot-state

    /** Vista de SOLO LECTURA que cumple el seam del motor (la lectura aquí es observada por Compose). */
    val scope: VariableScope = VariableScope { name -> vars[name] }

    fun set(name: String, value: JsonElement) { vars = vars + (name to value) }       // SetVar (HU-3.2)

    fun toggle(name: String) {                                                          // Toggle (HU-3.3)
        val curr = (vars[name] as? JsonPrimitive)?.booleanOrNull ?: false
        vars = vars + (name to JsonPrimitive(!curr))
    }

    fun increment(name: String, by: Int, min: Int?, max: Int?) {                        // Increment (HU-3.4/3.5)
        val curr = (vars[name] as? JsonPrimitive)?.intOrNull ?: 0
        var next = curr + by
        if (min != null) next = maxOf(next, min)
        if (max != null) next = minOf(next, max)
        vars = vars + (name to JsonPrimitive(next))
    }
}
```
- `scope` es la única superficie que el motor ve; cumple `VariableScope` sin exponer mutación. La lectura
  `vars[name]` dentro de `scope.get` ocurre en una lectura de snapshot-state, así que un `bind` que la invoque
  dentro de una composición se **re-ejecuta** cuando `vars` cambia → recomposición localizada (HU-2.3).
- Coerción tolerante (`?: false` / `?: 0`) cubre tipo incompatible y variable ausente sin crash (HU-5.3).

### APP · VariableActionHandler — HU-3.2/3.3/3.4
```kotlin
// dev.kuisd.app.vars  (internal)  — VariableActionHandler.kt
internal class VariableActionHandler(private val store: VariableStore) : SubHandler {
    override fun supports(action: UiAction): Boolean =
        action is SetVar || action is Toggle || action is Increment

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        when (action) {
            is SetVar    -> store.set(action.name, action.value)
            is Toggle    -> store.toggle(action.name)
            is Increment -> store.increment(action.name, action.by, action.min, action.max)
            else         -> { /* ignorar en silencio — el AppActionHandler decide qué loguear */ }
        }
    }
}
```

### APP · AppActionHandler (composición nav + vars) — HU-4
```kotlin
// dev.kuisd.app  (internal)  — AppActionHandler.kt
/**
 * Sub-handler de aplicación. Extiende `SduiActionHandler` (contrato del motor) con un predicado
 * de soporte para que el AppActionHandler compuesto decida a quién despachar **sin** doble log.
 */
internal interface SubHandler : SduiActionHandler {
    fun supports(action: UiAction): Boolean
}

/**
 * Handler compuesto: para cada acción busca el primer sub-handler que la soporte; si ninguno la
 * soporta, loguea (no-op+log). Así se mantiene SRP y se evita el doble log de la primera versión.
 */
internal class AppActionHandler(
    private val children: List<SubHandler>,
) : SduiActionHandler {
    override fun handle(actions: List<UiAction>) {
        actions.forEach { action ->
            val matched = children.firstOrNull { it.supports(action) }
            if (matched != null) {
                matched.handle(listOf(action))
            } else {
                appLog("acción no soportada (no-op): ${action::class.simpleName}")
            }
        }
    }
}
```
- **Decisión del usuario (005, HU-4.1):** los sub-handlers **ignoran en silencio** las acciones que no
  soportan (no loguean), y es el `AppActionHandler` compuesto quien loguea **una sola vez** las acciones
  realmente desconocidas (las que ningún sub-handler aceptó). Esto mantiene SRP y elimina el doble log.
- El contrato del motor `SduiActionHandler` (003) **no cambia**: `SubHandler` es un tipo de la app que
  extiende ese contrato con un predicado interno.
- `NavActionHandler` soporta `Navigate`/`NavigateBack`; `VariableActionHandler` soporta
  `SetVar`/`Toggle`/`Increment`; el resto (p.ej. `Track`, `FireEndpoint`, `CustomAction`) cae en el
  log del compuesto sin crash (HU-4.3).

### APP · SduiScreen (siembra el store + provee LocalVariables) — HU-3.1, HU-1.3
```kotlin
// dev.kuisd.app  (cambia)  — patrón: el store es estado de pantalla, sembrado por el envelope
is ScreenUiState.Content -> {
    val store = remember(current.envelope) { VariableStore(current.envelope.variables) }   // seed (HU-3.1)
    CompositionLocalProvider(LocalVariables provides store.scope) {                          // expone solo lectura
        Box(modifier = modifier) { RenderNode(current.envelope.root) }
    }
    // el VariableActionHandler de esta pantalla se compone en el host (ver nota de cableado abajo)
}
```
> **Cableado del handler de variables (importante):** el `VariableActionHandler` necesita el `VariableStore`
> de la pantalla **actual**, pero el `LocalSduiActionHandler` lo provee el `SduiHost` (003). Para no
> reestructurar la propiedad, esta spec **iza** el `VariableStore` al `SduiHost`, recreándolo por entrada con
> `key`/`remember(current.id)` y sembrándolo tras cargar el envelope. Hay **dos opciones** de diseño; el MVP
> elige la **Opción A** por mínima invasión:
>
> - **Opción A (elegida):** el `VariableStore` se crea en `SduiScreen` (estado de pantalla) y se **expone al
>   host** mediante un callback `onStoreReady: (VariableStore) -> Unit` o, más simple, el host crea el store
>   `remember(current.id)` y lo **pasa a `SduiScreen`** como parámetro, que lo siembra con
>   `LaunchedEffect(envelope) { store.seed(envelope.variables) }`. Así el host puede construir
>   `AppActionHandler(nav + VariableActionHandler(store))`. Requiere un método `seed(...)` en `VariableStore`.
> - **Opción B:** mover toda la propiedad del store y del provide de `LocalVariables` al host; `SduiScreen`
>   solo renderiza. Más limpio conceptualmente pero el host pasa a depender del `envelope` (que hoy obtiene
>   `SduiScreen` vía `produceState`), invirtiendo responsabilidades de 003.
>
> Se adopta **A** con `seed`: el store se `remember(current.id)` en el host (vida = entrada del back stack),
> se pasa a `SduiScreen` para sembrarlo cuando llega el `Content`, y el host lo usa para el handler. Firma:
```kotlin
// VariableStore añade:
fun seed(initial: Map<String, JsonElement>) { vars = initial }   // re-siembra al cargar el envelope
```

### APP · SduiHost (compone handlers + provee seams) — HU-4.1, HU-1.1
```kotlin
// dev.kuisd.app  (cambia)
val store = remember(current.id) { VariableStore() }                       // estado por entrada del back stack
val handler = remember(current.id, backStack) {
    AppActionHandler(listOf(NavActionHandler(backStack), VariableActionHandler(store)))
}
CompositionLocalProvider(
    LocalSduiActionHandler provides handler,        // seam de salida (acciones) — 003
    LocalComponentRegistry provides appRegistry,    // 004
    LocalVariables provides store.scope,            // seam de lectura de variables — 005
) {
    key(current.id) {
        SduiScreen(current.route, source, store = store, modifier = Modifier.padding(padding))
    }
}
```
- `SduiScreen` recibe el `store` y lo siembra (`store.seed(envelope.variables)`) al pasar a `Content`.
- `LocalVariables` se provee con `store.scope` (solo lectura) — el motor jamás ve la API mutante.

### SERVER · CounterScreen — HU-6
```kotlin
// server/.../screens/CounterScreen.kt
object CounterScreen : ScreenBuilder {
    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = 1,
        screenId = "counter",
        variables = mapOf(                                   // estado inicial (HU-6.1)
            "count" to JsonPrimitive(0),
            "flag"  to JsonPrimitive(false),
        ),
        root = SduiNode("column", id = "root", children = listOf(
            SduiNode("text", id = "label",
                props = JsonObject(mapOf("text" to JsonPrimitive("Cuenta: ")))),          // literal
            SduiNode("text", id = "value",
                props = JsonObject(mapOf("text" to JsonPrimitive("\$count")))),           // binding "$count"
            SduiNode("button", id = "inc",
                props = JsonObject(mapOf("label" to JsonPrimitive("+1"))),
                actions = mapOf("onClick" to listOf(Increment("count", by = 1, min = 0, max = 10)))),
            SduiNode("button", id = "toggle",
                props = JsonObject(mapOf("label" to JsonPrimitive("Toggle"))),
                actions = mapOf("onClick" to listOf(Toggle("flag")))),
        )),
    )
}
// ScreenRegistry.kt: "counter" to CounterScreen
```
- Como `bind` solo resuelve **un** campo a una variable, el rótulo "Cuenta: " va en un `text` aparte del
  `text` con `"$count"` (no se interpola "Cuenta: $count" en un solo string — eso es multi-variable, fuera de
  alcance). Opcional: una pantalla `counter` enlazada desde `home` para el smoke.

## public / internal
- **public (motor):** `VariableScope`, `LocalVariables`, `RenderScope.bind` (+ lo de 004).
- **public (app):** `VariableStore` (la app y los tests lo usan; su API mutante es de la app, no del motor).
- **internal:** `VariableActionHandler`, `AppActionHandler`, `asDisplayString`.

## Modelo de datos y estados
- Contrato `:sdui-core` **sin cambios** (`SetVar`/`Toggle`/`Increment`, `SduiEnvelope.variables`).
- `VariableStore.vars: Map<String, JsonElement>` en **snapshot-state** (sin persistencia). Sembrado por
  `envelope.variables`; mutado por las tres acciones; leído por el motor vía `VariableScope`.
- Coerción de display: `JsonPrimitive.content`; objeto/array → `toString()`; ausente → `""`.
- Coerción de mutación: toggle usa `booleanOrNull ?: false`; increment usa `intOrNull ?: 0` + clamp `[min,max]`.

## Resiliencia
- Variable ausente en binding → `""` (HU-5.1). Tipo no textual → `toString()` seguro (HU-5.2).
- `Toggle`/`Increment` sobre tipo incompatible → coerción a `false`/`0` (HU-5.3).
- `type` desconocido → `UnknownNode` (004); acción no soportada por ningún sub-handler → no-op+log (HU-4.3).
- El motor no contiene estado mutable → no hay fuga de estado entre pantallas a nivel de motor.

## Riesgos y mitigaciones
- **Recomposición no localizada** si `bind` lee todo el mapa → mitigado: `VariableScope.get(name)` lee
  `vars[name]` en la lectura de snapshot-state; Compose registra la dependencia por la **lectura del estado**,
  recomponiendo el `text` cuando `vars` cambia. (Granularidad por-clave fina se difiere; el coste de
  recomponer los pocos `text` de una pantalla es despreciable.)
- **Cableado store↔handler** (el handler necesita el store de la pantalla actual) → resuelto izando el store
  al host con `remember(current.id)` y pasándolo a `SduiScreen` para sembrarlo (Opción A). Riesgo de orden:
  el store se siembra al llegar `Content`; antes, las variables están vacías y `bind` cae en fallback (sin
  crash) durante el `Loading` (aceptable).
- **Doble log** en `AppActionHandler` (ambos sub-handlers loguean su no-op) → cosmético; mitigación futura
  con cadena que corta en el primer match (pregunta abierta).
- **`$` en textos legítimos** → cubierto por el escape `$$` → `$` (HU-2.2 / `bind`).

## Estrategia de verificación
- **Unit `:shared` (app · `VariableStore`):**
  - `set` fija/clave nueva; `seed` reemplaza el mapa.
  - `toggle`: ausente→true; true→false; valor no booleano→true (coerción).
  - `increment`: suma `by`; clamp a `max` (no supera) y a `min`; ausente/no-entero parte de `0`.
- **Unit `:shared` (app · `VariableActionHandler`):** `SetVar`/`Toggle`/`Increment` mutan el store;
  acción no soportada (p.ej. `Navigate`) no altera el store.
- **Unit `:shared` (app · `AppActionHandler`):** lista mixta `[Increment, Navigate]` → el store incrementa
  y el back stack apila; el orden se respeta; acción ajena → no-op (sin crash).
- **Unit `:shared` (motor · binding):** sin UI, validar la **regla** de `bind` extrayéndola a una función
  pura testeable `resolveBinding(raw, lookup): String` usada por `RenderScope.bind` (literal→literal;
  `"$x"`→valor; ausente→""; `"$$x"`→"$x"; objeto→toString). (El render @Composable se valida por smoke.)
- **Unit `:server`:** `GET /screen/counter` → 200; el envelope trae `variables["count"]==0` y un nodo `text`
  con `props.text=="$count"` y botones con `Increment`/`Toggle`.
- **Regla de dependencias:** `grep` confirma que `dev/kuisd/sdui/` no importa `io.ktor.`/`dev.kuisd.app` y no
  contiene `mutableStateOf`/`MutableStateFlow` de variables (el seam es solo lectura).
- **Smoke:** `:server:run` + `:desktopApp:run`: en `counter`, "+1" sube el número en el cliente sin red,
  se detiene en 10 (clamp); "Toggle" no crashea; navegación 003/badge 004 intactos.
- **Calidad:** `./gradlew :sdui-core:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` en verde.
