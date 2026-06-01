# Fundamentos SDUI — Ktor BFF + KMP

Blueprint de fundaciones. La pieza clave es un módulo de contrato `:sdui-contract` en `commonMain` que **el servidor Ktor y el cliente KMP comparten**: una sola jerarquía `sealed` de `@Serializable`, sin codegen, sin drift de esquema, todo verificado por el compilador de punta a punta.

---

## 0. Decisiones base (fijadas)

| Pieza | Elección |
|---|---|
| Frontend | KMP (Android + iOS) con Compose Multiplatform |
| Backend | Ktor BFF (JVM) |
| Contrato | Módulo `commonMain` compartido, `sealed interface UiNode` con kotlinx.serialization |
| Serialización | kotlinx.serialization JSON, discriminador `type` |
| Estilo | Tokens de diseño (nombres), resueltos en cliente contra `MaterialTheme` — no hex crudo |
| Composición de layout | El servidor define orden/anidamiento/acciones; el cliente posee el render y el estilo |
| Acciones | Segunda jerarquía `sealed UiAction`, despachadas por un único `dispatch` lambda |
| Resiliencia | `UnknownNode` por defecto (nunca crashea), versionado aditivo, capability negotiation |
| DI | Koin (común a server y cliente) |
| Deploy | Docker → GHCR → Fly.io, CI con GitHub Actions |

**Versiones de referencia** (verificar en Maven Central antes de fijar): Kotlin 2.x, Ktor 3.5.0, kotlinx.serialization 1.8.1, Compose Multiplatform 1.11.0, kotlinx.coroutines, Koin 4.x, Coil 3 (imágenes KMP), Turbine + kotlin.test (tests).

---

## 1. Estructura del monorepo (Gradle multi-módulo)

```
sdui/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml          # version catalog
│
├── sdui-contract/                  # ← CORAZÓN. KMP library, sin Android/iOS app
│   └── src/commonMain/kotlin/.../contract/
│       ├── UiNode.kt               # sealed interface + nodos
│       ├── UiAction.kt             # sealed interface + acciones
│       ├── UiModifier.kt           # estilo/layout serializable
│       ├── DesignTokens.kt         # enums/constantes de tokens
│       ├── SduiEnvelope.kt         # respuesta raíz (version, root, meta)
│       └── SduiJson.kt             # config Json compartida + SerializersModule
│
├── server/                         # ← Ktor BFF (JVM)
│   └── src/main/kotlin/.../
│       ├── Application.kt          # entrypoint, plugins
│       ├── plugins/                # Serialization, StatusPages, CORS, CallLogging
│       ├── routing/                # /screen/{id}, /action
│       ├── dsl/                    # builders del árbol (column{}, row{}, text()...)
│       ├── screens/                # ensamblado por pantalla (HomeScreen, etc.)
│       ├── actions/                # handlers de acciones que vuelven del cliente
│       ├── capability/             # negociación de versión/componentes
│       └── di/                     # módulos Koin
│   ├── src/test/kotlin/            # testApplication + fakes
│   ├── Dockerfile
│   └── fly.toml
│
└── composeApp/                     # ← Cliente KMP
    └── src/
        ├── commonMain/kotlin/.../
        │   ├── sdui/render/        # Renderer recursivo (Render), Registry
        │   ├── sdui/modifier/      # UiModifier.toCompose()
        │   ├── sdui/dispatch/      # ActionDispatcher (interface)
        │   ├── sdui/theme/         # resolución de tokens
        │   ├── sdui/net/           # SduiClient (Ktor client) + repo
        │   └── App.kt
        ├── androidMain/kotlin/.../ # engine OkHttp, dispatcher impl, MainActivity
        └── iosMain/kotlin/.../     # engine Darwin, dispatcher impl, entrypoint
```

Genera el esqueleto desde **kmp.jetbrains.com / kmp.new** (targets Android + iOS + Server) y agrega `:sdui-contract` como módulo library puro (sin app targets, solo `commonMain` + opcionalmente `jvmMain` si necesitas algo JVM-only del lado server).

`settings.gradle.kts`:
```kotlin
rootProject.name = "sdui"
include(":sdui-contract", ":server", ":composeApp")
```

Dependencias entre módulos:
- `:server` → `implementation(project(":sdui-contract"))`
- `:composeApp` (commonMain) → `implementation(project(":sdui-contract"))`

---

## 2. Stack de dependencias (`libs.versions.toml`)

```toml
[versions]
kotlin = "2.x"                 # fijar última estable
ktor = "3.5.0"
serialization = "1.8.1"
coroutines = "1.x"
composeMultiplatform = "1.11.0"
koin = "4.x"
coil = "3.x"
logback = "1.5.x"
turbine = "1.x"

[libraries]
# --- contrato ---
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-core    = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

# --- server (Ktor) ---
ktor-server-core            = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
ktor-server-netty           = { module = "io.ktor:ktor-server-netty", version.ref = "ktor" }
ktor-server-content-neg     = { module = "io.ktor:ktor-server-content-negotiation", version.ref = "ktor" }
ktor-serialization-json     = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-server-status-pages    = { module = "io.ktor:ktor-server-status-pages", version.ref = "ktor" }
ktor-server-cors            = { module = "io.ktor:ktor-server-cors", version.ref = "ktor" }
ktor-server-call-logging    = { module = "io.ktor:ktor-server-call-logging", version.ref = "ktor" }
ktor-server-test-host       = { module = "io.ktor:ktor-server-test-host", version.ref = "ktor" }
logback-classic             = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }
koin-ktor                   = { module = "io.insert-koin:koin-ktor", version.ref = "koin" }

# --- cliente (Ktor client) ---
ktor-client-core            = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-neg     = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-okhttp          = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }   # androidMain
ktor-client-darwin          = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }   # iosMain
ktor-client-logging         = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
coil-compose                = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor           = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
koin-core                   = { module = "io.insert-koin:koin-core", version.ref = "koin" }

# --- test ---
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

---

## 3. Módulo `:sdui-contract` — el corazón

Todo aquí vive en `commonMain` y lo importan **ambos** lados.

`SduiJson.kt` — la **misma** config Json en server y cliente:
```kotlin
val SduiJson = Json {
    classDiscriminator = "type"
    encodeDefaults = false        // no inflar el payload con defaults
    ignoreUnknownKeys = true      // forward-compat en el cliente
    explicitNulls = false
    serializersModule = SerializersModule {
        // fallback: type desconocido -> UnknownNode (nunca crashea)
        polymorphicDefaultDeserializer(UiNode::class) { UnknownNode.serializer() }
        polymorphicDefaultDeserializer(UiAction::class) { NoOpAction.serializer() }
    }
}
```

`SduiEnvelope.kt` — respuesta raíz con metadatos de versión:
```kotlin
@Serializable
data class SduiEnvelope(
    val schemaVersion: Int,           // versión del esquema que produjo el server
    val screenId: String,
    val root: UiNode,                 // árbol completo
    val meta: Map<String, String> = emptyMap()
)
```

`UiNode.kt` — el árbol sellado:
```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed interface UiNode

@Serializable @SerialName("column")
data class ColumnNode(
    val modifier: UiModifier = UiModifier(),
    val spacing: Int = 0,
    val children: List<UiNode> = emptyList()
) : UiNode

@Serializable @SerialName("row")
data class RowNode(
    val modifier: UiModifier = UiModifier(),
    val spacing: Int = 0,
    val children: List<UiNode> = emptyList()
) : UiNode

@Serializable @SerialName("box")
data class BoxNode(
    val modifier: UiModifier = UiModifier(),
    val children: List<UiNode> = emptyList()
) : UiNode

@Serializable @SerialName("lazyColumn")
data class LazyColumnNode(
    val modifier: UiModifier = UiModifier(),
    val itemSpacing: Int = 0,
    val children: List<UiNode> = emptyList()
) : UiNode

@Serializable @SerialName("text")
data class TextNode(
    val text: String,
    val style: String = "type.body",     // token tipográfico
    val color: String? = null,           // token de color, opcional
    val modifier: UiModifier = UiModifier()
) : UiNode

@Serializable @SerialName("image")
data class ImageNode(
    val url: String,
    val contentDescription: String? = null,
    val modifier: UiModifier = UiModifier()
) : UiNode

@Serializable @SerialName("button")
data class ButtonNode(
    val onClick: List<UiAction> = emptyList(),
    val modifier: UiModifier = UiModifier(),
    val children: List<UiNode> = emptyList()
) : UiNode

@Serializable @SerialName("spacer")
data class SpacerNode(val size: Int = 8) : UiNode

@Serializable @SerialName("unknown")
data object UnknownNode : UiNode         // destino del fallback
```

`UiAction.kt` — acciones de vuelta:
```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed interface UiAction

@Serializable @SerialName("navigate")
data class Navigate(val route: String, val args: Map<String, String> = emptyMap()) : UiAction

@Serializable @SerialName("deeplink")
data class DeepLink(val url: String) : UiAction

@Serializable @SerialName("submit")
data class Submit(val endpoint: String, val payload: Map<String, String> = emptyMap()) : UiAction

@Serializable @SerialName("track")
data class Track(val event: String, val props: Map<String, String> = emptyMap()) : UiAction

@Serializable @SerialName("noop")
data object NoOpAction : UiAction        // destino del fallback
```

`UiModifier.kt` — estilo/layout serializable que mapea a `Modifier` de Compose:
```kotlin
@Serializable
data class UiModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val padding: Padding? = null,
    val background: String? = null,   // token de color o #hex
    val cornerRadius: Int? = null,
    val weight: Float? = null,
    val alignment: String? = null      // "center", "start", "end"...
)

@Serializable
data class Padding(val l: Int = 0, val t: Int = 0, val r: Int = 0, val b: Int = 0)
```

`DesignTokens.kt` — catálogo cerrado de tokens (nombres que el cliente resuelve):
```kotlin
object ColorTokens { const val PRIMARY = "color.primary"; const val SURFACE = "color.surface"; /* ... */ }
object TypeTokens  { const val TITLE = "type.title"; const val BODY = "type.body"; const val BUTTON = "type.button" }
object SpaceTokens { const val SM = 8; const val MD = 16; const val LG = 24 }
```

> **Gotcha clave:** serializa siempre por el tipo base (`SduiJson.encodeToString<UiNode>(node)` / `SduiEnvelope`), nunca por el subtipo concreto, o se pierde el discriminador `type`.

---

## 4. Servidor (`:server` — Ktor BFF)

**Plugins a instalar** (`Application.kt`): `ContentNegotiation` con `SduiJson`, `StatusPages` (errores → respuesta degradada, nunca 500 desnudo), `CallLogging`, `CORS` (si hubiera web), Koin.

```kotlin
fun Application.module() {
    install(ContentNegotiation) { json(SduiJson) }
    install(StatusPages) { /* exception -> envelope de error */ }
    install(CallLogging)
    install(Koin) { modules(serverModule) }
    configureRouting()
}
```

**DSL de ensamblado** (`dsl/`) — construye el `UiNode` de forma legible:
```kotlin
fun column(modifier: UiModifier = UiModifier(), spacing: Int = 0, block: NodeScope.() -> Unit) =
    ColumnNode(modifier, spacing, NodeScope().apply(block).build())

class NodeScope {
    private val nodes = mutableListOf<UiNode>()
    fun text(t: String, style: String = TypeTokens.BODY) { nodes += TextNode(t, style) }
    fun image(url: String) { nodes += ImageNode(url) }
    fun button(label: String, onClick: List<UiAction>) {
        nodes += ButtonNode(onClick, children = listOf(TextNode(label, TypeTokens.BUTTON)))
    }
    fun add(node: UiNode) { nodes += node }
    fun build(): List<UiNode> = nodes
}
```

**Ensamblado por pantalla** (`screens/`) — personalización, flags y experimentos son simples ramas:
```kotlin
fun homeScreen(user: User): SduiEnvelope = SduiEnvelope(
    schemaVersion = CURRENT_SCHEMA,
    screenId = "home",
    root = column(UiModifier(fillMaxWidth = true, padding = Padding(16,16,16,16)), spacing = 12) {
        text("Hola, ${user.name}", TypeTokens.TITLE)
        if (user.hasPromo) add(promoBanner(user))
        button("Ver resumen", onClick = listOf(Navigate("summary")))
    }
)
```

**Routing** (`routing/`):
```kotlin
fun Application.configureRouting() = routing {
    get("/screen/{id}") {
        val clientVersion = call.request.headers["X-SDUI-Version"]?.toIntOrNull() ?: 1
        val user = resolveUser(call)              // auth/context
        val envelope = ScreenRegistry.build(call.parameters["id"]!!, user, clientVersion)
        call.respond(envelope)
    }
    post("/action") {
        val action = call.receive<UiAction>()     // acción que vuelve del cliente
        val next = ActionRouter.handle(action, resolveUser(call))  // -> nuevo árbol o patch
        call.respond(next)
    }
}
```

**Capability negotiation** (`capability/`): el server lee `X-SDUI-Version` (+ opcionalmente lista de componentes soportados) y solo emite nodos que ese cliente sabe renderizar, sustituyendo por fallbacks si no. El **servidor es dueño del versionado**.

**DI Koin** (`di/`): expón repos/servicios de datos y handlers de acciones como singletons, inyéctalos en los ensambladores de pantalla.

---

## 5. Cliente (`:composeApp` — KMP)

**Ktor client** (`sdui/net/`) — engine por plataforma vía source set:
```kotlin
// commonMain
fun sduiHttpClient(engine: HttpClientEngine) = HttpClient(engine) {
    install(ContentNegotiation) { json(SduiJson) }
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    install(Logging)
    defaultRequest { header("X-SDUI-Version", CLIENT_SCHEMA.toString()) }
}
// androidMain: OkHttp(...)   |   iosMain: Darwin(...)

class SduiClient(private val http: HttpClient, private val baseUrl: String) {
    suspend fun screen(id: String): SduiEnvelope = http.get("$baseUrl/screen/$id").body()
    suspend fun dispatch(action: UiAction): SduiEnvelope = http.post("$baseUrl/action") {
        contentType(ContentType.Application.Json); setBody(action)
    }.body()
}
```

**Renderer recursivo** (`sdui/render/`) — `when` sellado, exhaustivo, en `commonMain`:
```kotlin
@Composable
fun Render(node: UiNode, dispatch: ActionDispatcher) {
    when (node) {
        is ColumnNode -> Column(node.modifier.toCompose(),
            verticalArrangement = Arrangement.spacedBy(node.spacing.dp)) {
            node.children.forEach { Render(it, dispatch) }
        }
        is RowNode -> Row(node.modifier.toCompose(),
            horizontalArrangement = Arrangement.spacedBy(node.spacing.dp)) {
            node.children.forEach { Render(it, dispatch) }
        }
        is BoxNode -> Box(node.modifier.toCompose()) { node.children.forEach { Render(it, dispatch) } }
        is LazyColumnNode -> LazyColumn(node.modifier.toCompose()) {
            items(node.children) { Render(it, dispatch) }
        }
        is TextNode -> Text(node.text, style = node.style.resolveTypography(), color = node.color.resolveColor())
        is ImageNode -> AsyncImage(node.url, node.contentDescription, node.modifier.toCompose())
        is ButtonNode -> Button(onClick = { node.onClick.forEach(dispatch::dispatch) },
            modifier = node.modifier.toCompose()) { node.children.forEach { Render(it, dispatch) } }
        is SpacerNode -> Spacer(Modifier.size(node.size.dp))
        is UnknownNode -> { /* nada en prod; placeholder en debug */ }
    }
}
```

**Mapper de modifier** (`sdui/modifier/`):
```kotlin
@Composable
fun UiModifier.toCompose(): Modifier {
    var m: Modifier = Modifier
    if (fillMaxWidth) m = m.fillMaxWidth()
    if (fillMaxHeight) m = m.fillMaxHeight()
    width?.let { m = m.width(it.dp) }
    height?.let { m = m.height(it.dp) }
    background?.let { m = m.background(it.resolveColor() ?: Color.Unspecified) }
    cornerRadius?.let { m = m.clip(RoundedCornerShape(it.dp)) }
    padding?.let { m = m.padding(it.l.dp, it.t.dp, it.r.dp, it.b.dp) }
    return m   // ojo: el ORDEN importa (padding vs background vs clip)
}
```

**Dispatcher** (`sdui/dispatch/`) — único punto de efectos secundarios:
```kotlin
interface ActionDispatcher { fun dispatch(action: UiAction) }
// impl: navigate -> NavController, track -> analytics, submit/network -> SduiClient.dispatch() -> nuevo árbol
```

**Theming/tokens** (`sdui/theme/`): `String.resolveTypography()` / `String?.resolveColor()` mapean nombres de token al `MaterialTheme` activo. Así dark mode, dynamic color y rebrand quedan en el cliente.

**Registry opcional**: si quieres extensibilidad en runtime sin tocar el `when`, mantén un `Map<String, @Composable (UiNode, ActionDispatcher) -> Unit>`. Para empezar, el `when` sellado (exhaustividad del compilador) es más simple.

**Estado/perf**: DTOs inmutables (`data class`/`val`) para que Compose pueda saltar recomposición; `LazyColumn` para listas largas; estado mutable de interacción hoisteado a un `ViewModel`/state holder por id de nodo.

---

## 6. Versionado & capability negotiation

- **Solo aditivo**: nuevas props opcionales y nuevos componentes *hoja* no rompen; renombrar/cambiar semántica sí → bump de `schemaVersion`.
- **Fallback de desconocido**: `UnknownNode` renderiza nada. El `ignoreUnknownKeys = true` cubre props nuevas; el `polymorphicDefaultDeserializer` cubre `type` nuevos.
- **Header de capacidad**: cliente manda `X-SDUI-Version` (y opcionalmente set de componentes); server adapta.
- **Kill-switch**: como el layout es data, un feature flag deja de emitir un componente al instante, sin release de app.

---

## 7. Testing

**Server** (`testApplication`, fakes sobre mocks):
```kotlin
@Test fun homeScreenRendersPromoForPromoUser() = testApplication {
    application { module() }                    // con fakes inyectados por Koin
    val res = client.get("/screen/home") { header("X-SDUI-Version", "1") }
    val env = SduiJson.decodeFromString<SduiEnvelope>(res.bodyAsText())
    assertTrue(env.root is ColumnNode)
}
```
- Inyecta repos *fake* (in-memory) implementando los ports — sin mocks.
- Integración real (Postgres) más adelante con Testcontainers + migraciones.

**Contrato**: al compartir `:sdui-contract`, el contrato server↔cliente está garantizado en **tiempo de compilación**. Tests de round-trip de (de)serialización por nodo para detectar regresiones de `@SerialName`.

**Cliente**: snapshot/screenshot tests enumerando payloads representativos (un JSON por escenario); el renderer es Kotlin puro en `commonMain`, testeable sin device.

> Tus skills existentes aplican cuando generemos el código real: `http-layer` para `SduiClient`/repo, `kmp-testing` para los tests del cliente (fakes en el DataSource), `compose-screen` para la pantalla contenedora que llama al renderer.

---

## 8. Deploy & CI/CD

**Dockerfile** (`server/`, multi-stage → shadow JAR):
```dockerfile
FROM gradle:jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :server:shadowJar --no-daemon

FROM eclipse-temurin:21-jre
COPY --from=build /app/server/build/libs/*-all.jar /app/server.jar
EXPOSE 8080
CMD ["java","-jar","/app/server.jar"]
```

**Fly.io**: `fly launch` genera `fly.toml`; CI con `superfly/flyctl-actions/setup-flyctl@master` + `flyctl deploy`, `FLY_API_TOKEN` como secret. Imágenes a **GHCR** (`docker/build-push-action`) y Fly las pulla, o build remoto en Fly.

**GitHub Actions** (pipeline): `lint → test (fakes) → build shadowJar/imagen → deploy`. Tests de integración (si los hay) en runner con Docker.

---

## 9. Orden de implementación (checklist)

1. [ ] Generar esqueleto KMP (Android+iOS+Server) y crear módulo `:sdui-contract`.
2. [ ] `libs.versions.toml` + wiring de dependencias entre módulos.
3. [ ] **Contrato**: `SduiJson`, `SduiEnvelope`, `UiNode` (Column/Row/Box/LazyColumn/Text/Image/Button/Spacer/Unknown), `UiAction`, `UiModifier`, `DesignTokens`. Tests de round-trip de serialización.
4. [ ] **Server mínimo**: plugins + `GET /screen/{id}` devolviendo un `homeScreen` hardcodeado con el DSL. `testApplication` verde.
5. [ ] **Cliente mínimo**: `SduiClient`, `Render`, `toCompose()`, `resolveTypography/Color`, dispatcher que solo navega/trackea. Pantalla contenedora que pide `/screen/home` y renderiza el árbol. Verificar en **Android e iOS**.
6. [ ] **Acciones de ida y vuelta**: `POST /action` + `ActionRouter`; el dispatcher hace submit/network y reemplaza el árbol.
7. [ ] **Resiliencia**: confirmar fallback de `UnknownNode`, error boundaries por componente, `StatusPages` con envelope de error.
8. [ ] **Capability negotiation**: header `X-SDUI-Version`, gating en server, kill-switch por flag.
9. [ ] **Deploy**: Dockerfile + Fly.io + GitHub Actions a GHCR.
10. [ ] Recién entonces ampliar el catálogo de componentes **según reuso real** (no precargar la librería entera).

**Regla de oro**: construye el *mecanismo* de layout libre (árbol recursivo, anidamiento arbitrario) pero **gobiérnalo** con catálogo curado de componentes + estilo solo por tokens + composición/orden/acciones del lado server. Eso te da la flexibilidad sin reimplementar un navegador.
