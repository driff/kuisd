# Requisitos — Modificadores de scope por-hijo en Row/Column (weight + alignment)

> Spec ID: 019 · Estado: draft · Fecha: 2026-06-04

## Resumen
El motor (`:sdui-compose`) hoy NO aplica los modificadores que dependen del scope del contenedor:
`UiModifierResolver` ignora `weight` ("requiere RowScope/ColumnScope") y `RenderScope.renderChildren()`
(`children.forEach { RenderNode(it) }`) no aplica `Modifier.align` por-hijo (solo el `horizontalAlignment`/
`verticalAlignment` del contenedor). Esta feature de **motor** hace que un hijo de `Row`/`Column` pueda llevar
su propio `weight` y su propio `alignment` (override estilo Compose), habilitando layouts reales. Es la pieza
separada de la spec 017 (que es builder-only). Tras 019, exponer estos campos en el inspector del builder es
un añadido menor de catálogo.

## Fuera de alcance (provisional — refinar al activar la spec)
- UI del builder para editar `weight`/align por-hijo (catálogo/inspector): seguimiento ligero tras 019, o se
  incluye al final como tarea de exposición.
- Otros modificadores de scope (p. ej. `Box` `align`, `matchParentSize`): solo Row/Column en v1.

## Historias de usuario y criterios de aceptación (borrador)

### HU-1 — weight por-hijo en Row/Column
1. WHEN un hijo de `Row`/`Column` tiene `UiModifier.weight > 0`, the system SHALL aplicar
   `Modifier.weight(...)` a ESE hijo dentro del scope correspondiente.
2. IF `weight` es null o ≤ 0, THEN the system SHALL no aplicar weight (comportamiento actual, sin regresión).

### HU-2 — alignment por-hijo (override del contenedor)
1. WHEN un hijo de `Column` tiene `alignment` horizontal, the system SHALL aplicar `Modifier.align(...)` a ese
   hijo, sobreescribiendo el `horizontalAlignment` del `Column` solo para él.
2. WHEN un hijo de `Row` tiene `alignment` vertical, the system SHALL aplicar `Modifier.align(...)`.
3. The system SHALL combinar correctamente `fillMaxWidth`/`weight` y `alignment` en el mismo hijo.
4. IF un hijo no define `alignment`, THEN the system SHALL usar la alineación del contenedor (sin regresión).

## Requisitos no funcionales (borrador)
- Cambio en **`:sdui-compose`** (render); `:sdui-core` sin cambios (UiModifier ya tiene los campos).
- **Sin regresión**: blueprints existentes renderizan igual.
- El diseño debe resolver cómo inyectar modificadores de scope por-hijo sin romper `fillMaxWidth` (p. ej.
  threading de un modifier de scope en `RenderNode`/`RenderScope`, evitando envolver en `Box` que rompa el fill).
- Tests del motor en verde; lint/detekt/ktlint.

## Dependencias y supuestos
- Spec 017 (builder) entregada o en curso; 019 la complementa habilitando weight/align por-hijo.
- `UiModifierResolver` (aplica fillMaxWidth/padding/…); `RenderScope.renderChildren()`; renderers de
  `Column`/`Row` en `CorePack.kt`.
