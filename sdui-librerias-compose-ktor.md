# Generalizar el SDUI en librerías: `sdui-compose` + `sdui-ktor`

Objetivo: extraer el SDUI a librerías reutilizables — el **motor** del cliente Compose y el **motor** del servidor Ktor — de modo que cualquier app solo aporte su catálogo de componentes y sus pantallas, sin tocar el engine.

---

## 1. ¿Se puede? Sí. La idea clave

Un SDUI tiene dos capas que hoy están mezcladas:

| Capa | ¿Genérica? | Dónde vive |
|---|---|---|
| **Motor** — parsear árbol, render recursivo, mapear modifier, store de variables, aplicar patches, dispatcher; en server: routing, serialización, ensamblado, capability negotiation | Sí, agnóstica de la app | **Librerías** |
| **Catálogo** — qué componentes existen, sus props, cómo se ven; qué pantallas se ensamblan | No, propia de cada app | **App** |

La librería provee el motor + un pack de primitivas (column/row/text/image/button…). La app **registra** sus componentes y pantallas encima.

### El cambio de diseño que esto obliga

Hasta ahora `UiNode` era un `sealed interface` cerrado → exhaustividad del compilador en el `when`, pero **no extensible sin editar la librería**. Para que sea librería, el conjunto de componentes pasa a un **registry abierto**: el transporte es un nodo genérico y cada componente declara su propia clase de `Props` tipada.

> **No pierdes type-safety, lo mueves de nivel.** Antes: un `sealed UiNode` con exhaustividad global. Ahora: cada componente trae su `@Serializable Props`, y el engine la deserializa antes de invocar el renderer. El `when` global desaparece, pero cada renderer recibe props fuertemente tipadas. La seguridad server↔cliente se mantiene porque ambos usan el **mismo descriptor de componente** (mismo `type`, mismo serializer de Props).

---

## 2. Grafo de módulos (3 librerías publicables + módulos de app)

```
        ┌─────────────────────────────────────────────┐
        │  sdui-core   (KMP lib · commonMain)          │  ← transporte + tipos compartidos
        │  SduiNode, SduiEnvelope, SduiPatch,          │
        │  UiModifier, UiAction, SduiComponent<P>,     │
        │  SduiJson factory, modelo de Variables       │
        └───────────────┬──────────────────┬───────────┘
                        │                  │
        ┌───────────────▼──────┐   ┌───────▼───────────────┐
        │ sdui-compose (KMP)   │   │ sdui-ktor (JVM)        │   ← motores reutilizables
        │ ComponentRegistry,   │   │ Sdui plugin,           │
        │ RenderEngine, Render │   │ ScreenRegistry,        │
        │ Scope, Modifier map, │   │ node DSL, patch DSL,   │
        │ VariableStore, patch │   │ ActionRouter,          │
        │ applier, CorePack    │   │ capability negotiation │
        └───────────────┬──────┘   └───────┬───────────────┘
                        │                  │
   ════════════════════ CONSUMIDOR (tu app) ════════════════════
                        │                  │
        ┌───────────────▼──────────────────▼───────────┐
        │  :app-contract  (KMP lib · commonMain)        │  ← catálogo propio, compartido
        │  Props de cada componente + type ids + DSL    │     entre cliente y server de la app
        └───────────────┬──────────────────┬───────────┘
                        │                  │
        ┌───────────────▼──────┐   ┌───────▼───────────────┐
        │ :app-client          │   │ :app-server            │
        │ registra renderers   │   │ registra screens +     │
        │ de sus componentes   │   │ action handlers        │
        └──────────────────────┘   └────────────────────────┘
```

Claves del grafo:
- **`sdui-core` lo comparten las dos librerías** (el contrato de transporte es único).
- **`:app-contract`** preserva, *a nivel de tu app*, la sinergia de "un solo módulo de Props compartido server↔cliente". El engine es genérico; tu catálogo sigue siendo type-safe end-to-end.
- Una segunda app reusaría `sdui-core/compose/ktor` tal cual y tendría su propio `:app-contract`.

---

## 3. `sdui-core` — transporte y tipos compartidos

Nodo de transporte **genérico** (props sin tipar en el wire, tipadas al deserializar):

```kotlin
@Serializable
data class SduiNode(
    val type: String,                                  // "text", "stepper", "miComponente"
    val id: String? = null,
    val props: JsonObject = JsonObject(emptyMap()),    // se decodifica con el serializer del componente
    val modifier: UiModifier = UiModifier(),
    val actions: Map<String, List<UiAction>> = emptyMap(), // "onClick" -> [...], "onChange" -> [...]
    val children: List<SduiNode> = emptyList()
)

@Serializable
data class SduiEnvelope(
    val schemaVersion: Int,
    val screenId: String,
    val root: SduiNode,
    val variables: Map<String, JsonElement> = emptyMap(),
    val meta: Map<String, String> = emptyMap()
)
```

Patches y acciones (igual que antes, ahora en core):

```kotlin
@Serializable data class SduiPatch(val schemaVersion: Int, val changes: List<PatchOp>)
@Serializable @JsonClassDiscriminator("op") sealed interface PatchOp { /* replace/updateProps/insert/remove/setVars */ }

@Serializable @JsonClassDiscriminator("type") sealed interface UiAction
@Serializable @SerialName("navigate")  data class Navigate(val route:String, val args:Map<String,String> = emptyMap()): UiAction
@Serializable @SerialName("setVar")     data class SetVar(val name:String, val value:JsonElement): UiAction
@Serializable @SerialName("increment")  data class Increment(val name:String, val by:Int=1, val min:Int?=null, val max:Int?=null): UiAction
@Serializable @SerialName("toggle")     data class Toggle(val name:String): UiAction
@Serializable @SerialName("network")    data class FireEndpoint(val endpoint:String, val payloadVars:List<String> = emptyList()): UiAction
@Serializable @SerialName("track")      data class Track(val event:String, val props:Map<String,String> = emptyMap()): UiAction
@Serializable @SerialName("custom")     data class CustomAction(val name:String, val payload:JsonObject = JsonObject(emptyMap())): UiAction // escape hatch
@Serializable @SerialName("noop")       data object NoOpAction: UiAction
```

> Las **acciones** sí se mantienen como `sealed` (son pocas y estables) con un `CustomAction` de escape; los **componentes** van por registry abierto (son muchos y crecen por app). Es la combinación pragmática.

El **descriptor de componente** — el puente que comparten cliente y server:

```kotlin
interface SduiComponent<P : Any> {
    val type: String
    val serializer: KSerializer<P>
}
// helper para que la app declare uno en una línea:
inline fun <reified P : Any> sduiComponent(type: String) =
    object : SduiComponent<P> { override val type = type; override val serializer = serializer<P>() }
```

Factory de `Json` configurable (la librería expone, la app puede añadir su `SerializersModule`):

```kotlin
fun sduiJson(extra: SerializersModule = EmptySerializersModule()) = Json {
    classDiscriminator = "type"; encodeDefaults = false; ignoreUnknownKeys = true; explicitNulls = false
    serializersModule = extra + SerializersModule {
        polymorphicDefaultDeserializer(UiAction::class) { NoOpAction.serializer() }
    }
}
```

---

## 4. `sdui-compose` — motor de render del cliente (KMP)

API pública principal: un **registry**, un **RenderScope** y un **host** composable.

```kotlin
class ComponentRegistry internal constructor(
    private val json: Json,
    private val renderers: Map<String, ComponentEntry<*>>,
    val fallback: @Composable RenderScope.(SduiNode) -> Unit
)

class ComponentRegistryBuilder {
    fun <P:Any> register(component: SduiComponent<P>, content: @Composable RenderScope.(P) -> Unit)
    fun include(pack: ComponentPack)                 // packs reutilizables (CorePack, etc.)
    fun fallback(content: @Composable RenderScope.(SduiNode) -> Unit)
    fun build(json: Json): ComponentRegistry
}

fun componentRegistry(json: Json, block: ComponentRegistryBuilder.() -> Unit): ComponentRegistry
```

El `RenderScope` es lo que recibe cada renderer: recursión, acciones, modifier, variables.

```kotlin
interface RenderScope {
    val node: SduiNode
    val vars: VariableStore
    @Composable fun render(child: SduiNode)          // recurse en un hijo
    @Composable fun renderChildren()                 // recurse en todos los hijos
    fun fire(event: String)                          // dispara node.actions[event] al dispatcher
    @Composable fun UiModifier.toCompose(): Modifier // mapper, resuelve tokens contra el tema activo
    fun interpolate(template: String): String        // "@{var}" -> valor (reactivo)
}
```

El **host**: deserializa, monta el store, recorre el árbol; un componente desconocido cae al `fallback` (nunca crashea).

```kotlin
@Composable
fun SduiHost(
    envelope: SduiEnvelope,
    registry: ComponentRegistry,
    dispatcher: ActionDispatcher,
    modifier: Modifier = Modifier,
)
```

Genéricos que la librería provee y la app no reimplementa: `VariableStore` (StateFlow reactivo), `applyPatch(SduiPatch)` recursivo por `id`, mapper de `UiModifier`, resolución de tokens contra un `SduiTheme` que la app inyecta vía `CompositionLocal`, e interfaz `ActionDispatcher` (la app da la impl que navega / hace red / trackea).

**Pack core** que la librería incluye gratis (primitivas):

```kotlin
val CorePack = componentPack {
    register(Column) { p -> Column(Modifier.toCompose(), verticalArrangement = Arrangement.spacedBy(p.spacing.dp)) { renderChildren() } }
    register(Row)    { p -> Row(Modifier.toCompose(), horizontalArrangement = Arrangement.spacedBy(p.spacing.dp)) { renderChildren() } }
    register(Box)    { _ -> Box(Modifier.toCompose()) { renderChildren() } }
    register(Text)   { p -> Text(interpolate(p.text), style = p.style.asTextStyle(), color = p.color.asColor()) }
    register(Image)  { p -> AsyncImage(p.url, p.contentDescription, Modifier.toCompose()) }
    register(Button) { _ -> Button(onClick = { fire("onClick") }, Modifier.toCompose()) { renderChildren() } }
    register(LazyColumnC){ _ -> LazyColumn(Modifier.toCompose()) { items(node.children, key = { it.id ?: it.hashCode().toString() }) { render(it) } } }
    register(Spacer) { p -> Spacer(Modifier.size(p.size.dp)) }
    register(Divider){ p -> HorizontalDivider(thickness = p.thickness.dp) }
}
```

donde `Column`, `Text`, etc. son `SduiComponent<…Props>` con sus Props definidas en core (o en un `sdui-compose-core-components`).

---

## 5. `sdui-ktor` — motor del servidor (plugin de Ktor)

Se expone como **plugin** de Ktor. Instala serialización, rutas (`/screen/{id}`, `/action`), capability negotiation; la app registra screens y action handlers en el bloque de config.

```kotlin
class SduiConfig {
    var schemaVersion: Int = 1
    var basePath: String = ""
    internal val screens = mutableMapOf<String, suspend SduiContext.() -> SduiEnvelope>()
    internal val actions = mutableMapOf<String, suspend SduiActionContext.() -> SduiResponse>()
    var capability: (ApplicationCall) -> ClientCapability = { ClientCapability.from(it) }
    var json: Json = sduiJson()

    fun screen(id: String, build: suspend SduiContext.() -> SduiEnvelope) { screens[id] = build }
    fun action(name: String, handle: suspend SduiActionContext.() -> SduiResponse) { actions[name] = handle }
}

val Sdui = createApplicationPlugin("Sdui", ::SduiConfig) {
    // install(ContentNegotiation){ json(pluginConfig.json) } si no está
    application.routing {
        get("${pluginConfig.basePath}/screen/{id}") {
            val ctx = SduiContext(call, pluginConfig.capability(call))
            val id = call.parameters["id"]!!
            val build = pluginConfig.screens[id] ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(build(ctx))                    // SduiEnvelope
        }
        post("${pluginConfig.basePath}/action") {
            val action = call.receive<UiAction>()
            val name = action.routingKey()              // p.ej. endpoint o type
            val handle = pluginConfig.actions[name] ?: return@post call.respond(NoOpResponse)
            call.respond(handle(SduiActionContext(call, action)))  // SduiPatch o SduiEnvelope
        }
    }
}
```

**DSL de ensamblado tipado**, basado en los descriptores de componente (mismo `type` y Props que el cliente):

```kotlin
fun sduiTree(block: NodeScope.() -> Unit): SduiNode
class NodeScope {
    fun <P:Any> node(c: SduiComponent<P>, props: P, id: String? = null,
                     modifier: UiModifier = UiModifier(),
                     actions: Map<String,List<UiAction>> = emptyMap(),
                     children: NodeScope.() -> Unit = {})
    // azúcar para contenedores:
    fun column(modifier: UiModifier = UiModifier(), spacing: Int = 0, body: NodeScope.() -> Unit)
    fun text(value: String, style: String = "type.body")
    fun button(label: String, onClick: List<UiAction>)
}
```

Patch builder + capability negotiation también en la librería; el `ClientCapability` (versión + set de componentes soportados) llega al `SduiContext` para que la app decida qué emitir.

---

## 6. End-to-end: definir UN componente propio, usado por ambos lados

**Paso 1 — en `:app-contract`** (commonMain, compartido): declara Props + descriptor + (opcional) DSL.

```kotlin
@Serializable data class StepperProps(val bind: String, val min: Int = 1, val max: Int = 99)
val Stepper = sduiComponent<StepperProps>("stepper")
```

**Paso 2 — en `:app-client`**: registra el renderer.

```kotlin
val registry = componentRegistry(appJson) {
    include(CorePack)
    register(Stepper) { p ->
        val qty = vars.intOf(p.bind)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ vars.increment(p.bind, -1, min = p.min) }) { Icon(Icons.Default.Remove, null) }
            Text("$qty", style = MaterialTheme.typography.titleLarge)
            IconButton({ vars.increment(p.bind, +1, max = p.max) }) { Icon(Icons.Default.Add, null) }
        }
    }
    fallback { /* nada en prod, placeholder en debug */ }
}
```

**Paso 3 — en `:app-server`**: úsalo en una pantalla con el mismo descriptor.

```kotlin
install(Sdui) {
    screen("cart") {
        SduiEnvelope(schemaVersion, "cart", variables = mapOf("qty" to JsonPrimitive(2)),
            root = sduiTree {
                column(spacing = 12) {
                    text("Tu carrito", style = "type.title")
                    node(Stepper, StepperProps(bind = "qty", max = 10), id = "qty-stepper")
                    button("Pagar", onClick = listOf(FireEndpoint("/cart/checkout", payloadVars = listOf("qty"))))
                }
            })
    }
    action("/cart/checkout") {
        // ... recibe qty, calcula, devuelve SduiPatch
        SduiPatch(schemaVersion, changes = listOf(SetVars(mapOf("orderId" to JsonPrimitive("A-1029")))))
    }
}
```

Server y cliente referencian `Stepper` y `StepperProps` desde el **mismo** `:app-contract` → el contrato de ese componente es type-safe end-to-end, aunque el engine no sepa nada de "stepper".

---

## 7. Publicación

- **`sdui-core` y `sdui-compose`**: KMP libraries (`com.android.library` + targets iOS), publicadas con el Gradle Maven Publish plugin / `vanniktech.maven.publish`. Para iOS distribuyes vía el artefacto KMP normal (o XCFramework si quieres consumir desde Swift puro).
- **`sdui-ktor`**: JVM library.
- Versionado de las tres en lockstep con un BOM o un version catalog publicado para que el consumidor no desalinee `core`.
- Compatibilidad: el `schemaVersion` viaja en el envelope/patch; la capability negotiation vive en `sdui-ktor`. Evoluciona el **wire** (SduiNode/Envelope/Patch) solo de forma aditiva para no romper apps que dependan de `core`.
- Distribución interna: Maven local, GitHub Packages, o tu repo. Si es OSS, Maven Central.

---

## 8. Migración desde lo que ya tienes

1. Mueve `SduiEnvelope/Patch/UiAction/UiModifier/SduiJson` a **`sdui-core`**, y reemplaza el `sealed UiNode` por el `SduiNode` genérico + descriptores `SduiComponent<P>`.
2. Extrae el `Render` recursivo, el `VariableStore`, el `applyPatch` y el mapper de modifier a **`sdui-compose`**, detrás del `ComponentRegistry`/`RenderScope`. Mueve las primitivas (column/row/text…) al `CorePack`.
3. Extrae plugins + routing + DSL + capability a **`sdui-ktor`** como el plugin `Sdui`.
4. Lo que era específico (pantallas, componentes de dominio, dispatcher real, tema) queda en `:app-contract` / `:app-client` / `:app-server`.
5. Los tests del engine viven con las librerías (round-trip de serialización, applyPatch, render de CorePack); los tests de pantallas y componentes propios, en la app.

---

## 9. Tradeoffs (qué ganas / qué cedes)

**Ganas:** reutilización entre apps; engine testeable y versionado por separado; el equipo de la app solo piensa en componentes y pantallas; el pack core da primitivas gratis; segunda app = nuevo `:app-contract` y nada más.

**Cedes / cuidado:**
- **Exhaustividad del `when`** → la reemplaza el lookup en el registry. Mitigación: un test que valide que todo `type` emitido por el server está registrado en el cliente (o capability negotiation que lo garantice en runtime).
- **Props sin tipar en el wire** (`JsonObject`) → se re-tipan al deserializar con el serializer del descriptor. El riesgo de error queda contenido al borde (un `type` con Props mal formadas cae al fallback, no rompe la pantalla).
- **Una indirección más** (registry, packs) → más ceremonia para empezar, pero es la que habilita la reutilización. Para una sola app pequeña podría ser over-engineering; vale la pena cuando habrá ≥2 apps/superficies o un equipo que consume el engine sin tocarlo.
- **Acoplamiento de versiones** `core`↔`compose`↔`ktor` → manéjalo con BOM/catálogo y evolución aditiva del wire.

**Regla:** generaliza el **engine** a librería, mantén el **catálogo** en la app. El descriptor `SduiComponent<P>` compartido en `:app-contract` es lo que te devuelve el type-safety end-to-end sin sacrificar la extensibilidad.

---

## 10. Checklist de implementación

1. [ ] Crear `sdui-core` (KMP): `SduiNode`, `SduiEnvelope`, `SduiPatch`/`PatchOp`, `UiAction`, `UiModifier`, `SduiComponent<P>`, `sduiJson()`, modelo de variables.
2. [ ] Crear `sdui-compose` (KMP): `ComponentRegistry` + builder, `RenderScope`, `SduiHost`, `VariableStore`, `applyPatch`, mapper de modifier, `SduiTheme`/tokens, `ActionDispatcher`, `CorePack`.
3. [ ] Crear `sdui-ktor` (JVM): plugin `Sdui`, `SduiConfig` (screen/action), `ScreenRegistry`, `sduiTree` DSL, patch DSL, `ClientCapability`/negociación, `ActionRouter`.
4. [ ] Tests del engine: round-trip de serialización, `applyPatch` por `id`, render de `CorePack`, `testApplication` del plugin.
5. [ ] Publicar las 3 (Maven local primero) con BOM/catálogo de versiones.
6. [ ] Crear `:app-contract` con 1 componente propio (Stepper) para validar el flujo end-to-end en Android + iOS.
7. [ ] Recién entonces migrar el resto del catálogo, componente por componente.
