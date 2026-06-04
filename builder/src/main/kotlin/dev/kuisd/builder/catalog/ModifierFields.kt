package dev.kuisd.builder.catalog

import dev.kuisd.sdui.core.AlignmentToken
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SpaceToken
import dev.kuisd.sdui.core.UiModifier

/** Clave del campo enum de alineación del contenedor. */
internal const val KEY_ALIGNMENT = "alignment"

/** Clave del campo enum de padding (token único a los 4 lados en v1). */
internal const val KEY_PADDING = "padding"

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
