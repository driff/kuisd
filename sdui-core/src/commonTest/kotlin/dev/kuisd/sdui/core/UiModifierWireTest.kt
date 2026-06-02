package dev.kuisd.sdui.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UiModifierWireTest {
    @Test
    fun decodes_empty_modifier_with_default_nulls() {
        val um = DefaultSduiJson.decodeFromString(UiModifier.serializer(), "{}")
        assertNull(um.elevation)
        assertNull(um.alignment)
        assertNull(um.padding)
    }

    @Test
    fun elevation_token_round_trip() {
        val json = """{"elevation":"elevation.sm"}"""
        val um = DefaultSduiJson.decodeFromString(UiModifier.serializer(), json)
        assertEquals(ElevationToken("elevation.sm"), um.elevation)
        val encoded = DefaultSduiJson.encodeToString(UiModifier.serializer(), um)
        assertEquals(um, DefaultSduiJson.decodeFromString(UiModifier.serializer(), encoded))
    }

    @Test
    fun alignment_token_round_trip_as_string_wire() {
        // El cambio de tipo String? -> AlignmentToken? mantiene wire compat: el JSON sigue
        // siendo un string. Un cliente viejo que mande {"alignment":"alignment.center"}
        // decodifica correctamente al nuevo tipo.
        val json = """{"alignment":"alignment.center"}"""
        val um = DefaultSduiJson.decodeFromString(UiModifier.serializer(), json)
        assertEquals(Tokens.Alignment.Center, um.alignment)
        val encoded = DefaultSduiJson.encodeToString(UiModifier.serializer(), um)
        assertEquals("""{"alignment":"alignment.center"}""", encoded)
    }

    @Test
    fun full_modifier_round_trips() {
        val um = UiModifier(
            fillMaxWidth = true,
            padding = PaddingTokens(l = Tokens.Space.Md, t = Tokens.Space.Md),
            background = Tokens.Color.Surface,
            cornerRadius = Tokens.Radius.Card,
            elevation = Tokens.Elevation.Sm,
            alignment = Tokens.Alignment.CenterHorizontally,
        )
        val encoded = DefaultSduiJson.encodeToString(UiModifier.serializer(), um)
        val decoded = DefaultSduiJson.decodeFromString(UiModifier.serializer(), encoded)
        assertEquals(um, decoded)
    }

    @Test
    fun radius_scale_tokens_are_distinct() {
        // Sanity: Sm/Md/Lg/Xl/Card son distintos refs (preservamos Card como alias semántico).
        val refs = listOf(
            Tokens.Radius.None.ref,
            Tokens.Radius.Sm.ref,
            Tokens.Radius.Md.ref,
            Tokens.Radius.Lg.ref,
            Tokens.Radius.Xl.ref,
            Tokens.Radius.Card.ref,
            Tokens.Radius.Pill.ref,
        )
        assertEquals(refs.size, refs.toSet().size, "Tokens.Radius refs deben ser únicos")
    }
}
