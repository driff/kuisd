# Requisitos — Editar más props de componentes en el inspector (builder-only)

> Spec ID: 017 · Estado: approved · Fecha: 2026-06-04

## Resumen
El inspector del builder (specs 014/016) solo expone unos pocos campos por componente y, para `UiModifier`,
únicamente edita booleanos (`fillMaxWidth`/`fillMaxHeight`). Esta feature amplía el inspector para editar más
propiedades de layout **que el motor ya aplica hoy**: `fillMaxWidth` en hojas, `alignment` del contenedor
(Columna/Fila) y `padding`. La pieza nueva es un **editor Enum sobre `UiModifier`** (hoy `FieldTarget.Modifier`
solo soporta Bool). Es **solo `:builder`**: el motor y `:sdui-core` no cambian.

> Nota de partición: el `alignment` **por-hijo** (override estilo Compose `Modifier.align`) y `weight`
> requieren un cambio de **motor** (`UiModifierResolver` ignora `weight` y `renderChildren` no aplica align
> por-hijo: ambos necesitan `RowScope`/`ColumnScope`). Se separan a la **spec 019** y quedan FUERA de 017.

## Fuera de alcance
- **`alignment` por-hijo** (un hijo con alineación distinta a la del contenedor): necesita motor → **spec 019**.
- **`weight`**: el motor lo ignora hoy (necesita `RowScope`/`ColumnScope`) → **spec 019**.
- **`width`/`height`/`background`/`cornerRadius`/`elevation`**: follow-up.
- **`padding` por-lado** (l/t/r/b independientes): v1 aplica un único token de espacio a los 4 lados.
- **`:sdui-core`/`:sdui-compose`**: sin cambios; `UiModifier` ya tiene los campos y el motor ya aplica
  `fillMaxWidth`, `padding` y el `alignment` del contenedor.
- Multiplataforma: solo desktop.

## Historias de usuario y criterios de aceptación

### HU-1 — Editor Enum sobre UiModifier
**Como** mantenedor del builder **quiero** un editor de campo Enum cuyo `target` sea `Modifier`
**para** exponer propiedades de modificador no booleanas (como `alignment` o `padding`) de forma declarativa.

Criterios (EARS):
1. The system SHALL permitir declarar un `FieldSpec` con `editor = Enum` y `target = Modifier` que lea/escriba
   un campo del `UiModifier` del nodo seleccionado (no sus `props`).
2. WHEN el usuario elige una opción, the system SHALL actualizar ese campo del `UiModifier` (vía
   `updateModifier`) preservando el resto del modificador.
3. WHEN el campo no tiene valor, the system SHALL mostrar "(elegir)" y no escribir hasta que el usuario
   seleccione; y SHALL ofrecer una opción para **limpiar** el valor (volver a sin-asignar).
4. The system SHALL mapear las opciones del Enum a/desde el valor real del `UiModifier` (p. ej. token de
   `AlignmentToken` o de espacio), sin exponer literales crudos incorrectos.

### HU-2 — fillMaxWidth en hojas (Texto, Botón, Imagen)
**Como** diseñador **quiero** marcar `fillMaxWidth` en componentes hoja
**para** que ocupen el ancho disponible.

Criterios (EARS):
1. The system SHALL exponer un campo `fillMaxWidth` (Bool, target Modifier) en al menos `text`, `button`
   e `image`.
2. WHEN el usuario activa `fillMaxWidth`, the system SHALL reflejarlo en el preview, reutilizando el soporte
   ya existente del motor.

### HU-3 — alignment del contenedor (Columna/Fila)
**Como** diseñador **quiero** elegir la alineación por defecto de un contenedor
**para** alinear su contenido (start/center/end).

Criterios (EARS):
1. The system SHALL exponer un campo `alignment` (Enum, target Modifier) en `column` y `row`.
2. WHEN el contenedor es `column`, the system SHALL ofrecer alineación horizontal (start / center / end) →
   `AlignmentToken` (`Start`/`CenterHorizontally`/`End`).
3. WHEN el contenedor es `row`, the system SHALL ofrecer alineación vertical (top / center / bottom) →
   `AlignmentToken` (`Top`/`CenterVertically`/`Bottom`).
4. WHEN el usuario cambia la alineación, the system SHALL reflejarlo en el preview (el motor ya aplica
   `horizontalAlignment`/`verticalAlignment` desde el `modifier.alignment` del contenedor).

### HU-4 — padding
**Como** diseñador **quiero** dar margen interno a un componente
**para** separarlo de sus vecinos/bordes.

Criterios (EARS):
1. The system SHALL exponer `padding` (Enum de tokens de espacio, target Modifier) en los componentes
   principales del catálogo, aplicado a los 4 lados.
2. WHEN el usuario elige un token de padding, the system SHALL reflejarlo en el preview (el motor ya aplica
   `UiModifier.padding`).
3. The system SHALL permitir limpiar el padding (volver a sin-asignar) vía la opción de limpiar de HU-1.3.

## Requisitos no funcionales
- **Solo `:builder`**, sin cambios en el motor ni en `:sdui-core`; sin dependencias nuevas.
- **Data-driven**: las props se declaran como `FieldSpec` en el catálogo; el inspector las renderiza por su
  `editor`/`target` sin casos especiales por `type`.
- Resiliencia: un valor de modificador desconocido/ vacío no rompe el inspector ni el preview.
- Lint/detekt/ktlint en verde para `:builder`.

## Dependencias y supuestos
- Specs 014/016: `BuilderCatalog` (`FieldSpec`/`FieldEditor`/`FieldTarget`), `InspectorPane`
  (editores Text/Bool/Enum; Bool-sobre-modifier para `fillMaxWidth`/`fillMaxHeight`), `BuilderDocument.updateModifier`.
- `UiModifier` (`:sdui-core`) ya tiene `fillMaxWidth`, `alignment: AlignmentToken?`, `padding`.
- Motor: `UiModifierResolver` ya aplica `fillMaxWidth` y `padding`; `RenderScope` aplica el `alignment` del
  contenedor. (`weight` y align por-hijo NO se aplican hoy → spec 019.)
- Tokens de espacio disponibles para padding: `Tokens.Space.*` (mismos que ya usa el catálogo, p. ej. spacer).

## Decisiones tomadas
1. **Partición aprobada**: 017 = builder-only (`fillMaxWidth` + `alignment` de contenedor + `padding`). El
   `alignment` por-hijo **y** `weight` van a la **spec 019 (motor)**, porque ambos requieren `RowScope`/
   `ColumnScope` que el motor aún no inyecta por-hijo.
2. **padding**: token único de espacio aplicado a los 4 lados (per-lado, follow-up).
