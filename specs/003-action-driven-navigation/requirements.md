# Requisitos — Navegación por acciones + frontera Clean Architecture (motor puro / app)

> Spec ID: 003 · Estado: approved · Fecha: 2026-05-31
> Revisión: reescrita tras la corrección de arquitectura (motor SDUI "puro") y la auditoría Clean
> Architecture + SOLID del código KMP de cliente.

## Resumen
Implementar la **navegación dirigida por acciones** (`home → details → more`) estableciendo la
**frontera limpia** entre el **motor SDUI** (`dev.kuisd.sdui`: solo renderiza el árbol que recibe y
**delega** las acciones) y la **capa de app** (`dev.kuisd.app`: fetch, estado de carga y navegación).
Refactoriza el código del slice 001 (que mezclaba fetch + estado + render dentro del motor y acoplaba
a Ktor) para cumplir **Clean Architecture** (regla de dependencias) y **SOLID** (SRP/DIP/ISP). Añade
`NavigateBack` al contrato `:sdui-core`.

## Fuera de alcance
- Acciones de variables locales (`SetVar`/`Toggle`/`Increment`), `FireEndpoint`, `SduiPatch`, deep
  links, persistencia del back stack, back nativo de plataforma.
- **`ComponentRegistry` abierto** (OCP): el `when (node.type)` del motor se mantiene (4 componentes;
  YAGNI). `SduiComponent<P>` queda como punto de extensión preparado en `:sdui-core`.
- **Extracción a módulos Gradle** (`:sdui-compose`, `:app-client`): hoy la frontera es **solo por
  paquetes** dentro de `:shared`; el salto a módulos será mecánico después.
- Framework de DI (basta `remember` + defaults) y caché/local source (`ScreenSource` de un método).

## Historias de usuario y criterios de aceptación

### HU-1 — Motor de render puro (`dev.kuisd.sdui`)
**Como** integrador **quiero** un motor que solo pinte y delegue **para** reutilizarlo en cualquier app.

1. The engine SHALL renderizar un `SduiNode` dado (column/row/text/button + `UnknownNode`), sin fetch,
   estado de carga ni navegación.
2. The engine SHALL definir un seam `SduiActionHandler` (+ `LocalSduiActionHandler`, default no-op) y,
   al pulsar un botón, **delegar** `actions["onClick"]` a él **sin interpretarlas**.
3. The engine SHALL depender **solo** de `:sdui-core` + Compose; SHALL NOT importar Ktor ni `dev.kuisd.app`.
4. The engine SHALL exponer API mínima (`RenderNode`, `SduiActionHandler`, `LocalSduiActionHandler`);
   el resto (`NodeProps`, `sduiLog`) `internal`.
5. IF `onClick` ausente/vacío THEN no-op; IF `type` desconocido THEN `UnknownNode` (resiliencia).

### HU-2 — Datos tras una abstracción (`dev.kuisd.app.data`, DIP)
**Como** app **quiero** depender de una abstracción de carga **para** no acoplar presentación a Ktor.

1. The app SHALL definir `ScreenSource` (interface) que carga un `SduiEnvelope` por `screenId`; la impl
   Ktor (`KtorScreenSource`, `SduiClient`, `SduiHttp`+actuals) es `internal`.
2. The app SHALL traducir los errores de transporte a un mensaje legible **en la capa data**
   (`HttpErrorMapper`); la presentación SHALL NOT importar Ktor.

### HU-3 — Estado y render de pantalla (`dev.kuisd.app`)
1. The app `SduiScreen` SHALL, dado un `ScreenSource` y un `screenId`, producir estado
   Loading/Error/Content y renderizar el árbol del envelope vía el motor (`RenderNode`).
2. Reutiliza el modelo de estado del slice 001 (Loading/Error/Content) ya sin Ktor dentro.

### HU-4 — Navegación en la app
1. The app SHALL mantener `NavBackStack` (con `id` estable por entrada) e interpretar las acciones
   delegadas mediante `NavActionHandler : SduiActionHandler`: `Navigate`→push, `NavigateBack`→pop,
   resto no-op+log.
2. `SduiHost` SHALL poseer el `NavBackStack` y el `ScreenSource` (su ciclo de vida), proveer el
   `LocalSduiActionHandler`, renderizar la pantalla del tope (recarga total por `key(id)`), y ofrecer
   una afordancia "atrás" cross-platform; en la raíz, oculta y no-op.

### HU-5 — Contrato + plataformas
1. `:sdui-core` SHALL añadir `NavigateBack` (subtipo `sealed` aditivo; forward-compat → `NoOpAction`).
2. Todo el cliente en `commonMain`; `SduiHttp` por plataforma (`expect/actual`); el server añade
   `details` y `more`.

## Requisitos no funcionales
- **Regla de dependencias** observable: motor no importa Ktor ni `app`; data no importa Compose ni motor;
  `app` compone las tres. (Opcional: regla detekt `ForbiddenImport` que prohíba `io.ktor.*` en `dev.kuisd.sdui`.)
- **Resiliencia:** acción/ruta desconocida y `onClick` vacío nunca crashean.
- **API mínima** (public/internal segun el mapa de diseño); `detekt`/`ktlintCheck` en verde.

## Dependencias y supuestos
- Depende de la spec 001 (**refactoriza su código**: mueve fetch/estado fuera del motor) y de la 002.
- Reutiliza el contrato `:sdui-core` (+`NavigateBack`). El server añade `details`/`more`.
