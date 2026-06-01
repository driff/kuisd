package dev.kuisd.sdui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.Tokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KuisdThemeTest {
    @Test
    fun empty_theme_resolves_to_null_for_every_token() {
        assertNull(KuisdTheme.Empty.resolveColorOrNull(Tokens.Color.Primary))
        assertNull(KuisdTheme.Empty.resolveShapeOrNull(Tokens.Radius.Card))
        assertNull(KuisdTheme.Empty.resolveSpaceOrNull(Tokens.Space.Md))
        assertNull(KuisdTheme.Empty.resolveElevationOrNull(Tokens.Elevation.Sm))
    }

    @Test
    fun resolveOrNull_lookups_match() {
        val theme = kuisdTheme {
            color(Tokens.Color.Primary, Color.Red)
            shape(Tokens.Radius.Card, RoundedCornerShape(8.dp))
            space(Tokens.Space.Md, 12.dp)
            elevation(Tokens.Elevation.Sm, 1.dp)
        }
        assertEquals(Color.Red, theme.resolveColorOrNull(Tokens.Color.Primary))
        assertEquals(RoundedCornerShape(8.dp), theme.resolveShapeOrNull(Tokens.Radius.Card))
        assertEquals(12.dp, theme.resolveSpaceOrNull(Tokens.Space.Md))
        assertEquals(1.dp, theme.resolveElevationOrNull(Tokens.Elevation.Sm))
    }

    @Test
    fun plus_combines_themes_with_override_semantics_other_wins() {
        val base = kuisdTheme {
            color(Tokens.Color.Primary, Color.Red)
            color(Tokens.Color.Surface, Color.White)
            space(Tokens.Space.Md, 12.dp)
        }
        val override = kuisdTheme {
            color(Tokens.Color.Primary, Color.Blue) // gana
            space(Tokens.Space.Lg, 16.dp) // se añade
        }
        val combined = base + override
        assertEquals(Color.Blue, combined.resolveColorOrNull(Tokens.Color.Primary), "override gana")
        assertEquals(Color.White, combined.resolveColorOrNull(Tokens.Color.Surface), "base se preserva")
        assertEquals(12.dp, combined.resolveSpaceOrNull(Tokens.Space.Md), "base se preserva")
        assertEquals(16.dp, combined.resolveSpaceOrNull(Tokens.Space.Lg), "override se añade")
    }

    @Test
    fun null_token_resolves_to_null() {
        val theme = kuisdTheme { color(Tokens.Color.Primary, Color.Red) }
        assertNull(theme.resolveColorOrNull(null))
        assertNull(theme.resolveShapeOrNull(null))
        assertNull(theme.resolveSpaceOrNull(null))
        assertNull(theme.resolveElevationOrNull(null))
    }
}
