package dev.kuisd.sdui.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.UiModifier
import dev.kuisd.sdui.theme.KuisdTheme

/**
 * Resuelve un [UiModifier] del contrato contra el [theme] actual y construye un [Modifier]
 * de Compose. Orden fijo y documentado:
 *
 * `fillMaxWidth` → `fillMaxHeight` → `width` → `height` → `padding`
 * → `clip(cornerRadius)` + `background(background, cornerRadius)`
 *
 * Nota sobre `background` sin `cornerRadius`: si hay `background` pero el `cornerRadius` no
 * resuelve (o no se declara), se pinta `background(color, RectangleShape)` **sin** `clip` previo
 * — es un rectángulo plano y el contenido NO se recorta a una forma. Para recortar, declara
 * también `cornerRadius`.
 *
 * Campos no aplicados aquí (por requerir contexto de scope o wrappers especiales):
 *  - `weight`     → requiere `RowScope`/`ColumnScope`; ignorado en MVP (HU-3.9).
 *  - `elevation`  → requiere `Surface`; lo consumen `surface`/`card` directamente (HU-3.8).
 *  - `alignment`  → traducido a `Column.horizontalAlignment` / `Row.verticalAlignment` desde
 *                   los renderers de `column`/`row` (HU-3.7).
 */
internal fun UiModifier?.toModifier(theme: KuisdTheme): Modifier {
    val um = this ?: return Modifier
    var m: Modifier = Modifier
    if (um.fillMaxWidth) m = m.fillMaxWidth()
    if (um.fillMaxHeight) m = m.fillMaxHeight()
    theme.resolveSpaceOrNull(um.width)?.let { m = m.width(it) }
    theme.resolveSpaceOrNull(um.height)?.let { m = m.height(it) }
    um.padding?.let { p ->
        m = m.padding(
            start = theme.resolveSpaceOrNull(p.l) ?: 0.dp,
            top = theme.resolveSpaceOrNull(p.t) ?: 0.dp,
            end = theme.resolveSpaceOrNull(p.r) ?: 0.dp,
            bottom = theme.resolveSpaceOrNull(p.b) ?: 0.dp,
        )
    }
    val resolvedShape = theme.resolveShapeOrNull(um.cornerRadius)
    val resolvedBg = theme.resolveColorOrNull(um.background)
    if (resolvedShape != null) m = m.clip(resolvedShape)
    if (resolvedBg != null) m = m.background(resolvedBg, resolvedShape ?: RectangleShape)
    return m
}
