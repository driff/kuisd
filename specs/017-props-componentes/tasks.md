# Tareas — Editar más props de componentes en el inspector (builder-only)

> Spec ID: 017 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — `catalog/ModifierFields.kt` (puro): `KEY_ALIGNMENT`/`KEY_PADDING`, `modifierEnumValue`,
  `withModifierEnum`, `paddingAll`, `optionLabel`.
  - _ref:_ HU-1 · HU-4 · design §ModifierFields
  - _verif:_ `ModifierFieldsTest`: set/clear de alignment; padding a 4 lados con `space.md` y su lectura por `l`;
    `optionLabel("alignment.centerH") == "centerH"`.

- [x] **T2** — `BuilderCatalog`: añadir `FieldSpec` (target Modifier) — `fillMaxWidth`+`padding` a text/button/
  image; `alignment`(horizontal)+`padding` a column; `alignment`(vertical)+`fillMaxWidth`+`padding` a row;
  listas de opciones (`alignHorizontalOptions`/`alignVerticalOptions`/`spaceOptions`).
  - _ref:_ HU-2 · HU-3 · HU-4 · design §BuilderCatalog
  - _verif:_ `BuilderCatalogTest` (amplía): text/button/image tienen `fillMaxWidth` y `padding`; column tiene
    `alignment` con opciones horizontales; row con verticales; sin key vacía (ya cubierto).

- [x] **T3** — `InspectorPane`: `EnumField` target-aware (Prop|Modifier) + opción "(ninguno)" para limpiar;
  `FieldRow` pasa `onModifier` al Enum.
  - _ref:_ HU-1 · design §InspectorPane
  - _verif:_ compila; smoke `:builder:run` (alignment de una column mueve su contenido; fillMaxWidth ensancha
    un botón; padding separa; "(ninguno)" limpia).

## Verificación final (Definition of Done)
- [x] `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- [x] `:sdui-core`/`:sdui-compose`/`:shared` sin cambios (solo `:builder`).
- [ ] Smoke `:builder:run`: fillMaxWidth (hoja), alignment (column/row), padding editables y reflejados en el
  preview; limpiar un campo lo vuelve a sin-asignar; sin regresión en props existentes.
