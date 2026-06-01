# Diseño — Catálogo de componentes extensible (ComponentRegistry · OCP)

> Spec ID: 004 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Reemplazar el `when (node.type)` de `RenderNode` (motor) por un `ComponentRegistry` abierto: un mapa
`type → RegisteredComponent` donde cada entrada empareja un `SduiComponent<P>` (de `:sdui-core`, aporta
`type` + `serializer<P>`) con un renderer `@Composable RenderScope.(P) -> Unit`. El motor decodifica
`node.props` a `P` con el serializer y delega. El registro se inyecta por `LocalComponentRegistry`
(default `CorePack`), así la app extiende el catálogo sin tocar el motor. Las acciones se siguen
delegando por `LocalSduiActionHandler` (spec 003). Se preserva la frontera: el motor depende solo de
`:sdui-core` + Compose.

## Arquitectura (paquetes; todo en commonMain de `:shared`)
```
dev/kuisd/sdui/   (MOTOR)
├── ComponentRegistry.kt   public  RegisteredComponent, ComponentRegistry, builder/DSL, LocalComponentRegistry
├── RenderScope.kt         public  RenderScope (node + renderChildren())
├── RenderNode.kt          public  RenderNode(node) — resuelve por registry (sustituye el when)
├── CorePack.kt            public  CorePack + props (ColumnProps/RowProps/TextProps/ButtonProps)
├── SduiActionHandler.kt   (spec 003, sin cambios)
├── NodeProps.kt           internal (puede quedar; CorePack usa props tipadas)
└── Logging.kt             internal
dev/kuisd/app/
├── components/            ← NUEVO: catálogo propio de la app
│   ├── BadgeComponent.kt  internal  BadgeProps + sduiComponent + renderer
│   └── AppComponents.kt   internal  appRegistry = CorePack + badge
└── SduiHost.kt            (cambia) provee LocalComponentRegistry = appRegistry
server/.../screens/
└── DetailsScreen.kt       (cambia) añade un nodo `badge` (demo de extensión)
```

## Componentes y contratos (motor)

### RegisteredComponent + ComponentRegistry + DSL — HU-1.1
```kotlin
class RegisteredComponent<P : Any>(
    val component: SduiComponent<P>,
    val renderer: @Composable RenderScope.(P) -> Unit,
) {
    @Composable
    fun Render(node: SduiNode) {                       // captura P internamente (registry guarda <*>)
        val props = remember(node) {
            runCatching { DefaultSduiJson.decodeFromJsonElement(component.serializer, node.props) }.getOrNull()
        }
        if (props == null) { UnknownNode(node.type); return }   // HU-4.2
        with(RenderScope(node)) { renderer(props) }
    }
}

class ComponentRegistry internal constructor(private val byType: Map<String, RegisteredComponent<*>>) {
    fun rendererFor(type: String): RegisteredComponent<*>? = byType[type]
    operator fun plus(other: ComponentRegistry): ComponentRegistry =
        ComponentRegistry(byType + other.byType)       // combinar (CorePack + appComponents)
}

class ComponentRegistryBuilder {
    @PublishedApi internal val entries = mutableMapOf<String, RegisteredComponent<*>>()
    fun <P : Any> register(component: SduiComponent<P>, renderer: @Composable RenderScope.(P) -> Unit) {
        entries[component.type] = RegisteredComponent(component, renderer)
    }
}
fun componentRegistry(block: ComponentRegistryBuilder.() -> Unit): ComponentRegistry =
    ComponentRegistry(ComponentRegistryBuilder().apply(block).entries.toMap())

val LocalComponentRegistry: ProvidableCompositionLocal<ComponentRegistry> =
    staticCompositionLocalOf { CorePack }              // default = CorePack (HU-1.3)
```

### RenderScope — HU-2.2
```kotlin
class RenderScope internal constructor(val node: SduiNode) {
    @Composable fun renderChildren() { node.children.forEach { RenderNode(it) } }
}
```

### RenderNode (sustituye el when) — HU-1.2, HU-4.1
```kotlin
@Composable
fun RenderNode(node: SduiNode) {
    val entry = LocalComponentRegistry.current.rendererFor(node.type)
        ?: run { UnknownNode(node.type); return }
    entry.Render(node)
}

@Composable
internal fun UnknownNode(type: String) { Text("Componente no soportado: $type") }
```

### CorePack — HU-2
```kotlin
@Serializable class ColumnProps
@Serializable class RowProps
@Serializable data class TextProps(val text: String = "", val style: String? = null)
@Serializable data class ButtonProps(val label: String = "")

val CorePack: ComponentRegistry = componentRegistry {
    register(sduiComponent<ColumnProps>("column")) { Column { renderChildren() } }
    register(sduiComponent<RowProps>("row")) { Row { renderChildren() } }
    register(sduiComponent<TextProps>("text")) { p -> Text(p.text) }
    register(sduiComponent<ButtonProps>("button")) { p ->
        val handler = LocalSduiActionHandler.current
        Button(onClick = { handler.handle(node.actions["onClick"].orEmpty()) }) { Text(p.label) }
    }
}
```
- `renderChildren()` y `node` provienen del `RenderScope` receiver. El `button` usa
  `LocalSduiActionHandler` (spec 003) — el motor sigue solo delegando.

## App (demo de extensión) — HU-3
```kotlin
// dev.kuisd.app.components  (internal)
@Serializable data class BadgeProps(val text: String = "")

internal val BadgeComponent = sduiComponent<BadgeProps>("badge")

internal val appComponents: ComponentRegistry = componentRegistry {
    register(BadgeComponent) { p ->
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
            Text(p.text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

internal val appRegistry: ComponentRegistry = CorePack + appComponents
```
`SduiHost` provee el registro al árbol (junto al action handler):
```kotlin
CompositionLocalProvider(
    LocalSduiActionHandler provides handler,
    LocalComponentRegistry provides appRegistry,
) {
    key(current.id) { SduiScreen(current.route, source, Modifier.padding(padding)) }
}
```

## Server (demo) — HU-3.2
`DetailsScreen` añade un nodo `badge` (p.ej. junto al título): `SduiNode(type = "badge", props = {"text":"Nuevo"})`.
El motor por sí solo (CorePack) lo mostraría como `UnknownNode`; al estar registrado en la app, se renderiza.

## Build
`shared/build.gradle.kts`: aplicar `alias(libs.plugins.kotlin.serialization)` y `alias(libs.plugins.compose.compiler)` (ya está). El plugin de serialización es necesario para generar los serializers de las props `@Serializable` del motor y del `badge`. (`:sdui-core` ya expone kotlinx-serialization como `api`.)

## public / internal
- **public (motor):** `RenderNode`, `RenderScope`, `ComponentRegistry`, `RegisteredComponent`,
  `ComponentRegistryBuilder`, `componentRegistry`, `LocalComponentRegistry`, `CorePack`, las props de CorePack,
  `SduiActionHandler`/`LocalSduiActionHandler` (003).
- **internal:** `UnknownNode`, `NodeProps`/`sduiLog`, y en la app `BadgeProps`/`BadgeComponent`/`appComponents`/`appRegistry`.

## Riesgos y mitigaciones
- **Decodificar props por nodo** → `decodeFromJsonElement` con `ignoreUnknownKeys` (de `DefaultSduiJson`)
  + defaults en las props → tolerante; fallo → `UnknownNode` con log (HU-4.2). `remember(node)` evita
  redecodificar en cada recomposición.
- **`@Composable` function-type en `RegisteredComponent`** → soportado por Compose; el render se invoca
  dentro de `RenderNode` (contexto @Composable).
- **Captura de `P` con `RegisteredComponent<*>`** → resuelta porque `Render(node)` no expone `P` en su firma.
- **Plugin de serialización en `:shared`** → cambio de build acotado; sin nuevas dependencias (el plugin ya está en el catálogo).

## Estrategia de verificación
- **Unit (`:shared` commonTest):**
  - `ComponentRegistry`: `rendererFor` devuelve el componente por type; `plus` combina (CorePack + extra)
    y la extra gana/añade; `rendererFor` de un type ausente → null.
  - (decodificación) un `SduiComponent<TextProps>` decodifica `{"text":"hola"}` → `TextProps("hola")` y
    `{}` → `TextProps("")` (defaults); props inválidas → null (vía runCatching).
- **Unit (`:server`):** la pantalla con `badge` sirve 200 y el envelope contiene un nodo `type=="badge"`.
- **Regla de dependencias:** `grep` confirma que `dev/kuisd/sdui/` no importa `dev.kuisd.app` ni `io.ktor.`.
- **Smoke:** `:server:run` + `:desktopApp:run`: el `badge` ("Nuevo") se ve en `details` (extensión de la
  app), y un `type` inventado mostraría "Componente no soportado". Navegación 003 intacta.
- **Calidad:** `./gradlew :sdui-core:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` en verde.
