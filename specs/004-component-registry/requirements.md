# Requisitos — Catálogo de componentes extensible (ComponentRegistry · OCP)

> Spec ID: 004 · Estado: approved · Fecha: 2026-06-01

## Resumen
Sustituir el `when (node.type)` hardcodeado del motor de render por un **registro abierto** de
componentes basado en `SduiComponent<P>` (ya en `:sdui-core`). El motor (`dev.kuisd.sdui`) resuelve
cada nodo contra un `ComponentRegistry`, decodifica sus props tipadas `P` y delega el render al
componente registrado — de modo que **añadir un componente no requiere modificar el motor** (OCP).
Se entrega un **CorePack** con los componentes base (column/row/text/button) y se demuestra la
extensión registrando un componente propio en la **app** (`badge`) que el motor renderiza sin conocerlo.

## Fuera de alcance
- Acciones de variables locales, `FireEndpoint`, `SduiPatch` (specs futuras).
- Aplicar `UiModifier` (padding/background/weight) en el render — sigue ignorado como en 001/003.
- Extracción a módulos Gradle `:sdui-compose`/`:app-contract` (spec futura; hoy por paquetes).
- Theming por tokens, layout avanzado.

## Historias de usuario y criterios de aceptación

### HU-1 — Registro abierto en el motor (OCP)
**Como** integrador **quiero** registrar componentes sin editar el motor **para** extender el catálogo.

1. The engine SHALL exponer un `ComponentRegistry` que mapea `type` → componente registrado
   (`SduiComponent<P>` + un renderer `@Composable RenderScope.(P) -> Unit`).
2. WHEN se renderiza un `SduiNode`, the engine SHALL buscar el componente por `node.type`, **decodificar
   `node.props` a `P`** con el serializer del componente, y delegar el render a su renderer.
3. The engine SHALL resolver el registro vía `LocalComponentRegistry` (default = `CorePack`), de modo
   que la app pueda **extenderlo o sustituirlo sin tocar el motor**.
4. The engine SHALL seguir delegando las acciones de los nodos vía `LocalSduiActionHandler` (spec 003,
   sin cambios) y exponer una API mínima (registry, RenderScope, CorePack, RenderNode).

### HU-2 — CorePack (componentes base)
1. The engine SHALL proveer un `CorePack` que registra `column`, `row`, `text`, `button` con sus props
   tipadas (`TextProps`, `ButtonProps`, contenedores sin props), preservando el comportamiento actual.
2. Los contenedores (`column`/`row`) SHALL renderizar sus `children` recursivamente vía `RenderScope`.
3. El `button` SHALL delegar `actions["onClick"]` al `LocalSduiActionHandler` (igual que 003).

### HU-3 — Extensión desde la app (demo de OCP)
**Como** app **quiero** añadir un componente propio **para** validar que el motor es extensible.

1. The app SHALL registrar un componente propio (`badge`, `BadgeProps(text)`) combinándolo con el
   `CorePack` (`CorePack + appComponents`) y proveerlo vía `LocalComponentRegistry` en el `SduiHost`.
2. El server SHALL emitir un nodo `badge` en alguna pantalla; the system SHALL renderizarlo en el
   cliente **sin modificar el motor**.

### HU-4 — Resiliencia
1. IF `node.type` no está registrado, THEN the engine SHALL renderizar `UnknownNode(type)` (sin crash).
2. IF las props de un nodo no decodifican a `P`, THEN the engine SHALL degradar a `UnknownNode` con log,
   sin crash (las props tipadas tienen defaults razonables).

## Requisitos no funcionales
- **OCP verificable:** añadir el `badge` no toca ningún archivo de `dev.kuisd.sdui` (solo app + server).
- **Frontera Clean Architecture** (spec 003) intacta: el motor no importa Ktor ni `dev.kuisd.app`.
- API pública mínima; `detekt`/`ktlintCheck` en verde.

## Dependencias y supuestos
- Depende de 001/002/003 (mergeadas). Reutiliza `SduiComponent<P>`/`sduiComponent<P>()` de `:sdui-core`.
- **Cambio de build:** `:shared` pasa a aplicar el plugin `kotlin-serialization` (las props tipadas de
  CorePack y del `badge` son `@Serializable`).
- El motor sigue ignorando `UiModifier` (fuera de alcance).
