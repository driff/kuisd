package dev.kuisd.sdui.modifier

import androidx.compose.ui.Alignment
import dev.kuisd.sdui.core.AlignmentToken
import dev.kuisd.sdui.core.Tokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlignmentResolverTest {
    @Test
    fun horizontal_axis_resolves_start_center_end() {
        assertEquals(Alignment.Start, Tokens.Alignment.Start.toHorizontalAlignment())
        assertEquals(Alignment.CenterHorizontally, Tokens.Alignment.Center.toHorizontalAlignment())
        assertEquals(
            Alignment.CenterHorizontally,
            Tokens.Alignment.CenterHorizontally.toHorizontalAlignment(),
        )
        assertEquals(Alignment.End, Tokens.Alignment.End.toHorizontalAlignment())
    }

    @Test
    fun horizontal_axis_returns_null_for_vertical_tokens() {
        assertNull(Tokens.Alignment.Top.toHorizontalAlignment())
        assertNull(Tokens.Alignment.Bottom.toHorizontalAlignment())
        assertNull(Tokens.Alignment.CenterVertically.toHorizontalAlignment())
    }

    @Test
    fun vertical_axis_resolves_top_center_bottom() {
        assertEquals(Alignment.Top, Tokens.Alignment.Top.toVerticalAlignment())
        assertEquals(Alignment.CenterVertically, Tokens.Alignment.Center.toVerticalAlignment())
        assertEquals(
            Alignment.CenterVertically,
            Tokens.Alignment.CenterVertically.toVerticalAlignment(),
        )
        assertEquals(Alignment.Bottom, Tokens.Alignment.Bottom.toVerticalAlignment())
    }

    @Test
    fun vertical_axis_returns_null_for_horizontal_tokens() {
        assertNull(Tokens.Alignment.Start.toVerticalAlignment())
        assertNull(Tokens.Alignment.End.toVerticalAlignment())
        assertNull(Tokens.Alignment.CenterHorizontally.toVerticalAlignment())
    }

    @Test
    fun null_token_returns_null_on_both_axes() {
        val nullToken: AlignmentToken? = null
        assertNull(nullToken.toHorizontalAlignment())
        assertNull(nullToken.toVerticalAlignment())
    }
}
