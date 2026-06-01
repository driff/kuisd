# Requisitos — Slice vertical SDUI (cliente ↔ servidor)

> Spec ID: 001 · Estado: approved · Fecha: 2026-05-31

## Resumen
Conectar el cliente Compose con el `:server` Ktor: el cliente pide `GET /screen/home`,
recibe un `SduiEnvelope` (contrato `:sdui-core`) y **renderiza su árbol** con un conjunto
mínimo de componentes Compose, sustituyendo la llamada hardcodeada actual a
`SampleScreens.home()` en `PlaceholderApp`. Es el primer flujo SDUI de punta a punta y
funciona en Android, Desktop e iOS.

## Fuera de alcance
- Motor de render reutilizable completo (`:sdui-compose` como librería): aquí el render vive
  mínimamente en `:shared`; su extracción será una spec posterior.
- Despacho real de acciones (`UiAction`): los `onClick` se reconocen pero no ejecutan navegación
  ni red (placeholder/log). Spec futura.
- Variables/binding reactivo, patches (`SduiPatch`), theming por tokens, caché offline.
- Catálogo de componentes amplio: solo los necesarios para `home` (column, row, text, button).

## Historias de usuario y criterios de aceptación

### HU-1 — Cargar una pantalla desde el servidor
**Como** app cliente **quiero** obtener la descripción de una pantalla del BFF **para** que el
servidor controle qué se muestra.

Criterios (EARS):
1. The system SHALL exponer un `SduiClient` con una operación suspendida que recupera un
   `SduiEnvelope` para un `screenId` dado desde `{baseUrl}/screen/{screenId}`.
2. WHEN se solicita `screenId = "home"` con el `:server` en marcha, the system SHALL devolver
   un `SduiEnvelope` con `screenId == "home"` decodificado con `DefaultSduiJson` del contrato.
3. IF la petición HTTP falla o el cuerpo no es decodificable, THEN the system SHALL propagar un
   estado de error consumible por la UI (sin crash).

### HU-2 — Renderizar el árbol SDUI en Compose
**Como** usuario **quiero** ver la pantalla descrita por el servidor **para** interactuar con ella.

Criterios (EARS):
1. The system SHALL mapear los nodos `column`, `row`, `text` y `button` de `SduiNode` a
   composables equivalentes, renderando recursivamente `children`.
2. The system SHALL leer las props relevantes del nodo (`text.text`, `button.label`) desde el
   `JsonObject` de props.
3. WHEN un nodo `button` recibe un click, the system SHALL reconocer sus `actions["onClick"]`
   (registro/log), sin ejecutar navegación ni red en este slice.
4. IF el `type` de un nodo es desconocido, THEN the system SHALL renderizar un marcador de
   reemplazo legible en lugar de fallar (resiliencia / forward-compat).

### HU-3 — Estados de carga visibles
**Como** usuario **quiero** feedback mientras carga o si algo falla **para** entender el estado.

Criterios (EARS):
1. WHILE la pantalla se está cargando, the system SHALL mostrar un indicador de progreso.
2. IF la carga termina en error, THEN the system SHALL mostrar un mensaje de error legible.
3. WHEN la carga tiene éxito, the system SHALL mostrar el árbol renderizado del envelope.

### HU-4 — Funcionar en las tres plataformas
**Como** equipo **quiero** el mismo flujo en Android, Desktop e iOS **para** validar KMP.

Criterios (EARS):
1. The system SHALL usar un motor HTTP de Ktor por plataforma (OkHttp en Android, Darwin en iOS,
   CIO en Desktop) vía `expect/actual`.
2. The system SHALL resolver una `baseUrl` por defecto adecuada por plataforma (Android emulador
   `http://10.0.2.2:8080`; Desktop e iOS `http://localhost:8080`).
3. WHERE la plataforma es Android, the system SHALL permitir tráfico HTTP en claro hacia el host
   de desarrollo (build debug).

## Requisitos no funcionales
- **Resiliencia:** un componente desconocido o una prop ausente nunca debe provocar crash.
- **Reutilización del contrato:** la (de)serialización usa `DefaultSduiJson` de `:sdui-core`
  (mismo `classDiscriminator` y políticas que el servidor).
- **Sin nuevas reglas de calidad rotas:** pasa `detekt` y `ktlintCheck`.

## Dependencias y supuestos
- **Depende de la spec 002** (server estructurado + `ProblemDetail`): se implementa después de la 002.
- Depende de los módulos existentes `:sdui-core` (contrato) y `:server` (sirve `/screen/home`).
- Requiere el `:server` corriendo localmente para el smoke manual (`./gradlew :server:run`).
- Ktor 3.5.0 y los engines de cliente ya están en el catálogo de versiones.
