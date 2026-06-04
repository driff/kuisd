# Requisitos — Modificadores de scope por-hijo en Row/Column (weight + alignment)

> Spec ID: 019 · Estado: approved · Fecha: 2026-06-04

## Resumen
El motor (`:sdui-compose`) hoy NO aplica los modificadores que dependen del scope del contenedor:
`UiModifierResolver` ignora `weight` ("requiere RowScope/ColumnScope") y `RenderScope.renderChildren()`
(`children.forEach { RenderNode(it) }`) no aplica `Modifier.align` por-hijo (solo el `horizontalAlignment`/
`verticalAlignment` del contenedor). Esta feature hace que un hijo de `Row`/`Column` lleve su propio `weight`
y su propio `alignment` (override estilo Compose `Modifier.align`), **y** lo expone en el inspector del
builder. Es la pieza separada de la 017. Abarca **`:sdui-compose`** (aplicar los modifiers de scope por-hijo)
y **`:builder`** (editar `alignment` por-hijo con el eje del contenedor padre + `weight` numérico).
`:sdui-core` no cambia (`UiModifier` ya tiene `weight: Float?` y `alignment: AlignmentToken?`).

## Fuera de alcance
- **`LazyColumn`/`LazyRow`**: solo `Row`/`Column` eager en v1 (las lazy no soportan `weight` de hijo igual).
- **Otros modifiers de scope** (`Box` align, `matchParentSize`, `weight(fill=false)`): v1 = weight (fill por
  defecto) + align en Row/Column.
- **Edición de weight por-lado/avanzada**: weight es un único `Float`.
- **`:sdui-core`**: sin cambios.

## Historias de usuario y criterios de aceptación

### HU-1 — weight por-hijo en Row/Column (motor)
**Como** autor de blueprints **quiero** que un hijo reparta espacio con `weight`
**para** layouts proporcionales (p. ej. dos botones 1:1, un texto que ocupa el resto).

Criterios (EARS):
1. WHEN un hijo directo de `Column`/`Row` tiene `UiModifier.weight` con valor > 0, the system SHALL aplicar
   `Modifier.weight(valor)` a ESE hijo dentro del scope del contenedor.
2. IF `weight` es null o ≤ 0, THEN the system SHALL no aplicar weight a ese hijo (sin regresión).
3. The system SHALL combinar `weight` con el resto del `UiModifier` del hijo (p. ej. `padding`, `background`)
   sin perderlos.

### HU-2 — alignment por-hijo que sobreescribe al contenedor (motor)
**Como** autor de blueprints **quiero** alinear un hijo distinto al resto
**para** colocar un componente concreto (como `Modifier.align` en Compose).

Criterios (EARS):
1. WHEN un hijo directo de `Column` tiene `alignment` resoluble a una alineación **horizontal**, the system
   SHALL aplicar `Modifier.align(esa)` a ese hijo, sobreescribiendo el `horizontalAlignment` del `Column`
   solo para él.
2. WHEN un hijo directo de `Row` tiene `alignment` resoluble a una alineación **vertical**, the system SHALL
   aplicar `Modifier.align(esa)` a ese hijo.
3. IF el `alignment` del hijo no aplica al eje del contenedor (p. ej. token horizontal en un `Row`) o es null,
   THEN the system SHALL usar la alineación del contenedor para ese hijo (sin regresión).
4. The system SHALL combinar correctamente `fillMaxWidth`/`weight` y `alignment` en el mismo hijo.

### HU-3 — Editar alignment por-hijo en el builder (con eje del padre)
**Como** diseñador **quiero** elegir la alineación de un componente dentro de su contenedor
**para** ajustarlo sin afectar a sus hermanos.

Criterios (EARS):
1. WHILE el nodo seleccionado es hijo de un `Column`, the system SHALL ofrecer en el inspector un campo
   `alignment` con opciones **horizontales** (start / center / end).
2. WHILE el nodo seleccionado es hijo de un `Row`, the system SHALL ofrecer `alignment` con opciones
   **verticales** (top / center / bottom).
3. IF el nodo seleccionado no es hijo de un `Row`/`Column` (p. ej. la raíz, o dentro de otro contenedor),
   THEN the system SHALL no ofrecer el campo de alignment por-hijo (no tendría efecto).
4. WHEN el usuario elige/limpia la alineación del hijo, the system SHALL actualizar su `UiModifier.alignment`
   (vía `updateModifier`) y reflejarlo en el preview.

### HU-4 — Editar weight en el builder
**Como** diseñador **quiero** fijar el `weight` de un hijo de Row/Column
**para** repartir el espacio.

Criterios (EARS):
1. WHILE el nodo seleccionado es hijo de un `Row`/`Column`, the system SHALL ofrecer un campo numérico
   `weight` (target Modifier).
2. WHEN el usuario introduce un número > 0, the system SHALL fijar `UiModifier.weight` y reflejarlo en el
   preview.
3. IF el valor es vacío, no numérico o ≤ 0, THEN the system SHALL limpiar/no escribir `weight` (no romper el
   inspector ni dejar un valor inválido).

## Requisitos no funcionales
- **`:sdui-compose` + `:builder`**; `:sdui-core` sin cambios. Sin dependencias nuevas.
- **Sin regresión**: blueprints sin weight/align por-hijo renderizan idénticos (HU-1.2/HU-2.3); el threading
  del modifier de scope es retrocompatible (default = `Modifier`, no-op) para todos los renderers existentes.
- **Resiliencia**: tokens/valores inválidos se ignoran (no crash).
- Tests del motor en verde (`:sdui-compose`); lint/detekt/ktlint en `:sdui-compose` y `:builder`.

## Dependencias y supuestos
- `RegisteredComponent.Render(node)` crea `RenderScope(node)` y el renderer usa `modifier` = `um.toModifier(theme)`.
- `RenderScope.renderChildren()` = `children.forEach { RenderNode(it) }`; Column/Row renderers usan
  `renderChildren()` dentro de su `ColumnScope`/`RowScope`.
- `modifier/toHorizontalAlignment`/`toVerticalAlignment` ya mapean `AlignmentToken` a `Alignment.Horizontal?`/
  `Alignment.Vertical?` (null si el token no aplica al eje).
- Builder: `BuilderDocument`/`TreeOps` permiten encontrar el padre de un nodo; `InspectorPane` + catálogo de
  la 017 (editores Enum-sobre-modifier; `ModifierFields`).

## Decisiones tomadas
1. **Alcance**: motor **+** edición en el builder (aprobado).
2. El **eje** del campo alignment por-hijo lo determina el **tipo del contenedor padre** (column→horizontal,
   row→vertical); el inspector lo resuelve en tiempo de render (no es un FieldSpec estático del catálogo).
3. `weight` se edita con un **editor numérico** (Text-sobre-modifier que parsea `Float`).
