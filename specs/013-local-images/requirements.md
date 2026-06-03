# Requisitos — Imágenes locales (`ImageRegistry`)

> Spec ID: 013 · Estado: approved · Fecha: 2026-06-03

## Resumen
El componente `image` (012) solo carga imágenes **remotas por URL**. Falta poder referenciar imágenes
**locales/empaquetadas por nombre** (logos, ilustraciones de marca, placeholders incluidos en la app),
sin red. Esta spec añade un **`ImageRegistry`** —catálogo abierto `nombre → Painter`, mismo patrón que
el `IconRegistry` (008)— provisto por `LocalImageRegistry`. El componente `image` (012) gana un modo
local: si el nodo trae `name`, el motor resuelve el `Painter` del registry y lo pinta; si trae `url`,
sigue el camino remoto del seam `LocalAsyncImageLoader` (012). El registry lo provee la app con sus
recursos (Compose Multiplatform Resources); el motor permanece **agnóstico** (no empaqueta assets).

## Fuera de alcance
- **Carga de imágenes desde el sistema de archivos / `file://` / `content://` en runtime** — son fuentes
  remotas/dinámicas; las cubre (o las cubrirá) el seam de red, no el registry local.
- **Base64/bytes embebidos** en el envelope — fuera (igual que 012).
- **`tint`/teñido de imágenes locales** como en `icon` — fuera de v1 (las imágenes raster no se tiñen
  como vectores; un `tint` por token sería follow-up).
- **Resolución por densidad/tema (claro-oscuro) más allá de lo que Compose Resources ya ofrece** — la
  selección de variante la hace el sistema de recursos de la app, no el contrato SDUI.
- **Animados (GIF/WebP animado) locales** — fuera.
- **Un componente nuevo** distinto de `image`: se reutiliza `image` con un modo local (ver HU-1).

## Historias de usuario y criterios de aceptación

### HU-1 — `image` con fuente local por nombre
**Como** autor de pantallas **quiero** referenciar una imagen empaquetada por su nombre **para** mostrar
logos/ilustraciones de la app sin depender de la red.

Criterios (EARS):
1. The engine SHALL extender `ImageProps` (012) con una prop `name` (string **bindable**, 005) que
   identifica una imagen local en el `ImageRegistry`.
2. WHEN `name` no está vacío, the engine SHALL resolver su `Painter` vía `LocalImageRegistry` y pintarlo
   con `androidx.compose.foundation.Image` aplicando `contentScale`, `contentDescription` y el
   `UiModifier` del nodo (008).
3. IF `name` está vacío/ausente y `url` no, THEN the engine SHALL seguir el camino remoto de 012 (seam
   `LocalAsyncImageLoader`) — comportamiento retrocompatible.
4. IF tanto `name` como `url` están presentes, THEN the engine SHALL priorizar `name` (local) y
   documentarlo (resolución determinista).
5. IF `name` no resuelve en el `ImageRegistry` (nombre desconocido), THEN the engine SHALL degradar a un
   fallback visual neutro (sin crash), análogo al icono de fallback de 008.

### HU-2 — `ImageRegistry` extensible (patrón `IconRegistry`)
**Como** integrador **quiero** un catálogo abierto de imágenes locales **para** registrar las de mi app
sin tocar el motor.

Criterios (EARS):
1. The engine SHALL exponer un tipo `ImageRegistry` (catálogo `nombre → @Composable () -> Painter`),
   con `Empty`, un operador `plus` (override semantics) y un DSL builder `imageRegistry { register(name)
   { painter } }`, **misma mecánica** que `IconRegistry` (008).
2. The engine SHALL exponer `LocalImageRegistry` (CompositionLocal) con default `ImageRegistry.Empty`.
3. The app SHALL proveer su `ImageRegistry` (con recursos Compose Multiplatform) por `LocalImageRegistry`
   en `SduiHost`, sin que el motor empaquete assets ni dependa de recursos de la app.
4. The engine (`:sdui-compose`) SHALL NO incluir imágenes propias; el registry base es vacío.

### HU-3 — Resiliencia y catálogo
**Como** integrador **quiero** que el modo local degrade con elegancia **para** no romper la pantalla.

Criterios (EARS):
1. The engine SHALL mantener `image` como único componente (sin tipo nuevo); el modo local/remoto se
   decide por las props (`name` vs `url`), preservando OCP (004) y el registro existente.
2. IF `LocalImageRegistry` no está provisto (motor aislado), THEN `image` con `name` SHALL usar el
   fallback de HU-1.5 (sin crash).
3. The engine SHALL mantener el binding **reactivo** de `name` (005): cambiar la variable recarga la
   imagen local.

## Requisitos no funcionales
- **Multiplataforma:** `ImageRegistry` + `LocalImageRegistry` + el modo local del renderer en `commonMain`
  de `:sdui-compose`; el `Painter` se crea con APIs Compose (p.ej. `painterResource`), sin librerías de red.
- **Frontera del motor:** el motor no empaqueta assets; los `Painter` los aporta la app (sus recursos).
- **Sin números mágicos:** tamaño/forma desde `UiModifier`/`KuisdTheme` (008).
- **Accesibilidad:** `contentDescription` se propaga; decorativa → `null` (igual que 012).

## Dependencias y supuestos
- Depende de: 004 (`ComponentRegistry`/OCP), 005 (binding reactivo), 008 (`IconRegistry` como patrón +
  `UiModifier`), 012 (`image` + `ImageProps`; se extiende, no se duplica).
- **Decisión tomada — API: reutilizar `image` con prop `name`.** Un solo componente; `name`→local,
  `url`→remoto (012). (Descartado: componente `localImage` nuevo — duplicaría props y catálogo.)
- **Decisión tomada — valor del registry: `@Composable () -> Painter` (factory).** Porque
  `painterResource` es `@Composable`. (Descartado: `Painter` directo — menos idiomático en CMP.)
- **Decisión tomada — incluir asset de demo.** Se empaqueta una imagen real en Compose Resources de
  `:shared`, se registra en el `ImageRegistry` y se sirve un `image(name=…)` en una pantalla piloto
  (asset concreto y ubicación se fijan en §tasks).
- **Supuesto:** Compose Multiplatform Resources (`org.jetbrains.compose.components.resources`) disponible
  para empaquetar/resolver el asset de demo en los targets del proyecto.
