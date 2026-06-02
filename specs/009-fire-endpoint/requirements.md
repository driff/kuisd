# Requisitos — `FireEndpoint`: acciones con red (loading/success/error)

> Spec ID: 009 · Estado: approved · Fecha: 2026-06-01

## Resumen
Cerrar el lazo **cliente → servidor → cliente**: que una acción del usuario (p.ej. pulsar
"Enviar") dispare una petición HTTP al BFF, refleje el estado (`loading`/`success`/`error`) en la
UI por variables locales (spec 005/007), y aplique la respuesta del server como una **lista de
acciones** (`SetVar`, `Navigate`, etc.) que el `AppActionHandler` ya sabe ejecutar.

Hasta ahora el SDUI es de **una sola dirección de red**: el cliente hace `GET /screen/{id}` una vez
y luego todo es local (variables, navegación). Con `FireEndpoint` el cliente puede **escribir** al
server y reaccionar a su respuesta — formularios, mutaciones, recargas — sin que el motor sepa nada
de HTTP.

La acción `FireEndpoint` **ya existe en el contrato** (`:sdui-core`) pero **no tiene handler**: hoy
cae silenciosamente a "acción no soportada (no-op)". Esta spec la implementa. De paso, `Track`
(también sin handler) gana un handler mínimo que loguea (seam de analytics), eliminando la deuda
latente de acciones muertas.

## Fuera de alcance
- **`SduiPatch`** (parches incrementales del árbol): el server responde con **acciones**, no con
  un sub-árbol; el reemplazo de pantalla completa se hace vía `Navigate`/recarga. Spec posterior.
- **Reintentos automáticos, backoff, polling, websockets, streaming**: una sola petición por
  `FireEndpoint`. Reintentar = el server devuelve un botón con otro `FireEndpoint`.
- **Cancelación explícita** de una petición en vuelo desde la UI (botón cancelar): fuera; sí se
  cancela implícitamente al salir de la pantalla (el scope se cancela).
- **Optimistic UI / rollback**: el estado se actualiza solo con la respuesta real.
- **Autenticación / headers dinámicos / cookies**: el cliente HTTP usa su config base (005); sin
  tokens por petición. Spec posterior.
- **Subida de archivos / multipart**: solo cuerpos JSON.
- **Componentes nuevos** (`progressIndicator`, `snackbar`): la demo refleja el estado con un `text`
  enlazado a la variable de estado. Componentes visuales de carga = spec posterior.
- **Deduplicación / idempotencia** de peticiones concurrentes sobre el mismo endpoint: para el MVP,
  cada disparo lanza una petición; se documenta la condición de carrera (HU-6.4).

## Historias de usuario y criterios de aceptación

### HU-1 — `FireEndpoint` enriquecido (contrato)
**Como** autor de pantallas **quiero** declarar una petición con su payload y sus reacciones
**para** describir una mutación de servidor de forma declarativa.

Criterios (EARS):
1. The contract (`:sdui-core`) SHALL enriquecer `FireEndpoint` preservando los campos actuales
   (`endpoint`, `payloadVars`) y añadiendo, todos opcionales con default:
   - `method: String = "POST"` — `"GET"` o `"POST"`.
   - `statusVar: String? = null` — nombre de la variable donde la app escribe el estado del ciclo
     (`"idle"`/`"loading"`/`"success"`/`"error"`).
   - `resultVar: String? = null` — nombre de la variable donde la app escribe un mensaje/resultado
     legible de la respuesta (o del error), para enlazar con un `text`.
   - `onSuccess: List<UiAction> = emptyList()` — acciones a despachar tras un 2xx (además de las
     que devuelva el server).
   - `onError: List<UiAction> = emptyList()` — acciones a despachar si la petición falla.
2. The change SHALL ser retrocompatible en wire: un `FireEndpoint` viejo (solo `endpoint` +
   `payloadVars`) decodifica con los nuevos campos a default.
3. The contract SHALL exponer un tipo de **respuesta** `ActionResponse(actions: List<UiAction> =
   emptyList(), message: String? = null)` que el server devuelve y el cliente decodifica.
4. The contract SHALL exponer constantes de estado (`object EndpointStatus { const val Idle,
   Loading, Success, Error }`) para que server y cliente compartan los literales sin duplicarlos.

### HU-2 — El motor solo despacha (frontera intacta)
**Como** mantenedor **quiero** que el motor NO sepa de HTTP **para** preservar la frontera de 003/006.

Criterios (EARS):
1. The engine (`:sdui-compose`) SHALL seguir despachando `FireEndpoint` por
   `LocalSduiActionHandler.handle(...)` igual que cualquier otra acción; **no** introduce HTTP,
   coroutines de red, ni nuevos seams.
2. The engine SHALL NOT importar `dev.kuisd.app.*` ni `io.ktor.*` (regla 006 sin cambios).
3. The `button`/`iconButton` existentes SHALL poder llevar un `FireEndpoint` en `actions["onClick"]`
   sin cambios en sus renderers (ya despachan la lista de acciones).

### HU-3 — La app ejecuta la petición y refleja el estado
**Como** usuario **quiero** ver "enviando…", luego el resultado o el error **para** entender qué
pasó con mi acción.

Criterios (EARS):
1. The app SHALL implementar un `FireEndpointActionHandler` (SubHandler de 005) que `supports`
   `FireEndpoint` y, al recibirlo:
   1. **Inmediatamente** (síncrono) escribe `statusVar = "loading"` en el `VariableStore` (si
      `statusVar != null`), de modo que la UI recompone mostrando el estado de carga.
   2. Lanza la petición en un `CoroutineScope` provisto por el host (no bloquea el hilo de UI).
   3. Construye el cuerpo como un JSON object `{ <var> : <valor actual> }` para cada nombre en
      `payloadVars`, leyendo del `VariableStore`.
2. WHEN la petición devuelve 2xx, the app SHALL:
   - escribir `statusVar = "success"` y, si hay `resultVar`, `resultVar = response.message` (o `""`);
   - despachar `onSuccess` **y** `response.actions` (en ese orden) por el `AppActionHandler`.
3. WHEN la petición falla (no-2xx, timeout, red), the app SHALL:
   - escribir `statusVar = "error"` y, si hay `resultVar`, `resultVar = <mensaje de error legible>`
     (reutiliza `HttpErrorMapper` de 005);
   - despachar `onError`.
4. The app SHALL ejecutar la petición vía un seam de datos (DIP), no acoplando el handler a Ktor
   directamente: un `ActionEndpoint` (fun interface) cuya impl Ktor vive en la capa `data`.
5. WHILE la petición está en vuelo, the app SHALL permitir que el usuario siga interactuando (no
   hay bloqueo modal); si dispara otra petición, ver HU-6.4 (condición de carrera documentada).

### HU-4 — `Track` deja de ser acción muerta
**Como** mantenedor **quiero** que `Track` tenga un handler **para** no tener acciones que caen a
"no soportada" en silencio.

Criterios (EARS):
1. The app SHALL implementar un `TrackActionHandler` (SubHandler) que `supports` `Track` y, al
   recibirlo, lo registra vía `appLog` con el `event` y sus `props` (seam de analytics mínimo).
2. The handler SHALL ser un punto de extensión documentado: una app real sustituiría el log por su
   SDK de analytics; el MVP solo loguea.

### HU-5 — Demo end-to-end servida por el server
**Como** demo **quiero** un formulario que envíe datos y muestre la respuesta del server **para**
validar el ciclo de red de punta a punta.

Criterios (EARS):
1. The server SHALL servir una pantalla nueva `form` (`GET /screen/form`, 200) con:
   - un `textField(bind="name")`;
   - un `text` enlazado a `$submitStatus` (estado) y otro a `$submitResult` (mensaje);
   - un `button` "Enviar" cuyo `onClick` es
     `FireEndpoint(endpoint="/action/greet", method="POST", payloadVars=["name"],
     statusVar="submitStatus", resultVar="submitResult")`.
2. The server SHALL exponer `POST /action/greet` que lee `{ "name": "<x>" }` del cuerpo y devuelve
   `ActionResponse(message="Hola, <x>!", actions=[SetVar("greeted", true)])` con 200.
3. WHEN `name` está vacío, the server SHALL devolver un error 4xx con `ProblemDetail` (reusa el
   error handling de 002); the app SHALL reflejar `submitStatus="error"` y el mensaje en
   `submitResult`.
4. The system SHALL enlazar `home → form` con un `button` adicional (`Navigate("form")`).
5. The visual smoke (`:desktopApp:run`) SHALL mostrar: teclear nombre → "Enviar" → el texto pasa de
   "" a "loading" a "Hola, X!" sin recargar la pantalla; con nombre vacío, muestra el error.

### HU-6 — Resiliencia y ciclo de vida
1. IF el `CoroutineScope` del host se cancela (el usuario sale de la pantalla) mientras hay una
   petición en vuelo, THEN la petición SHALL cancelarse y NO escribir estado sobre un store que ya
   no se muestra (el scope cancelado no ejecuta los `set`).
2. IF `endpoint` es inválido o el server no responde, THEN the app SHALL escribir `statusVar="error"`
   + `resultVar=<mensaje>` y NO crashear.
3. IF `payloadVars` referencia una variable ausente, THEN the app SHALL omitirla del cuerpo (no
   enviar la clave) sin crash.
4. IF el usuario dispara dos `FireEndpoint` sobre el mismo `statusVar` casi a la vez, THEN el
   estado final reflejará la **última** respuesta en llegar (last-write-wins); esta condición de
   carrera se documenta como limitación MVP (sin dedupe/cancelación de la anterior).
5. IF el `LocalSduiActionHandler` es el default (no-op, fuera de un host), THEN `FireEndpoint` se
   loguea como no soportada sin crash (el motor no diferencia este caso).

## Requisitos no funcionales
- **Frontera Clean Architecture (003/006):** la red, el `CoroutineScope`, el estado y el dispatch
  del resultado viven en `:shared` (app). El motor (`:sdui-compose`) **no cambia** salvo, a lo
  sumo, nada. `:sdui-core` gana campos opcionales + 1 tipo de respuesta + constantes.
- **Coroutines correctas:** la petición se lanza en un scope ligado a la composición del host
  (`rememberCoroutineScope`), no en `GlobalScope`; se cancela con la pantalla. El `statusVar=loading`
  se escribe **antes** de suspender, en el hilo de UI, para feedback inmediato.
- **Reutilización:** el ciclo de estado usa el `VariableStore` (005) y el dispatch del resultado usa
  el `AppActionHandler` (005) — sin duplicar lógica de acciones. El HTTP reutiliza el `HttpClient`
  config de 002/005 y `HttpErrorMapper`.
- **DIP:** el handler depende de un `ActionEndpoint` (abstracción), no de Ktor.
- **Resiliencia:** error/timeout/var ausente/handler default no crashean.
- **`detekt`/`ktlintCheck`** en verde; tests sin red real (fake `ActionEndpoint`).

## Dependencias y supuestos
- Depende de 002 (server + ProblemDetail), 003 (acciones + frontera), 005 (VariableStore +
  AppActionHandler + SubHandler + HttpErrorMapper), 007 (textField para el demo). Todas mergeadas.
- Reutiliza `FireEndpoint`/`Track` del contrato (ya existen; se enriquece `FireEndpoint`).
- No introduce dependencias nuevas en `gradle/libs.versions.toml` (Ktor client ya está).
- Supone que el server expone endpoints de acción bajo un prefijo (`/action/...`) distinto de las
  pantallas (`/screen/...`).
