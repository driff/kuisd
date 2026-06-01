# Diseño — Slice vertical SDUI (cliente ↔ servidor)

> Spec ID: 001 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Añadir en `:shared` una capa de cliente HTTP (Ktor) y un render recursivo mínimo de Compose.
`PlaceholderApp` deja de leer `SampleScreens.home()` localmente y pasa a montar un
`SduiScreen("home")` que **carga el envelope del `:server` y lo pinta**. La decodificación
reutiliza `DefaultSduiJson` de `:sdui-core` (no se añade Ktor ContentNegotiation: se hace
`bodyAsText()` + `decodeFromString`, más simple y 100 % alineado con el contrato). El motor HTTP
es `expect/actual` por plataforma.

## Arquitectura
Todo el código nuevo vive en `:shared` (que ya depende de `:sdui-core` y de Compose):

```
shared/src/
├── commonMain/kotlin/dev/kuisd/sdui/
│   ├── SduiClient.kt        # fetchScreen(id) -> SduiEnvelope   (HU-1)
│   ├── SduiHttp.kt          # expect fun sduiHttpClient(): HttpClient ; expect val defaultBaseUrl
│   ├── SduiScreen.kt        # @Composable estado Loading/Error/Content   (HU-3)
│   ├── RenderNode.kt        # mapeo SduiNode -> @Composable (recursivo)   (HU-2)
│   └── NodeProps.kt         # helpers para leer props del JsonObject
├── androidMain/kotlin/dev/kuisd/sdui/SduiHttp.android.kt   # OkHttp + 10.0.2.2
├── iosMain/kotlin/dev/kuisd/sdui/SduiHttp.ios.kt           # Darwin + localhost
└── desktopMain/kotlin/dev/kuisd/sdui/SduiHttp.desktop.kt   # CIO + localhost
```

`presentation/PlaceholderApp.kt` se reescribe para delegar en `SduiScreen("home")`.
Flujo: `SduiScreen` → `produceState` → `SduiClient.fetchScreen` → `HttpClient.get().bodyAsText()`
→ `DefaultSduiJson.decode` → `RenderNode(root)`.

## Componentes y contratos

### SduiHttp (expect/actual) — HU-4, R9
- **Ubicación:** `commonMain` + `androidMain`/`iosMain`/`desktopMain`.
```kotlin
// commonMain
expect fun sduiHttpClient(): HttpClient
expect val defaultBaseUrl: String

// commonMain — config compartida: timeouts (R9) para cumplir HU-1.3 (sin cuelgue)
internal fun HttpClientConfig<*>.sduiConfig() {
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 5_000
    }
}

// androidMain
actual fun sduiHttpClient(): HttpClient = HttpClient(OkHttp) { sduiConfig() }
actual val defaultBaseUrl: String = "http://10.0.2.2:8080"   // loopback del emulador

// iosMain
actual fun sduiHttpClient(): HttpClient = HttpClient(Darwin) { sduiConfig() }
actual val defaultBaseUrl: String = "http://localhost:8080"

// desktopMain
actual fun sduiHttpClient(): HttpClient = HttpClient(CIO) { sduiConfig() }
actual val defaultBaseUrl: String = "http://localhost:8080"
```
- **Decisión:** factory por plataforma vía `expect/actual` (patrón KMP estándar). Alternativa
  descartada: un único engine multiplataforma (no existe uno bueno para iOS distinto de Darwin).
- **R9 (timeouts):** `HttpTimeout` (incluido en `ktor-client-core`, sin dependencia extra) evita el
  cuelgue ante un servidor lento/caído; el timeout se propaga como excepción capturada por `SduiScreen`.

### SduiClient — HU-1
- **Ubicación:** `commonMain` · `dev.kuisd.sdui.SduiClient`.
```kotlin
class SduiClient(
    private val baseUrl: String = defaultBaseUrl,
    private val http: HttpClient = sduiHttpClient(),
) {
    suspend fun fetchScreen(screenId: String): SduiEnvelope {
        val body = http.get("$baseUrl/screen/$screenId").bodyAsText()
        return DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), body)
    }
}
```
- **Decisión:** `bodyAsText()` + `DefaultSduiJson` en vez de Ktor ContentNegotiation → una
  dependencia menos y serialización idéntica a la del contrato. Los errores (red/parse) se
  propagan como excepción y los captura `SduiScreen` (HU-1.3).

### Estado de UI — HU-3
- **Ubicación:** `commonMain` (junto a `SduiScreen`).
```kotlin
sealed interface SduiUiState {
    data object Loading : SduiUiState
    data class Error(val message: String) : SduiUiState
    data class Content(val envelope: SduiEnvelope) : SduiUiState
}
```

### SduiScreen — HU-3
- **Ubicación:** `commonMain` · `@Composable`.
```kotlin
@Composable
fun SduiScreen(
    screenId: String,
    client: SduiClient = remember { SduiClient() },
)
```
- **Comportamiento:** `produceState<SduiUiState>(Loading, screenId) { value = runCatching{…}… }`.
  `Loading` → `CircularProgressIndicator`; `Error` → `Text`; `Content` → `RenderNode(env.root)`.

### RenderNode — HU-2
- **Ubicación:** `commonMain` · `@Composable fun RenderNode(node: SduiNode)`.
- **Mapeo:** `when (node.type)`:
  - `"column"` → `Column { node.children.forEach { RenderNode(it) } }`
  - `"row"` → `Row { … }`
  - `"text"` → `Text(node.stringProp("text").orEmpty())`
  - `"button"` → `Button(onClick = { /* HU-2.3: log de node.actions["onClick"] */ }) { Text(node.stringProp("label").orEmpty()) }`
  - `else` → `UnknownNode(node.type)` (HU-2.4): un `Text("⚠︎ componente no soportado: <type>")`.
- **Decisión:** mapeo `when` directo (no registry todavía); el registry extensible llega con
  `:sdui-compose` (fuera de alcance). Render recursivo simple.

### NodeProps — HU-2.2
- **Ubicación:** `commonMain` · helpers de extensión.
```kotlin
fun SduiNode.stringProp(key: String): String? =
    (props[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
```

## Modelo de datos y estados
Reutiliza los tipos de `:sdui-core` (`SduiEnvelope`, `SduiNode`, `UiAction`). Estado de pantalla
local: `SduiUiState` (arriba). Sin persistencia.

## Dependencias nuevas (catálogo de versiones)
Ya existen en `gradle/libs.versions.toml`; solo se referencian en `shared/build.gradle.kts`:

| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| `ktor-client-core` | 3.5.0 | commonMain | cliente HTTP |
| `kotlinx-coroutines-core` | 1.11.0 | commonMain | `suspend` + estado |
| `ktor-client-okhttp` | 3.5.0 | androidMain | engine Android |
| `ktor-client-darwin` | 3.5.0 | iosMain | engine iOS |
| `ktor-client-cio` | 3.5.0 | desktopMain | engine Desktop |

`shared` ya recibe `kotlinx-serialization-json` transitivamente (`api` de `:sdui-core`).
No se añade plugin de serialización a `shared` (los serializers ya están compilados en `:sdui-core`).

Android (build debug): habilitar tráfico en claro hacia el host de desarrollo
(`android:usesCleartextTraffic="true"` en `androidApp/AndroidManifest.xml`) — HU-4.3.

## Riesgos y mitigaciones
- **Cleartext bloqueado en Android** (HTTP a 10.0.2.2) → `usesCleartextTraffic=true` (debug).
- **`localhost` en iOS simulador** resuelve al host del Mac → OK para dev; documentarlo.
- **Bloqueo del hilo / cancelación** → usar `produceState` (cancelación atada a la composición).
- **`props` con tipos inesperados** → helper `stringProp` devuelve `null` y la UI usa `orEmpty()`.

## Estrategia de verificación
- **Unit (commonTest de `:shared`):** `RenderNode`/props puro no necesita servidor; test del
  helper `stringProp` y un test de `SduiUiState`. (Render composable se valida en smoke.)
- **Smoke manual end-to-end:**
  1. `./gradlew :server:run` (en una terminal).
  2. `./gradlew :desktopApp:run` → la ventana muestra el árbol de `home` (texto "Bienvenido a
     kuisd" + botón "Empezar") servido por el BFF.
  3. `./gradlew :androidApp:assembleDebug` y, opcional, instalar en emulador para ver el flujo.
- **Calidad:** `./gradlew :shared:assemble detekt ktlintCheck` en verde.
