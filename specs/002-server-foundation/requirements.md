# Requisitos — Endurecimiento de la fundación del servidor

> Spec ID: 002 · Estado: approved · Fecha: 2026-05-31
> Orden de implementación: **antes** de la spec 001 (la 001 depende de esta).

## Resumen
Fijar las decisiones transversales de servicio del BFF Ktor antes de apilar features de UI,
según el análisis de arquitectura: pureza del contrato, modelo de error JSON, CORS seguro,
estructura modular del `:server` con un `ScreenRegistry`, convención de header de capability, e
higiene HTTP (compresión, headers, correlación). Son cambios baratos ahora y caros de
retrofitear cuando haya clientes consumiendo la API.

## Fuera de alcance
- Migración a `EngineMain` + HOCON (R4), métricas/Micrometer, caché/ETag, Koin, `POST /action`,
  idempotencia, auth, Dockerfile/CI: son evolutivos y van en specs posteriores.
- Ramificación real por capability (gating de componentes): aquí solo se **lee** el header.

## Historias de usuario y criterios de aceptación

### HU-1 — El contrato publicado no contiene datos de app (R1)
**Como** mantenedor de `dev.kuisd:sdui-core` **quiero** que la librería no incluya pantallas de
demo **para** no acoplar el catálogo de una app al contrato publicado.

Criterios (EARS):
1. The system SHALL trasladar el ensamblado de pantallas de ejemplo (`SampleScreens`) fuera de
   `:sdui-core` al módulo `:server`.
2. The system SHALL mantener `:sdui-core` compilando y con sus tests verdes construyendo los
   `SduiEnvelope` de prueba localmente (sin depender de un catálogo de app).
3. WHILE no exista aún la spec 001, the system SHALL mantener los clientes (`:shared`) compilando
   sin referencia a `SampleScreens`.

### HU-2 — Respuestas de error estructuradas y sin fugas (R2)
**Como** cliente **quiero** errores legibles y estables **para** mostrarlos sin exponer detalle interno.

Criterios (EARS):
1. The system SHALL definir un tipo `ProblemDetail` (estilo RFC 7807) en `:sdui-core`,
   serializable y compartido con el cliente.
2. WHEN una ruta lanza una excepción no controlada, the system SHALL responder
   `application/problem+json` con un `ProblemDetail` (status 500, título genérico) y **no** incluir
   `cause.message` salvo en modo desarrollo.
3. WHEN se solicita una pantalla inexistente, the system SHALL responder 404 con un `ProblemDetail`.
4. WHEN ocurre un error, the system SHALL registrar (log) la causa correlacionada con el `callId`.

### HU-3 — CORS seguro por defecto (R3)
**Como** operador **quiero** no exponer `anyHost` **para** evitar un default inseguro.

Criterios (EARS):
1. The system SHALL restringir CORS a una lista de orígenes configurable (deny-by-default).
2. WHERE no se configuran orígenes, the system SHALL no permitir orígenes cruzados arbitrarios.

### HU-4 — Estructura modular del servidor con registro de pantallas (R5)
**Como** desarrollador **quiero** separar bootstrap, plugins, routing y ensamblado **para** crecer
sin reescribir y preparar la futura `:sdui-ktor`.

Criterios (EARS):
1. The system SHALL organizar `:server` en paquetes `plugins/`, `routing/`, `screens/`.
2. The system SHALL resolver las pantallas mediante un `ScreenRegistry` (mapa
   `screenId → builder`) en lugar de un `when` hardcodeado en la ruta.
3. WHEN se solicita un `screenId` registrado, the system SHALL devolver su `SduiEnvelope`; IF no
   está registrado, THEN responde 404 (HU-2.3).

### HU-5 — Convención de header de capability leída en el servidor (R6)
**Como** plataforma **quiero** que el cliente declare su versión de esquema desde el día 1 **para**
no acumular deuda de compatibilidad.

Criterios (EARS):
1. The system SHALL definir el header `X-Kuisd-Version` como parte del contrato HTTP.
2. WHEN llega una petición a `/screen/{id}`, the system SHALL leer ese header (si está presente) y
   ponerlo a disposición del builder vía un contexto, **sin** ramificar la respuesta todavía.

### HU-6 — Higiene HTTP (R7)
**Como** cliente móvil **quiero** respuestas comprimidas y correlacionables **para** mejorar TTI y
diagnóstico.

Criterios (EARS):
1. The system SHALL instalar `Compression` (gzip) para las respuestas.
2. The system SHALL instalar `DefaultHeaders`.
3. The system SHALL instalar `CallId` y exponer el id de correlación (reutilizado por HU-2.4).

## Requisitos no funcionales
- `:server` SHALL seguir sirviendo `GET /health` y `GET /screen/home` con el mismo contenido.
- `detekt` y `ktlintCheck` en verde; tests de `:sdui-core` y `:server` en verde.

## Dependencias y supuestos
- Sobre el estado actual (bootstrap + spec 001 escrita pero no implementada).
- Requiere añadir al catálogo los artefactos de plugins de Ktor server que falten
  (`call-id`, `compression`, `default-headers`).
