package dev.kuisd.sdui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
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

/** Catálogo base del motor (specs 001/003/004/005/007 + 008). */
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
    val shape: Shape = theme.resolveShapeOrNull(p.shape)
        ?: theme.resolveShapeOrNull(Tokens.Radius.Card)
        ?: RoundedCornerShape(12.dp)
    val containerColor = theme.resolveColorOrNull(p.background)
    val elevation = theme.resolveElevationOrNull(p.elevation)
        ?: theme.resolveElevationOrNull(Tokens.Elevation.Sm)
        ?: 1.dp
    Card(
        modifier = baseModifier,
        shape = shape,
        colors = if (containerColor != null) {
            CardDefaults.cardColors(containerColor = containerColor)
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
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
    val size = theme.resolveSpaceOrNull(p.size) ?: 8.dp
    Spacer(modifier = baseModifier.then(Modifier.size(size)))
}

@Composable
private fun RenderScope.IconRenderer(p: IconProps, baseModifier: Modifier) {
    val theme = LocalKuisdTheme.current
    val registry = LocalIconRegistry.current
    val vector = registry.get(p.name) ?: Icons.AutoMirrored.Outlined.HelpOutline
    val size = theme.resolveSpaceOrNull(p.size) ?: 24.dp
    Icon(
        imageVector = vector,
        contentDescription = p.contentDescription,
        modifier = baseModifier.then(Modifier.size(size)),
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
    val registry = LocalIconRegistry.current
    val handler = LocalSduiActionHandler.current
    val vector = registry.get(p.name) ?: Icons.AutoMirrored.Outlined.HelpOutline
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
