package dev.kuisd.server.screens

import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Pantalla "home" — antes vivía como `SampleScreens.home()` en `:sdui-core` (R1). */
object HomeScreen : ScreenBuilder {
    private const val SCHEMA_VERSION = 1

    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = SCHEMA_VERSION,
        screenId = "home",
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
                            "text" to JsonPrimitive("Bienvenido a kuisd"),
                            "style" to JsonPrimitive(Tokens.Type.Title.ref),
                        ),
                    ),
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
