package dev.kuisd.server.screens

import dev.kuisd.sdui.core.Increment
import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Toggle
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiAction
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pantalla "counter" (spec 005): variables locales con un `text` enlazado a `$count` y otro a `$flag`;
 * botones que emiten `Increment(count, by, min=0, max=10)` y `Toggle(flag)`.
 */
object CounterScreen : ScreenBuilder {
    private const val SCHEMA_VERSION = 1
    private const val MIN_COUNT = 0
    private const val MAX_COUNT = 10

    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = SCHEMA_VERSION,
        screenId = "counter",
        variables = mapOf(
            "count" to JsonPrimitive(0),
            "flag" to JsonPrimitive(false),
        ),
        root = SduiNode(
            type = "column",
            id = "root",
            modifier = UiModifier(
                fillMaxWidth = true,
                padding = PaddingTokens(l = Tokens.Space.Md, t = Tokens.Space.Md),
            ),
            children = listOf(
                titleNode(),
                bindingTextNode(id = "count-value", binding = "\$count"),
                bindingTextNode(id = "flag-value", binding = "\$flag"),
                buttonNode("inc", "+1", Increment("count", by = 1, min = MIN_COUNT, max = MAX_COUNT)),
                buttonNode("dec", "-1", Increment("count", by = -1, min = MIN_COUNT, max = MAX_COUNT)),
                buttonNode("toggle", "Toggle", Toggle("flag")),
                buttonNode("back", "Atrás", NavigateBack),
            ),
        ),
    )

    private fun titleNode(): SduiNode = SduiNode(
        type = "text",
        id = "title",
        props = JsonObject(
            mapOf(
                "text" to JsonPrimitive("Contador"),
                "style" to JsonPrimitive(Tokens.Type.Title.ref),
            ),
        ),
    )

    private fun bindingTextNode(id: String, binding: String): SduiNode = SduiNode(
        type = "text",
        id = id,
        props = JsonObject(mapOf("text" to JsonPrimitive(binding))),
    )

    private fun buttonNode(id: String, label: String, action: UiAction): SduiNode = SduiNode(
        type = "button",
        id = id,
        props = JsonObject(mapOf("label" to JsonPrimitive(label))),
        actions = mapOf("onClick" to listOf(action)),
    )
}
