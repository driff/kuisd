# Diseño — Editar más props de componentes en el inspector (builder-only)

> Spec ID: 017 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Ampliar el inspector del `:builder` para editar `fillMaxWidth` (hojas), `alignment` (contenedores) y
`padding`, **sin tocar el motor**: los tres ya los aplica `UiModifierResolver`/`RenderScope`. La única pieza
nueva es un **editor Enum cuyo `target` es `Modifier`** (hoy `FieldTarget.Modifier` solo se cablea para Bool).
Todo es **data-driven**: las props se declaran como `FieldSpec` en el catálogo y el inspector las renderiza
por `editor`/`target`, sin casos especiales por `type`. El mapeo Enum↔valor de `UiModifier` (token) se
centraliza en helpers puros, testeables sin Compose.

## Arquitectura
Módulo afectado: **solo `:builder`**. `:sdui-core`/`:sdui-compose`/`:shared` SIN cambios.

```
catalog/BuilderCatalog.kt   ── + FieldSpec de fillMaxWidth/alignment/padding en las entradas pertinentes
catalog/ModifierFields.kt   ── (NUEVO, puro) lectura/escritura de campos enum de UiModifier + opciones
ui/InspectorPane.kt         ── EnumField pasa a ser target-aware (Prop|Modifier) + opción "limpiar"
```

## Componentes y contratos

### ModifierFields — mapeo Enum ↔ UiModifier (puro, testeable)
- **Ubicación:** `catalog/ModifierFields.kt` (nuevo). Sin Compose.
- **Responsabilidad:** leer/escribir un campo enum del `UiModifier` por su `key`, y proveer la etiqueta
  legible. Único punto que conoce los tokens concretos (`AlignmentToken`, `SpaceToken`).
- **API (firmas):**
```kotlin
internal const val KEY_ALIGNMENT = "alignment"
internal const val KEY_PADDING = "padding"

/** Valor actual (ref del token) del campo enum [key] del modifier, o "" si no asignado. */
internal fun modifierEnumValue(modifier: UiModifier, key: String): String = when (key) {
    KEY_ALIGNMENT -> modifier.alignment?.ref.orEmpty()
    KEY_PADDING -> modifier.padding?.l?.ref.orEmpty() // v1: 4 lados iguales ⇒ se representa con el lado l
    else -> ""
}

/** Copia del modifier con [key] = [ref]; [ref] vacío ⇒ limpia el campo (null). */
internal fun withModifierEnum(modifier: UiModifier, key: String, ref: String): UiModifier = when (key) {
    KEY_ALIGNMENT -> modifier.copy(alignment = ref.ifEmpty { null }?.let(::AlignmentToken))
    KEY_PADDING -> modifier.copy(padding = ref.ifEmpty { null }?.let { paddingAll(SpaceToken(it)) })
    else -> modifier
}

/** PaddingTokens con el mismo token en los 4 lados (v1). */
internal fun paddingAll(s: SpaceToken): PaddingTokens = PaddingTokens(l = s, t = s, r = s, b = s)

/** Etiqueta corta para una opción (último segmento del ref): "alignment.start" → "start". */
internal fun optionLabel(ref: String): String = ref.substringAfterLast('.')
```
- **Decisiones:**
  - `padding` v1 = un token a los 4 lados → se lee/escribe vía el lado `l` (invariante: si se editó por la UI,
    los 4 lados coinciden). El per-lado es follow-up.
  - El valor vacío `""` representa "sin asignar" y limpia el campo (HU-1.3/HU-4.3).

### BuilderCatalog — nuevos FieldSpec (data-driven)
- **Ubicación:** `catalog/BuilderCatalog.kt` (amplía). Listas de opciones como `private val` (refs de token):
```kotlin
private val alignHorizontalOptions = listOf(Tokens.Alignment.Start.ref, Tokens.Alignment.CenterHorizontally.ref, Tokens.Alignment.End.ref)
private val alignVerticalOptions = listOf(Tokens.Alignment.Top.ref, Tokens.Alignment.CenterVertically.ref, Tokens.Alignment.Bottom.ref)
private val spaceOptions = listOf(Tokens.Space.Xs.ref, Tokens.Space.Sm.ref, Tokens.Space.Md.ref, Tokens.Space.Lg.ref, Tokens.Space.Xl.ref)
```
- **Campos añadidos** (todos `target = Modifier`):
  - `text`, `button`, `image`: `fillMaxWidth` (Bool) + `padding` (Enum, `spaceOptions`).
  - `column`: `alignment` (Enum, `alignHorizontalOptions`) + `padding`. (Ya tiene `fillMaxWidth`.)
  - `row`: `alignment` (Enum, `alignVerticalOptions`) + `fillMaxWidth` + `padding`.
  - `card`/`surface`/`scaffold`: `padding` (opcional; no rompe nada).
- **Decisión:** `column` usa opciones horizontales y `row` verticales → el carácter per-eje vive en el
  catálogo (cada entrada declara sus opciones), no en código.

### InspectorPane — EnumField target-aware + limpiar
- **Ubicación:** `ui/InspectorPane.kt` (amplía).
- **Cambios:**
  - `EnumField` recibe también `onModifier` y, según `field.target`, lee/escribe vía `props` (como hoy) o vía
    `modifierEnumValue`/`withModifierEnum` + `onModifier`. El menú muestra `optionLabel(ref)` y, además de las
    opciones, una entrada **"(ninguno)"** que escribe `""` (limpia).
  - `FieldRow` enruta `Enum` a `EnumField(node, field, onProps, onModifier)`.
  - `BoolField` ya soporta `target = Modifier` (fillMaxWidth) — sin cambios.
- **Contrato (firma actualizada):**
```kotlin
@Composable private fun EnumField(
    node: SduiNode, field: FieldSpec,
    onProps: (JsonObject) -> Unit, onModifier: (UiModifier) -> Unit,
)
```
- **Decisión:** no se introduce un tipo de editor nuevo; se generaliza el `Enum` existente a ambos targets,
  manteniendo Text/Bool igual. El `current` para modifier-enum muestra `optionLabel` o "(elegir)".

## Modelo de datos y estados
- Sin tipos de dominio nuevos: se usan `UiModifier`/`AlignmentToken`/`SpaceToken`/`PaddingTokens` de
  `:sdui-core`. La edición fluye por `BuilderDocument.updateModifier` (existente, marca `isModified`).

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| _(ninguna)_ | — | — | Solo catálogo + inspector del `:builder`. |

## Riesgos y mitigaciones
- **`Center` ambiguo**: usar `CenterHorizontally` (column) y `CenterVertically` (row), no `Center`, para que
  `toHorizontalAlignment`/`toVerticalAlignment` del motor lo resuelvan en el eje correcto. Test lo cubre.
- **padding per-lado vs único**: v1 escribe los 4 lados iguales; leer por `l` asume ese invariante. Si un
  archivo trae padding asimétrico, el inspector muestra el lado `l` y, al editar, lo iguala (documentado).
- **Etiqueta de opción**: `optionLabel` deriva del ref; si un ref no tuviera '.', devuelve el ref completo.
- **Regresión**: Text/Bool y los campos de props existentes no cambian (mismos tests 014/016).

## Estrategia de verificación
- **`ModifierFieldsTest`** (puro): `withModifierEnum(KEY_ALIGNMENT, "alignment.end")` setea `alignment`;
  `""` lo limpia; `KEY_PADDING` con `space.md` pone los 4 lados a `space.md` y `modifierEnumValue` lo lee;
  `optionLabel("alignment.centerH") == "centerH"`.
- **`BuilderCatalogTest`** (amplía): `text`/`button`/`image` tienen `fillMaxWidth` y `padding`; `column` tiene
  `alignment` con opciones horizontales; `row` con verticales.
- **`InspectorPane`**: compila; smoke `:builder:run` (alignment de una column mueve su contenido; fillMaxWidth
  ensancha un botón; padding separa).
- **Compilación/lint:** `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
