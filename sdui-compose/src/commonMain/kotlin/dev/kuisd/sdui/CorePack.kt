package dev.kuisd.sdui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.ColorToken
import dev.kuisd.sdui.core.ElevationToken
import dev.kuisd.sdui.core.RadiusToken
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.SpaceToken
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.sduiComponent
import dev.kuisd.sdui.icons.LocalIconRegistry
import dev.kuisd.sdui.theme.LocalKuisdTheme
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
class ColumnProps

@Serializable
class RowProps

@Serializable
data class TextProps(
    val text: String = "",
    val style: String? = null,
)

@Serializable
data class ButtonProps(
    val label: String = "",
)

/**
 * Props del input two-way (spec 007). API state-based: el `TextFieldState` posee el texto y la
 * selección; el sync hacia el store es vía `snapshotFlow` colectado en un `LaunchedEffect`.
 *
 * - `bind`: nombre **directo** de la variable a enlazar (sin `$`); `""` => uncontrolled (sin emisión).
 * - `placeholder`: texto Material3 mostrado cuando el campo está vacío.
 * - `label`: etiqueta opcional; resuelta por `bind(label)` (admite `$ref` por la regla 005).
 */
@Serializable
data class TextFieldProps(
    val bind: String = "",
    val placeholder: String = "",
    val label: String? = null,
)

@Serializable
data class SurfaceProps(
    val background: ColorToken? = null,
    val shape: RadiusToken? = null,
    val elevation: ElevationToken? = null,
    val contentColor: ColorToken? = null,
)

@Serializable
data class CardProps(
    val background: ColorToken? = null,
    val shape: RadiusToken? = null,
    val elevation: ElevationToken? = null,
)

@Serializable
data class DividerProps(
    val color: ColorToken? = null,
    val thickness: SpaceToken? = null,
)

@Serializable
data class SpacerProps(
    val size: SpaceToken? = null,
)

@Serializable
class LazyColumnProps

@Serializable
class LazyRowProps

@Serializable
data class IconProps(
    val name: String = "",
    val tint: ColorToken? = null,
    val size: SpaceToken? = null,
    val contentDescription: String? = null,
)

@Serializable
data class IconButtonProps(
    val name: String = "",
    val tint: ColorToken? = null,
    val contentDescription: String? = null,
)

/**
 * Slots por children + apariencia del `UiModifier` (HU-1). `contentDirection` decide cómo se apilan
 * los nodos del slot content: `"column"` (vertical, por defecto) o `"row"` (horizontal). Sin esto, un
 * content de 2+ nodos se solaparía en un `Box`.
 */
@Serializable
data class ScaffoldProps(
    val contentDirection: String = "column",
)

/** Barra superior: título bindable e icono de navegación opcional (HU-2). */
@Serializable
data class TopAppBarProps(
    val title: String = "",
    val navigationIcon: String? = null,
)

/** Barra inferior: `selectedBind` = nombre DIRECTO de la variable de selección (HU-3). */
@Serializable
data class BottomBarProps(
    val selectedBind: String? = null,
)

/** Ítem de `bottomBar`; sus props se decodifican dentro del renderer (no es componente standalone). */
@Serializable
data class BottomBarItemProps(
    val icon: String = "",
    val label: String = "",
    val value: String = "",
)

/** Catálogo base del motor (specs 001/003/004/005/007 + 008 + 010). */
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
    register(sduiComponent<TextProps>("text")) { p ->
        Text(text = bind(p.text), modifier = modifier)
    }
    register(sduiComponent<ButtonProps>("button")) { p ->
        val handler = LocalSduiActionHandler.current
        Button(
            onClick = { handler.handle(node.actions["onClick"].orEmpty()) },
            modifier = modifier,
        ) { Text(bind(p.label)) }
    }
    register(sduiComponent<TextFieldProps>("textField")) { p -> TextFieldRenderer(p, modifier) }
    register(sduiComponent<SurfaceProps>("surface")) { p -> SurfaceRenderer(p, modifier) { renderChildren() } }
    register(sduiComponent<CardProps>("card")) { p -> CardRenderer(p, modifier) { renderChildren() } }
    register(sduiComponent<DividerProps>("divider")) { p -> DividerRenderer(p, modifier) }
    register(sduiComponent<SpacerProps>("spacer")) { p -> SpacerRenderer(p, modifier) }
    register(sduiComponent<LazyColumnProps>("lazyColumn")) {
        LazyColumn(modifier = modifier) {
            itemsIndexed(
                items = node.children,
                key = { idx, child -> child.id ?: "$idx-${child.type}" },
            ) { _, child -> RenderNode(child) }
        }
    }
    register(sduiComponent<LazyRowProps>("lazyRow")) {
        LazyRow(modifier = modifier) {
            itemsIndexed(
                items = node.children,
                key = { idx, child -> child.id ?: "$idx-${child.type}" },
            ) { _, child -> RenderNode(child) }
        }
    }
    register(sduiComponent<IconProps>("icon")) { p -> IconRenderer(p, modifier) }
    register(sduiComponent<IconButtonProps>("iconButton")) { p ->
        IconButtonRenderer(p, modifier, node.actions["onClick"].orEmpty())
    }
    register(sduiComponent<ScaffoldProps>("scaffold")) { p -> ScaffoldRenderer(p, modifier) }
    register(sduiComponent<TopAppBarProps>("topAppBar")) { p -> TopAppBarRenderer(p, modifier) }
    register(sduiComponent<BottomBarProps>("bottomBar")) { p -> BottomBarRenderer(p, modifier) }
}

/**
 * Renderer del `textField`. Acota el `@OptIn(ExperimentalMaterial3Api::class)` a esta función para
 * no contaminar todo `CorePack`.
 *
 * Semilla puntual: `vars.get(p.bind)?.asDisplayString()` se calcula UNA SOLA VEZ por `p.bind` y por
 * slot de composición vía `remember(p.bind)`.
 *
 * Nota sobre la key del `remember`: deliberadamente NO incluye `LocalVariables.current` como
 * dependencia. Si el `VariableScope` provisto cambia en runtime, la semilla recordada queda fija
 * — el `TextFieldState` es local-source-of-truth tras la 1ª composición.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderScope.TextFieldRenderer(p: TextFieldProps, baseModifier: Modifier) {
    val handler = LocalSduiActionHandler.current
    val vars = LocalVariables.current
    val initial = remember(p.bind) { vars.get(p.bind)?.asDisplayString().orEmpty() }
    val state = rememberTextFieldState(initial)

    if (p.bind.isNotEmpty()) {
        LaunchedEffect(state, p.bind) {
            snapshotFlow { state.text.toString() }
                .drop(1)
                .collect { newText ->
                    handler.handle(listOf(SetVar(p.bind, JsonPrimitive(newText))))
                }
        }
    }

    OutlinedTextField(
        state = state,
        modifier = baseModifier,
        placeholder = { Text(p.placeholder) },
        label = p.label?.let { { Text(bind(it)) } },
    )
}

@Composable
private fun RenderScope.SurfaceRenderer(
    p: SurfaceProps,
    baseModifier: Modifier,
    content: @Composable () -> Unit,
) {
    val theme = LocalKuisdTheme.current
    Surface(
        modifier = baseModifier,
        color = theme.resolveColorOrNull(p.background) ?: Color.Unspecified,
        shape = theme.resolveShapeOrNull(p.shape) ?: RectangleShape,
        tonalElevation = theme.resolveElevationOrNull(p.elevation) ?: 0.dp,
        contentColor = theme.resolveColorOrNull(p.contentColor) ?: LocalContentColor.current,
    ) { content() }
}

@Composable
private fun RenderScope.CardRenderer(
    p: CardProps,
    baseModifier: Modifier,
    content: @Composable () -> Unit,
) {
    val theme = LocalKuisdTheme.current
    // Fallbacks neutros = defaults de Material3 (CardDefaults), NO literales: el motor no
    // materializa números `dp` propios. Con `KuisdTheme.Empty` el card cae a su apariencia
    // Material por defecto (degradación visible, no crash).
    val shape = theme.resolveShapeOrNull(p.shape) ?: theme.resolveShapeOrNull(Tokens.Radius.Card)
    val containerColor = theme.resolveColorOrNull(p.background)
    val elevationDp = theme.resolveElevationOrNull(p.elevation)
        ?: theme.resolveElevationOrNull(Tokens.Elevation.Sm)
    Card(
        modifier = baseModifier,
        shape = shape ?: CardDefaults.shape,
        colors = if (containerColor != null) {
            CardDefaults.cardColors(containerColor = containerColor)
        } else {
            CardDefaults.cardColors()
        },
        elevation = if (elevationDp != null) {
            CardDefaults.cardElevation(defaultElevation = elevationDp)
        } else {
            CardDefaults.cardElevation()
        },
    ) { content() }
}

@Composable
private fun RenderScope.DividerRenderer(p: DividerProps, baseModifier: Modifier) {
    val theme = LocalKuisdTheme.current
    HorizontalDivider(
        modifier = baseModifier,
        color = theme.resolveColorOrNull(p.color) ?: DividerDefaults.color,
        thickness = theme.resolveSpaceOrNull(p.thickness) ?: DividerDefaults.Thickness,
    )
}

@Composable
private fun RenderScope.SpacerRenderer(p: SpacerProps, baseModifier: Modifier) {
    val theme = LocalKuisdTheme.current
    // Sin token resuelto → 0.dp (neutro, sin número mágico). Un spacer sin `size` no separa.
    val size = theme.resolveSpaceOrNull(p.size) ?: 0.dp
    Spacer(modifier = baseModifier.size(size))
}

/** Resuelve un icono por nombre contra el [LocalIconRegistry]; fallback visible si no está registrado. */
@Composable
private fun resolveIcon(name: String) =
    LocalIconRegistry.current.get(name) ?: Icons.AutoMirrored.Outlined.HelpOutline

@Composable
private fun RenderScope.IconRenderer(p: IconProps, baseModifier: Modifier) {
    val theme = LocalKuisdTheme.current
    val vector = resolveIcon(p.name)
    // Sin token de tamaño → no se aplica modifier de size: el Icon usa su tamaño intrínseco
    // (Material default), evitando materializar un `dp` literal en el motor.
    val sizeModifier = theme.resolveSpaceOrNull(p.size)?.let { Modifier.size(it) } ?: Modifier
    Icon(
        imageVector = vector,
        contentDescription = p.contentDescription,
        modifier = baseModifier.then(sizeModifier),
        tint = theme.resolveColorOrNull(p.tint) ?: LocalContentColor.current,
    )
}

@Composable
private fun RenderScope.IconButtonRenderer(
    p: IconButtonProps,
    baseModifier: Modifier,
    actions: List<dev.kuisd.sdui.core.UiAction>,
) {
    val theme = LocalKuisdTheme.current
    val handler = LocalSduiActionHandler.current
    val vector = resolveIcon(p.name)
    IconButton(
        onClick = { handler.handle(actions) },
        modifier = baseModifier,
    ) {
        Icon(
            imageVector = vector,
            contentDescription = p.contentDescription,
            tint = theme.resolveColorOrNull(p.tint) ?: LocalContentColor.current,
        )
    }
}

/**
 * Insets cero para el `scaffold`/barras del motor. El host (`SduiHost`) es el ÚNICO dueño de los
 * `WindowInsets` del sistema: su `Scaffold` los consume y entrega el `padding` ya acotado. El
 * `scaffold` del motor vive DENTRO de ese área, así que es "inset-naive" para no duplicar el padding
 * (status/navigation bar contado dos veces). Limitación conocida: si una pantalla NO-raíz adopta
 * `scaffold` con `canGoBack`, su `topAppBar` quedaría apilado bajo la barra "Atrás" del host
 * (follow-up: mover el chrome de navegación al árbol SDUI).
 */
private val EngineBarInsets = WindowInsets(0, 0, 0, 0)

/**
 * Renderer del `scaffold`: monta `material3.Scaffold` con los slots resueltos por `type` (HU-1).
 * El `innerPadding` se aplica al `Column`/`Row` que envuelve el content (RenderNode no acepta
 * modifier); cada child conserva su propio `UiModifier`. Barras ausentes ⇒ lambda vacía (slot omitido).
 */
@Composable
private fun RenderScope.ScaffoldRenderer(p: ScaffoldProps, baseModifier: Modifier) {
    val slots = remember(node.children) { partitionScaffoldSlots(node.children) }
    Scaffold(
        modifier = baseModifier,
        contentWindowInsets = EngineBarInsets,
        topBar = { slots.topBar?.let { RenderNode(it) } },
        bottomBar = { slots.bottomBar?.let { RenderNode(it) } },
    ) { innerPadding ->
        // Apila el content (no `Box`, que solaparía 2+ nodos). `row` → horizontal; resto → vertical.
        val contentModifier = Modifier.padding(innerPadding)
        if (p.contentDirection == "row") {
            Row(contentModifier) { slots.content.forEach { RenderNode(it) } }
        } else {
            Column(contentModifier) { slots.content.forEach { RenderNode(it) } }
        }
    }
}

/**
 * Renderer del `topAppBar` (HU-2): título bindable, icono de navegación SOLO si hay
 * `onNavigationClick`, y children como acciones a la derecha. `@OptIn` acotado al renderer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderScope.TopAppBarRenderer(p: TopAppBarProps, baseModifier: Modifier) {
    val handler = LocalSduiActionHandler.current
    val navActions = node.actions["onNavigationClick"].orEmpty()
    TopAppBar(
        modifier = baseModifier,
        windowInsets = EngineBarInsets,
        title = { Text(bind(p.title)) },
        navigationIcon = {
            if (navActions.isNotEmpty()) {
                IconButton(onClick = { handler.handle(navActions) }) {
                    Icon(resolveIcon(p.navigationIcon.orEmpty()), contentDescription = null)
                }
            }
        },
        actions = { renderChildren() },
    )
}

/**
 * Renderer del `bottomBar` (HU-3): un `NavigationBarItem` por child decodificable a
 * `BottomBarItemProps`; los no-ítem se ignoran. `selectedBind` se lee reactivamente del store
 * (005) y marca seleccionado el ítem cuyo `value` coincide.
 */
@Composable
private fun RenderScope.BottomBarRenderer(p: BottomBarProps, baseModifier: Modifier) {
    val handler = LocalSduiActionHandler.current
    val vars = LocalVariables.current
    // `selectedBind` admite el nombre directo o con `$` (se normaliza por `removePrefix`); la lectura
    // del store es reactiva (005): al cambiar la variable, el ítem activo se recompone.
    val selectedValue = p.selectedBind?.removePrefix("$")?.let { vars.get(it)?.asDisplayString() }
    // Decodifica los ítems UNA vez por `node.children` (no en cada recomposición de la barra). Guard por
    // `type`: con `ignoreUnknownKeys=true` cualquier child decodificaría a props por defecto (HU-3.2/4.2).
    val items = remember(node.children) {
        node.children
            .filter { it.type == "bottomBarItem" }
            .mapNotNull { child -> decodeOrNull<BottomBarItemProps>(child)?.let { child to it } }
    }
    NavigationBar(modifier = baseModifier, windowInsets = EngineBarInsets) {
        items.forEach { (child, item) ->
            NavigationBarItem(
                selected = isItemSelected(selectedValue, item.value),
                onClick = { handler.handle(child.actions["onClick"].orEmpty()) },
                icon = { Icon(resolveIcon(item.icon), contentDescription = null) },
                label = { Text(bind(item.label)) },
            )
        }
    }
}
