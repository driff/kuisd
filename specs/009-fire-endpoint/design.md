# Diseño — `FireEndpoint`: acciones con red (loading/success/error)

> Spec ID: 009 · Estado: approved · Trazabilidad: ./requirements.md

> **Decisiones cerradas (T0):** respuesta = `ActionResponse(actions, message)`; `Track` = log;
> demo = formulario `greet`. "Arrancamos con esto y mejoramos después" (acordado).

## Enfoque
`FireEndpoint` es una **acción** más: el motor la despacha por `LocalSduiActionHandler` igual que
`Navigate` o `SetVar`. Toda la red vive en la app. Un nuevo `FireEndpointActionHandler` (SubHandler
de 005):

1. Escribe `statusVar = "loading"` **síncrono** (feedback inmediato, mismo frame).
2. Lanza la petición en un `CoroutineScope` ligado a la entrada del back stack
   (`rememberCoroutineScope()` dentro del bloque `key(current.id)` del host) → se cancela al salir.
3. Construye el cuerpo `{ var: valor }` leyendo el `VariableStore`, llama a un seam `ActionEndpoint`
   (DIP; impl Ktor en `data`), y al volver escribe `status`/`result` y **re-despacha** las acciones
   resultantes (`onSuccess`/`onError` + `response.actions`) por el mismo `AppActionHandler`.

El ciclo "handler → AppActionHandler → handler" se rompe con una propiedad `dispatch` inyectada por
el host tras construir el compuesto. El estado reusa el `VariableStore` (005); el dispatch del
resultado reusa el `AppActionHandler` (005). El motor **no cambia**.

`Track` gana un `TrackActionHandler` mínimo que loguea (seam de analytics), eliminando la acción
muerta.

## Arquitectura (módulos afectados)
```
:sdui-core
   └── UiAction.kt          (cambia)  FireEndpoint +method/+statusVar/+resultVar/+onSuccess/+onError
                                       + ActionResponse + EndpointStatus  (NUEVOS)
   ▲
:sdui-compose               (SIN CAMBIOS — el motor ya despacha cualquier UiAction)
   ▲
:shared
   ├── app/data/ActionEndpoint.kt        (NUEVO)  fun interface (DIP) + ActionRequest
   ├── app/data/KtorActionEndpoint.kt    (NUEVO)  impl Ktor (POST/GET → ActionResponse)
   ├── app/data/SduiClient.kt            (cambia) + postAction/getAction
   ├── app/FireEndpointActionHandler.kt  (NUEVO)  SubHandler: loading→request→status/result+dispatch
   ├── app/TrackActionHandler.kt         (NUEVO)  SubHandler: loguea Track
   └── app/SduiHost.kt                   (cambia) crea scope+handlers por entrada; inyecta dispatch
server/.../
   ├── screens/FormScreen.kt             (NUEVO)  demo: textField + Enviar(FireEndpoint) + status/result
   ├── screens/HomeScreen.kt             (cambia) + button "Formulario" → Navigate("form")
   ├── screens/ScreenRegistry.kt         (cambia) + "form"
   ├── actions/ActionRouting.kt          (NUEVO)  POST /action/greet → ActionResponse
   └── Routing.kt / Application.kt       (cambia) monta el routing de acciones
```
Frontera 006 preservada: `:sdui-compose` sin cambios; nada de HTTP entra al motor.

## Contrato — `:sdui-core`

### `FireEndpoint` enriquecido + `ActionResponse` + `EndpointStatus` (HU-1)
```kotlin
@Serializable
@SerialName("network")
data class FireEndpoint(
    val endpoint: String,
    val method: String = "POST",                       // "GET" | "POST"
    val payloadVars: List<String> = emptyList(),       // var -> {name: valorActual} en el body
    val statusVar: String? = null,                     // app escribe idle/loading/success/error
    val resultVar: String? = null,                     // app escribe message (o error legible)
    val onSuccess: List<UiAction> = emptyList(),       // despachadas tras 2xx (antes de las del server)
    val onError: List<UiAction> = emptyList(),         // despachadas si falla
) : UiAction

/** Respuesta de un endpoint de acción: acciones a despachar + mensaje legible opcional. */
@Serializable
data class ActionResponse(
    val actions: List<UiAction> = emptyList(),
    val message: String? = null,
)

/** Literales compartidos del ciclo de estado (server y cliente). */
object EndpointStatus {
    const val IDLE = "idle"
    const val LOADING = "loading"
    const val SUCCESS = "success"
    const val ERROR = "error"
}
```
- **Wire compat:** los campos nuevos de `FireEndpoint` son opcionales con default; un payload viejo
  (`{endpoint, payloadVars}`) decodifica igual. `onSuccess`/`onError` son `List<UiAction>` — el
  serializer polimórfico ya soporta acciones anidadas (recursivo); kotlinx lo maneja.
- `ActionResponse` se serializa con el mismo `DefaultSduiJson` (classDiscriminator "type").

## App — `:shared`

### Seam de datos `ActionEndpoint` (DIP) — HU-3.4
```kotlin
// app/data/ActionEndpoint.kt  (internal)
/** Cuerpo de una petición de acción: pares var->valor actual del store. */
internal data class ActionRequest(
    val endpoint: String,
    val method: String,
    val payload: Map<String, JsonElement>,
)

/** Seam de salida (DIP): el handler depende de esto, no de Ktor. Testeable con un fake. */
internal fun interface ActionEndpoint {
    suspend fun fire(request: ActionRequest): ActionResponse
}
```
```kotlin
// app/data/KtorActionEndpoint.kt  (internal)
internal class KtorActionEndpoint(
    private val client: SduiClient,
) : ActionEndpoint {
    override suspend fun fire(request: ActionRequest): ActionResponse =
        client.postAction(request.endpoint, request.method, JsonObject(request.payload))
}
```
`SduiClient` gana `postAction(endpoint, method, body): ActionResponse` (POST con body JSON, o GET
ignorando body), reusando el `HttpClient`/config y `expectSuccess` de 002/005; un no-2xx lanza
`ResponseException` que el handler mapea con `HttpErrorMapper`.

### `FireEndpointActionHandler` (SubHandler) — HU-3
```kotlin
// app/FireEndpointActionHandler.kt  (internal)
internal class FireEndpointActionHandler(
    private val scope: CoroutineScope,
    private val endpoint: ActionEndpoint,
    private val store: VariableStore,
) : SubHandler {

    /**
     * Inyectado por el host TRAS construir el AppActionHandler, para re-despachar
     * `onSuccess`/`onError`/`response.actions`. Rompe el ciclo handler<->compuesto. Default no-op
     * para que el handler sea usable/testeable sin host.
     */
    var dispatch: (List<UiAction>) -> Unit = {}

    override fun supports(action: UiAction): Boolean = action is FireEndpoint

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        if (action !is FireEndpoint) return@forEach
        // 1) loading SÍNCRONO (mismo frame) antes de suspender.
        action.statusVar?.let { store.set(it, JsonPrimitive(EndpointStatus.LOADING)) }
        // 2) cuerpo desde el store (vars ausentes se omiten — HU-6.3).
        val payload = action.payloadVars
            .mapNotNull { name -> store.scope.get(name)?.let { name to it } }
            .toMap()
        // 3) petición en el scope de la entrada (se cancela al salir — HU-6.1).
        scope.launch {
            try {
                val res = endpoint.fire(ActionRequest(action.endpoint, action.method, payload))
                action.statusVar?.let { store.set(it, JsonPrimitive(EndpointStatus.SUCCESS)) }
                action.resultVar?.let { store.set(it, JsonPrimitive(res.message.orEmpty())) }
                dispatch(action.onSuccess + res.actions)
            } catch (cancellation: CancellationException) {
                throw cancellation                                   // respeta cancelación del scope
            } catch (error: Throwable) {
                action.statusVar?.let { store.set(it, JsonPrimitive(EndpointStatus.ERROR)) }
                action.resultVar?.let { store.set(it, JsonPrimitive(HttpErrorMapper.message(error))) }
                dispatch(action.onError)
            }
        }
    }
}
```
- `statusVar=loading` se escribe **fuera** del `launch` → recompone en el frame del click.
- El `catch (CancellationException)` re-lanza para no convertir una cancelación de scope en "error"
  visible (HU-6.1): si el usuario salió, el scope cancelado ni siquiera ejecuta los `set`.

### `TrackActionHandler` (SubHandler) — HU-4
```kotlin
// app/TrackActionHandler.kt  (internal)
internal class TrackActionHandler : SubHandler {
    override fun supports(action: UiAction): Boolean = action is Track
    override fun handle(actions: List<UiAction>) = actions.forEach { a ->
        if (a is Track) appLog("track: ${a.event} ${a.props}")    // seam: aquí iría el SDK real
    }
}
```

### `SduiHost` — scope + handlers por entrada, inyección de dispatch — HU-3, HU-6.1
Se reestructura para crear `store`, `scope` y los handlers **dentro** del bloque por-entrada, de
modo que el `CoroutineScope` se cancele al cambiar de pantalla:
```kotlin
val current = backStack.current
key(current.id) {
    val store = remember { VariableStore() }
    val scope = rememberCoroutineScope()                           // cancelado al salir de la entrada
    val actionEndpoint = remember { KtorActionEndpoint(sharedClient) }
    val fire = remember { FireEndpointActionHandler(scope, actionEndpoint, store) }
    val handler = remember {
        AppActionHandler(
            listOf(
                NavActionHandler(backStack),
                VariableActionHandler(store),
                TrackActionHandler(),
                fire,
            ),
        )
    }
    fire.dispatch = handler::handle                                // rompe el ciclo (idempotente)

    CompositionLocalProvider(
        LocalSduiActionHandler provides handler,
        LocalComponentRegistry provides appRegistry,
        LocalVariables provides store.scope,
        LocalKuisdTheme provides theme,
        LocalIconRegistry provides icons,
    ) {
        SduiScreen(screenId = current.route, source = source, store = store, modifier = ...)
    }
}
```
> El `sharedClient: SduiClient` se eleva a campo del host (un solo `HttpClient` reusado por
> `ScreenSource` y `ActionEndpoint`), creado con `remember` y cerrado en el `DisposableEffect`
> existente. Nota: el `topBar`/back queda fuera del `key` (usa solo `backStack`).

## Server — `:server`

### `POST /action/greet` → `ActionResponse` (HU-5.2/5.3)
```kotlin
// actions/ActionRouting.kt
fun Route.actionRoutes() {
    post("/action/greet") {
        val body = call.receive<JsonObject>()
        val name = (body["name"] as? JsonPrimitive)?.content.orEmpty().trim()
        if (name.isEmpty()) {
            // reutiliza el StatusPages/ProblemDetail de 002
            throw BadRequestException("name requerido")
        }
        call.respond(
            ActionResponse(
                message = "Hola, $name!",
                actions = listOf(SetVar("greeted", JsonPrimitive(true))),
            ),
        )
    }
}
```
Se monta en `Application.module()` junto al `screenRoutes()` existente. `ContentNegotiation` ya usa
`DefaultSduiJson`, así que `ActionResponse` (con acciones polimórficas) serializa con el mismo
discriminador "type".

### `FormScreen` (HU-5.1) y `HomeScreen` (HU-5.4)
`form`: `column` con `textField(bind="name")`, `text "$submitStatus"`, `text "$submitResult"`, y un
`button "Enviar"` con `actions["onClick"] = [FireEndpoint("/action/greet", "POST",
payloadVars=["name"], statusVar="submitStatus", resultVar="submitResult")]`. Variables sembradas:
`name=""`, `submitStatus=EndpointStatus.IDLE`, `submitResult=""`. `home` gana un `button`
"Formulario" → `Navigate("form")`.

## Modelo de datos y estados
- Contrato: `FireEndpoint` +5 campos opcionales, `ActionResponse`, `EndpointStatus`. Wire compat.
- Estado del ciclo: variables del `VariableStore` (`statusVar`/`resultVar`) — sin estructura nueva.
- Concurrencia: una corutina por disparo en el scope de la entrada; last-write-wins sobre
  `statusVar` si hay dos en vuelo (HU-6.4, documentado).

## Dependencias nuevas
Ninguna. `io.ktor.client.request.post`/`setBody` ya disponibles con el cliente de 002/005;
`rememberCoroutineScope`/`kotlinx.coroutines` ya transitivos.

## Riesgos y mitigaciones
- **Ciclo handler ↔ AppActionHandler.** Resuelto con `var dispatch` inyectado por el host tras
  construir el compuesto; default no-op (testeable aislado). Asignación idempotente por
  recomposición.
- **Scope y cancelación.** `rememberCoroutineScope()` dentro de `key(current.id)` ata el scope a la
  entrada del back stack → al navegar/volver se cancela y las peticiones en vuelo no escriben sobre
  un store ya no mostrado (HU-6.1). `CancellationException` se re-lanza, no se trata como error.
- **`statusVar=loading` y recomposición.** Se escribe síncrono antes del `launch` → el frame del
  click ya muestra "loading". El `success`/`error` llega en otra recomposición al volver la red.
- **Condición de carrera (doble disparo).** Last-write-wins sobre `statusVar`; sin dedupe. Si
  molesta, una spec futura añade un token de petición o deshabilita el botón vía variable.
- **Acciones recursivas en el wire.** `FireEndpoint.onSuccess: List<UiAction>` permite anidar otro
  `FireEndpoint`. kotlinx-serialization lo soporta; el riesgo de bucle infinito es del autor del
  envelope, no del motor (igual que un `Navigate` cíclico). Documentado.
- **Un solo `HttpClient`.** El host comparte un `SduiClient` entre `ScreenSource` y `ActionEndpoint`
  para no abrir dos engines; se cierra en el `DisposableEffect` del host.

## Estrategia de verificación
- **Unit `:sdui-core`:** `FireEndpoint` decodifica con y sin campos nuevos; `ActionResponse` con
  acciones polimórficas round-trips; `onSuccess` anida un `SetVar` y decodifica.
- **Unit `:shared` (sin red — fake `ActionEndpoint`):**
  - `FireEndpointActionHandler`: al manejar, `statusVar` pasa a `loading` síncrono; tras resolver el
    fake con éxito, `statusVar=success`, `resultVar=message`, y `dispatch` recibe
    `onSuccess + response.actions`. Con fake que lanza, `statusVar=error`, `resultVar=mensaje`,
    `dispatch` recibe `onError`. (Usar `runTest` + un scope de test.)
  - `payloadVars` con var ausente → se omite del `ActionRequest.payload`.
  - `TrackActionHandler.supports(Track)`; `AppActionHandler` ya no cae a "no soportada" para Track.
- **Unit `:server`:** `POST /action/greet {"name":"Ana"}` → 200 + `ActionResponse.message=="Hola,
  Ana!"` + un `SetVar("greeted", true)`; `{"name":""}` → 4xx ProblemDetail. `GET /screen/form` →
  200 con `textField(bind=name)` + `button` con `FireEndpoint` en onClick. `home` trae botón
  "Formulario".
- **Regla de dependencias:** `grep -rn "dev.kuisd.app\|io.ktor\." sdui-compose/src/` sigue vacío.
- **Smoke** (`:server:run` + `:desktopApp:run`): en `form`, teclear "Ana" → Enviar → status
  "loading" → "success", result "Hola, Ana!"; nombre vacío → "error" + mensaje; sin recargar.
- **Calidad:** `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble :shared:check
  :server:build :androidApp:assembleDebug detekt ktlintCheck` (tras `ktlintFormat`) — verde.

## public / internal
- **contrato (public):** `FireEndpoint` (campos nuevos), `ActionResponse`, `EndpointStatus`.
- **app (internal):** `ActionEndpoint`, `ActionRequest`, `KtorActionEndpoint`,
  `FireEndpointActionHandler`, `TrackActionHandler`, `SduiClient.postAction`.
- **server:** `actionRoutes()`, `FormScreen` (object).
- **motor:** sin cambios de superficie.

## Preguntas abiertas (a cerrar en aprobación)
1. **Respuesta = acciones (elegido) vs envelope completo vs SduiPatch.** El MVP usa
   `ActionResponse(actions, message)`: máxima reutilización (el `AppActionHandler` ya ejecuta
   acciones) y permite "recargar" vía `Navigate`. ¿OK, o quieres que el endpoint pueda devolver
   un `SduiEnvelope` para reemplazo total de pantalla?
2. **`Track` = log (elegido).** ¿Suficiente como seam, o quieres una interfaz `Analytics`
   inyectable desde el host ya en esta spec?
3. **Demo:** ¿formulario `greet` simple (elegido), o algo con más recorrido (p.ej. un POST que
   incremente un contador server-side y devuelva el nuevo valor por `SetVar`)?
