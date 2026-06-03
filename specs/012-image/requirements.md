# Requisitos — `image` (carga de imágenes remotas)

> Spec ID: 012 · Estado: draft · Fecha: 2026-06-03

## Resumen
El catálogo del motor no puede mostrar imágenes de red: el server solo dispone de `icon` (vectores del
`IconRegistry`, 008). Esta spec añade un componente **`image`** que muestra una imagen por **URL**,
con `contentScale`, descripción de accesibilidad y la apariencia del `UiModifier` (tamaño/forma, 008).

Para respetar la **frontera del motor** (el motor renderiza + delega; fetch/estado son de la app — como
navegación en 003, red en 009 y overlays en 011), el motor **no** hace la descarga ni se acopla a una
librería de imágenes concreta: define un **seam** (`LocalAsyncImage`, mismo patrón que
`LocalIconRegistry`/`LocalComponentRegistry`/`LocalSduiActionHandler`) que la app provee. La app
(`:shared`) implementa ese seam con una librería KMP (propuesta: **Coil 3** reutilizando el cliente
**Ktor 3** ya presente) y la cablea en `SduiHost`. Así `:sdui-compose` queda **agnóstico al loader**.

## Fuera de alcance
- **Imágenes locales por nombre / recursos empाquetados** (`painterResource`, assets, drawables) — v1
  solo URL remota; un `ImageRegistry` análogo al `IconRegistry` puede ser follow-up.
- **`Image` de bytes/base64 embebidos** en el envelope — fuera (infla el payload).
- **Transformaciones avanzadas** (blur, círculo con borde, crossfade configurable, filtros) más allá de
  `contentScale` + forma del `UiModifier` (008) — fuera; defaults del loader.
- **GIF / vídeo / SVG remoto / imágenes animadas** — fuera de v1 (Coil los soporta vía add-ons; se deja
  para una spec posterior si se necesita).
- **Caché/precarga configurable desde el envelope** — la política de caché la decide el loader de la app,
  no el contrato SDUI.
- **Placeholder/error como subárboles SDUI arbitrarios** — v1: estados básicos (indicador de carga y un
  fallback visual); componer placeholders ricos desde el envelope queda fuera.

## Historias de usuario y criterios de aceptación

### HU-1 — `image`: mostrar una imagen por URL
**Como** autor de pantallas **quiero** declarar una imagen por su URL **para** mostrar contenido visual
remoto (avatares, miniaturas, banners).

Criterios (EARS):
1. The engine SHALL registrar en `CorePack` un componente `image` con prop `url` (string **bindable**,
   regla 005) y renderizar la imagen cargada por el seam de la app.
2. The engine SHALL aplicar el `UiModifier` del nodo (tamaño/forma/clip, 008) al composable de la imagen.
3. The engine SHALL exponer una prop `contentScale` (string: `crop`|`fit`|`fillBounds`|`inside`|`none`,
   default `fit`) mapeada al `ContentScale` de Compose.
4. The engine SHALL exponer `contentDescription` (string opcional, bindable) para accesibilidad; ausente
   ⇒ `null` (imagen decorativa).
5. WHILE la imagen se está cargando, the engine SHALL mostrar un estado de carga (indicador básico).
6. IF la URL es inválida/vacía o la carga falla, THEN the engine SHALL mostrar un fallback visual (sin
   crash) en lugar de la imagen.

### HU-2 — Seam de carga agnóstico (`LocalAsyncImage`)
**Como** integrador **quiero** que el motor no imponga una librería de imágenes **para** elegir/cambiar
el loader (Coil, Kamel, propio) sin tocar el motor.

Criterios (EARS):
1. The engine SHALL definir un seam `LocalAsyncImage` (CompositionLocal) que provee un `@Composable` que
   recibe (`url`, `contentDescription`, `contentScale`, `modifier`) y pinta la imagen + estados de
   carga/error.
2. The engine SHALL proveer un **default** del seam que degrada con elegancia (p.ej. un placeholder/box
   neutro) para poder usar `image` sin host (tests, previews) sin crash.
3. The app SHALL proveer una implementación del seam (Coil 3 + Ktor) por `LocalAsyncImage` en `SduiHost`,
   sin que el motor dependa de la librería.
4. The engine (`:sdui-compose`) SHALL NO depender de Coil/Kamel ni de ninguna librería de red.

### HU-3 — Resiliencia y catálogo
**Como** integrador **quiero** que `image` degrade con elegancia **para** no romper la pantalla.

Criterios (EARS):
1. The engine SHALL exponer `image` vía `CorePack` (OCP, 004), resoluble por `rendererFor("image")`.
2. IF el seam no está provisto (motor aislado), THEN `image` SHALL usar el default de HU-2.2 (sin crash).
3. The engine SHALL mantener el binding **reactivo** de `url` (005): cambiar la variable recarga la imagen.

## Requisitos no funcionales
- **Multiplataforma:** el componente `image` y el seam en `commonMain` de `:sdui-compose`; la
  implementación Coil en `commonMain` de `:shared` (con los artefactos de plataforma que Coil requiera).
- **Frontera del motor:** `:sdui-compose` agnóstico al loader; la descarga/caché viven en la app.
- **Sin números mágicos:** tamaño/forma desde `UiModifier`/`KuisdTheme` (008); sin `dp` literales propios.
- **Accesibilidad:** `contentDescription` se propaga; imágenes decorativas → `null`.

## Dependencias y supuestos
- Depende de: 004 (`ComponentRegistry`/OCP), 005 (binding `$var` reactivo), 008 (`UiModifier` aplicado +
  forma/tamaño por token). Reutiliza el patrón de seam de `IconRegistry` (008).
- **Decisión tomada — arquitectura: seam.** El motor define `LocalAsyncImage` y queda **agnóstico al
  loader**; `:sdui-compose` no depende de ninguna librería de imágenes. (Descartado: Coil directo en el
  motor — acopla `:sdui-compose` y mete red en el motor, rompe la frontera.)
- **Decisión tomada — librería: Coil 3 + Ktor.** La app implementa el seam con `coil-compose` +
  `coil-network-ktor3`, reutilizando el cliente Ktor 3.5 ya presente. (Descartado: Kamel.) Versión y
  artefactos por plataforma (android/ios/desktop) se fijan en §design.
- **Supuesto:** pantalla piloto del server para e2e (candidata: añadir un `image` a `home`/`feed` con una
  URL pública estable); se elige en §tasks.
- **Supuesto:** Coil 3 es compatible con Compose Multiplatform 1.11.0 / Kotlin 2.3.21 en los targets del
  proyecto (android, iosArm64, iosSimulatorArm64, desktop/jvm). A verificar al fijar la versión.
