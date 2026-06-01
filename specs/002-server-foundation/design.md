# Diseño — Endurecimiento de la fundación del servidor

> Spec ID: 002 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Refactor de `:server` a una estructura por responsabilidades con un `ScreenRegistry`, más un
modelo de error compartido en `:sdui-core` y tres plugins de higiene. Se mantiene
`embeddedServer(Netty)` (la migración a `EngineMain`/HOCON es R4, fuera de alcance); la
configuración (CORS, modo dev) se lee de variables de entorno para no bloquear en HOCON.

## Arquitectura

### `:sdui-core` (contrato)
- **Quitar** `SampleScreens.kt` (R1 / HU-1).
- **Añadir** `ProblemDetail.kt` (R2 / HU-2.1).
- `commonTest`: `SduiContractTest` deja de usar `SampleScreens`; construye su `SduiEnvelope`
  inline (helper privado `sampleEnvelope()` dentro del test).

### `:server` — nueva estructura (R5 / HU-4)
```
server/src/main/kotlin/dev/kuisd/server/
├── Application.kt                 # main() + module(): solo wiring (instala plugins + routing)
├── plugins/
│   ├── Serialization.kt          # install(ContentNegotiation){ json(DefaultSduiJson) }
│   ├── Monitoring.kt             # CallLogging + CallId                       (HU-6.3)
│   ├── Http.kt                   # CORS (restringido) + Compression + DefaultHeaders (HU-3, HU-6)
│   └── ErrorHandling.kt          # StatusPages -> ProblemDetail               (HU-2)
├── routing/
│   ├── HealthRoutes.kt           # GET /health
│   └── ScreenRoutes.kt           # GET /screen/{id} (lee capability header)   (HU-4.3, HU-5.2)
└── screens/
    ├── ScreenContext.kt          # contexto pasado al builder
    ├── ScreenRegistry.kt         # Map<String, ScreenBuilder>                 (HU-4.2)
    └── HomeScreen.kt             # builder de "home" (migrado de SampleScreens) (HU-1.1)
```

### `:shared` (mantener compilando entre 002 y 001)
- `PlaceholderApp.kt` vuelve al placeholder simple (texto "Hello, kuisd") **sin** referenciar
  `SampleScreens` (HU-1.3). La spec 001 lo reemplazará por `SduiScreen("home")`.

## Componentes y contratos

### ProblemDetail (`:sdui-core`) — HU-2.1
```kotlin
@Serializable
data class ProblemDetail(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,   // p.ej. el callId
)
```

### ScreenContext + ScreenRegistry + ScreenBuilder — HU-4, HU-5
```kotlin
// screens/ScreenContext.kt
data class ClientCapability(val schemaVersion: Int?)          // HU-5
data class ScreenContext(val screenId: String, val capability: ClientCapability)

// screens/ScreenRegistry.kt
fun interface ScreenBuilder { suspend fun build(ctx: ScreenContext): SduiEnvelope }

class ScreenRegistry(private val builders: Map<String, ScreenBuilder>) {
    fun builderFor(screenId: String): ScreenBuilder? = builders[screenId]
}

fun defaultScreenRegistry(): ScreenRegistry =
    ScreenRegistry(mapOf("home" to HomeScreen))
```
- **Decisión:** `ScreenContext`/`ClientCapability` se introducen ya (aunque no se ramifique) para
  que la firma del builder sea estable — es la forma del futuro `SduiConfig.screens` de `:sdui-ktor`.

### HomeScreen (`:server`) — HU-1.1
- Migra el árbol que hoy construye `SampleScreens.home()` (column → text "Bienvenido a kuisd" +
  button "Empezar" con `Navigate("details")`), como `object HomeScreen : ScreenBuilder`.

### ScreenRoutes — HU-4.3, HU-5.2, HU-2.3
```kotlin
const val KUISD_VERSION_HEADER = "X-Kuisd-Version"   // en :sdui-core junto al contrato

fun Route.screenRoutes(registry: ScreenRegistry) {
    get("/screen/{id}") {
        val id = call.parameters["id"].orEmpty()
        val capability = ClientCapability(
            schemaVersion = call.request.headers[KUISD_VERSION_HEADER]?.toIntOrNull(),
        )
        val builder = registry.builderFor(id)
            ?: throw ScreenNotFoundException(id)        // -> 404 ProblemDetail (HU-2.3)
        call.respond(builder.build(ScreenContext(id, capability)))
    }
}
```

### ErrorHandling (StatusPages) — HU-2.2, HU-2.4
```kotlin
class ScreenNotFoundException(val screenId: String) : RuntimeException("Unknown screen: $screenId")

fun Application.configureErrorHandling(isDev: Boolean) {
    install(StatusPages) {
        exception<ScreenNotFoundException> { call, cause ->
            call.respondProblem(HttpStatusCode.NotFound, "Screen not found", cause.message, call.callId)
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error (callId=${call.callId})", cause)
            call.respondProblem(
                status = HttpStatusCode.InternalServerError,
                title = "Internal Server Error",
                detail = if (isDev) cause.message else null,   // no fuga en prod (HU-2.2)
                instance = call.callId,
            )
        }
    }
}
// respondProblem: respond(status, ProblemDetail(...)) con ContentType application/problem+json
```
- `isDev` se lee de `System.getenv("KUISD_DEV") == "true"`.

### Http (CORS + Compression + DefaultHeaders) — HU-3, HU-6
```kotlin
fun Application.configureHttp() {
    install(DefaultHeaders)
    install(Compression)   // gzip por defecto
    install(CORS) {
        // deny-by-default: solo orígenes de KUISD_CORS_HOSTS (csv). Vacío => sin allowHost.
        System.getenv("KUISD_CORS_HOSTS").orEmpty()
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { allowHost(it, schemes = listOf("http", "https")) }
        allowHeader(KUISD_VERSION_HEADER)
    }
}
```

### Monitoring (CallLogging + CallId) — HU-6.3
```kotlin
fun Application.configureMonitoring() {
    install(CallId) { generate { java.util.UUID.randomUUID().toString() }; header(HttpHeaders.XRequestId) }
    install(CallLogging) { callIdMdc("callId") }
}
```

## Modelo de datos y estados
Nuevos tipos: `ProblemDetail` (contrato), `ClientCapability`, `ScreenContext`, `ScreenBuilder`,
`ScreenNotFoundException`. Sin estado mutable; `:server` permanece stateless.

## Dependencias nuevas (catálogo de versiones)
Añadir a `gradle/libs.versions.toml` (mismo `ktor` 3.5.0) y a `server/build.gradle.kts`:

| Librería | Artefacto | Source set |
|----------|-----------|------------|
| Call ID | `io.ktor:ktor-server-call-id` | server main |
| Compression | `io.ktor:ktor-server-compression` | server main |
| Default headers | `io.ktor:ktor-server-default-headers` | server main |

`call-logging`, `status-pages`, `cors`, `content-negotiation`, `netty` ya están.

## Riesgos y mitigaciones
- **Romper `:sdui-core` test y `:shared` al quitar `SampleScreens`** → mismo PR: actualizar
  `SduiContractTest` (envelope inline) y revertir `PlaceholderApp` al placeholder simple. (HU-1.2/1.3)
- **`application/problem+json` y `respond` de tipos distintos** → helper `respondProblem` fija el
  ContentType explícitamente.
- **CORS vacío rompe un cliente web** → no hay cliente web hoy; documentar `KUISD_CORS_HOSTS`.

## Estrategia de verificación
- **Unit (`:sdui-core` commonTest):** round-trip sigue verde con envelope inline; nuevo test de
  round-trip de `ProblemDetail`.
- **Unit (`:server` testApplication):**
  - `GET /health` → 200 "OK".
  - `GET /screen/home` → 200, decodifica `SduiEnvelope` (screenId "home").
  - `GET /screen/missing` → 404 con `ProblemDetail` (`application/problem+json`).
  - Respuesta incluye header de correlación (`X-Request-Id`).
  - (Opcional) enviar `X-Kuisd-Version: 1` no rompe la respuesta.
- **Calidad:** `./gradlew :sdui-core:check :server:build :shared:assemble detekt ktlintCheck` verde.
