package dev.kuisd.server.screens

import dev.kuisd.sdui.core.DismissOverlay
import dev.kuisd.sdui.core.EndpointStatus
import dev.kuisd.sdui.core.FireEndpoint
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.ShowBottomSheet
import dev.kuisd.sdui.core.ShowDialog
import dev.kuisd.sdui.core.ShowSnackbar
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
                statusRow(),
                bindingTextNode(id = "result", binding = "\$submitResult"),
                submitButton(),
                confirmDemoButton(),
                sheetDemoButton(),
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

    /**
     * El binding (spec 005) resuelve el **campo completo** solo si empieza por `$`; no interpola
     * dentro de un string ("Estado: $submitStatus" se pintaría literal). Por eso se parte en un
     * `row`: literal "Estado: " + binding "$submitStatus" (mismo patrón que el saludo del feed).
     */
    private fun statusRow(): SduiNode = SduiNode(
        type = "row",
        id = "status",
        modifier = UiModifier(alignment = Tokens.Alignment.CenterVertically),
        children = listOf(
            bindingTextNode(id = "status-label", binding = "Estado: "),
            bindingTextNode(id = "status-value", binding = "\$submitStatus"),
        ),
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

    /**
     * Demo de `ShowDialog` (spec 011): un diálogo de confirmación cuyo `onConfirm` lanza un snackbar
     * y cuyo `onDismiss` cierra el overlay activo.
     */
    private fun confirmDemoButton(): SduiNode = SduiNode(
        type = "button",
        id = "confirm-demo",
        props = JsonObject(mapOf("label" to JsonPrimitive("Confirmar algo"))),
        actions = mapOf(
            "onClick" to listOf(
                ShowDialog(
                    title = "¿Confirmar?",
                    text = "Esto lanzará un aviso.",
                    confirmLabel = "Sí",
                    onConfirm = listOf(
                        ShowSnackbar(message = "¡Confirmado!", actionLabel = "OK"),
                    ),
                    dismissLabel = "No",
                    onDismiss = listOf(DismissOverlay),
                ),
            ),
        ),
    )

    /**
     * Demo de `ShowBottomSheet` (spec 011): abre una hoja con un subárbol renderizado por el host;
     * su `onDismiss` cierra el overlay activo.
     */
    private fun sheetDemoButton(): SduiNode = SduiNode(
        type = "button",
        id = "sheet-demo",
        props = JsonObject(mapOf("label" to JsonPrimitive("Abrir hoja"))),
        actions = mapOf(
            "onClick" to listOf(
                ShowBottomSheet(
                    content = listOf(
                        SduiNode(
                            type = "column",
                            children = listOf(
                                SduiNode(
                                    type = "text",
                                    props = JsonObject(
                                        mapOf("text" to JsonPrimitive("Contenido de la hoja")),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = listOf(DismissOverlay),
                ),
            ),
        ),
    )
}
