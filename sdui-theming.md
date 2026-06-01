# Theming del SDUI

Cómo dar estilo a un SDUI sin ensuciar el JSON con valores crudos. Regla rectora:

> **El server referencia tokens semánticos por nombre; el cliente los resuelve contra el tema activo.** El backend nunca decide un color/tamaño literal. Dark mode, rebrand y multi-tenant viven en el cliente, sin tocar el server.

Alineado con el split de librerías: el **contrato de tokens** vive en `sdui-core`, el **resolver + provider** en `sdui-compose`, y el **tema concreto** (paleta, fuentes) lo aporta la app.

---

## 1. Los tres niveles de token (no mezclar)

```
Primitivos        →   Semánticos          →   De componente (opcional)
#0A84FF, 16dp         color.primary            button.primary.bg
Roboto 14sp           color.onSurface          card.radius
                      type.title               input.border
                      space.md
```

- **Primitivos**: la paleta cruda. **Nunca** viajan en el JSON. Viven solo en la app (el tema).
- **Semánticos**: lo que el server nombra (`color.primary`, `type.title`, `space.md`, `radius.card`). Es el **único vocabulario** que cruza el wire.
- **De componente**: opcionales, para casos donde un componente necesita su propio token (`button.primary.bg`). Resuelven a un semántico o primitivo en el cliente.

Por qué semánticos y no primitivos: si el server manda `#0A84FF`, el dark mode es imposible sin reenviar el árbol, y un rebrand obliga a cambiar el backend. Con `color.primary`, el cliente decide qué pinta ese nombre según tema/plataforma/tenant.

---

## 2. Contrato de tokens en `sdui-core`

El wire solo transporta **nombres de token** (strings) o, como escape hatch acotado, un literal. Un value class lo hace explícito y barato:

```kotlin
// sdui-core (commonMain)
@Serializable @JvmInline value class ColorToken(val ref: String)   // "color.primary"  | "#RRGGBB" (escape)
@Serializable @JvmInline value class TypeToken(val ref: String)    // "type.title"
@Serializable @JvmInline value class SpaceToken(val ref: String)   // "space.md"       | "12" (escape, dp)
@Serializable @JvmInline value class RadiusToken(val ref: String)  // "radius.card"
```

`UiModifier` deja de usar `String?`/`Int?` sueltos y pasa a tokens:

```kotlin
@Serializable
data class UiModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: SpaceToken? = null,
    val height: SpaceToken? = null,
    val padding: PaddingTokens? = null,
    val background: ColorToken? = null,
    val cornerRadius: RadiusToken? = null,
    val weight: Float? = null,
    val alignment: String? = null
)
@Serializable data class PaddingTokens(
    val l: SpaceToken? = null, val t: SpaceToken? = null,
    val r: SpaceToken? = null, val b: SpaceToken? = null
)
```

Catálogo de nombres **canónicos** publicado por la librería (constantes, para que server y cliente no escriban strings a mano y typos no compilen):

```kotlin
object Tokens {
    object Color { val Primary = ColorToken("color.primary"); val OnPrimary = ColorToken("color.onPrimary")
                   val Surface = ColorToken("color.surface"); val OnSurface = ColorToken("color.onSurface")
                   val Error = ColorToken("color.error"); val Outline = ColorToken("color.outline") }
    object Type  { val Display = TypeToken("type.display"); val Title = TypeToken("type.title")
                   val Body = TypeToken("type.body"); val Label = TypeToken("type.label"); val Button = TypeToken("type.button") }
    object Space { val Xs = SpaceToken("space.xs"); val Sm = SpaceToken("space.sm")
                   val Md = SpaceToken("space.md"); val Lg = SpaceToken("space.lg"); val Xl = SpaceToken("space.xl") }
    object Radius{ val None = RadiusToken("radius.none"); val Card = RadiusToken("radius.card"); val Pill = RadiusToken("radius.pill") }
}
```

> El vocabulario semántico es **parte del contrato**, igual que los componentes. Evoluciónalo de forma aditiva (agregar tokens es seguro; renombrar/quitar es breaking).

---

## 3. Resolución en el cliente (`sdui-compose`)

El tema es un mapa `token → valor real`, provisto vía `CompositionLocal`. La librería define la **forma** del tema y el resolver; la app aporta los **valores**.

```kotlin
// sdui-compose
@Immutable
data class SduiTheme(
    val colors: Map<String, Color>,
    val typography: Map<String, TextStyle>,
    val spacing: Map<String, Dp>,
    val radii: Map<String, Dp>,
    val fallbackColor: Color = Color.Unspecified,
)
val LocalSduiTheme = staticCompositionLocalOf { SduiTheme(emptyMap(), emptyMap(), emptyMap(), emptyMap()) }

@Composable fun rememberSduiTheme(): SduiTheme = LocalSduiTheme.current

// resolvers — toleran el escape hatch (literal) y degradan con gracia si falta el token
@Composable fun ColorToken?.resolve(): Color {
    val t = rememberSduiTheme(); if (this == null) return Color.Unspecified
    return when { ref.startsWith("#") -> ref.toColorOrNull() ?: t.fallbackColor
                  else -> t.colors[ref] ?: t.fallbackColor }
}
@Composable fun TypeToken?.resolve(): TextStyle { val t = rememberSduiTheme()
    return this?.let { t.typography[it.ref] } ?: LocalTextStyle.current }
@Composable fun SpaceToken?.resolve(): Dp { val t = rememberSduiTheme()
    return this?.let { t.spacing[it.ref] ?: it.ref.toIntOrNull()?.dp } ?: 0.dp }
@Composable fun RadiusToken?.resolve(): Dp { val t = rememberSduiTheme()
    return this?.let { t.radii[it.ref] } ?: 0.dp }
```

El mapper de modifier (ya en la librería) usa estos resolvers:

```kotlin
@Composable fun UiModifier.toCompose(): Modifier {
    var m: Modifier = Modifier
    if (fillMaxWidth) m = m.fillMaxWidth()
    width?.let { m = m.width(it.resolve()) }
    background?.let { m = m.background(it.resolve()) }
    cornerRadius?.let { m = m.clip(RoundedCornerShape(it.resolve())) }
    padding?.let { m = m.padding(it.l.resolve(), it.t.resolve(), it.r.resolve(), it.b.resolve()) }
    return m   // recordar: el ORDEN importa (padding vs background vs clip)
}
```

**Regla clave:** un token ausente **degrada** (color → `fallbackColor`, type → estilo actual, space → 0). Nunca crashea por un token desconocido, igual que `UnknownNode` para componentes.

---

## 4. El tema concreto lo aporta la app — puenteado con Material

Lo más cómodo en KMP/Compose: derivar el `SduiTheme` del `MaterialTheme` activo, así reusas `ColorScheme`/`Typography` de Material 3 y obtienes dark mode/dynamic color gratis.

```kotlin
// :app-client
@Composable
fun AppSduiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
                  typography = AppTypography) {
        val cs = MaterialTheme.colorScheme; val ty = MaterialTheme.typography
        val theme = remember(cs, ty) {
            SduiTheme(
                colors = mapOf(
                    "color.primary" to cs.primary, "color.onPrimary" to cs.onPrimary,
                    "color.surface" to cs.surface, "color.onSurface" to cs.onSurface,
                    "color.error" to cs.error, "color.outline" to cs.outline),
                typography = mapOf(
                    "type.display" to ty.displaySmall, "type.title" to ty.titleLarge,
                    "type.body" to ty.bodyMedium, "type.label" to ty.labelMedium,
                    "type.button" to ty.labelLarge),
                spacing = mapOf("space.xs" to 4.dp, "space.sm" to 8.dp, "space.md" to 16.dp,
                                "space.lg" to 24.dp, "space.xl" to 32.dp),
                radii = mapOf("radius.none" to 0.dp, "radius.card" to 16.dp, "radius.pill" to 999.dp),
                fallbackColor = cs.onSurface)
        }
        CompositionLocalProvider(LocalSduiTheme provides theme) { content() }
    }
}
```

Como el resolver lee `MaterialTheme` indirectamente a través del mapa, **dark mode es automático**: cambia `colorScheme`, el `SduiTheme` se recomputa, el árbol recompone. El server no se entera.

---

## 5. Qué decide cada lado (la línea divisoria)

| Aspecto | Server | Cliente |
|---|---|---|
| Qué componente, orden, anidamiento | ✅ | |
| Qué token semántico aplica (`color.primary`) | ✅ | |
| Qué pinta ese token (paleta real, dark/light) | | ✅ |
| Fuentes, escalas tipográficas | | ✅ |
| Escala de espaciado/radios (valores en dp) | | ✅ |
| Dark mode / dynamic color | | ✅ |
| Rebrand / multi-tenant | | ✅ (ver §6) |
| Densidad / accesibilidad (font scale) | | ✅ (Compose) |

El server piensa en **intención** (“esto es primario”, “esto es un título”); el cliente en **apariencia**. Mantén el JSON libre de hex y de px salvo el escape hatch acotado.

---

## 6. Temas múltiples: dark, rebrand, multi-tenant, A/B

Como el `SduiTheme` es solo un mapa inyectado por `CompositionLocal`, cambiar de tema es cambiar el mapa:

- **Dark / light**: dos `ColorScheme`; lo cubre `isSystemInDarkTheme()` o un toggle de usuario.
- **Multi-tenant / white-label**: cada tenant define su `SduiTheme` (su paleta y fuentes). El **mismo árbol** del server se ve distinto por tenant. Útil si tu app sirve varias marcas.
- **A/B de estilo**: experimenta con la *resolución* en cliente (otra paleta) sin tocar el server; o, si el experimento es estructural, que el server mande otro árbol. Prefiere lo primero para estilo puro.
- **Negociación opcional de tema**: el cliente puede mandar `X-SDUI-Theme: brandA` y el server adaptar contenido si hiciera falta — pero el **estilo** sigue resolviéndose en cliente. No muevas la paleta al server.

---

## 7. Overrides puntuales (con disciplina)

A veces el server necesita forzar un estilo concreto (un banner promocional con color de campaña). Tres niveles, de preferido a último recurso:

1. **Variante semántica** (preferido): define un token nuevo (`color.promo`) y deja que el cliente decida su valor. El server expresa intención, no apariencia.
2. **Override por componente vía props**: el componente acepta un token opcional en sus Props (`bannerStyle: ColorToken? = null`) que cae a un default semántico. Sigue siendo token, no hex.
3. **Escape hatch literal** (último recurso): `ColorToken("#FF5722")`. Permitido por el resolver, pero **rompe dark mode y rebrand** para ese nodo. Úsalo solo para contenido verdaderamente efímero (una campaña con color de marca externo) y márcalo en revisión.

> Disciplina: si te ves usando hex literal seguido, falta un token semántico. Agrégalo al catálogo de `core`.

---

## 8. Theming de componentes propios

Un componente de la app resuelve tokens igual que el CorePack, usando el `RenderScope`/resolvers de la librería:

```kotlin
// :app-client — renderer de un componente propio
register(PromoBanner) { p ->
    Surface(color = p.bg.resolve(), shape = RoundedCornerShape(Tokens.Radius.Card.resolve())) {
        Text(interpolate(p.text), style = Tokens.Type.Title.resolve(),
             color = Tokens.Color.OnPrimary.resolve(),
             modifier = Modifier.padding(Tokens.Space.Md.resolve()))
    }
}
```

El componente nunca conoce hex; pide tokens y la librería los resuelve contra el tema activo. Así un componente nuevo hereda dark mode y multi-tenant sin esfuerzo.

---

## 9. Testing del theming

- **Resolución**: tests de que cada token canónico de `Tokens` existe en el `SduiTheme` de la app (test que recorre `Tokens` por reflexión/lista y verifica presencia) → atrapa tokens emitidos por el server sin valor en cliente.
- **Degradación**: token inexistente → fallback, no excepción.
- **Snapshot por tema**: el mismo árbol renderizado en light, dark y un tenant alterno (screenshot tests) — el engine es Kotlin puro en commonMain, testeable sin device.
- **Contrato**: el catálogo de tokens es parte del wire; round-trip de serialización de `UiModifier` con tokens.

---

## 10. Resumen ejecutable

1. [ ] En `sdui-core`: value classes `ColorToken/TypeToken/SpaceToken/RadiusToken`, `UiModifier` basado en tokens, y el catálogo `Tokens` canónico.
2. [ ] En `sdui-compose`: `SduiTheme` + `LocalSduiTheme`, resolvers `.resolve()` con degradación, mapper de modifier sobre tokens.
3. [ ] En `:app-client`: `AppSduiTheme` que deriva `SduiTheme` de `MaterialTheme` (light/dark) e inyecta el `CompositionLocal`.
4. [ ] Server: usar **solo** constantes de `Tokens` (nunca hex) al ensamblar; reservar el escape hatch literal para casos efímeros.
5. [ ] Multi-tenant/rebrand: un `SduiTheme` por marca; mismo árbol, distinta resolución.
6. [ ] Tests: presencia de tokens, degradación, snapshots por tema.

**Línea divisoria, una vez más:** server = intención semántica; cliente = apariencia. El JSON transporta nombres de token, no estilos. Eso es lo que mantiene el theming limpio, con dark mode y rebrand gratis y el backend ajeno a cómo se ve la app.
