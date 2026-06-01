# Tareas — Contenedores + Theme + IconRegistry + `UiModifier` aplicado

> Spec ID: 008 · Trazabilidad: ./requirements.md · ./design.md

- [ ] **T0** — Aprobar `requirements.md` y `design.md` (cabecera `Estado: approved`); preguntas
  abiertas cerradas en design (weight ignorado, alignment tokenizado, icon/iconButton incluidos,
  override sin números mágicos).

## Contrato
- [ ] **T1** — `:sdui-core` · `DesignTokens.kt`: añadir
  `ElevationToken` + `Tokens.Elevation { None, Sm, Md, Lg }`.
- [ ] **T2** — `:sdui-core` · `DesignTokens.kt`: añadir
  `AlignmentToken` + `Tokens.Alignment { Start, Center, End, Top, Bottom, CenterHorizontally,
  CenterVertically }`.
- [ ] **T3** — `:sdui-core` · `DesignTokens.kt`: expandir
  `Tokens.Radius` con `Sm, Md, Lg, Xl` (preservar `None, Card, Pill`).
- [ ] **T4** — `:sdui-core` · `UiModifier.kt`: añadir
  `elevation: ElevationToken? = null` y cambiar
  `alignment: String? = null` → `alignment: AlignmentToken? = null`.
- [ ] **T5** — `:sdui-core` tests: decodificación
  `UiModifier` con y sin nuevos campos; `AlignmentToken("alignment.center")` decodifica desde
  string; el campo `alignment` mantiene el wire (string) tras el cambio de tipo.

## Motor — theme
- [ ] **T6** — `:sdui-compose/theme/KuisdTheme.kt`: `KuisdTheme` + builder + DSL +
  `plus` + `Empty`.
- [ ] **T7** — `:sdui-compose/theme/MaterialKuisdTheme.kt`:
  `rememberMaterialKuisdTheme()` con escala completa (Color/Radius Sm-Xl+Card+Pill/Space/Elevation).
- [ ] **T8** — `:sdui-compose/theme/LocalKuisdTheme.kt`:
  `CompositionLocal` con default `KuisdTheme.Empty`.
- [ ] **T9** — Tests theme: `plus` override-semantics; `resolve…OrNull` correcto; Empty
  devuelve null para todo.

## Motor — icons
- [ ] **T10** — `:sdui-compose/build.gradle.kts`: añadir
  `api(libs.material.icons.extended)` (ya en catálogo).
- [ ] **T11** — `:sdui-compose/icons/IconRegistry.kt`: `IconRegistry` + builder + DSL +
  `plus` + `Empty`.
- [ ] **T12** — `:sdui-compose/icons/DefaultIconRegistry.kt`: el set base
  (home/search/settings/add/edit/delete/close/check/arrowBack/arrowForward/favorite/moreVert).
- [ ] **T13** — `:sdui-compose/icons/LocalIconRegistry.kt`: `CompositionLocal` con default
  `DefaultIconRegistry`.
- [ ] **T14** — Tests icons: `plus` combina; `get` por nombre; nombre ausente → null.

## Motor — UiModifier resolver
- [ ] **T15** — `:sdui-compose/modifier/UiModifierResolver.kt`:
  `internal fun UiModifier?.toModifier(theme): Modifier` con orden fijo (fillMax → size →
  padding → clip + background). NO aplica weight/elevation/alignment.
- [ ] **T16** — `:sdui-compose/modifier/AlignmentResolver.kt`:
  `internal fun AlignmentToken?.toHorizontalAlignment()` y `toVerticalAlignment()`.
- [ ] **T17** — Tests resolver: null → identity; full → modifier compuesto; tokens
  ausentes en theme → no crash, default; alignment helper mapea correctamente cada token al eje.

## Motor — RenderScope + CorePack
- [ ] **T18** — `:sdui-compose/RenderScope.kt`: añadir `modifier: Modifier` (`@Composable get`,
  memoizado por `(node.modifier, theme)`) y `horizontalAlignmentOrNull()`/
  `verticalAlignmentOrNull()`.
- [ ] **T19** — `:sdui-compose/CorePack.kt`: los renderers existentes
  (`column`/`row`/`text`/`button`/`textField`) consumen `scope.modifier`. `column` y `row`
  también consumen `horizontalAlignmentOrNull()` / `verticalAlignmentOrNull()`.
- [ ] **T20** — `:sdui-compose/CorePack.kt`: registrar `surface` + `SurfaceProps`.
- [ ] **T21** — `:sdui-compose/CorePack.kt`: registrar `card` + `CardProps` (defaults
  opinionados: shape=Tokens.Radius.Card, elevation=Tokens.Elevation.Sm).
- [ ] **T22** — `:sdui-compose/CorePack.kt`: registrar `divider` + `DividerProps`.
- [ ] **T23** — `:sdui-compose/CorePack.kt`: registrar `spacer` + `SpacerProps`.
- [ ] **T24** — `:sdui-compose/CorePack.kt`: registrar `lazyColumn`/`lazyRow` con
  `items(node.children, key = { it.id })`.
- [ ] **T25** — `:sdui-compose/CorePack.kt`: registrar `icon` + `IconProps` (resuelve nombre
  contra `LocalIconRegistry`, tint/size contra theme; fallback `HelpOutline`).
- [ ] **T26** — `:sdui-compose/CorePack.kt`: registrar `iconButton` + `IconButtonProps` (idem
  + `IconButton(onClick = { handler.handle(actions["onClick"]) })`).
- [ ] **T27** — Tests motor: decodificación de las 7 props nuevas (full/defaults).

## App
- [ ] **T28** — `:shared/app/theme/AppTheme.kt`: `@Composable rememberAppTheme(): KuisdTheme`
  que devuelve `base + override` donde `override` re-mapea `Tokens.Radius.Card` a
  `base.resolveShapeOrNull(Tokens.Radius.Xl)` (sin `dp` literales).
- [ ] **T29** — `:shared/app/icons/AppIcons.kt`:
  `internal fun appIconsOverride(): IconRegistry` (vacío MVP; documenta el patrón).
- [ ] **T30** — `:shared/app/SduiHost.kt`: añadir `LocalKuisdTheme` y `LocalIconRegistry` al
  `CompositionLocalProvider` existente; usar `rememberAppTheme()` y
  `DefaultIconRegistry + appIconsOverride()`.
- [ ] **T31** — Verificación HU-6.5:
  `grep -E "\b[0-9]+\.dp\b" shared/src/commonMain/kotlin/dev/kuisd/app/theme/` vacío.

## Server
- [ ] **T32** — `FeedScreen.kt` (NUEVO): `column` raíz con cabecera (`row` con `iconButton`
  arrowBack + `text` "Feed") y `lazyColumn` con 5 cards. Cada card con tokens completos
  (padding/cornerRadius/background/elevation/alignment).
- [ ] **T33** — `ScreenRegistry.kt`: registrar `"feed" → FeedScreen`.
- [ ] **T34** — `HomeScreen.kt`: añadir `button` "Feed" con `Navigate("feed")`.
- [ ] **T35** — `ApplicationTest`: validar `/screen/feed` 200 + estructura (lazyColumn raíz +
  ≥5 cards con tokens); `/screen/home` con botón "Feed".

## Calidad
- [ ] **T36** — Regla de dependencias:
  `grep -rn "dev.kuisd.app\|io.ktor\." sdui-compose/src/` vacío.
- [ ] **T37** — Smoke manual `:server:run` + `:desktopApp:run`:
  1. `home` muestra botón "Feed" → navega a `feed`.
  2. `feed` muestra cards con padding/radius/elevation; el radius es el de `Xl` (override).
  3. Cabecera con `iconButton arrowBack` → vuelve a `home`.
  4. Iconos visibles en cada card.
  5. Hijos del column interno centrados horizontalmente.
  6. Scroll fluido en LazyColumn.
  7. `counter` ahora muestra padding aplicado en el column raíz.
- [ ] **T38** — Calidad: `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble
  :shared:check :server:build :androidApp:assembleDebug detekt ktlintCheck` (tras
  `ktlintFormat`) — verde.

## Verificación final (Definition of Done)
- [ ] Contrato gana `ElevationToken` + `AlignmentToken` + `Tokens.Radius` ampliado +
  `UiModifier.elevation`/`alignment: AlignmentToken?`; wire retrocompatible.
- [ ] Motor gana `KuisdTheme` + `IconRegistry` + `UiModifierResolver` + `AlignmentResolver` +
  `RenderScope.modifier` + 7 componentes; sin imports prohibidos.
- [ ] Los renderers existentes aplican `modifier` y, en `column`/`row`, el `alignment`.
- [ ] `feed` se sirve 200 con estructura tokenizada; `home` tiene botón "Feed".
- [ ] App provee `rememberAppTheme()` y `DefaultIconRegistry + appIconsOverride()` por
  `LocalKuisdTheme`/`LocalIconRegistry`.
- [ ] El override del theme NO contiene `dp` literales (HU-6.5).
- [ ] Tests motor + server + app verdes; detekt + ktlintCheck verdes.
- [ ] `tasks.md` todo `[x]` salvo smoke manual (si harness no abre UI, justificado).
