package dev.kuisd.sdui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Modifier
import dev.kuisd.sdui.core.UiModifier
import dev.kuisd.sdui.modifier.toHorizontalAlignment
import dev.kuisd.sdui.modifier.toVerticalAlignment

/**
 * Modificadores de SCOPE que un `Column` impone a un hijo a partir de su [UiModifier] (spec 019):
 * `weight` (si > 0) y `align` horizontal (si el `alignment` resuelve al eje). Son `ParentDataModifier`,
 * así que solo pueden construirse dentro de un [ColumnScope]; se pasan a `RenderNode(child, …)`.
 */
internal fun ColumnScope.childLayout(um: UiModifier): Modifier {
    var m: Modifier = Modifier
    um.weight?.takeIf { it > 0f && it.isFinite() }?.let { m = m.weight(it) }
    um.alignment.toHorizontalAlignment()?.let { m = m.align(it) }
    return m
}

/** Equivalente para `Row`: `weight` (si > 0 y finito) y `align` vertical (si el `alignment` resuelve al eje). */
internal fun RowScope.childLayout(um: UiModifier): Modifier {
    var m: Modifier = Modifier
    um.weight?.takeIf { it > 0f && it.isFinite() }?.let { m = m.weight(it) }
    um.alignment.toVerticalAlignment()?.let { m = m.align(it) }
    return m
}
