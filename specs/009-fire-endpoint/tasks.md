# Tareas — `FireEndpoint`: acciones con red

> Spec ID: 009 · Trazabilidad: ./requirements.md · ./design.md

- [ ] **T0** — Aprobar `requirements.md` y `design.md` (`Estado: approved`); cerrar las 3 preguntas
  abiertas (respuesta=acciones; Track=log; demo=greet).

## Contrato
- [ ] **T1** — `:sdui-core` · `UiAction.kt`: enriquecer `FireEndpoint` con `method`, `statusVar`,
  `resultVar`, `onSuccess`, `onError` (todos opcionales, default). Añadir `ActionResponse` y
  `object EndpointStatus`.
  - _verif:_ `:sdui-core:check` compila.
- [ ] **T2** — `:sdui-core` test: `FireEndpoint` decodifica con/sin campos nuevos (wire compat);
  `ActionResponse` con acciones polimórficas round-trips; `onSuccess` anida `SetVar` y decodifica.
  - _verif:_ `:sdui-core:check` verde.

## App — data
- [ ] **T3** — `:shared/app/data/ActionEndpoint.kt`: `ActionRequest` + `fun interface ActionEndpoint`.
- [ ] **T4** — `:shared/app/data/SduiClient.kt`: `postAction(endpoint, method, body): ActionResponse`
  (POST con setBody JSON; GET ignora body). Reusa HttpClient/config y expectSuccess.
- [ ] **T5** — `:shared/app/data/KtorActionEndpoint.kt`: impl que delega en `SduiClient.postAction`.
  - _verif:_ `:shared:assemble`.

## App — handlers
- [ ] **T6** — `:shared/app/FireEndpointActionHandler.kt`: SubHandler con `var dispatch`,
  `supports(FireEndpoint)`, loading síncrono, payload desde store (omite ausentes), `scope.launch`
  con try/catch (re-lanza CancellationException), set status/result + dispatch onSuccess+actions /
  onError.
- [ ] **T7** — `:shared/app/TrackActionHandler.kt`: SubHandler que loguea `Track` por `appLog`.
  - _verif:_ `:shared:assemble`.

## App — host
- [ ] **T8** — `:shared/app/SduiHost.kt`: elevar un único `SduiClient` compartido (cerrar en el
  DisposableEffect); mover `store`/`scope`/handlers dentro de `key(current.id)`; añadir
  `TrackActionHandler` + `FireEndpointActionHandler` al compuesto; `fire.dispatch = handler::handle`.
  - _verif:_ `:shared:check`, `:androidApp:assembleDebug`, `:desktopApp` compilan.

## App — tests (sin red)
- [ ] **T9** — `:shared/commonTest` `FireEndpointActionHandlerTest` (fake `ActionEndpoint` + `runTest`):
  loading síncrono; éxito → status=success, result=message, dispatch recibe onSuccess+response.actions;
  fallo → status=error, result=mensaje, dispatch recibe onError; payloadVars con var ausente se omite.
- [ ] **T10** — `:shared/commonTest`: `TrackActionHandler.supports(Track)`; `AppActionHandler` con
  Track ya no cae a "no soportada" (extiende `AppActionHandlerTest`).
  - _verif:_ `:shared:check` verde.

## Server
- [ ] **T11** — `:server/actions/ActionRouting.kt`: `Route.actionRoutes()` con
  `POST /action/greet` → valida `name`, devuelve `ActionResponse(message, [SetVar("greeted", true)])`;
  vacío → `BadRequestException` (ProblemDetail de 002).
- [ ] **T12** — `:server` montar `actionRoutes()` en `Application.module()`/`Routing.kt`.
- [ ] **T13** — `:server/screens/FormScreen.kt`: pantalla `form` (textField name + status/result +
  button Enviar con FireEndpoint). Variables sembradas (name, submitStatus=idle, submitResult).
- [ ] **T14** — `:server/screens/ScreenRegistry.kt`: + `"form" → FormScreen`. `HomeScreen.kt`: +
  button "Formulario" → `Navigate("form")`.
- [ ] **T15** — `:server` test `ApplicationTest`: `POST /action/greet` éxito/vacío;
  `GET /screen/form` estructura; `home` con botón Formulario.
  - _verif:_ `:server:build` verde.

## Calidad
- [ ] **T16** — Regla de dependencias: `grep -rn "dev.kuisd.app\|io.ktor\." sdui-compose/src/` vacío
  (el motor no cambió).
- [ ] **T17** — Smoke `:server:run` + `:desktopApp:run`: en `form`, "Ana" → Enviar → loading →
  "Hola, Ana!"; vacío → error + mensaje; sin recarga de pantalla.
- [ ] **T18** — Calidad: `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble
  :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` (tras `ktlintFormat`).

## Verificación final (Definition of Done)
- [ ] `FireEndpoint` enriquecido + `ActionResponse` + `EndpointStatus`; wire retrocompatible.
- [ ] El motor (`:sdui-compose`) NO cambió; sigue sin importar `dev.kuisd.app`/`io.ktor.*`.
- [ ] La app ejecuta la petición en un scope por-entrada (cancelable), refleja loading/success/error
  en variables y re-despacha onSuccess+response.actions / onError por el AppActionHandler.
- [ ] `Track` tiene handler (loguea); ya no cae a "acción no soportada".
- [ ] El server sirve `form` + `POST /action/greet`; `home` enlaza a `form`.
- [ ] Tests contrato + app (fake, sin red) + server verdes; detekt + ktlintCheck verdes.
- [ ] `tasks.md` todo `[x]` salvo smoke manual si el harness no abre UI (justificado).
