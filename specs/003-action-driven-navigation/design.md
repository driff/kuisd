# Diseño — Navegación por acciones + frontera Clean Architecture (motor puro / app)

> Spec ID: 003 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Separar el código de `:shared` en **capas** (paquetes) con regla de dependencias estricta, e implementar
la navegación en la capa de app. El **motor** (`dev.kuisd.sdui`) solo renderiza un `SduiNode` y delega
las acciones por un seam (`SduiActionHandler`/`LocalSduiActionHandler`). La **app** (`dev.kuisd.app`)
posee el `ScreenSource` (fetch tras interfaz), el estado de carga, el `NavBackStack` y el
`NavActionHandler` que interpreta `Navigate`/`NavigateBack`. El contrato (`:sdui-core`) gana
`NavigateBack`. No se crean módulos Gradle: la frontera es por paquetes (preparada para extraer
`:sdui-compose`/`:app-client` después).

## Capas y regla de dependencias
```
:sdui-core (dev.kuisd.sdui.core)        contrato @Serializable — no depende de nadie del cliente
        ▲
dev.kuisd.sdui      (MOTOR, Compose)    depende SOLO de core + Compose; PROHIBIDO Ktor y app
        ▲
dev.kuisd.app.data  (DATA, Ktor)        depende de core; NO conoce Compose ni el motor
        ▲
dev.kuisd.app       (APP/composición)   depende de motor + data + core (único que ve Ktor y Compose)
```

## Mapa de paquetes dentro de `:shared` (commonMain salvo nota)
```
dev/kuisd/
├── sdui/                          ← MOTOR (candidato a :sdui-compose)
│   ├── RenderNode.kt              public  @Composable RenderNode(node)  — delega acciones
│   ├── SduiActionHandler.kt       public  fun interface + LocalSduiActionHandler (SEAM)
│   ├── NodeProps.kt               internal helpers de props
│   └── Logging.kt                 internal sduiLog
└── app/
    ├── data/
    │   ├── ScreenSource.kt        public   interface (SEAM de entrada, DIP)
    │   ├── KtorScreenSource.kt    internal impl (usa SduiClient)
    │   ├── SduiClient.kt          internal (mover desde sdui/)
    │   ├── SduiHttp.kt            internal expect (commonMain) + actuals (android/ios/desktopMain)
    │   └── HttpErrorMapper.kt     internal Throwable→mensaje legible (saca Ktor de presentación)
    ├── ScreenUiState.kt           public   sealed Loading/Error/Content
    ├── SduiScreen.kt              public   @Composable: estado + render (sin Ktor)
    ├── nav/
    │   ├── NavBackStack.kt        public   pila (snapshot-state, id estable) + NavEntry
    │   └── NavActionHandler.kt    internal SduiActionHandler que interpreta Navigate/NavigateBack
    ├── SduiHost.kt                public   @Composable: dueño de NavBackStack + ScreenSource + handler
    └── presentation/PlaceholderApp.kt   monta SduiHost("home")
```
> Nota expect/actual: `SduiHttp` se mueve a `dev.kuisd.app.data` manteniendo `expect` en commonMain y
> `actual` en android/ios/desktopMain (mismo patrón del slice 001, solo cambia el paquete).

## Componentes y contratos

### MOTOR · SduiActionHandler (seam de salida) — HU-1.2, DIP
```kotlin
// dev.kuisd.sdui  (public)
fun interface SduiActionHandler {
    fun handle(actions: List<UiAction>)
}

val LocalSduiActionHandler: ProvidableCompositionLocal<SduiActionHandler> =
    staticCompositionLocalOf { SduiActionHandler { } }   // default no-op: RenderNode usable sin host
```

### MOTOR · RenderNode — HU-1.1, HU-1.5
- `@Composable fun RenderNode(node: SduiNode)` con `when (node.type)`: column/row/text/button + `UnknownNode`.
- El botón **delega**, no interpreta:
```kotlin
"button" -> {
    val handler = LocalSduiActionHandler.current
    Button(onClick = { handler.handle(node.actions["onClick"].orEmpty()) }) {
        Text(node.stringProp("label").orEmpty())
    }
}
```
- `NodeProps`/`sduiLog` pasan a `internal`. El motor no importa Ktor ni `dev.kuisd.app`.

### DATA · ScreenSource (+ impl Ktor) — HU-2, DIP-1
```kotlin
// dev.kuisd.app.data  (public)
fun interface ScreenSource {
    // Nota de impl: un `fun interface` (SAM) de Kotlin no admite valores por defecto en su método
    // abstracto, así que `args` no lleva `= emptyMap()`; el default se pasa explícito en el call site.
    suspend fun load(screenId: String, args: Map<String, String>): SduiEnvelope
}

// internal — la impl Ktor; SduiClient/SduiHttp quedan como detalle privado
internal class KtorScreenSource(
    private val client: SduiClient = SduiClient(),
) : ScreenSource {
    override suspend fun load(screenId: String, args: Map<String, String>): SduiEnvelope =
        client.fetchScreen(screenId)
    fun close() { client.close() }
}
```
- `HttpErrorMapper` (internal, data): traduce `ResponseException`/timeouts/parse a un `String` legible
  (mueve aquí `toUserMessage`/`problemMessage` de `SduiScreen`, con los imports de Ktor). Reutiliza el
  `ProblemDetail` del server.

### APP · ScreenUiState + SduiScreen — HU-3, SRP
```kotlin
// dev.kuisd.app  (public)
sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Error(val message: String) : ScreenUiState
    data class Content(val envelope: SduiEnvelope) : ScreenUiState
}

@Composable
fun SduiScreen(
    screenId: String,
    source: ScreenSource,                 // depende de la abstracción, NO de SduiClient/Ktor
    modifier: Modifier = Modifier,
) {
    val state by produceState<ScreenUiState>(ScreenUiState.Loading, screenId, source) {
        value = try {
            ScreenUiState.Content(source.load(screenId))
        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            ScreenUiState.Error(HttpErrorMapper.message(e))   // mapeo vive en data
        }
    }
    when (val s = state) { /* Loading -> spinner; Error -> Text; Content -> RenderNode(s.envelope.root) */ }
}
```
- `SduiScreen` ya **no** crea ni cierra cliente (lo hace el host) ni importa Ktor (SRP-1, CA-1).

### APP · NavBackStack + NavActionHandler — HU-4.1
```kotlin
// dev.kuisd.app.nav
data class NavEntry(val id: Long, val route: String, val args: Map<String, String> = emptyMap())   // public

@Stable
class NavBackStack(startRoute: String, startArgs: Map<String, String> = emptyMap()) {               // public
    private var nextId = 0L
    var entries: List<NavEntry> by mutableStateOf(listOf(NavEntry(nextId++, startRoute, startArgs)))
        private set
    val current: NavEntry get() = entries.last()
    val canGoBack: Boolean get() = entries.size > 1
    fun push(route: String, args: Map<String, String> = emptyMap()) { entries = entries + NavEntry(nextId++, route, args) }
    fun pop(): Boolean { if (entries.size <= 1) return false; entries = entries.dropLast(1); return true }
}

internal class NavActionHandler(private val backStack: NavBackStack) : SduiActionHandler {
    override fun handle(actions: List<UiAction>) = actions.forEach {
        when (it) {
            is Navigate -> backStack.push(it.route, it.args)
            NavigateBack -> backStack.pop()
            else -> sduiLog("acción no soportada (no-op): ${it::class.simpleName}")
        }
    }
}
```

### APP · SduiHost — HU-4.2
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiHost(startRoute: String, modifier: Modifier = Modifier) {
    val backStack = remember { NavBackStack(startRoute) }
    val source = remember { KtorScreenSource() }
    DisposableEffect(source) { onDispose { source.close() } }     // el host posee el ciclo de vida (SRP/DIP)
    val handler = remember { NavActionHandler(backStack) }

    val current = backStack.current
    Scaffold(
        modifier = modifier,
        topBar = { if (backStack.canGoBack) TopAppBar(title = { Text(current.route) }, navigationIcon = {
            TextButton(onClick = { backStack.pop() }) { Text("‹ Atrás") }
        }) },
    ) { padding ->
        CompositionLocalProvider(LocalSduiActionHandler provides handler) {
            key(current.id) { SduiScreen(current.route, source, Modifier.padding(padding)) }
        }
    }
}
```
- `KtorScreenSource` se usa aquí como impl concreta del seam; si se quisiera testear el host con un
  `FakeScreenSource`, basta sobrecargar el parámetro (no requerido en esta spec).

### CONTRATO · NavigateBack (`:sdui-core`) — HU-5.1
```kotlin
@Serializable @SerialName("navigateBack") data object NavigateBack : UiAction
```
Forward-compat: un `:sdui-core` antiguo lo degrada a `NoOpAction` vía `polymorphicDefaultDeserializer`.

### SERVER · DetailsScreen + MoreScreen
- `details`: texto "Detalles" + botón "Ver más" → `Navigate("more")` + botón "Atrás" → `NavigateBack`.
- `more`: texto "Más" + botón "Atrás" → `NavigateBack`.
- `defaultScreenRegistry()` registra `home`, `details`, `more`.

## public / internal (exponer lo necesario)
- **public:** `RenderNode`, `SduiActionHandler`, `LocalSduiActionHandler`, `ScreenSource`,
  `ScreenUiState`, `SduiScreen`, `SduiHost`, `NavBackStack`/`NavEntry`.
- **internal:** `SduiClient`, `SduiHttp`(+actuals), `KtorScreenSource`, `HttpErrorMapper`,
  `NavActionHandler`, `NodeProps`, `sduiLog`.

## Modelo de datos y estados
Contrato (+`NavigateBack`) reutilizado. `ScreenUiState` (app). `NavBackStack.entries` snapshot-state
(id estable). Sin persistencia.

## Riesgos y mitigaciones
- **Refactor amplio (mueve código de la 001)** → es mecánico (mover archivos entre paquetes + 2
  interfaces); la funcionalidad no cambia. Verificación por compilación + tests existentes adaptados.
- **Regla de dependencias** → revisable por imports (motor sin `io.ktor.*`); opcional reforzar con detekt.
- `NavigateBack` en raíz → `pop` no-op; ruta inexistente → 404 `ProblemDetail` mapeado a `Error` sin
  corromper la pila.

## Estrategia de verificación
- **Unit `:sdui-core`:** round-trip de `NavigateBack`; unknown type → `NoOpAction`.
- **Unit `:shared` (motor):** (si es testeable sin UI) — el grueso del motor se valida por compilación +
  smoke; `NodeProps` test si aplica.
- **Unit `:shared` (app):** `NavBackStack` (push/pop/raíz/ids distintos misma route); `NavActionHandler`
  (Navigate push, NavigateBack pop, raíz no-op, acción no soportada / lista vacía no alteran la pila).
- **Unit `:server`:** `GET /screen/details` y `/more` → 200 con su `screenId`; `home` sigue 200.
- **Regla de dependencias:** `grep` confirma que `dev/kuisd/sdui/` no importa `io.ktor.` ni `dev.kuisd.app`.
- **Smoke:** `:server:run` + `:desktopApp:run`: `home`→"Empezar"→`details`→"Ver más"→`more`; "‹ Atrás"
  sube hasta `home`; barra oculta en raíz.
- **Calidad:** `./gradlew :sdui-core:check :shared:assemble :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` en verde.
