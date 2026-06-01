package dev.kuisd.server.screens

import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Pantalla "more" — tercer paso de la cadena home → details → more (HU-1, HU-2). */
object MoreScreen : ScreenBuilder {
    private const val SCHEMA_VERSION = 1

    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = SCHEMA_VERSION,
        screenId = "more",
        root = SduiNode(
            type = "column",
            id = "root",
            modifier = UiModifier(
                fillMaxWidth = true,
                padding = PaddingTokens(l = Tokens.Space.Md, t = Tokens.Space.Md),
            ),
            children = listOf(
                SduiNode(
                    type = "text",
                    id = "title",
                    props = JsonObject(
                        mapOf(
                            "text" to JsonPrimitive("Más"),
                            "style" to JsonPrimitive(Tokens.Type.Title.ref),
                        ),
                    ),
                ),
                SduiNode(
                    type = "button",
                    id = "back",
                    props = JsonObject(mapOf("label" to JsonPrimitive("Atrás"))),
                    actions = mapOf("onClick" to listOf(NavigateBack)),
                ),
            ),
        ),
    )
}
