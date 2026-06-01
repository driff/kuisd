package dev.kuisd.server.screens

import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Pantalla "details" — segundo paso de la cadena home → details → more (HU-1, HU-2). */
object DetailsScreen : ScreenBuilder {
    private const val SCHEMA_VERSION = 1

    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = SCHEMA_VERSION,
        screenId = "details",
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
                            "text" to JsonPrimitive("Detalles"),
                            "style" to JsonPrimitive(Tokens.Type.Title.ref),
                        ),
                    ),
                ),
                SduiNode(
                    type = "button",
                    id = "more",
                    props = JsonObject(mapOf("label" to JsonPrimitive("Ver más"))),
                    actions = mapOf("onClick" to listOf(Navigate(route = "more"))),
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
