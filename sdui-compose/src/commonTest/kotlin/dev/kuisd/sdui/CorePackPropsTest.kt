package dev.kuisd.sdui

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.sduiComponent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

class CorePackPropsTest {
    @Test
    fun surface_props_decode_with_full_tokens() {
        val component = sduiComponent<SurfaceProps>("surface")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(
                mapOf(
                    "background" to JsonPrimitive(Tokens.Color.Surface.ref),
                    "shape" to JsonPrimitive(Tokens.Radius.Card.ref),
                    "elevation" to JsonPrimitive(Tokens.Elevation.Sm.ref),
                ),
            ),
        )
        assertEquals(Tokens.Color.Surface, props.background)
        assertEquals(Tokens.Radius.Card, props.shape)
        assertEquals(Tokens.Elevation.Sm, props.elevation)
    }

    @Test
    fun card_props_decode_with_defaults() {
        val component = sduiComponent<CardProps>("card")
        val props = DefaultSduiJson.decodeFromJsonElement(component.serializer, JsonObject(emptyMap()))
        assertEquals(CardProps(), props)
    }

    @Test
    fun divider_props_decode() {
        val component = sduiComponent<DividerProps>("divider")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(mapOf("thickness" to JsonPrimitive(Tokens.Space.Xs.ref))),
        )
        assertEquals(Tokens.Space.Xs, props.thickness)
    }

    @Test
    fun spacer_props_decode() {
        val component = sduiComponent<SpacerProps>("spacer")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(mapOf("size" to JsonPrimitive(Tokens.Space.Md.ref))),
        )
        assertEquals(Tokens.Space.Md, props.size)
    }

    @Test
    fun icon_props_decode() {
        val component = sduiComponent<IconProps>("icon")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(
                mapOf(
                    "name" to JsonPrimitive("home"),
                    "tint" to JsonPrimitive(Tokens.Color.Primary.ref),
                    "contentDescription" to JsonPrimitive("Inicio"),
                ),
            ),
        )
        assertEquals("home", props.name)
        assertEquals(Tokens.Color.Primary, props.tint)
        assertEquals("Inicio", props.contentDescription)
    }

    @Test
    fun icon_button_props_decode() {
        val component = sduiComponent<IconButtonProps>("iconButton")
        val props = DefaultSduiJson.decodeFromJsonElement(
            component.serializer,
            JsonObject(mapOf("name" to JsonPrimitive("arrowBack"))),
        )
        assertEquals("arrowBack", props.name)
    }
}
