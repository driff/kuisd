# Requisitos — Cargar/guardar a archivo en el :builder

> Spec ID: 015 · Estado: approved · Fecha: 2026-06-04

## Resumen
El builder (spec 014) hoy solo **exporta** el árbol a un panel de texto (un `SduiEnvelope` JSON que
re-parsea). No sabe **abrir** un envelope existente ni **persistir** el trabajo a disco: cerrar la app
pierde el diseño. Esta feature añade **abrir** un `.json` desde disco al árbol del builder y **guardar**
el documento actual a un archivo, cerrando el round-trip ida-y-vuelta del export. Para quien diseña
blueprints SDUI en desktop, esto convierte el builder de una demo efímera en una herramienta con la que
se puede iterar sobre un archivo real.

## Fuera de alcance
- **Auto-guardado / guardado en segundo plano** — v1 guarda solo cuando el usuario lo pide.
- **Historial de archivos recientes / "abrir reciente"** — diferido.
- **Guardia de cambios sin guardar al CERRAR la ventana** — la confirmación cubre **Abrir** y **Nuevo**;
  interceptar el cierre de la app queda fuera de v1.
- **Edición del `screenId`, `variables` o `meta` del envelope por UI** — se **preservan** al hacer
  round-trip (load→save) pero no se editan en v1. (Posible follow-up.)
- **Validación/normalización del esquema más allá de parsear** — los `type` desconocidos ya degradan a
  `UnknownNode` en el preview sin crash (motor resiliente); el builder no los rechaza.
- **Multiplataforma:** sigue siendo **solo desktop** (JVM/Compose Desktop), como el resto del builder.
- **Undo/redo** sobre la operación de cargar (cargar reemplaza el documento).
- **Formatos distintos a JSON** (YAML, binario, etc.).

## Historias de usuario y criterios de aceptación

### HU-1 — Guardar el documento a un archivo
**Como** diseñador de blueprints **quiero** guardar el árbol actual a un archivo `.json`
**para** no perder mi trabajo al cerrar el builder y poder versionarlo/compartirlo.

Criterios (EARS):
1. The system SHALL ofrecer en la UI una acción **Guardar** que persista el documento actual como un
   `SduiEnvelope` JSON serializado con `DefaultSduiJson` (idéntico en forma al export actual).
2. WHEN el usuario invoca **Guardar como…**, the system SHALL abrir un diálogo nativo de selección de
   archivo para elegir ruta y nombre, proponiendo la extensión `.json`.
3. WHEN el usuario confirma una ruta en el diálogo, the system SHALL escribir el JSON a ese archivo
   (UTF-8) y recordar esa ruta como "archivo actual" del documento.
4. WHILE el documento tenga un "archivo actual" asociado (cargado o guardado antes), the system SHALL
   ofrecer **Guardar** que reescriba directamente ese archivo sin volver a preguntar la ruta, y
   **Guardar como…** para elegir una ruta nueva.
5. IF la escritura falla (permisos, ruta inválida, E/S), THEN the system SHALL mostrar un mensaje de
   error legible y dejar el documento en memoria intacto.
6. WHEN el usuario cancela el diálogo de archivo, the system SHALL no escribir nada y no alterar el
   estado del documento.

### HU-2 — Abrir un archivo existente
**Como** diseñador de blueprints **quiero** abrir un `SduiEnvelope` JSON existente
**para** seguir editándolo en el builder en vez de empezar de cero.

Criterios (EARS):
1. The system SHALL ofrecer en la UI una acción **Abrir** que cargue un archivo `.json` como documento
   del builder.
2. WHEN el usuario selecciona un archivo válido, the system SHALL parsearlo con `DefaultSduiJson` a un
   `SduiEnvelope`, reemplazar el árbol del documento por su `root`, y garantizar ids únicos en todo el
   árbol (invariante del builder: selección/borrado por id).
3. WHEN un archivo se carga correctamente, the system SHALL preservar `schemaVersion`, `screenId`,
   `variables` y `meta` del envelope de modo que un **Guardar** posterior los reproduzca (round-trip).
4. WHEN un archivo se carga correctamente, the system SHALL reflejar el árbol cargado en el preview y en
   el outline, y dejar una selección válida (la raíz).
5. IF el archivo no existe, no es JSON válido, o no es un `SduiEnvelope` parseable, THEN the system SHALL
   mostrar un mensaje de error legible y **conservar** el documento actual sin reemplazarlo.
6. WHEN el usuario cancela el diálogo de abrir, the system SHALL no alterar el documento actual.
7. WHILE el documento tenga cambios sin guardar, WHEN el usuario invoca **Abrir**, the system SHALL pedir
   confirmación antes de descartar el documento actual; IF el usuario cancela la confirmación, THEN the
   system SHALL no abrir el diálogo de archivo ni alterar el documento.

### HU-3 — Nuevo documento
**Como** diseñador **quiero** empezar un documento en blanco
**para** crear un blueprint nuevo sin reiniciar la app.

Criterios (EARS):
1. The system SHALL ofrecer en la UI una acción **Nuevo** que reemplace el documento por un árbol vacío
   (la columna raíz con id `root`), con la raíz seleccionada y sin "archivo actual" asociado.
2. WHILE el documento tenga cambios sin guardar, WHEN el usuario invoca **Nuevo**, the system SHALL pedir
   confirmación antes de descartar; IF el usuario cancela, THEN the system SHALL no alterar el documento.

### HU-4 — Estado de cambios sin guardar
**Como** diseñador **quiero** saber si tengo cambios sin guardar
**para** no perder trabajo por accidente al abrir otro archivo o empezar de cero.

Criterios (EARS):
1. The system SHALL marcar el documento como **modificado** cuando se inserta, borra o edita un nodo
   después de la última operación de cargar/guardar/nuevo.
2. WHEN el documento se guarda, se carga o se reinicia (**Nuevo**), the system SHALL marcarlo como **no
   modificado**.
3. The system SHALL indicar visualmente el estado modificado (p. ej. un marcador en el título o cabecera)
   y, si hay archivo actual, mostrar su ruta o nombre.

### HU-5 — Round-trip fiel
**Como** diseñador **quiero** que abrir y volver a guardar no corrompa ni pierda datos
**para** poder confiar en el builder sobre archivos reales (incluidos campos que el builder no edita).

Criterios (EARS):
1. WHEN un envelope se carga y se guarda sin más cambios, the system SHALL producir un envelope
   **semánticamente equivalente** (mismo árbol, `schemaVersion`, `screenId`, `variables`, `meta`) salvo
   la asignación determinista de ids a nodos que carecieran de id.
2. The system SHALL conservar `variables` y `meta` aunque el builder no los exponga para edición.

## Requisitos no funcionales
- **Solo desktop** (JVM/Compose Desktop); reutiliza `DefaultSduiJson` de `:sdui-core` (sin nuevos formatos
  ni parsers).
- **Sin cambios** en `:sdui-core`/`:sdui-compose`; el motor permanece intacto. `:shared` no cambia (o
  cambio mínimo justificado).
- **Resiliencia:** ningún error de E/S o de parseo debe cerrar la app; siempre se degrada a un mensaje.
- **Lógica de archivo testeable** sin abrir UI (E/S y serialización separadas del diálogo nativo).
- Lint/detekt/ktlint en verde para `:builder`.

## Dependencias y supuestos
- Spec 014 (builder) entregada: `BuilderDocument`, `TreeOps` (incluye `ensureUniqueTree`), `exportEnvelope`,
  UI split con paleta/outline/inspector/export.
- `SduiEnvelope` (`schemaVersion`, `screenId`, `root`, `variables`, `meta`) y `DefaultSduiJson` de
  `:sdui-core`.
- Diálogo de archivo: AWT/Swing disponible en Compose Desktop (JVM) — sin dependencias nuevas previstas.
- El preview ya degrada `type`s desconocidos a `UnknownNode`; no se requiere validación de tipos al cargar.
