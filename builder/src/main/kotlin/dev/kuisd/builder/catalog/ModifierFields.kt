package dev.kuisd.builder.catalog

import dev.kuisd.sdui.core.AlignmentToken
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SpaceToken
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier

/** Clave del campo enum de alineación (de contenedor en 017, o por-hijo en 019). */
internal const val KEY_ALIGNMENT = "alignment"

/** Clave del campo enum de padding (token único a los 4 lados en v1). */
internal const val KEY_PADDING = "padding"

/** Clave del campo numérico de weight por-hijo (spec 019). */
internal const val KEY_WEIGHT = "weight"

/** Opciones de alineación HORIZONTAL (hijo de Column o el propio Column): refs de `AlignmentToken`. */
internal val alignHorizontalRefs = listOf(
    Tokens.Alignment.Start.ref,
    Tokens.Alignment.CenterHorizontally.ref,
    Tokens.Alignment.End.ref,
)

/** Opciones de alineación VERTICAL (hijo de Row o el propio Row): refs de `AlignmentToken`. */
internal val alignVerticalRefs = listOf(
    Tokens.Alignment.Top.ref,
    Tokens.Alignment.CenterVertically.ref,
    Tokens.Alignment.Bottom.ref,
)

/** Valor actual (ref del token) del campo enum [key] del modifier, o "" si no asignado. */
internal fun modifierEnumValue(modifier: UiModifier, key: String): String = when (key) {
    KEY_ALIGNMENT -> modifier.alignment?.ref.orEmpty()
    KEY_PADDING -> modifier.padding?.l?.ref.orEmpty() // v1: 4 lados iguales ⇒ se representa con el lado l
    else -> ""
}

/** Copia del modifier con [key] = [ref]; [ref] vacío ⇒ limpia el campo (null). */
internal fun withModifierEnum(modifier: UiModifier, key: String, ref: String): UiModifier = when (key) {
    KEY_ALIGNMENT -> modifier.copy(alignment = ref.ifEmpty { null }?.let(::AlignmentToken))
    KEY_PADDING -> modifier.copy(padding = ref.ifEmpty { null }?.let { paddingAll(SpaceToken(it)) })
    else -> modifier
}

/** PaddingTokens con el mismo token en los 4 lados (v1). */
internal fun paddingAll(s: SpaceToken): PaddingTokens = PaddingTokens(l = s, t = s, r = s, b = s)

/** Etiqueta corta para una opción (último segmento del ref): "alignment.start" → "start". */
internal fun optionLabel(ref: String): String = ref.substringAfterLast('.')

/** Texto del `weight` del modifier para el editor numérico ("" si no asignado; entero sin decimales). */
internal fun modifierWeight(modifier: UiModifier): String =
    modifier.weight?.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() }.orEmpty()

/** Copia del modifier con `weight` = [text] si es un Float > 0; en otro caso (vacío/inválido/≤0) lo limpia. */
internal fun withModifierWeight(modifier: UiModifier, text: String): UiModifier {
    val w = text.trim().toFloatOrNull()
    return modifier.copy(weight = if (w != null && w > 0f && w.isFinite()) w else null)
}
