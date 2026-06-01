package dev.kuisd.sdui.modifier

import androidx.compose.ui.Alignment
import dev.kuisd.sdui.core.AlignmentToken
import dev.kuisd.sdui.core.Tokens

/**
 * Traduce un [AlignmentToken] al eje horizontal de un `Column` (alineación de sus hijos).
 * Devuelve `null` cuando el token solo aplica al eje vertical (`Top`/`Bottom`/`CenterVertically`).
 */
internal fun AlignmentToken?.toHorizontalAlignment(): Alignment.Horizontal? = when (this) {
    Tokens.Alignment.Start -> Alignment.Start
    Tokens.Alignment.Center, Tokens.Alignment.CenterHorizontally -> Alignment.CenterHorizontally
    Tokens.Alignment.End -> Alignment.End
    else -> null
}

/**
 * Traduce un [AlignmentToken] al eje vertical de un `Row` (alineación de sus hijos).
 * Devuelve `null` cuando el token solo aplica al eje horizontal
 * (`Start`/`End`/`CenterHorizontally`).
 */
internal fun AlignmentToken?.toVerticalAlignment(): Alignment.Vertical? = when (this) {
    Tokens.Alignment.Top -> Alignment.Top
    Tokens.Alignment.Center, Tokens.Alignment.CenterVertically -> Alignment.CenterVertically
    Tokens.Alignment.Bottom -> Alignment.Bottom
    else -> null
}
