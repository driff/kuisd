package dev.kuisd.builder.export

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportTest {
    @Test
    fun `exportEnvelope hace round-trip preservando el root (HU-6_3)`() {
        val root = SduiNode(
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
        )

        val json = exportEnvelope(root)
        val decoded = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), json)

        assertEquals(1, decoded.schemaVersion)
        assertEquals("builder", decoded.screenId)
        assertEquals(root, decoded.root)
    }
}
