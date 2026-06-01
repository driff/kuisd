# Requisitos — Navegación dirigida por acciones (Navigate)

> Spec ID: 003 · Estado: approved · Fecha: 2026-05-31

## Resumen
Cuando el usuario pulsa un botón cuyo `onClick` contiene una acción `Navigate(route)`, el cliente
**carga la pantalla destino desde el BFF** (`GET /screen/{route}`) y la muestra, manteniendo una
**pila de navegación** en memoria. Se añade al contrato `:sdui-core` una acción **`NavigateBack`**
(pop server-driven) y, además, el host ofrece una afordancia de "atrás" cross-platform. Da contenido
real al `Navigate("details")` que el `HomeScreen` ya emite y continúa el no-op que el slice 001 dejó
en `RenderNode`. Demo en cadena de 3 pantallas: `home → details → more`. Funciona en Android, Desktop e iOS.

## Fuera de alcance
- Acciones de **variables locales** (`SetVar` / `Toggle` / `Increment`) y `VariableStore`: spec futura.
- `FireEndpoint` (red real / `POST /action`), `SduiPatch` (patches), `Track`, `CustomAction`: aquí
  solo se interpretan `Navigate` y `NavigateBack`; el resto queda como no-op/log.
- Navegación con **argumentos complejos** más allá de `args: Map<String,String>`, **deep links**,
  `popUpTo`/navegación a un índice concreto, y **persistencia** del back stack (proceso muerto).
- **Back nativo** de plataforma (botón físico Android, gesto iOS): se usa una afordancia en la UI.
- Extracción del motor a la librería `:sdui-compose`: aquí todo el cliente vive en `:shared`.
- Transiciones/animaciones de navegación; título de barra estilizado por tokens.

## Historias de usuario y criterios de aceptación

### HU-1 — Navegar al pulsar un botón con `Navigate`
**Como** usuario **quiero** que al pulsar un botón se abra la pantalla que indica el servidor
**para** moverme entre pantallas controladas por el BFF.

Criterios (EARS):
1. The system SHALL exponer un `ActionDispatcher` que interpreta una `List<UiAction>`: para
   `Navigate` **apila** su `route`; para `NavigateBack` **desapila** el tope (ver HU-2).
2. WHEN un nodo `button` recibe un click, the system SHALL despachar sus `actions["onClick"]` al
   `ActionDispatcher` (en lugar del `sduiLog` no-op del slice 001).
3. WHEN se navega a una `route`, the system SHALL cargar `GET /screen/{route}` del BFF mediante el
   `SduiClient` compartido del host y renderizar el árbol de la pantalla destino.
4. WHILE la pantalla destino se carga, the system SHALL mostrar el `SduiUiState.Loading` existente;
   IF falla, THEN SHALL mostrar el `SduiUiState.Error` legible, sin perder la entrada en la pila.

### HU-2 — Volver atrás (acción `NavigateBack` + afordancia del host)
**Como** usuario **quiero** volver a la pantalla anterior **para** deshacer una navegación.

Criterios (EARS):
1. The system SHALL añadir al contrato `:sdui-core` una acción **`NavigateBack`**; WHEN se despacha,
   the system SHALL desapilar la pantalla del tope (si hay anterior) y mostrar la previa (recarga del BFF).
2. The system SHALL ofrecer **además** una afordancia de "atrás" **cross-platform** en una barra
   superior del host, visible solo cuando hay una pantalla anterior; activarla equivale a `NavigateBack`.
3. WHILE la pila contiene una sola pantalla (la raíz), the system SHALL ocultar la afordancia de la
   barra y `NavigateBack` SHALL ser no-op (no se desapila la raíz).
4. WHEN se navega a una nueva `route`, the system SHALL conservar las pantallas anteriores en orden LIFO.

### HU-3 — Resiliencia ante acciones y rutas desconocidas
**Como** usuario **quiero** que una acción o ruta inválida no rompa la app **para** seguir usándola.

Criterios (EARS):
1. IF una `UiAction` no es `Navigate` ni `NavigateBack` (p. ej. `SetVar`, `FireEndpoint`, `Track`,
   `CustomAction`, `NoOpAction`), THEN the system SHALL ignorarla con un log (no-op), sin crash.
2. IF `actions["onClick"]` está ausente o vacío, THEN the system SHALL no hacer nada (no-op silencioso).
3. IF se navega a una `route` no registrada, THEN el server responde 404 (`ProblemDetail`, spec 002)
   y the system SHALL mostrar el estado de error de esa entrada, sin corromper la pila (se puede volver).
4. A clientes con un `:sdui-core` antiguo que no conozcan `NavigateBack`, the system SHALL degradarla a
   `NoOpAction` vía el fallback polimórfico del contrato (forward-compat; aditivo, sin bump major).

### HU-4 — Funcionar en las tres plataformas
**Como** equipo **quiero** la misma navegación en Android, Desktop e iOS **para** validar KMP.

Criterios (EARS):
1. The system SHALL implementar la pila, el dispatcher y la afordancia de "atrás" **íntegramente en
   `commonMain`** de `:shared` (sin `expect/actual` ni navegación específica de plataforma).
2. The system SHALL usar **un único `SduiClient`** propiedad del `SduiHost` (compartido entre las
   pantallas de la pila), reutilizando el `SduiClient`/`SduiHttp` del slice 001 sin cliente nuevo.

## Requisitos no funcionales
- **Resiliencia:** acción desconocida, lista vacía o ruta inexistente nunca provocan crash.
- **Reutilización:** se reutilizan `SduiClient`, `SduiScreen`/`SduiUiState`, `RenderNode` y el contrato.
- **Contrato aditivo:** `NavigateBack` es un nuevo subtipo `sealed` de `UiAction` (cambio aditivo,
  forward-compatible; no rompe a consumidores existentes — HU-3.4).
- **Simplicidad:** back stack en memoria y afordancia propia, sin librería de navegación ni back nativo.
- **Calidad:** pasa `detekt` y `ktlintCheck`.

## Dependencias y supuestos
- **Depende de la spec 001** (`SduiClient`, `SduiScreen`/`SduiUiState`, `RenderNode`, `SduiHttp`) y de
  la **spec 002** (server con `ScreenRegistry`, `ProblemDetail`, 404 estructurado) — mergeadas.
- Modifica el contrato `:sdui-core` (añade `NavigateBack`) y el server (añade screens `details` y `more`).
- El `HomeScreen` ya emite `Navigate("details")`; esta spec añade los destinos reales `details` y `more`.
- Requiere el `:server` corriendo para el smoke (`./gradlew :server:run`). No añade dependencias al catálogo.
