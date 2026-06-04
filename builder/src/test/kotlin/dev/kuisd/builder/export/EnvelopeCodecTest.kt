package dev.kuisd.builder.export

import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvelopeCodecTest {
    @Test
    fun `encode luego decode hace round-trip preservando variables y meta (HU-5)`() {
        val envelope = SduiEnvelope(
            schemaVersion = 2,
            screenId = "home",
            root = SduiNode(
                type = "column",
                id = "root",
                modifier = UiModifier(fillMaxWidth = true),
                children = listOf(
                    SduiNode(
                        type = "text",
                        id = "text-1",
                        props = JsonObject(mapOf("text" to JsonPrimitive("hola"))),
                    ),
                    SduiNode(type = "button", id = "button-1"),
                ),
            ),
            variables = mapOf("greeting" to JsonPrimitive("hola")),
            meta = mapOf("author" to "kuisd"),
        )

        val decoded = decodeEnvelope(envelope.encodeToJson())

        assertEquals(envelope, decoded)
    }

    @Test
    fun `decodeEnvelope lanza con JSON invalido`() {
        assertFailsWith<Exception> { decodeEnvelope("{ not json") }
    }
}
