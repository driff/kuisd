package dev.kuisd.sdui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.Tokens

/**
 * Construye el `KuisdTheme` base derivando de `MaterialTheme.colorScheme`/`shapes` y completando
 * con escalas estándar para spacing (4/8/12/16/24 dp) y elevation (0/1/3/6 dp). Es la **única
 * fuente de valores `dp` literales** en todo el sistema; cualquier override de la app deriva de
 * estos valores resolviendo otros tokens.
 *
 * Memoizado por `(colorScheme, shapes)` para reaccionar a cambios de Material (p.ej. dark mode).
 */
@Composable
fun rememberMaterialKuisdTheme(): KuisdTheme {
    val cs = MaterialTheme.colorScheme
    val sh = MaterialTheme.shapes
    return remember(cs, sh) {
        kuisdTheme {
            // Colors
            color(Tokens.Color.Primary, cs.primary)
            color(Tokens.Color.OnPrimary, cs.onPrimary)
            color(Tokens.Color.Surface, cs.surface)
            color(Tokens.Color.OnSurface, cs.onSurface)
            color(Tokens.Color.Error, cs.error)
            color(Tokens.Color.Outline, cs.outline)

            // Shapes — escala radius → MaterialTheme.shapes
            shape(Tokens.Radius.None, RectangleShape)
            shape(Tokens.Radius.Sm, sh.extraSmall)
            shape(Tokens.Radius.Md, sh.small)
            shape(Tokens.Radius.Lg, sh.medium)
            shape(Tokens.Radius.Xl, sh.large)
            shape(Tokens.Radius.Card, sh.medium)
            shape(Tokens.Radius.Pill, RoundedCornerShape(percent = 50))

            // Space — escala estándar Material
            space(Tokens.Space.Xs, 4.dp)
            space(Tokens.Space.Sm, 8.dp)
            space(Tokens.Space.Md, 12.dp)
            space(Tokens.Space.Lg, 16.dp)
            space(Tokens.Space.Xl, 24.dp)

            // Elevation
            elevation(Tokens.Elevation.None, 0.dp)
            elevation(Tokens.Elevation.Sm, 1.dp)
            elevation(Tokens.Elevation.Md, 3.dp)
            elevation(Tokens.Elevation.Lg, 6.dp)
        }
    }
}
