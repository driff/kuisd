package dev.kuisd.builder.catalog

import dev.kuisd.sdui.core.UiModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModifierFieldsTest {
    @Test
    fun `set y lee alignment`() {
        val set = withModifierEnum(UiModifier(), KEY_ALIGNMENT, "alignment.end")
        assertEquals("alignment.end", set.alignment?.ref)
        assertEquals("alignment.end", modifierEnumValue(set, KEY_ALIGNMENT))
    }

    @Test
    fun `vacio limpia alignment`() {
        val set = withModifierEnum(UiModifier(), KEY_ALIGNMENT, "alignment.end")
        val cleared = withModifierEnum(set, KEY_ALIGNMENT, "")
        assertNull(cleared.alignment)
        assertEquals("", modifierEnumValue(cleared, KEY_ALIGNMENT))
    }

    @Test
    fun `padding pone los 4 lados y se lee por l`() {
        val set = withModifierEnum(UiModifier(), KEY_PADDING, "space.md")
        val padding = set.padding
        assertEquals("space.md", padding?.l?.ref)
        assertEquals("space.md", padding?.t?.ref)
        assertEquals("space.md", padding?.r?.ref)
        assertEquals("space.md", padding?.b?.ref)
        assertEquals("space.md", modifierEnumValue(set, KEY_PADDING))
    }

    @Test
    fun `vacio limpia padding`() {
        val set = withModifierEnum(UiModifier(), KEY_PADDING, "space.md")
        val cleared = withModifierEnum(set, KEY_PADDING, "")
        assertNull(cleared.padding)
        assertEquals("", modifierEnumValue(cleared, KEY_PADDING))
    }

    @Test
    fun `optionLabel toma el ultimo segmento`() {
        assertEquals("centerH", optionLabel("alignment.centerH"))
    }
}
