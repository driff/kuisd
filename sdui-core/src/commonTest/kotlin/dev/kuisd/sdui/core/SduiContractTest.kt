package dev.kuisd.sdui.core

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SduiContractTest {
    @Test
    fun envelope_round_trips_through_json() {
        val original = sampleEnvelope()
        val encoded = DefaultSduiJson.encodeToString(SduiEnvelope.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals("home", decoded.screenId)
        assertEquals("column", decoded.root.type)
    }

    @Test
    fun unknown_action_decodes_to_noop_instead_of_crashing() {
        val json = """{"type":"button","actions":{"onClick":[{"type":"totally-unknown-action"}]}}"""

        val node = DefaultSduiJson.decodeFromString(SduiNode.serializer(), json)
        val actions = node.actions["onClick"].orEmpty()

        assertTrue(actions.singleOrNull() is NoOpAction)
    }

    @Test
    fun navigate_back_round_trips_through_json() {
        val encoded = DefaultSduiJson.encodeToString(UiAction.serializer(), NavigateBack)
        assertEquals("""{"type":"navigateBack"}""", encoded)

        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), encoded)
        assertTrue(decoded is NavigateBack)
    }

    @Test
    fun problem_detail_round_trips_through_json() {
        val original = ProblemDetail(title = "Screen not found", status = 404, instance = "abc-123")
        val encoded = DefaultSduiJson.encodeToString(ProblemDetail.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(ProblemDetail.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals(404, decoded.status)
    }

    private fun sampleEnvelope(): SduiEnvelope = SduiEnvelope(
        schemaVersion = 1,
        screenId = "home",
        root = SduiNode(
            type = "column",
            id = "root",
            children = listOf(
                SduiNode(
                    type = "text",
                    id = "title",
                    props = JsonObject(mapOf("text" to JsonPrimitive("Bienvenido a kuisd"))),
                ),
                SduiNode(
                    type = "button",
                    id = "cta",
                    props = JsonObject(mapOf("label" to JsonPrimitive("Empezar"))),
                    actions = mapOf("onClick" to listOf(Navigate(route = "details"))),
                ),
            ),
        ),
    )
}
