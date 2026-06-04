# Tareas — Modificadores de scope por-hijo en Row/Column

> Spec ID: 019 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.
Orden: motor primero (T1-T2), luego builder (T3-T5).

- [x] **T1** — Motor: threading de `layoutModifier`. `RenderNode(node, layoutModifier = Modifier)`;
  `RegisteredComponent.Render(node, layoutModifier = Modifier)`; `RenderScope(node, layoutModifier)` con
  `modifier = layoutModifier.then(own)`.
  - _ref:_ HU-1 · HU-2 · design §Threading
  - _verif:_ `:sdui-compose` compila; tests existentes del motor en verde (no-regresión: default Modifier ⇒
    render idéntico).

- [x] **T2** — Motor: `ScopeChildren.kt` (`ColumnScope.childLayout`/`RowScope.childLayout` con weight>0 +
  align del eje); Column/Row renderers iteran hijos con `RenderNode(child, childLayout(child.modifier))`.
  - _ref:_ HU-1 · HU-2 · design §ScopeChildren · §Column/Row
  - _verif:_ `:sdui-compose` compila + lint; smoke en `:builder:run` (un hijo con weight reparte; un hijo con
    alignment se alinea distinto). Tests de motor existentes verdes.

- [x] **T3** — Builder: `TreeOps.findParent(root, id)` + `BuilderDocument.parentType(id)`.
  - _ref:_ HU-3 · design §TreeOps/parentType
  - _verif:_ `TreeOpsTest`: `findParent` devuelve el padre correcto y null para la raíz. `BuilderDocumentTest`:
    `parentType` de un hijo de column == "column"; de la raíz == null.

- [x] **T4** — Builder: `ModifierFields` — `KEY_WEIGHT`, `modifierWeight`/`withModifierWeight` (Text↔Float,
  inválido/≤0 limpia) + `alignHorizontalRefs`/`alignVerticalRefs` (reutilizadas por el catálogo 017).
  - _ref:_ HU-3 · HU-4 · design §ModifierFields
  - _verif:_ `ModifierFieldsTest` (amplía): `withModifierWeight("2")`==2f; `"0"`/`"abc"`/`""` limpian;
    `modifierWeight` lee.

- [x] **T5** — Builder: `InspectorPane` sección contextual "Layout en el contenedor" (alignment con eje del
  padre + `WeightField` numérico) cuando el nodo es hijo de column/row; `BuilderApp`/`RightColumn` pasa
  `parentType` al inspector.
  - _ref:_ HU-3 · HU-4 · design §InspectorPane · §BuilderApp
  - _verif:_ compila + lint; smoke `:builder:run` (hijo de Column → alignment horizontal + weight; hijo de Row
    → alignment vertical; raíz/no-hijo → sin sección).

## Verificación final (Definition of Done)
- [x] `./gradlew :sdui-compose:detekt :sdui-compose:ktlintCheck :builder:test :builder:detekt :builder:ktlintCheck` en verde + tests del motor (`:sdui-compose`) en verde.
- [x] `:sdui-core`/`:shared` sin cambios.
- [x] **Sin regresión** del motor (blueprints sin weight/align por-hijo renderizan igual).
- [ ] Smoke `:builder:run`: weight reparte; alignment por-hijo sobreescribe al contenedor; eje correcto según
  el padre; editar no afecta a los hermanos.
