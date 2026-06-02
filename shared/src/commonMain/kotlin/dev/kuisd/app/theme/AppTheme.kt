package dev.kuisd.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.theme.KuisdTheme
import dev.kuisd.sdui.theme.kuisdTheme
import dev.kuisd.sdui.theme.rememberMaterialKuisdTheme

/**
 * Theme de la app: base Material + override **sin números literales**. El override re-mapea
 * `Tokens.Radius.Card` a la shape de `Tokens.Radius.Xl` del propio catálogo base (visible: cards
 * más redondeadas que el default Material). El único `dp` literal del sistema vive en
 * `rememberMaterialKuisdTheme`; este override deriva de él vía resoluciones.
 */
@Composable
internal fun rememberAppTheme(): KuisdTheme {
    val base = rememberMaterialKuisdTheme()
    return remember(base) {
        // Invariante: el base es `rememberMaterialKuisdTheme()`, que SIEMPRE mapea Radius.Xl
        // (spec 008). El fallback a `Card` cubre el caso teórico de un base custom incompleto
        // sin romper el render (degradación: el card mantiene su shape original).
        val cardShape = base.resolveShapeOrNull(Tokens.Radius.Xl)
            ?: base.resolveShapeOrNull(Tokens.Radius.Card)
        if (cardShape == null) {
            base
        } else {
            base + kuisdTheme { shape(Tokens.Radius.Card, cardShape) }
        }
    }
}
