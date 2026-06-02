package dev.kuisd.server.screens

import dev.kuisd.sdui.core.EndpointStatus
import dev.kuisd.sdui.core.FireEndpoint
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Demo de `FireEndpoint` (spec 009): un formulario que envía `name` a `POST /action/greet` y
 * refleja el ciclo (loading/success/error) por las variables `submitStatus`/`submitResult`, sin
 * recargar la pantalla.
 */
object FormScreen : ScreenBuilder {
    private const val SCHEMA_VERSION = 1

    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = SCHEMA_VERSION,
        screenId = "form",
        variables = mapOf(
            "name" to JsonPrimitive(""),
            "submitStatus" to JsonPrimitive(EndpointStatus.IDLE),
            "submitResult" to JsonPrimitive(""),
            "greeted" to JsonPrimitive(false),
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
                textFieldNode(id = "name-input", bind = "name", placeholder = "Tu nombre"),
                bindingTextNode(id = "status", binding = "Estado: \$submitStatus"),
                bindingTextNode(id = "result", binding = "\$submitResult"),
                submitButton(),
            ),
        ),
    )

    private fun titleNode(): SduiNode = SduiNode(
        type = "text",
        id = "title",
        props = JsonObject(
            mapOf(
                "text" to JsonPrimitive("Formulario"),
                "style" to JsonPrimitive(Tokens.Type.Title.ref),
            ),
        ),
    )

    private fun textFieldNode(id: String, bind: String, placeholder: String): SduiNode = SduiNode(
        type = "textField",
        id = id,
        props = JsonObject(
            mapOf(
                "bind" to JsonPrimitive(bind),
                "placeholder" to JsonPrimitive(placeholder),
            ),
        ),
    )

    private fun bindingTextNode(id: String, binding: String): SduiNode = SduiNode(
        type = "text",
        id = id,
        props = JsonObject(mapOf("text" to JsonPrimitive(binding))),
    )

    private fun submitButton(): SduiNode = SduiNode(
        type = "button",
        id = "submit",
        props = JsonObject(mapOf("label" to JsonPrimitive("Enviar"))),
        actions = mapOf(
            "onClick" to listOf(
                FireEndpoint(
                    endpoint = "/action/greet",
                    method = "POST",
                    payloadVars = listOf("name"),
                    statusVar = "submitStatus",
                    resultVar = "submitResult",
                ),
            ),
        ),
    )
}
