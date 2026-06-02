package dev.kuisd.sdui.core

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FireEndpointWireTest {
    @Test
    fun decodes_legacy_fire_endpoint_with_defaults() {
        // Payload "viejo" (solo endpoint + payloadVars) debe decodificar con los campos nuevos a default.
        val json = """{"type":"network","endpoint":"/action/greet","payloadVars":["name"]}"""
        val action = DefaultSduiJson.decodeFromString(UiAction.serializer(), json)
        assertTrue(action is FireEndpoint)
        assertEquals("/action/greet", action.endpoint)
        assertEquals("POST", action.method)
        assertEquals(listOf("name"), action.payloadVars)
        assertEquals(null, action.statusVar)
        assertEquals(null, action.resultVar)
        assertTrue(action.onSuccess.isEmpty())
        assertTrue(action.onError.isEmpty())
    }

    @Test
    fun full_fire_endpoint_round_trips_with_nested_actions() {
        val original = FireEndpoint(
            endpoint = "/action/greet",
            method = "POST",
            payloadVars = listOf("name"),
            statusVar = "submitStatus",
            resultVar = "submitResult",
            onSuccess = listOf(SetVar("greeted", JsonPrimitive(true))),
            onError = listOf(Navigate(route = "error")),
        )
        val encoded = DefaultSduiJson.encodeToString(UiAction.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun action_response_round_trips_with_polymorphic_actions() {
        val original = ActionResponse(
            actions = listOf(
                SetVar("greeted", JsonPrimitive(true)),
                Navigate(route = "home"),
            ),
            message = "Hola, Ana!",
        )
        val encoded = DefaultSduiJson.encodeToString(ActionResponse.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(ActionResponse.serializer(), encoded)
        assertEquals(original, decoded)
        assertEquals("Hola, Ana!", decoded.message)
        assertEquals(2, decoded.actions.size)
    }

    @Test
    fun action_response_defaults_to_empty() {
        val decoded = DefaultSduiJson.decodeFromString(ActionResponse.serializer(), "{}")
        assertTrue(decoded.actions.isEmpty())
        assertEquals(null, decoded.message)
    }
}
