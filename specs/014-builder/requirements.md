# Requisitos — Builder de blueprints SDUI (desktop)

> Spec ID: 014 · Estado: draft · Fecha: 2026-06-03

## Resumen
Una herramienta visual **de escritorio** para construir pantallas SDUI ("blueprints") sin escribir JSON
a mano, apalancando el contrato y el motor existentes:

- El documento editado **es** un árbol `SduiNode`/`SduiEnvelope` (`:sdui-core`, `@Serializable`).
- El **preview** es el motor real (`:sdui-compose` `RenderNode`) con los seams de la app (`:shared`:
  theme/iconos/imágenes/registry), así el preview es idéntico a producción.
- La salida es un **envelope JSON** real (el mismo que serviría el BFF).

UI: pantalla dividida — **izquierda preview**, **derecha paleta de componentes por categoría** — más un
**inspector de propiedades** del nodo seleccionado. Vive en un **módulo nuevo `:builder`** (Compose
Desktop), que **consume** `:sdui-compose`/`:shared`/`:sdui-core` sin modificar el motor ni el contrato.

## Fuera de alcance
- **Android/iOS/web:** v1 es **solo desktop** (JVM/Compose Desktop).
- **Cargar/guardar a archivo** (abrir un envelope existente, persistir a disco) — diferido; v1 exporta a
  un panel/portapapeles. (Posible follow-up.)
- **Drag & drop** para reordenar/anidar y **undo/redo** — v1: añadir (append), seleccionar, borrar.
- **Ejecutar acciones** en el preview (navigate/fireEndpoint/overlays reales) — el handler del preview es
  **no-op + log** a un panel; el builder no navega ni hace red.
- **Editar `actions`/eventos** de los nodos (onClick→navigate, etc.) por UI — v1 edita **props** y unos
  pocos campos de `UiModifier`; las acciones quedan vacías o por defecto. (Follow-up.)
- **Multi-pantalla / navegación entre blueprints** — v1 edita **una** pantalla a la vez.
- **Componentes que requieren estado/host vivo** (textField two-way real, bottomBar selección reactiva)
  se renderizan, pero su interacción dinámica no se ejercita en el preview (es una vista de diseño).
- **Introspección automática del esquema de props** desde el `ComponentRegistry` — v1 usa un **catálogo
  de descriptores hecho a mano** (paleta + campos editables).

## Historias de usuario y criterios de aceptación

### HU-1 — App de escritorio con pantalla dividida
**Como** diseñador de pantallas **quiero** una app de escritorio con preview a la izquierda y paleta a la
derecha **para** componer una pantalla visualmente.

Criterios (EARS):
1. The system SHALL ser una app **Compose Desktop** en el módulo nuevo `:builder`, ejecutable con
   `./gradlew :builder:run`.
2. The builder SHALL mostrar un layout con **preview** (izquierda) y, a la **derecha**, la **paleta** por
   categoría, un **outline** (árbol de nodos) y el **inspector** de propiedades; más una acción de
   **exportar**. (La disposición exacta de paleta/outline/inspector en la columna derecha se concreta en
   §design.)
3. The builder SHALL depender de `:sdui-compose` (motor), `:shared` (seams de la app) y `:sdui-core`
   (contrato), **sin modificar** esos módulos.

### HU-2 — Paleta de componentes por categoría
**Como** diseñador **quiero** una paleta agrupada por categoría **para** encontrar y añadir componentes.
Criterios (EARS):
1. The builder SHALL exponer un **catálogo curado** de entradas de paleta (subconjunto representativo de
   CorePack: p.ej. `text`, `button`, `column`, `row`, `card`, `surface`, `divider`, `spacer`, `image`,
   `icon`, `scaffold`/`topAppBar`; ampliable añadiendo descriptores), cada una con `type`, `categoría`,
   etiqueta legible y un **nodo plantilla** (`SduiNode` con props por defecto válidas).
2. The builder SHALL agrupar la paleta por categoría (p.ej. Texto, Contenedores, Estructura, Media).
3. WHEN el usuario activa una entrada de paleta, the builder SHALL **insertar** su nodo plantilla en el
   árbol del documento: como hijo del nodo **contenedor seleccionado** si lo hay y admite hijos, o si no
   al final de la raíz.
4. IF el nodo seleccionado no es un contenedor, THEN the builder SHALL insertar a nivel de la raíz
   (comportamiento determinista, documentado).

### HU-3 — Selección y borrado de nodos
**Como** diseñador **quiero** seleccionar un nodo y borrarlo **para** corregir la composición.
Criterios (EARS):
1. The builder SHALL permitir **seleccionar** un nodo del documento vía el **panel outline** (árbol de
   nodos) y resaltar la selección. (Selección por click en el preview = follow-up, fuera de v1.)
2. WHEN el usuario borra el nodo seleccionado, the builder SHALL quitarlo del árbol (con sus hijos) y
   actualizar el preview; la raíz no se borra (queda al menos el contenedor raíz).
3. The builder SHALL mantener una **identidad estable** de nodos (p.ej. `id` autogenerado) para
   selección y edición deterministas.

### HU-4 — Inspector de propiedades
**Como** diseñador **quiero** editar las propiedades del nodo seleccionado **para** ajustar su contenido
y apariencia sin tocar JSON.
Criterios (EARS):
1. The builder SHALL mostrar un **inspector** con los campos editables del nodo seleccionado, derivados
   del **descriptor** de su `type` (catálogo HU-2.1): cada campo con su clave, tipo de editor (texto,
   booleano, enum/token) y opciones.
2. WHEN el usuario cambia un campo, the builder SHALL actualizar las `props` (y/o `UiModifier`) del nodo
   en el documento y **recomponer el preview** en vivo.
3. The builder SHALL soportar al menos editores de **texto** (string), **enum/opciones** (p.ej.
   `contentScale`, tokens de estilo) y **booleano**; los campos no soportados se omiten sin romper.
4. IF un valor introducido es inválido para el tipo, THEN the builder SHALL ignorarlo/normalizarlo sin
   crash (el documento permanece serializable).

### HU-5 — Preview con el motor real
**Como** diseñador **quiero** ver la pantalla como en la app **para** confiar en el resultado.
Criterios (EARS):
1. The builder SHALL renderizar el documento con `RenderNode` bajo los `CompositionLocals` reales de la
   app (`LocalComponentRegistry`/`LocalKuisdTheme`/`LocalIconRegistry`/`LocalImageRegistry`/
   `LocalVariables`) reutilizados de `:shared`.
2. The builder SHALL proveer un `LocalSduiActionHandler` **no-op que loguea** las acciones a un panel
   (sin navegar ni hacer red) — el preview es una vista de diseño.
3. WHEN el documento cambia (añadir/borrar/editar), the builder SHALL reflejarlo en el preview de forma
   reactiva.
4. IF un nodo tiene un `type` no registrado, THEN el preview SHALL mostrar el `UnknownNode` del motor
   (resiliencia 004), sin crash.

### HU-6 — Exportar el blueprint a JSON
**Como** diseñador **quiero** exportar el árbol a JSON **para** servirlo desde el BFF o versionarlo.
Criterios (EARS):
1. WHEN el usuario exporta, the builder SHALL serializar el documento a un `SduiEnvelope` JSON con
   `DefaultSduiJson` (`:sdui-core`), idéntico en formato al que sirve el server.
2. The builder SHALL mostrar el JSON resultante (panel y/o copiar al portapapeles).
3. The exported JSON SHALL **re-parsear** a un `SduiEnvelope` equivalente (round-trip válido).

## Requisitos no funcionales
- **Solo desktop:** módulo `:builder` JVM/Compose Desktop; sin targets móviles.
- **No invasivo:** `:sdui-core`/`:sdui-compose`/`:shared` no se modifican; el builder solo los consume
  (si hace falta exponer algo de `:shared`, se hará mínimo y se justifica en §design).
- **Determinismo/serializabilidad:** el documento siempre es un `SduiNode` válido y serializable.
- **Catálogo extensible:** añadir un componente a la paleta = añadir un descriptor (sin tocar el resto).

## Dependencias y supuestos
- Depende de: 004 (`ComponentRegistry`), 005/008/010/012/013 (componentes a ofrecer en la paleta),
  y de los seams/registries que expone `:shared` (`appRegistry`, `rememberAppTheme`, iconos/imágenes).
- **Decisión tomada:** módulo **nuevo `:builder`** (Compose Desktop), no dentro de `:desktopApp`.
- **Decisión tomada:** MVP = paleta por categoría + preview en vivo + añadir/seleccionar/borrar +
  **inspector de propiedades** + exportar JSON. (Cargar/guardar a archivo y editar `actions`: follow-up.)
- **Decisión tomada:** selección vía **panel outline** (árbol); click-en-preview es follow-up.
- **Decisión tomada:** paleta = **subconjunto curado** de CorePack (ampliable por descriptores).
- **Decisión abierta (a confirmar en §design): cómo se reutilizan los seams de `:shared`.** El `SduiHost`
  actual los provee internamente para su flujo de navegación; el builder necesita los **registries/tema**
  pero con su **propio** handler de acciones (no-op). Se evaluará exponer una función de `:shared`
  (`rememberAppSeams()` o similar) o que el builder componga `appRegistry`/`rememberAppTheme`/iconos/
  imágenes directamente (ya son públicos/accesibles).
- **Supuesto:** Compose Desktop (`org.jetbrains.compose`, ya en uso) disponible para un módulo de
  aplicación JVM con `application {}`.
