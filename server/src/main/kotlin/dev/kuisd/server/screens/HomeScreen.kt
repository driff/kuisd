package dev.kuisd.server.screens

import dev.kuisd.sdui.core.Navigate
import dev.kuisd.sdui.core.PaddingTokens
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.SetVar
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
        variables = mapOf("section" to JsonPrimitive("home")),
        root = SduiNode(
            type = "scaffold",
            id = "root",
            modifier = UiModifier(fillMaxWidth = true, fillMaxHeight = true),
            children = listOf(
                topBarNode(),
                contentNode(),
                bottomBarNode(),
            ),
        ),
    )

    private fun topBarNode(): SduiNode = SduiNode(
        type = "topAppBar",
        id = "topBar",
        props = JsonObject(mapOf("title" to JsonPrimitive("Inicio"))),
    )

    private fun contentNode(): SduiNode = SduiNode(
        type = "column",
        id = "content",
        modifier = UiModifier(
            fillMaxWidth = true,
            padding = PaddingTokens(l = Tokens.Space.Md, t = Tokens.Space.Md),
        ),
        children = listOf(
            titleNode(),
            logoNode(), // imagen local (013)
            bannerNode(), // imagen remota (012)
            navButton(id = "cta", label = "Empezar", route = "details"),
            navButton(id = "counter", label = "Contador", route = "counter"),
            navButton(id = "feed", label = "Feed", route = "feed"),
            navButton(id = "form", label = "Formulario", route = "form"),
        ),
    )

    private fun titleNode(): SduiNode = SduiNode(
        type = "text",
        id = "title",
        props = JsonObject(
            mapOf(
                "text" to JsonPrimitive("Bienvenido a kuisd"),
                "style" to JsonPrimitive(Tokens.Type.Title.ref),
            ),
        ),
    )

    /** Imagen LOCAL por nombre (spec 013): resuelta por el `ImageRegistry` de la app, sin red. */
    private fun logoNode(): SduiNode = SduiNode(
        type = "image",
        id = "logo",
        modifier = UiModifier(width = Tokens.Space.Xl, height = Tokens.Space.Xl),
        props = JsonObject(
            mapOf(
                "name" to JsonPrimitive("kuisd_logo"),
                "contentScale" to JsonPrimitive("fit"),
                "contentDescription" to JsonPrimitive("Logo de kuisd"),
            ),
        ),
    )

    /** Imagen REMOTA por URL (spec 012): cargada por el seam Coil. */
    private fun bannerNode(): SduiNode = SduiNode(
        type = "image",
        id = "banner",
        modifier = UiModifier(fillMaxWidth = true),
        props = JsonObject(
            mapOf(
                "url" to JsonPrimitive("https://picsum.photos/seed/kuisd/800/300"),
                "contentScale" to JsonPrimitive("crop"),
                "contentDescription" to JsonPrimitive("Imagen de portada"),
            ),
        ),
    )

    private fun navButton(id: String, label: String, route: String): SduiNode = SduiNode(
        type = "button",
        id = id,
        props = JsonObject(mapOf("label" to JsonPrimitive(label))),
        actions = mapOf("onClick" to listOf(Navigate(route = route))),
    )

    private fun bottomBarNode(): SduiNode = SduiNode(
        type = "bottomBar",
        id = "bottomBar",
        props = JsonObject(mapOf("selectedBind" to JsonPrimitive("section"))),
        children = listOf(
            bottomBarItem(id = "tab-home", icon = "home", label = "Inicio", value = "home"),
            bottomBarItem(id = "tab-search", icon = "search", label = "Buscar", value = "search"),
            bottomBarItem(id = "tab-settings", icon = "settings", label = "Ajustes", value = "settings"),
        ),
    )

    private fun bottomBarItem(id: String, icon: String, label: String, value: String): SduiNode = SduiNode(
        type = "bottomBarItem",
        id = id,
        props = JsonObject(
            mapOf(
                "icon" to JsonPrimitive(icon),
                "label" to JsonPrimitive(label),
                "value" to JsonPrimitive(value),
            ),
        ),
        actions = mapOf("onClick" to listOf(SetVar("section", JsonPrimitive(value)))),
    )
}
