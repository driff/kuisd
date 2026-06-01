# Diseño — Contenedores + Theme extensible + `UiModifier` aplicado

> Spec ID: 008 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Cuatro piezas atómicas que se acoplan:

1. **`KuisdTheme`** (motor): valor inmutable con 4 mapas (`ColorToken→Color`,
   `RadiusToken→Shape`, `SpaceToken→Dp`, `ElevationToken→Dp`), operador `plus` (override
   semantics) y builder DSL. Provisto por `LocalKuisdTheme`; default seguro = theme vacío.
   `@Composable rememberMaterialKuisdTheme()` arma el theme base mapeando los tokens conocidos
   de `:sdui-core` a `MaterialTheme.colorScheme`/`MaterialTheme.shapes` + escala spacing
   4/8/12/16/24 dp + elevation 0/1/3/6 dp + Radius escalado a Material shapes.

2. **`IconRegistry`** (motor): `Map<String, ImageVector>` + `plus` + builder DSL. Default
   `DefaultIconRegistry` con set base de Material icons. Provisto por `LocalIconRegistry`.
   Mismo patrón que `KuisdTheme` y `ComponentRegistry` — composición sobre extensión.

3. **`UiModifier.toModifier(theme)`** (motor, `internal`): extensión pura sobre `UiModifier?` que
   resuelve `fillMax`/`size`/`padding`/`background`+`cornerRadius` contra el theme y construye un
   `Modifier`. Si todos los campos son nulos/false, devuelve `Modifier` (identidad).
   `alignment` NO se aplica aquí — se traduce a `Alignment.Horizontal/Vertical` y se pasa al
   constructor de `Column`/`Row` (HU-3.7). `elevation` se aplica solo en `surface`/`card`
   (HU-3.8). `weight` queda no aplicado (HU-3.9).

4. **`RenderScope.modifier: Modifier`** (motor): propiedad memoizada
   `node.modifier?.toModifier(theme) ?: Modifier`. Cada renderer del `CorePack` la consume.
   Adicionalmente, `RenderScope` expone helpers `horizontalAlignmentOrNull()` /
   `verticalAlignmentOrNull()` para que `column`/`row` traduzcan el alignment del nodo.

Componentes nuevos en `CorePack`: `surface`, `card`, `divider`, `spacer`, `lazyColumn`,
`lazyRow`, `icon`, `iconButton`. Pantalla demo nueva en server: `feed`.

## Arquitectura (módulos afectados)
```
:sdui-core
   ├── DesignTokens.kt   (cambia)  + ElevationToken + Tokens.Elevation { None, Sm, Md, Lg }
   │                                + AlignmentToken + Tokens.Alignment { Start, Center, End, ... }
   │                                + Tokens.Radius { Sm, Md, Lg, Xl } (preserva None/Card/Pill)
   └── UiModifier.kt     (cambia)  + elevation: ElevationToken? = null
                                    + alignment: AlignmentToken? (era String?)
   ▲
:sdui-compose
   ├── theme/                            (NUEVO subpaquete)
   │   ├── KuisdTheme.kt                 public  KuisdTheme + builder + DSL + plus
   │   ├── MaterialKuisdTheme.kt         public  rememberMaterialKuisdTheme()
   │   └── LocalKuisdTheme.kt            public  ProvidableCompositionLocal<KuisdTheme>
   ├── icons/                            (NUEVO subpaquete)
   │   ├── IconRegistry.kt               public  IconRegistry + builder + DSL + plus
   │   ├── DefaultIconRegistry.kt        public  DefaultIconRegistry (Material icons base)
   │   └── LocalIconRegistry.kt          public  ProvidableCompositionLocal<IconRegistry>
   ├── modifier/
   │   ├── UiModifierResolver.kt         internal  fun UiModifier?.toModifier(KuisdTheme): Modifier
   │   └── AlignmentResolver.kt          internal  AlignmentToken? → Alignment.Horizontal/Vertical
   ├── RenderScope.kt                    (cambia)  + val modifier: Modifier (resuelto, memoizado)
   │                                                + helpers de alignment para column/row
   ├── RenderNode.kt                     (sin cambio sustancial)
   ├── CorePack.kt                       (cambia)  + surface/card/divider/spacer/
   │                                                  lazyColumn/lazyRow/icon/iconButton
   └── existentes (column/row/text/button/textField)  (cambian)  aplican scope.modifier al raíz
                                                                  column/row aplican alignment
   ▲
:shared
   ├── app/SduiHost.kt                   (cambia)  provee LocalKuisdTheme + LocalIconRegistry
   ├── app/theme/AppTheme.kt             (NUEVO)   internal  @Composable rememberAppTheme()
   │                                                          (override sin números dp literales)
   └── app/icons/AppIcons.kt             (NUEVO)   internal  appIconsOverride() (vacío MVP)
server/.../screens/
   ├── FeedScreen.kt                     (NUEVO)   pantalla demo con cards + lazyColumn + iconos
   ├── HomeScreen.kt                     (cambia)  + button "Feed" → Navigate("feed")
   └── ScreenRegistry.kt                 (cambia)  + "feed" → FeedScreen

:sdui-compose/build.gradle.kts          (cambia)  + api(libs.material.icons.extended)
                                                    (ya en libs.versions.toml)
```
La frontera de 006 se preserva: `:sdui-compose` solo usa `:sdui-core` + Compose (incluyendo
material-icons-extended, que es `org.jetbrains.compose.material:*` — Compose puro, sin Ktor).

## Componentes y contratos

### Contrato — `ElevationToken`, `AlignmentToken`, `UiModifier` + escala `Radius` (HU-2)
```kotlin
// :sdui-core/DesignTokens.kt — añade
@Serializable @JvmInline
value class ElevationToken(val ref: String)

@Serializable @JvmInline
value class AlignmentToken(val ref: String)

object Tokens {
    // ... Color/Type/Space (existentes intactos)

    object Radius {
        val None = RadiusToken("radius.none")
        val Sm   = RadiusToken("radius.sm")     // NUEVO
        val Md   = RadiusToken("radius.md")     // NUEVO
        val Lg   = RadiusToken("radius.lg")     // NUEVO
        val Xl   = RadiusToken("radius.xl")     // NUEVO
        val Card = RadiusToken("radius.card")   // (existente — alias semántico; theme default lo mapea a Md)
        val Pill = RadiusToken("radius.pill")   // (existente)
    }

    object Elevation {
        val None = ElevationToken("elevation.none")
        val Sm   = ElevationToken("elevation.sm")
        val Md   = ElevationToken("elevation.md")
        val Lg   = ElevationToken("elevation.lg")
    }

    object Alignment {
        val Start             = AlignmentToken("alignment.start")
        val Center            = AlignmentToken("alignment.center")             // ambiguo, resuelve por eje
        val End               = AlignmentToken("alignment.end")
        val Top               = AlignmentToken("alignment.top")
        val Bottom            = AlignmentToken("alignment.bottom")
        val CenterHorizontally= AlignmentToken("alignment.centerH")            // explícito
        val CenterVertically  = AlignmentToken("alignment.centerV")            // explícito
    }
}

// :sdui-core/UiModifier.kt — cambia
@Serializable
data class UiModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: SpaceToken? = null,
    val height: SpaceToken? = null,
    val padding: PaddingTokens? = null,
    val background: ColorToken? = null,
    val cornerRadius: RadiusToken? = null,
    val elevation: ElevationToken? = null,       // NUEVO
    val weight: Float? = null,                    // sigue ignorado en MVP
    val alignment: AlignmentToken? = null,        // CAMBIO de tipo: String? -> AlignmentToken?
)
```
**Compatibilidad de wire:** `AlignmentToken` es `@JvmInline value class String` → serializa como
string idéntico al campo `String?` anterior. Por wire es compatible. Por código Kotlin es
breaking (típo cambia), pero hoy ningún screen del server lo usa, así que no afecta a producción.
`elevation` es opcional con default `null` (cliente viejo lo ignora vía `ignoreUnknownKeys`).

### Motor — `KuisdTheme` (HU-1)
```kotlin
// dev.kuisd.sdui.theme  (public)
class KuisdTheme internal constructor(
    private val colors:    Map<ColorToken, Color>,
    private val shapes:    Map<RadiusToken, Shape>,
    private val spaces:    Map<SpaceToken, Dp>,
    private val elevations:Map<ElevationToken, Dp>,
) {
    fun resolveColorOrNull(t: ColorToken?): Color?      = t?.let { colors[it] }
    fun resolveShapeOrNull(t: RadiusToken?): Shape?     = t?.let { shapes[it] }
    fun resolveSpaceOrNull(t: SpaceToken?): Dp?         = t?.let { spaces[it] }
    fun resolveElevationOrNull(t: ElevationToken?): Dp? = t?.let { elevations[it] }

    operator fun plus(other: KuisdTheme): KuisdTheme = KuisdTheme(
        colors = colors + other.colors,
        shapes = shapes + other.shapes,
        spaces = spaces + other.spaces,
        elevations = elevations + other.elevations,
    )

    companion object {
        val Empty: KuisdTheme = KuisdTheme(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

class KuisdThemeBuilder @PublishedApi internal constructor() {
    @PublishedApi internal val colors = mutableMapOf<ColorToken, Color>()
    @PublishedApi internal val shapes = mutableMapOf<RadiusToken, Shape>()
    @PublishedApi internal val spaces = mutableMapOf<SpaceToken, Dp>()
    @PublishedApi internal val elevations = mutableMapOf<ElevationToken, Dp>()

    fun color(token: ColorToken, value: Color)         { colors[token] = value }
    fun shape(token: RadiusToken, value: Shape)        { shapes[token] = value }
    fun space(token: SpaceToken, value: Dp)            { spaces[token] = value }
    fun elevation(token: ElevationToken, value: Dp)    { elevations[token] = value }
}

inline fun kuisdTheme(block: KuisdThemeBuilder.() -> Unit): KuisdTheme {
    val b = KuisdThemeBuilder().apply(block)
    return KuisdTheme(b.colors.toMap(), b.shapes.toMap(), b.spaces.toMap(), b.elevations.toMap())
}

val LocalKuisdTheme: ProvidableCompositionLocal<KuisdTheme> =
    staticCompositionLocalOf { KuisdTheme.Empty }
```

### Motor — `rememberMaterialKuisdTheme` (HU-1.5)
```kotlin
@Composable
fun rememberMaterialKuisdTheme(): KuisdTheme {
    val cs = MaterialTheme.colorScheme
    val sh = MaterialTheme.shapes
    return remember(cs, sh) {
        kuisdTheme {
            // Colors: tokens de :sdui-core → MaterialTheme.colorScheme
            color(Tokens.Color.Primary,   cs.primary)
            color(Tokens.Color.OnPrimary, cs.onPrimary)
            color(Tokens.Color.Surface,   cs.surface)
            color(Tokens.Color.OnSurface, cs.onSurface)
            color(Tokens.Color.Error,     cs.error)
            color(Tokens.Color.Outline,   cs.outline)

            // Shapes: escala radius -> Material3 shapes
            shape(Tokens.Radius.None, RectangleShape)
            shape(Tokens.Radius.Sm,   sh.extraSmall)
            shape(Tokens.Radius.Md,   sh.small)
            shape(Tokens.Radius.Lg,   sh.medium)
            shape(Tokens.Radius.Xl,   sh.large)
            shape(Tokens.Radius.Card, sh.medium)             // alias semántico = Lg
            shape(Tokens.Radius.Pill, RoundedCornerShape(percent = 50))

            // Space: escala 4/8/12/16/24 dp
            space(Tokens.Space.Xs, 4.dp)
            space(Tokens.Space.Sm, 8.dp)
            space(Tokens.Space.Md, 12.dp)
            space(Tokens.Space.Lg, 16.dp)
            space(Tokens.Space.Xl, 24.dp)

            // Elevation: 0/1/3/6 dp
            elevation(Tokens.Elevation.None, 0.dp)
            elevation(Tokens.Elevation.Sm,   1.dp)
            elevation(Tokens.Elevation.Md,   3.dp)
            elevation(Tokens.Elevation.Lg,   6.dp)
        }
    }
}
```
> Nota: los `4.dp`/`8.dp`/etc. del base son la **única fuente de números literales** en todo el
> sistema. El override de la app NO contiene `.dp` (HU-6.5) — se construye derivando del base.

### Motor — `IconRegistry` (HU-3bis)
```kotlin
// dev.kuisd.sdui.icons (public)
class IconRegistry internal constructor(internal val byName: Map<String, ImageVector>) {
    fun get(name: String): ImageVector? = byName[name]
    operator fun plus(other: IconRegistry): IconRegistry =
        IconRegistry(byName + other.byName)
    companion object { val Empty: IconRegistry = IconRegistry(emptyMap()) }
}

class IconRegistryBuilder @PublishedApi internal constructor() {
    @PublishedApi internal val byName = mutableMapOf<String, ImageVector>()
    fun register(name: String, vector: ImageVector) { byName[name] = vector }
}

inline fun iconRegistry(block: IconRegistryBuilder.() -> Unit): IconRegistry =
    IconRegistry(IconRegistryBuilder().apply(block).byName.toMap())

val DefaultIconRegistry: IconRegistry = iconRegistry {
    register("home",         Icons.Default.Home)
    register("search",       Icons.Default.Search)
    register("settings",     Icons.Default.Settings)
    register("add",          Icons.Default.Add)
    register("edit",         Icons.Default.Edit)
    register("delete",       Icons.Default.Delete)
    register("close",        Icons.Default.Close)
    register("check",        Icons.Default.Check)
    register("arrowBack",    Icons.AutoMirrored.Filled.ArrowBack)
    register("arrowForward", Icons.AutoMirrored.Filled.ArrowForward)
    register("favorite",     Icons.Default.Favorite)
    register("moreVert",     Icons.Default.MoreVert)
}

val LocalIconRegistry: ProvidableCompositionLocal<IconRegistry> =
    staticCompositionLocalOf { DefaultIconRegistry }
```

### Motor — `UiModifierResolver` + `AlignmentResolver` (HU-3)
```kotlin
// dev.kuisd.sdui.modifier  (internal)
internal fun UiModifier?.toModifier(theme: KuisdTheme): Modifier {
    val um = this ?: return Modifier
    var m: Modifier = Modifier
    if (um.fillMaxWidth)  m = m.fillMaxWidth()
    if (um.fillMaxHeight) m = m.fillMaxHeight()
    theme.resolveSpaceOrNull(um.width)?.let  { m = m.width(it) }
    theme.resolveSpaceOrNull(um.height)?.let { m = m.height(it) }
    um.padding?.let { p ->
        m = m.padding(
            start  = theme.resolveSpaceOrNull(p.l) ?: 0.dp,
            top    = theme.resolveSpaceOrNull(p.t) ?: 0.dp,
            end    = theme.resolveSpaceOrNull(p.r) ?: 0.dp,
            bottom = theme.resolveSpaceOrNull(p.b) ?: 0.dp,
        )
    }
    val resolvedShape = theme.resolveShapeOrNull(um.cornerRadius)
    val resolvedBg = theme.resolveColorOrNull(um.background)
    if (resolvedShape != null) m = m.clip(resolvedShape)
    if (resolvedBg != null)    m = m.background(resolvedBg, resolvedShape ?: RectangleShape)
    return m
}

/** Traduce un AlignmentToken al eje horizontal de un Column (null si no aplica al eje). */
internal fun AlignmentToken?.toHorizontalAlignment(): Alignment.Horizontal? = when (this) {
    Tokens.Alignment.Start,                    -> Alignment.Start
    Tokens.Alignment.Center,
    Tokens.Alignment.CenterHorizontally        -> Alignment.CenterHorizontally
    Tokens.Alignment.End                       -> Alignment.End
    else                                       -> null   // Top/Bottom/CenterVertically no aplican
}

/** Traduce un AlignmentToken al eje vertical de un Row (null si no aplica al eje). */
internal fun AlignmentToken?.toVerticalAlignment(): Alignment.Vertical? = when (this) {
    Tokens.Alignment.Top                       -> Alignment.Top
    Tokens.Alignment.Center,
    Tokens.Alignment.CenterVertically          -> Alignment.CenterVertically
    Tokens.Alignment.Bottom                    -> Alignment.Bottom
    else                                       -> null   // Start/End/CenterHorizontally no aplican
}
```
**Notas explícitas:**
- `weight` queda no aplicado en MVP (requiere scope; HU-3.9).
- `elevation` no se aplica vía `Modifier` — `surface`/`card` la consumen directamente del nodo
  (HU-3.8). Si el nodo tiene `UiModifier.elevation` **y** el componente tiene props.elevation,
  gana props (más específica).
- `alignment` se traduce con `toHorizontalAlignment()`/`toVerticalAlignment()` solo dentro de
  `column`/`row` (HU-3.7); en otros nodos se ignora.

### Motor — `RenderScope.modifier` + helpers de alignment (HU-3.3, HU-3.7)
```kotlin
class RenderScope internal constructor(
    val node: SduiNode,
) {
    /** Modifier resuelto del UiModifier del nodo contra el theme actual. Memoizado. */
    val modifier: Modifier
        @Composable get() {
            val theme = LocalKuisdTheme.current
            val um = node.modifier
            return remember(um, theme) { um.toModifier(theme) }
        }

    /** Alineación horizontal de los hijos (para `column`); null = default `Start`. */
    fun horizontalAlignmentOrNull(): Alignment.Horizontal? =
        node.modifier?.alignment.toHorizontalAlignment()

    /** Alineación vertical de los hijos (para `row`); null = default `Top`. */
    fun verticalAlignmentOrNull(): Alignment.Vertical? =
        node.modifier?.alignment.toVerticalAlignment()

    @Composable fun renderChildren() { node.children.forEach { RenderNode(it) } }
    @Composable fun bind(raw: String?): String { /* sin cambios (005) */ }
}
```

### Motor — `CorePack` aplica el modifier y registra contenedores (HU-3.6 + HU-4)
```kotlin
val CorePack: ComponentRegistry = componentRegistry {
    register(sduiComponent<ColumnProps>("column")) {
        Column(
            modifier = modifier,
            horizontalAlignment = horizontalAlignmentOrNull() ?: Alignment.Start,
        ) { renderChildren() }
    }
    register(sduiComponent<RowProps>("row")) {
        Row(
            modifier = modifier,
            verticalAlignment = verticalAlignmentOrNull() ?: Alignment.Top,
        ) { renderChildren() }
    }
    register(sduiComponent<TextProps>("text"))     { p -> Text(text = bind(p.text), modifier = modifier) }
    register(sduiComponent<ButtonProps>("button")) { p ->
        val h = LocalSduiActionHandler.current
        Button(onClick = { h.handle(node.actions["onClick"].orEmpty()) }, modifier = modifier) {
            Text(bind(p.label))
        }
    }
    register(sduiComponent<TextFieldProps>("textField")) { p -> TextFieldRenderer(p) }  // ya pasa modifier

    register(sduiComponent<SurfaceProps>("surface")) { p ->
        val theme = LocalKuisdTheme.current
        Surface(
            modifier = modifier,
            color = theme.resolveColorOrNull(p.background) ?: Color.Unspecified,
            shape = theme.resolveShapeOrNull(p.shape) ?: RectangleShape,
            tonalElevation = theme.resolveElevationOrNull(p.elevation) ?: 0.dp,
            contentColor = theme.resolveColorOrNull(p.contentColor) ?: Color.Unspecified,
        ) { renderChildren() }
    }
    register(sduiComponent<CardProps>("card")) { p ->
        val theme = LocalKuisdTheme.current
        Card(
            modifier = modifier,
            shape = theme.resolveShapeOrNull(p.shape) ?: theme.resolveShapeOrNull(Tokens.Radius.Card) ?: RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = theme.resolveColorOrNull(p.background) ?: CardDefaults.cardColors().containerColor,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = theme.resolveElevationOrNull(p.elevation)
                    ?: theme.resolveElevationOrNull(Tokens.Elevation.Sm) ?: 1.dp,
            ),
        ) { renderChildren() }
    }
    register(sduiComponent<DividerProps>("divider")) { p ->
        val theme = LocalKuisdTheme.current
        HorizontalDivider(
            modifier = modifier,
            color = theme.resolveColorOrNull(p.color) ?: DividerDefaults.color,
            thickness = theme.resolveSpaceOrNull(p.thickness) ?: DividerDefaults.Thickness,
        )
    }
    register(sduiComponent<SpacerProps>("spacer")) { p ->
        val theme = LocalKuisdTheme.current
        val size = theme.resolveSpaceOrNull(p.size) ?: 8.dp
        Spacer(modifier = modifier.then(Modifier.size(size)))
    }
    register(sduiComponent<LazyColumnProps>("lazyColumn")) {
        LazyColumn(modifier = modifier) {
            items(node.children, key = { it.id }) { RenderNode(it) }
        }
    }
    register(sduiComponent<LazyRowProps>("lazyRow")) {
        LazyRow(modifier = modifier) {
            items(node.children, key = { it.id }) { RenderNode(it) }
        }
    }
    register(sduiComponent<IconProps>("icon")) { p ->
        val theme = LocalKuisdTheme.current
        val registry = LocalIconRegistry.current
        val vector = registry.get(p.name) ?: Icons.Default.HelpOutline
        val size = theme.resolveSpaceOrNull(p.size) ?: 24.dp
        Icon(
            imageVector = vector,
            contentDescription = p.contentDescription,
            modifier = modifier.then(Modifier.size(size)),
            tint = theme.resolveColorOrNull(p.tint) ?: LocalContentColor.current,
        )
    }
    register(sduiComponent<IconButtonProps>("iconButton")) { p ->
        val theme = LocalKuisdTheme.current
        val registry = LocalIconRegistry.current
        val handler = LocalSduiActionHandler.current
        val vector = registry.get(p.name) ?: Icons.Default.HelpOutline
        IconButton(
            onClick = { handler.handle(node.actions["onClick"].orEmpty()) },
            modifier = modifier,
        ) {
            Icon(
                imageVector = vector,
                contentDescription = p.contentDescription,
                tint = theme.resolveColorOrNull(p.tint) ?: LocalContentColor.current,
            )
        }
    }
}
```
Props nuevas (todas `@Serializable`, públicas):
```kotlin
@Serializable data class SurfaceProps(
    val background: ColorToken? = null,
    val shape: RadiusToken? = null,
    val elevation: ElevationToken? = null,
    val contentColor: ColorToken? = null,
)
@Serializable data class CardProps(
    val background: ColorToken? = null,
    val shape: RadiusToken? = null,
    val elevation: ElevationToken? = null,
)
@Serializable data class DividerProps(
    val color: ColorToken? = null,
    val thickness: SpaceToken? = null,
)
@Serializable data class SpacerProps(val size: SpaceToken? = null)
@Serializable class LazyColumnProps
@Serializable class LazyRowProps
@Serializable data class IconProps(
    val name: String = "",
    val tint: ColorToken? = null,
    val size: SpaceToken? = null,
    val contentDescription: String? = null,
)
@Serializable data class IconButtonProps(
    val name: String = "",
    val tint: ColorToken? = null,
    val contentDescription: String? = null,
)
```

### App — override demo SIN números mágicos (HU-6)
```kotlin
// dev.kuisd.app.theme  (internal)
@Composable
internal fun rememberAppTheme(): KuisdTheme {
    val base = rememberMaterialKuisdTheme()
    return remember(base) {
        // Override: el card del cliente usa la shape del nivel Xl del propio catálogo
        // (más grande que el default Card del base). NO hay dp literales aquí: el dp lo aporta
        // el `rememberMaterialKuisdTheme` (única fuente de números).
        val xlShape = base.resolveShapeOrNull(Tokens.Radius.Xl)
            ?: error("Tokens.Radius.Xl debe estar mapeado en el theme base")
        val override = kuisdTheme {
            shape(Tokens.Radius.Card, xlShape)
        }
        base + override
    }
}

// dev.kuisd.app.icons (internal)
internal fun appIconsOverride(): IconRegistry = iconRegistry {
    // MVP: vacío — demuestra el patrón composicional. La app puede añadir aquí su set propio.
}

// SduiHost.kt — añade
val theme = rememberAppTheme()
val icons = remember { DefaultIconRegistry + appIconsOverride() }
CompositionLocalProvider(
    LocalSduiActionHandler provides handler,
    LocalComponentRegistry provides appRegistry,
    LocalVariables provides store.scope,
    LocalKuisdTheme provides theme,
    LocalIconRegistry provides icons,
) { /* ... */ }
```
**Verificación HU-6.5:** `grep -E "\b[0-9]+\.dp\b" shared/src/commonMain/kotlin/dev/kuisd/app/theme/` debe estar vacío. Cualquier `dp` que la app necesite viene del base via `resolve…OrNull`.

### Server — `FeedScreen` (HU-5)
Pantalla con un `column` raíz que contiene una cabecera (`iconButton` arrowBack + `text` título)
y un `lazyColumn` con 5 `card`s. Cada card tiene `padding = Md`, `cornerRadius = Card`,
`background = Surface`, `elevation = Sm`, y `alignment = CenterHorizontally`; dentro un `row`
(icon + text del título) → `divider` → `text` del cuerpo. Helpers privados
`cardNode(id, iconName, title, body)`, `iconRowNode(iconName, title)`. `HomeScreen` añade un
`button` con `Navigate("feed")`. `ScreenRegistry` añade la entrada.

Server usa `Tokens.Alignment.CenterHorizontally`, `Tokens.Radius.Card`, `Tokens.Color.Surface`,
`Tokens.Space.Md`, `Tokens.Elevation.Sm` — todos del catálogo de `:sdui-core`. Iconos por nombre:
`"home"`, `"settings"`, `"favorite"`, `"search"`, `"check"` (en las 5 cards) y `"arrowBack"` en
la cabecera.

## Modelo de datos y estados
- Contrato `:sdui-core`: + `ElevationToken` + `UiModifier.elevation`. Retrocompatible.
- Motor: `KuisdTheme` inmutable; 4 mapas. Sin estado mutable. `LocalKuisdTheme` default
  `KuisdTheme.Empty`.
- App: theme `remember`-eado a partir de `MaterialTheme` actual (rota con dark mode); el override
  es función pura.

## Dependencias nuevas
Ninguna. `LazyColumn`/`LazyRow`/`Surface`/`Card`/`HorizontalDivider`/`Spacer` ya están en
`api(material3)` y `api(foundation)`.

## Riesgos y mitigaciones
- **`UiModifier.weight` ignorado fuera de Row/Column.** Coste de ignorarlo: nodos con `weight`
  fuera de su scope no se romperán, simplemente se ignorarán. Para usarlo *correctamente* hay que
  acceder a `RowScope.weight()`/`ColumnScope.weight()`, lo que requiere que el renderer del `row`/
  `column` extienda el modifier resuelto con `weight` cuando aplique. Decisión MVP: **no se aplica
  `weight` automáticamente**; el server controla layouts vía `fillMaxWidth` + `padding`. Si se
  necesita, se sube a una spec.
- **Orden de modifiers fijo.** El orden documentado (fill → size → padding → clip+background →
  elevation external) cubre el 95% de los casos. Para layouts inusuales el server tendría que
  componer con nodos anidados (un `column` con padding dentro de otro con background).
- **`Card` con `elevation` y `Surface` interno.** `Material3 Card` ya maneja shadow+tonal; usamos
  `CardDefaults.cardElevation(defaultElevation = ...)` para no romper interacciones (Card en
  Material3 es un Surface clickable bajo el capó si se le pasa onClick — no lo hacemos aquí, así
  que se comporta como contenedor puro).
- **Themes con valores conflictivos.** `plus` da prioridad al `other` (override semantics).
  Documentado y testeado.
- **Recomposición masiva al cambiar theme.** `LocalKuisdTheme` es `staticCompositionLocalOf` —
  cambiar el theme recompone TODO el subárbol. Aceptable para cambios infrecuentes (dark mode,
  override demo). Si se necesita cambio frecuente, se cambia a `compositionLocalOf`.
- **`UiModifier.toModifier` se llama por cada nodo en cada cambio de theme.** Mitigación:
  `RenderScope.modifier` memoiza por `(UiModifier, KuisdTheme)`; un cambio de theme invalida toda
  la jerarquía (esperado), pero cambios de la variable o de hijos no invalidan los modifiers
  hermanos.
- **Wire compat de `elevation`.** Nuevo campo opcional en `UiModifier`; clientes viejos lo
  ignoran (kotlinx `ignoreUnknownKeys` + default `null`). Verificado por reflexión del JSON.

## Estrategia de verificación
- **Unit `:sdui-core:check`:** decodificación de `UiModifier` con y sin `elevation`; decodificación
  de `ElevationToken` desde string.
- **Unit `:sdui-compose:commonTest`:**
  - `KuisdThemeTest`: `plus` combina y override gana; `resolve…OrNull` lookup correcto; theme
    vacío devuelve null para todo.
  - Decodificación de las 5 props nuevas (full / defaults / inválidas).
  - `UiModifierResolverTest` (puro, no UI): un `UiModifier` con todos los campos llenos +
    theme con todos los tokens → la función devuelve un `Modifier` no-Identity (se asierta vía
    `.toString()` que el modifier compuesto incluye los esperados, o vía un harness mínimo).
    Caso `UiModifier = null` → `Modifier` identity.
- **Unit `:server:test`:** `GET /screen/feed` 200 + envelope con `lazyColumn` con ≥5 cards;
  `GET /screen/home` ahora trae el botón "Feed".
- **Regla de dependencias:** `grep -rn "dev.kuisd.app\|io.ktor\." sdui-compose/src/` sigue vacío.
- **Smoke manual** (`:desktopApp:run`): navegar a `feed`, ver lista de cards con padding/radio/
  sombra distintos del background; cambiar el override del theme (p.ej. radius 24 dp) y ver el
  cambio.
- **Calidad:** `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble :shared:check
  :server:build :androidApp:assembleDebug detekt ktlintCheck` (tras `ktlintFormat`) — verde.

## public / internal
- **public (motor):**
  `KuisdTheme`, `KuisdThemeBuilder`, `kuisdTheme()`, `rememberMaterialKuisdTheme()`,
  `LocalKuisdTheme`, las 5 props nuevas (`SurfaceProps`/`CardProps`/`DividerProps`/`SpacerProps`/
  `LazyColumnProps`/`LazyRowProps`), `RenderScope.modifier` (ya RenderScope es public).
- **internal:** `UiModifier?.toModifier(KuisdTheme)`.
- **app/server:** `appThemeOverride()` internal.

## Preguntas abiertas — cerradas en aprobación
1. **`weight`** — no se aplica en MVP; documentado en §Riesgos. Se sube a spec posterior si
   surge demanda real.
2. **Alignment tokenizado** — `String?` → `AlignmentToken?` en `UiModifier`; se aplica en
   `column`/`row` como `horizontalAlignment`/`verticalAlignment` del propio contenedor (a sus
   hijos). Alineación de hijos individuales fuera de scope (necesitaría `box` o cambio de firma
   del renderer).
3. **`icon`/`iconButton` SI** — añadidos a la spec, con `IconRegistry` extensible.
4. **Override sin números mágicos** — la app re-mapea `Tokens.Radius.Card` a la shape de
   `Tokens.Radius.Xl` del propio catálogo (sin `dp` literales en el código del override). El
   `dp` solo vive en `rememberMaterialKuisdTheme` (única fuente).
