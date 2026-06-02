package dev.kuisd.server.screens

import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Demo end-to-end de spec 008: contenedores + theme tokenizado + iconRegistry + alignment.
 * Cabecera con `iconButton(arrowBack)` + `text` título; `lazyColumn` con 5 cards iguales.
 * Cada card centra horizontalmente sus hijos, muestra un `row` con icono + título, un `divider`
 * y un `text` con el cuerpo.
 */
object FeedScreen : ScreenBuilder {
    private const val SCHEMA_VERSION = 1

    private val cards = listOf(
        Triple("card-home", "home", "Bienvenida"),
        Triple("card-search", "search", "Explora"),
        Triple("card-settings", "settings", "Configura"),
        Triple("card-favorite", "favorite", "Favoritos"),
        Triple("card-check", "check", "Completado"),
    )

    override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
        schemaVersion = SCHEMA_VERSION,
        screenId = "feed",
        root = SduiNode(
            type = "column",
            id = "root",
            modifier = UiModifier(
                fillMaxWidth = true,
                padding = PaddingTokens(l = Tokens.Space.Md, t = Tokens.Space.Md),
            ),
            children = listOf(
                headerNode(),
                SduiNode(
                    type = "lazyColumn",
                    id = "list",
                    modifier = UiModifier(fillMaxWidth = true),
                    children = cards.map { (id, icon, title) ->
                        cardNode(id, icon, title, "Cuerpo del card $title — contenido de demo.")
                    },
                ),
            ),
        ),
    )

    private fun headerNode(): SduiNode = SduiNode(
        type = "row",
        id = "header",
        modifier = UiModifier(
            fillMaxWidth = true,
            padding = PaddingTokens(b = Tokens.Space.Md),
            alignment = Tokens.Alignment.CenterVertically,
        ),
        children = listOf(
            SduiNode(
                type = "iconButton",
                id = "back",
                props = JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("arrowBack"),
                        "contentDescription" to JsonPrimitive("Atrás"),
                    ),
                ),
                actions = mapOf("onClick" to listOf(NavigateBack)),
            ),
            SduiNode(
                type = "text",
                id = "title",
                props = JsonObject(
                    mapOf(
                        "text" to JsonPrimitive("Feed"),
                        "style" to JsonPrimitive(Tokens.Type.Title.ref),
                    ),
                ),
            ),
        ),
    )

    private fun cardNode(id: String, iconName: String, title: String, body: String): SduiNode = SduiNode(
        type = "card",
        id = id,
        modifier = UiModifier(
            fillMaxWidth = true,
            padding = PaddingTokens(b = Tokens.Space.Md),
        ),
        props = JsonObject(
            mapOf(
                "background" to JsonPrimitive(Tokens.Color.Surface.ref),
                "shape" to JsonPrimitive(Tokens.Radius.Card.ref),
                "elevation" to JsonPrimitive(Tokens.Elevation.Sm.ref),
            ),
        ),
        children = listOf(
            SduiNode(
                type = "column",
                id = "$id-inner",
                modifier = UiModifier(
                    fillMaxWidth = true,
                    padding = PaddingTokens(
                        l = Tokens.Space.Md,
                        t = Tokens.Space.Md,
                        r = Tokens.Space.Md,
                        b = Tokens.Space.Md,
                    ),
                    alignment = Tokens.Alignment.CenterHorizontally,
                ),
                children = listOf(
                    iconTitleRow(iconName, title),
                    SduiNode(
                        type = "divider",
                        id = "$id-divider",
                        modifier = UiModifier(
                            fillMaxWidth = true,
                            padding = PaddingTokens(t = Tokens.Space.Sm, b = Tokens.Space.Sm),
                        ),
                    ),
                    SduiNode(
                        type = "text",
                        id = "$id-body",
                        props = JsonObject(mapOf("text" to JsonPrimitive(body))),
                    ),
                ),
            ),
        ),
    )

    private fun iconTitleRow(iconName: String, title: String): SduiNode = SduiNode(
        type = "row",
        id = "row-$iconName",
        modifier = UiModifier(alignment = Tokens.Alignment.CenterVertically),
        children = listOf(
            SduiNode(
                type = "icon",
                id = "icon-$iconName",
                props = JsonObject(
                    mapOf(
                        "name" to JsonPrimitive(iconName),
                        "tint" to JsonPrimitive(Tokens.Color.Primary.ref),
                    ),
                ),
            ),
            SduiNode(
                type = "spacer",
                id = "spacer-$iconName",
                props = JsonObject(mapOf("size" to JsonPrimitive(Tokens.Space.Sm.ref))),
            ),
            SduiNode(
                type = "text",
                id = "title-$iconName",
                props = JsonObject(mapOf("text" to JsonPrimitive(title))),
            ),
        ),
    )
}
