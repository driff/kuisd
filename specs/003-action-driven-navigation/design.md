# Diseño — Navegación dirigida por acciones (Navigate)

> Spec ID: 003 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Introducir en `:shared` un `ActionDispatcher` que interpreta `List<UiAction>`: `Navigate` apila una
ruta y `NavigateBack` desapila, sobre una **pila de navegación en memoria** (`NavBackStack`). Un nuevo
composable **`SduiHost`** es dueño del back stack y de **un único `SduiClient` compartido**, renderiza
la pantalla del tope vía el `SduiScreen` existente, y muestra una **barra superior cross-platform** con
"atrás" (afordancia de texto, sin dependencia de iconos) cuando hay pantalla anterior. `RenderNode`
deja de hacer `sduiLog` en el `onClick` y despacha `node.actions["onClick"]` al dispatcher, inyectado
por `CompositionLocal`. El cambio de pantalla usa "recarga total" (`GET /screen/{route}`). Se añade al
contrato la acción **`NavigateBack`**, y en el server dos screens (`details`, `more`) para una cadena
`home → details → more`.

## Arquitectura
Cliente 100 % en `commonMain` de `:shared` (HU-4.1). Contrato y server también cambian:

```
sdui-core/src/commonMain/.../core/
└── UiAction.kt           # (cambia) + data object NavigateBack : UiAction  @SerialName("navigateBack")

shared/src/commonMain/kotlin/dev/kuisd/sdui/
├── NavBackStack.kt       # pila en memoria (snapshot-state)              (HU-2)
├── ActionDispatcher.kt   # interpreta Navigate/NavigateBack              (HU-1, HU-3)
├── SduiHost.kt           # @Composable: dueño del back stack + SduiClient único + barra atrás
├── SduiScreen.kt         # (cambia) recibe client + modifier; ya no cierra el cliente
├── RenderNode.kt         # (cambia) button.onClick -> dispatcher
└── presentation/PlaceholderApp.kt   # (cambia) monta SduiHost("home")

server/src/main/kotlin/dev/kuisd/server/screens/
├── DetailsScreen.kt      # NUEVO "details": botón "Ver más" -> Navigate("more") + "Atrás" -> NavigateBack
├── MoreScreen.kt         # NUEVO "more": botón "Atrás" -> NavigateBack
└── ScreenRegistry.kt     # (cambia) registra "details" y "more"
```

Flujo: `SduiHost("home")` posee `NavBackStack(initial="home")` + un `SduiClient` →
renderiza `SduiScreen(current.route, client=shared)` → `RenderNode` lee `LocalActionDispatcher` y en el
`onClick` del botón llama `dispatcher.dispatch(node.actions["onClick"])` → el dispatcher hace
`push`/`pop` sobre el back stack → recomposición → `SduiHost` renderiza el nuevo tope (nuevo
`produceState` por `key(route)` → recarga del BFF) y muestra/oculta la barra "atrás".

## Componentes y contratos

### Contrato: `NavigateBack` (`:sdui-core`) — HU-2.1, HU-3.4
- **Cambio en `UiAction.kt`:** nuevo subtipo `sealed` (aditivo, forward-compatible).
```kotlin
@Serializable
@SerialName("navigateBack")
data object NavigateBack : UiAction
```
- Clientes con `:sdui-core` antiguo: `NavigateBack` cae al `polymorphicDefaultDeserializer` →
  `NoOpAction` (HU-3.4). Test de round-trip en `sdui-core` `commonTest`.

### NavBackStack — HU-2, HU-4.1
- **Ubicación:** `commonMain` · `dev.kuisd.sdui.NavBackStack`.
```kotlin
data class NavEntry(val route: String, val args: Map<String, String> = emptyMap())

@Stable
class NavBackStack(initial: NavEntry) {
    var entries: List<NavEntry> by mutableStateOf(listOf(initial))
        private set
    val current: NavEntry get() = entries.last()
    val canGoBack: Boolean get() = entries.size > 1

    fun push(entry: NavEntry) { entries = entries + entry }
    fun pop(): Boolean {                       // no desapila la raíz (HU-2.3)
        if (entries.size <= 1) return false
        entries = entries.dropLast(1)
        return true
    }
}
```
- **Decisión:** estado con `mutableStateOf` (no `StateFlow`): único consumidor es la composición de
  `SduiHost`. Descartado: Navigation Compose/3 (dependencia + rutas tipadas que chocan con rutas
  dinámicas del BFF) y `expect/actual` sobre back nativo.

### ActionDispatcher — HU-1, HU-2.1, HU-3
- **Ubicación:** `commonMain` · `dev.kuisd.sdui.ActionDispatcher`.
```kotlin
fun interface ActionDispatcher {
    fun dispatch(actions: List<UiAction>)
}

class NavigationActionDispatcher(
    private val backStack: NavBackStack,
) : ActionDispatcher {
    override fun dispatch(actions: List<UiAction>) {
        actions.forEach { action ->
            when (action) {
                is Navigate -> backStack.push(NavEntry(action.route, action.args))
                NavigateBack -> backStack.pop()           // no-op si solo queda la raíz
                else -> sduiLog("acción no soportada en spec 003 (no-op): ${action::class.simpleName}")
            }
        }
    }
}

val LocalActionDispatcher: ProvidableCompositionLocal<ActionDispatcher> =
    staticCompositionLocalOf { ActionDispatcher { /* no-op por defecto fuera de un SduiHost */ } }
```
- **Decisión:** firma mínima `dispatch(List<UiAction>)` (sin store/red, specs futuras). `CompositionLocal`
  evita propagar el dispatcher por parámetro en `RenderNode`. El default no-op cubre `RenderNode` sin host.

### SduiHost — HU-1, HU-2, HU-4.2
- **Ubicación:** `commonMain` · `@Composable`. **Dueño del back stack y del `SduiClient` único** (HU-4.2).
```kotlin
@Composable
fun SduiHost(
    startRoute: String,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { NavBackStack(NavEntry(startRoute)) }
    val dispatcher = remember { NavigationActionDispatcher(backStack) }
    val client = remember { SduiClient() }
    DisposableEffect(client) { onDispose { client.close() } }   // el host posee el ciclo de vida (HU-4.2)

    val current = backStack.current
    Scaffold(
        modifier = modifier,
        topBar = {
            if (backStack.canGoBack) {                          // afordancia cross-platform (HU-2.2)
                TopAppBar(
                    title = { Text(current.route) },            // título provisional = route
                    navigationIcon = {
                        TextButton(onClick = { backStack.pop() }) { Text("‹ Atrás") }  // sin dep de iconos
                    },
                )
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalActionDispatcher provides dispatcher) {
            key(current.route) {                                // produceState nuevo => recarga total (HU-1.3)
                SduiScreen(
                    screenId = current.route,
                    client = client,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
```
- **Decisiones:**
  - **`SduiClient` único** creado y cerrado por el host (HU-4.2): no se abre/cierra el engine HTTP en
    cada navegación. `SduiScreen` deja de gestionar el ciclo de vida del cliente (ver abajo).
  - **Afordancia de texto** (`TextButton("‹ Atrás")`) en `TopAppBar`: `material-icons` no está en
    `:shared`; se evita añadir dependencia. La barra también dispara `pop` (equivalente a `NavigateBack`).
  - **`key(current.route)`**: fuerza recomposición/`produceState` nuevo al cambiar el tope → recarga total.
  - **`title = current.route`**: provisional; título por `meta`/tokens es spec futura.

### SduiScreen (cambio) — propiedad del cliente movida al host
- **Ubicación:** `commonMain` · `SduiScreen.kt` (existente). Cambios respecto al slice 001:
  - Recibe `client: SduiClient` (lo provee el host) y un `modifier: Modifier = Modifier`.
  - **Ya NO crea ni cierra** el `SduiClient` (sin `DisposableEffect.onDispose { client.close() }`): el
    dueño es el `SduiHost`. Evita cerrar un cliente compartido al desmontar una pantalla al navegar.
```kotlin
@Composable
fun SduiScreen(
    screenId: String,
    client: SduiClient,                 // requerido: lo posee el SduiHost
    modifier: Modifier = Modifier,
)
```
- Lógica de `produceState`/estados (Loading/Error/Content) y mapeo de error legible: **sin cambios**.

### RenderNode (cambio) — HU-1.2, HU-3.1, HU-3.2
```kotlin
"button" -> {
    val dispatcher = LocalActionDispatcher.current
    Button(
        onClick = { dispatcher.dispatch(node.actions["onClick"].orEmpty()) },  // vacío => no-op (HU-3.2)
    ) {
        Text(node.stringProp("label").orEmpty())
    }
}
```
- El filtrado por tipo de acción lo hace el `ActionDispatcher` (HU-3.1), no `RenderNode`.

### Server: DetailsScreen + MoreScreen (nuevos) — cadena home→details→more
- `DetailsScreen` ("details"): texto "Detalles" + botón "Ver más" → `Navigate("more")` + botón
  "Atrás" → `NavigateBack`.
- `MoreScreen` ("more"): texto "Más" + botón "Atrás" → `NavigateBack`.
- `ScreenRegistry.defaultScreenRegistry()` registra `home`, `details`, `more`:
```kotlin
ScreenRegistry(mapOf("home" to HomeScreen, "details" to DetailsScreen, "more" to MoreScreen))
```
- (El `HomeScreen` ya emite `Navigate("details")` en su botón "Empezar"; no cambia.)

## Modelo de datos y estados
- Contrato: `UiAction` gana `NavigateBack`. Resto reutilizado (`Navigate`, `SduiNode`, `SduiEnvelope`).
- Estado de navegación local: `NavBackStack.entries: List<NavEntry>` (snapshot-state), sin persistencia.
- `SduiUiState` (slice 001) sin cambios.

## Dependencias nuevas (catálogo)
Ninguna. Se usan Compose Material3 (`Scaffold`, `TopAppBar`, `TextButton`) y Coroutines ya presentes.

## Riesgos y mitigaciones
- **Recarga por navegación** (push/pop recargan del BFF) → es la estrategia "recarga total" elegida;
  caché por entrada es mejora futura.
- **Cierre indebido del cliente compartido** → mitigado: el ciclo de vida del `SduiClient` vive solo en
  `SduiHost`; `SduiScreen` ya no lo cierra. Cubierto al verificar navegación ida/vuelta sin error de red.
- **`NavigateBack` en la raíz** → `pop()` devuelve `false` y no vacía la pila (HU-2.3); barra oculta.
- **Acción/ruta desconocida** → no-op con log / 404 `ProblemDetail` mostrado sin corromper la pila (HU-3).
- **Back nativo no integrado** → en Android el back físico podría salir de la app; aceptado (afordancia en barra).

## Estrategia de verificación
- **Unit (`:sdui-core` commonTest):** round-trip de `NavigateBack`; un `type` desconocido sigue cayendo a `NoOpAction`.
- **Unit (`:shared` commonTest):**
  - `NavBackStack`: `push`/`pop`/`canGoBack`; `pop` en pila de 1 → `false` y no vacía.
  - `NavigationActionDispatcher`: `Navigate("details")` apila; `NavigateBack` desapila; en raíz no-op;
    acción no soportada y `emptyList()` no alteran la pila.
- **Unit (`:server` testApplication):** `GET /screen/details` y `/screen/more` → 200 con su `screenId`;
  `GET /screen/home` sigue 200.
- **Smoke manual end-to-end:** `:server:run` + `:desktopApp:run`: `home` ("Empezar") → `details`
  ("Ver más") → `more`; "Atrás" (barra) sube en la pila hasta `home`; la barra desaparece en la raíz.
- **Calidad:** `./gradlew :sdui-core:check :shared:assemble :server:build :androidApp:assembleDebug detekt ktlintCheck` en verde.
