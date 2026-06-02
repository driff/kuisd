package dev.kuisd.sdui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier
import dev.kuisd.sdui.theme.KuisdTheme
import dev.kuisd.sdui.theme.kuisdTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UiModifierResolverTest {
    private val theme: KuisdTheme = kuisdTheme {
        color(Tokens.Color.Surface, Color.White)
        space(Tokens.Space.Md, 12.dp)
    }

    @Test
    fun null_modifier_returns_identity() {
        val um: UiModifier? = null
        assertEquals(Modifier, um.toModifier(theme))
    }

    @Test
    fun empty_modifier_returns_identity() {
        assertEquals(Modifier, UiModifier().toModifier(theme))
    }

    @Test
    fun fillMaxWidth_produces_a_non_identity_modifier() {
        val result = UiModifier(fillMaxWidth = true).toModifier(theme)
        assertNotEquals(Modifier, result, "fillMaxWidth debería producir un Modifier no-identidad")
    }

    @Test
    fun padding_with_token_produces_a_non_identity_modifier() {
        val result = UiModifier(padding = PaddingTokens(l = Tokens.Space.Md))
            .toModifier(theme)
        assertNotEquals(Modifier, result)
    }

    @Test
    fun unresolved_token_in_theme_does_not_crash() {
        // padding referencia un token que no está en el Empty theme → resuelve a 0.dp en cada
        // lado; el `.padding(...)` se aplica igual (sin crash) y produce un Modifier no-identidad.
        val result = UiModifier(padding = PaddingTokens(l = Tokens.Space.Md))
            .toModifier(KuisdTheme.Empty)
        assertNotEquals(Modifier, result)
    }
}
