# Requisitos — Contenedores + Theme extensible + `UiModifier` aplicado

> Spec ID: 008 · Estado: approved · Fecha: 2026-06-01

## Resumen
Hasta ahora el motor solo renderiza primitivos (`column`/`row`/`text`/`button`/`textField` + `badge`
en la app) y **ignora** el `UiModifier` del nodo (decisión heredada de 001/003/004/005/007). Esta
spec entrega cuatro piezas que se acoplan entre sí:

1. **Theme tokenizado extensible (`KuisdTheme`).** Sistema de resolución de tokens semánticos
   (Color/Shape/Space/Elevation) a valores de Compose. `MaterialKuisdTheme` como default (deriva de
   `MaterialTheme`); el usuario de la lib puede **sumar** un theme propio (`KuisdTheme + miOverride`)
   y proveerlo por `LocalKuisdTheme` sin tocar el motor (OCP por composición de themes, mismo
   patrón que `ComponentRegistry` en 004).
2. **`IconRegistry` extensible (`IconRegistry`).** Mismo patrón de composición: un `Map<String,
   ImageVector>` con set base de iconos Material + extensión por `plus`. Provisto por
   `LocalIconRegistry`. Consumido por `icon`/`iconButton`. El server enlaza por nombre
   (`"home"`, `"arrowBack"`); el cliente lo resuelve contra el registry actual.
3. **`UiModifier` aplicado en el motor.** Función pura `UiModifier.toModifier(theme): Modifier`
   resuelve `padding`/`fillMaxWidth`/`fillMaxHeight`/`width`/`height`/`background`/`cornerRadius`/
   `elevation` contra el theme actual y devuelve un `Modifier` de Compose. `alignment` se aplica
   en `column`/`row` como `horizontalAlignment`/`verticalAlignment` (a sus hijos, vía propiedad
   del contenedor — no como `Modifier.align` que requiere scope). El motor lo aplica vía
   `RenderScope.modifier`; cada renderer del `CorePack` lo pasa a su composable raíz.
4. **Siete contenedores/visuales nuevos en `CorePack`:** `surface`, `card`, `divider`, `spacer`,
   `lazyColumn`/`lazyRow`, `icon`, `iconButton`. Todos consumen el `KuisdTheme` (y el
   `IconRegistry` los dos últimos) para sus apariencias por defecto y respetan el `UiModifier`
   del nodo.

## Fuera de alcance
- **Typography** (`Tokens.Type`) — sigue ignorado (lo usa el server hoy y se deja como literal sin
  resolución). Spec posterior: aplicar `style` tokenizado al `text`.
- **Border / state-styling (focus/pressed/disabled)** — fuera.
- **`scaffold` / `topAppBar` / `bottomBar`** — slots de pantalla; spec posterior.
- **`alertDialog` / `bottomSheet`** — modales; spec posterior.
- **Template-driven lists** (`type="lazyColumn"` con un solo template + array de items) — sigue
  el modelo "children pre-expandidos por el server" (HU-3.4) para esta primera versión.
- **`image`** — abre KMP image loading (Coil, Kamel, etc.); spec dedicada.
- **`UiModifier` orden de modifiers configurable** — el orden lo decide el motor (documentado en
  §design). Cambiarlo en el envelope queda fuera.
- **Animaciones** de cambio de theme — fuera.

## Historias de usuario y criterios de aceptación

### HU-1 — Theme extensible (`KuisdTheme`)
**Como** integrador **quiero** un sistema de tokens resoluble en tiempo de render **para** que
los componentes adapten su apariencia sin que el server hard-codee colores/medidas concretos.

Criterios (EARS):
1. The engine SHALL exponer un tipo público `KuisdTheme` que mantiene 4 mapas inmutables:
   `Map<ColorToken, Color>`, `Map<RadiusToken, Shape>`, `Map<SpaceToken, Dp>`,
   `Map<ElevationToken, Dp>`.
2. The engine SHALL exponer 4 métodos `resolve…OrNull(token)` (lookup puro, no `@Composable`) que
   devuelven el valor o `null` si no está mapeado.
3. The engine SHALL exponer un operador `plus(other: KuisdTheme): KuisdTheme` que combina dos
   themes; el `other` **gana** sobre el `this` en las claves que ambos tengan (override semantics).
4. The engine SHALL exponer un **DSL builder** `kuisdTheme { color(token, value); shape(...);
   space(...); elevation(...) }` para construir themes desde código.
5. The engine SHALL exponer una función `@Composable rememberMaterialKuisdTheme(): KuisdTheme`
   que devuelve un theme con los tokens base de `Tokens.Color/Radius/Space/Elevation` mapeados a
   `MaterialTheme.colorScheme`/`MaterialTheme.shapes` + defaults razonables para spacing y
   elevation (4/8/12/16/24 dp para Space; 0/1/3/6 dp para Elevation).
6. The engine SHALL exponer `LocalKuisdTheme: ProvidableCompositionLocal<KuisdTheme>` con default
   = un theme vacío (resoluciones devolverán `null` → degradación a defaults de Compose). La app
   **debe** proveer un theme real desde el host; el motor no asume nada.
7. WHEN una resolución devuelve `null` y el componente necesita un valor, the renderer SHALL caer
   al default de Compose (p.ej. `Color.Unspecified`, `RectangleShape`, `0.dp`) sin crash.

### HU-2 — Contrato: `ElevationToken` + `AlignmentToken` + `elevation` tokenizado en `UiModifier`, escala `Tokens.Radius` ampliada
**Como** integrador **quiero** declarar elevación, alineación y radios consistentes en el envelope
**para** que el server no embuta valores literales en strings/floats.

Criterios (EARS):
1. The contract (`:sdui-core`) SHALL exponer un nuevo
   `@Serializable @JvmInline value class ElevationToken(val ref: String)` y un namespace
   `Tokens.Elevation { None, Sm, Md, Lg }`.
2. The contract SHALL exponer un nuevo
   `@Serializable @JvmInline value class AlignmentToken(val ref: String)` y un namespace
   `Tokens.Alignment { Start, Center, End, Top, Bottom, CenterVertically, CenterHorizontally }`.
   Los nombres modelan ambos ejes (Start/Center/End para horizontal en LTR; Top/Center/Bottom
   para vertical; los nombres `CenterVertically`/`CenterHorizontally` son redundantes para
   `Center` pero útiles en contenedores donde el eje es ambiguo).
3. The contract SHALL cambiar `UiModifier.alignment` de `String?` a `AlignmentToken?`. Este es un
   cambio **no breaking en wire** (el campo es `value class` con string interno; la representación
   JSON sigue siendo un string), pero **breaking en código Kotlin** (clientes que construyen
   `UiModifier(alignment = "start")` deben migrar a `UiModifier(alignment = Tokens.Alignment.Start)`).
   Verificado: hoy ningún screen del server lo usa.
4. The contract SHALL añadir un campo opcional `elevation: ElevationToken? = null` a `UiModifier`,
   compatible hacia atrás (default `null` → ignorado en clientes anteriores).
5. The contract SHALL **expandir** `Tokens.Radius` con la escala `Sm`, `Md`, `Lg`, `Xl`,
   **preservando** `None`, `Card`, `Pill` (los actuales) para no romper consumidores. La intención
   es que `Card` sea conceptualmente un alias semántico (el theme lo mapea a la misma shape que
   `Md` por defecto; el usuario puede re-mapearlo).
6. The contract SHALL preservar el orden y los campos existentes de `UiModifier` (sin breaking
   wire format más allá del cambio interno de tipo de `alignment`, que sigue serializando como
   string).

### HU-3 — Aplicar `UiModifier` en el motor
**Como** autor de pantallas **quiero** que `padding`/`background`/`shape`/etc. se honren en cada
nodo **para** que el server controle el layout sin componentes especializados.

Criterios (EARS):
1. The engine SHALL exponer una función pura `UiModifier.toModifier(theme: KuisdTheme): Modifier`
   (`internal`) que resuelve cada campo no nulo contra el theme y compone un `Modifier` de
   Compose. La función SHALL ser **testeable** sin UI.
2. The engine SHALL aplicar los modifiers en este **orden documentado y fijo**:
   `fillMaxWidth` → `fillMaxHeight` → `width` → `height` → `padding` →
   `background` + `cornerRadius` (combinados: `clip(shape)` + `background(color, shape)`).
   El orden es interno al motor; no es configurable por nodo.
3. The engine SHALL exponer `RenderScope.modifier: Modifier` (resuelto del `UiModifier` del nodo
   actual contra `LocalKuisdTheme.current`) para que cada renderer del `CorePack` lo aplique a su
   composable raíz.
4. WHEN `UiModifier` es `null` o todos sus campos relevantes son `null/false`, the engine SHALL
   exponer `Modifier` (identidad) — sin wrappers espurios.
5. WHEN un token referenciado por `UiModifier` no existe en el theme actual, the engine SHALL
   caer al default de Compose (Color.Unspecified / 0.dp) y emitir un `sduiLog` de warning una
   sola vez por (token, sesión) — no spam.
6. The engine SHALL aplicar el `UiModifier` también a los renderers actuales (`column`/`row`/
   `text`/`button`/`textField`). Esto es un cambio de comportamiento del MVP previo: el server
   que mandaba `padding` ya esperaba que se honrara (en 001 se documentó como pendiente).
7. **`alignment` aplicado en `column`/`row`.** WHEN un nodo `column`/`row` declara `alignment`
   en su `UiModifier`, the engine SHALL traducirlo a `Column(horizontalAlignment = …)` /
   `Row(verticalAlignment = …)` (alineación de los **hijos** del contenedor). Mapeo:
   `Start` → `Alignment.Start` (horizontal en LTR); `Center`/`CenterHorizontally` →
   `Alignment.CenterHorizontally`; `End` → `Alignment.End`; `Top` → `Alignment.Top` (vertical);
   `CenterVertically`/`Center` (en `row`) → `Alignment.CenterVertically`; `Bottom` →
   `Alignment.Bottom`. Si el token no encaja en el eje del contenedor (p.ej. `Top` en un `column`),
   the engine SHALL ignorarlo silenciosamente (documentado, sin crash). `alignment` en otros
   nodos (no contenedores) es **ignorado** para el MVP — requiere `BoxScope.align(...)` que
   solo es accesible dentro de un `Box`; spec posterior con `box`.
8. **`elevation` aplicado solo en `surface`/`card`.** WHEN un nodo distinto de `surface`/`card`
   declara `elevation` en su `UiModifier`, the engine SHALL ignorarlo silenciosamente (Compose
   no expresa "elevation" como `Modifier` plano; requiere `Surface` wrapper). En `surface`/`card`
   la `elevation` del nodo **se combina con** la de las props del componente (si solo una está
   presente, se usa esa; si ambas, gana la del componente — más específica).
9. **`weight` ignorado.** `UiModifier.weight` requiere `RowScope`/`ColumnScope` que no son
   accesibles desde la función pura `toModifier`. The engine SHALL ignorarlo en este MVP
   (documentado en §Riesgos del design); spec posterior si surge demanda real.

### HU-3bis — `IconRegistry` extensible
**Como** integrador **quiero** un registry abierto de iconos **para** que el server pueda
referenciarlos por nombre y la app pueda registrar iconos propios sin tocar el motor.

Criterios (EARS):
1. The engine SHALL exponer un tipo público `IconRegistry` con `byName: Map<String, ImageVector>`,
   un operador `plus(other: IconRegistry): IconRegistry` (override semantics), un builder DSL
   `iconRegistry { register(name, vector) }`, y un companion `Empty`.
2. The engine SHALL exponer un default `DefaultIconRegistry` con al menos: `home`, `search`,
   `settings`, `add`, `edit`, `delete`, `close`, `check`, `arrowBack`, `arrowForward`, `favorite`,
   `moreVert`. Iconos tomados de `Icons.Default.*` (Material Icons).
3. The engine SHALL exponer
   `LocalIconRegistry: ProvidableCompositionLocal<IconRegistry>` con default `DefaultIconRegistry`.
4. The app MAY proveer un override por `LocalIconRegistry` con
   `DefaultIconRegistry + iconRegistry { register("brand.logo", myVector) }` sin tocar el motor.
5. WHEN se renderiza un `icon` o `iconButton` cuyo `name` no existe en el registry actual, the
   engine SHALL renderizar un placeholder (icono `?` o `Icons.Default.HelpOutline`) y emitir
   un `sduiLog` (1 vez por nombre).

### HU-4 — Contenedores y visuales nuevos en `CorePack`
**Como** integrador **quiero** primitivos de superficie y listas **para** componer pantallas
ricas sin escribir Composables custom.

Criterios (EARS):
1. **`surface`** — The engine SHALL registrar `surface` en `CorePack` con
   `SurfaceProps(background: ColorToken? = null, shape: RadiusToken? = null,
   elevation: ElevationToken? = null, contentColor: ColorToken? = null)`. Renderiza un
   `Surface(...)` Material3 con los tokens resueltos y delega `renderChildren()` dentro.
2. **`card`** — The engine SHALL registrar `card` con
   `CardProps(background: ColorToken? = null, shape: RadiusToken? = null,
   elevation: ElevationToken? = null)`. Default opinionado: `shape = RoundedCornerShape(12.dp)`,
   `elevation = 1.dp` si no se especifica. Renderiza un `Card(...)` Material3.
3. **`divider`** — The engine SHALL registrar `divider` con
   `DividerProps(color: ColorToken? = null, thickness: SpaceToken? = null)`. Renderiza
   `HorizontalDivider(...)` Material3 (es el único; las filas de divisores verticales se reservan
   a una spec futura).
4. **`spacer`** — The engine SHALL registrar `spacer` con `SpacerProps(size: SpaceToken? = null)`.
   Renderiza `Spacer(Modifier.size(resolved))`. Si `size` es nulo, default `8.dp`.
5. **`lazyColumn`** / **`lazyRow`** — The engine SHALL registrar ambos con
   `LazyColumnProps`/`LazyRowProps` sin campos propios (todo viene del `UiModifier` del nodo).
   Itera `node.children` con `items(items = children, key = { it.id })` y delega a `RenderNode`.
6. **`icon`** — The engine SHALL registrar `icon` con
   `IconProps(name: String, tint: ColorToken? = null, size: SpaceToken? = null,
   contentDescription: String? = null)`. Resuelve `name` contra `LocalIconRegistry.current`;
   `tint`/`size` contra el theme; default size = 24 dp.
7. **`iconButton`** — The engine SHALL registrar `iconButton` con
   `IconButtonProps(name: String, tint: ColorToken? = null,
   contentDescription: String? = null)`. Renderiza `IconButton(onClick = { handler.handle(
   node.actions["onClick"].orEmpty()) }) { Icon(...) }`. Delegación al `LocalSduiActionHandler`
   (regla 003).
8. The engine SHALL respetar el `UiModifier` del nodo para los 7 nuevos componentes
   (cumplimiento de HU-3.3).
9. The system SHALL caer a `UnknownNode` si las props no decodifican (regla 004); ningún
   contenedor crashea por props mal formadas.

### HU-5 — Demo end-to-end servida por el server
**Como** demo **quiero** una pantalla nueva `feed` que combine `card` + `lazyColumn` + tokens +
iconos + alignment **para** validar el ciclo de punta a punta.

Criterios (EARS):
1. The server SHALL servir `GET /screen/feed` (200) con un envelope que contiene un `lazyColumn`
   raíz cuyas `children` son ≥5 `card`s. Cada card SHALL contener un `column` interno con
   `alignment = Tokens.Alignment.CenterHorizontally`, un `row` con un `icon` + `text` (título), un
   `divider`, y un `text` (cuerpo).
2. The server SHALL usar `padding`/`background`/`cornerRadius`/`elevation` tokenizados (referencias
   `Tokens.Space.Md`, `Tokens.Color.Surface`, `Tokens.Radius.Card`, `Tokens.Elevation.Sm`).
3. The server SHALL incluir un `iconButton` con `name="arrowBack"` en la cabecera del `feed` que
   emita `NavigateBack`.
4. The system SHALL enlazar `home → feed` por un `button` adicional en `home` con
   `Navigate(route="feed")` (extensión retrocompatible).
5. The visual smoke test (`:desktopApp:run`) SHALL mostrar las cards con espaciado, redondeo,
   sombra y iconos distintos del background; el botón `arrowBack` regresa a `home`; el `column`
   interno de las cards centra sus hijos horizontalmente.

### HU-6 — Override desde la app (demo de extensibilidad sin números mágicos)
**Como** usuario de la lib **quiero** sobrescribir tokens propios **para** demostrar que el
sistema es extensible — **sin embutir `dp` literales** en el código del override.

Criterios (EARS):
1. The app SHALL exponer `@Composable rememberAppTheme(): KuisdTheme` que internamente:
   (a) obtiene el base con `rememberMaterialKuisdTheme()`;
   (b) construye un override que re-mapea `Tokens.Radius.Card` a una shape resuelta a partir de
   **otro token** del base (p.ej. `base.resolveShapeOrNull(Tokens.Radius.Xl)`), evitando
   `RoundedCornerShape(16.dp)` literal en el código del override;
   (c) devuelve `base + override` memoizado.
2. The app SHALL exponer un `IconRegistry` resultante de
   `DefaultIconRegistry + appIconsOverride()` (vacío para el MVP, demuestra el patrón
   composicional) por `LocalIconRegistry` en el `SduiHost`.
3. The app SHALL proveer ambos (`LocalKuisdTheme` + `LocalIconRegistry`) en el
   `CompositionLocalProvider` del `SduiHost`, junto a los seams ya existentes.
4. WHEN se renderiza `feed`, the cards SHALL reflejar el override del theme (radio distinto del
   Material default, derivado del token `Tokens.Radius.Xl` del propio catálogo).
5. The implementation SHALL **no contener números literales `dp`** en el código del override;
   cualquier valor `dp` viene de resolver un token del theme base. Verificable por inspección
   (`grep ".dp" shared/.../theme/`  debe estar vacío salvo en defaults documentados, p.ej. un
   fallback de `error` si un token base faltase).

### HU-7 — Resiliencia
1. IF un token referenciado no está mapeado, THEN the engine SHALL aplicar el default de Compose
   sin crash y loguear una vez (HU-3.5).
2. IF las props de un contenedor no decodifican (`SurfaceProps`/`CardProps`/etc.), THEN the
   engine SHALL degradar a `UnknownNode` (regla 004).
3. IF un `lazyColumn` tiene 0 children, THEN the engine SHALL renderizarlo vacío (sin crash, sin
   placeholder).
4. IF `UiModifier.weight` se aplica a un nodo fuera de un `Row`/`Column`, THEN the engine SHALL
   ignorarlo (los weights de Compose son extensiones de `RowScope`/`ColumnScope`; aplicarlos fuera
   compilaría pero no haría efecto — documentado y silencioso).

## Requisitos no funcionales
- **Clean Architecture / OCP:** el motor (`:sdui-compose`) gana superficie nueva (theme +
  resolver + 5 componentes) **sin** importar `dev.kuisd.app.*` ni `io.ktor.*`. La app gana **solo**
  el override demo + el cableado en `SduiHost`. El contrato (`:sdui-core`) gana 1 type
  (`ElevationToken`) + 1 campo opcional en `UiModifier` (`elevation`) — cambio retrocompatible.
- **Motor puro:** sin estado mutable en el motor; el theme es inmutable; los maps son `Map`
  (no `MutableMap`).
- **OCP del theme:** añadir un token custom NO requiere modificar `:sdui-core` (basta usar
  `ColorToken("brand.primary")` directo desde la app).
- **Performance:** `UiModifier.toModifier` se calcula **una vez por nodo y por cambio de theme**
  (vía `remember(node.modifier, LocalKuisdTheme.current)` en `RenderScope`). Cero allocations en
  el camino crítico de recomposición si modifier/theme no cambian.
- **Multiplataforma:** todo en `commonMain`; sin APIs específicas de plataforma.
- **API mínima:** público nuevo en `:sdui-compose` = `KuisdTheme`, `KuisdThemeBuilder`,
  `kuisdTheme()`, `rememberMaterialKuisdTheme()`, `LocalKuisdTheme`, las 5 props de los
  contenedores. `UiModifier.toModifier` puede quedar `internal` (la app no lo necesita).
- **Resiliencia:** props inválidas → UnknownNode; tokens no mapeados → default + log (1 vez).
- **`detekt` / `ktlintCheck`** en verde.

## Dependencias y supuestos
- Depende de 001/003/004/005/006/007 (mergeadas).
- Reutiliza Material3 (`api(material3)` ya en `:sdui-compose` desde 006): `Surface`, `Card`,
  `HorizontalDivider`, `LazyColumn`, `LazyRow`, `Spacer`, `MaterialTheme`.
- Reutiliza `RenderScope` (004) y `LocalVariables`/`LocalSduiActionHandler` (005/003) intactos.
- El contrato gana **1 type** + **1 campo opcional** en `UiModifier`; clientes anteriores siguen
  funcionando (el JSON sin `elevation` decodifica a `elevation = null`).
- Supone que el orden de modifiers de Compose es consistente entre plataformas para los modifiers
  que aplicamos (verificado en docs Compose).
