package dev.kuisd.builder.catalog

import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Categorías de la paleta del builder. */
enum class Category { Texto, Contenedores, Estructura, Media }

/** Tipo de editor del inspector para un campo. */
enum class FieldEditor { Text, Bool, Enum }

/** Destino de un campo: una clave de `props` o un campo del `UiModifier`. */
enum class FieldTarget { Prop, Modifier }

/** Descriptor de un campo editable en el inspector. */
data class FieldSpec(
    val key: String,
    val label: String,
    val editor: FieldEditor,
    val options: List<String> = emptyList(),
    val target: FieldTarget = FieldTarget.Prop,
)

/** Entrada de la paleta: un `type` curado con su plantilla válida y sus campos editables. */
data class PaletteEntry(
    val type: String,
    val category: Category,
    val label: String,
    val acceptsChildren: Boolean,
    val template: SduiNode,
    val fields: List<FieldSpec>,
)

/** Construye un [JsonObject] de props desde pares clave→valor string. */
private fun props(vararg pairs: Pair<String, String>): JsonObject =
    JsonObject(pairs.associate { (k, v) -> k to JsonPrimitive(v) })

/** Refs de estilo de texto disponibles (tokens de tipografía). */
private val typeStyleOptions: List<String> = listOf(
    Tokens.Type.Display.ref,
    Tokens.Type.Title.ref,
    Tokens.Type.Body.ref,
    Tokens.Type.Label.ref,
    Tokens.Type.Button.ref,
)

/** Opciones de `contentScale` del componente `image`. */
private val contentScaleOptions: List<String> = listOf("crop", "fit", "fillBounds", "inside", "none")

/**
 * Catálogo curado: subconjunto de `CorePack` con plantillas válidas (decodificables) y campos
 * editables por el inspector. Hecho a mano para controlar editores, opciones y plantillas.
 */
val builderCatalog: List<PaletteEntry> = listOf(
    // Texto
    PaletteEntry(
        type = "text",
        category = Category.Texto,
        label = "Texto",
        acceptsChildren = false,
        template = SduiNode(type = "text", props = props("text" to "Texto")),
        fields = listOf(
            FieldSpec(key = "text", label = "Texto", editor = FieldEditor.Text),
            FieldSpec(
                key = "style",
                label = "Estilo",
                editor = FieldEditor.Enum,
                options = typeStyleOptions,
            ),
        ),
    ),
    PaletteEntry(
        type = "button",
        category = Category.Texto,
        label = "Botón",
        acceptsChildren = false,
        template = SduiNode(type = "button", props = props("label" to "Botón")),
        fields = listOf(
            FieldSpec(key = "label", label = "Etiqueta", editor = FieldEditor.Text),
        ),
    ),
    // Contenedores
    PaletteEntry(
        type = "column",
        category = Category.Contenedores,
        label = "Columna",
        acceptsChildren = true,
        template = SduiNode(type = "column"),
        fields = listOf(
            FieldSpec(
                key = "fillMaxWidth",
                label = "Ancho máximo",
                editor = FieldEditor.Bool,
                target = FieldTarget.Modifier,
            ),
        ),
    ),
    PaletteEntry(
        type = "row",
        category = Category.Contenedores,
        label = "Fila",
        acceptsChildren = true,
        template = SduiNode(type = "row"),
        fields = listOf(
            FieldSpec(
                key = "fillMaxWidth",
                label = "Ancho máximo",
                editor = FieldEditor.Bool,
                target = FieldTarget.Modifier,
            ),
        ),
    ),
    PaletteEntry(
        type = "card",
        category = Category.Contenedores,
        label = "Tarjeta",
        acceptsChildren = true,
        template = SduiNode(type = "card"),
        fields = emptyList(),
    ),
    PaletteEntry(
        type = "surface",
        category = Category.Contenedores,
        label = "Superficie",
        acceptsChildren = true,
        template = SduiNode(type = "surface"),
        fields = emptyList(),
    ),
    PaletteEntry(
        type = "divider",
        category = Category.Contenedores,
        label = "Divisor",
        acceptsChildren = false,
        template = SduiNode(type = "divider"),
        fields = emptyList(),
    ),
    PaletteEntry(
        type = "spacer",
        category = Category.Contenedores,
        label = "Espaciador",
        acceptsChildren = false,
        template = SduiNode(type = "spacer", props = props("size" to Tokens.Space.Md.ref)),
        fields = emptyList(),
    ),
    // Estructura
    PaletteEntry(
        type = "scaffold",
        category = Category.Estructura,
        label = "Scaffold",
        acceptsChildren = true,
        template = SduiNode(type = "scaffold"),
        fields = emptyList(),
    ),
    PaletteEntry(
        type = "topAppBar",
        category = Category.Estructura,
        label = "Barra superior",
        acceptsChildren = false,
        template = SduiNode(type = "topAppBar", props = props("title" to "Título")),
        fields = listOf(
            FieldSpec(key = "title", label = "Título", editor = FieldEditor.Text),
        ),
    ),
    // Media
    PaletteEntry(
        type = "image",
        category = Category.Media,
        label = "Imagen",
        acceptsChildren = false,
        template = SduiNode(type = "image", props = props("name" to "kuisd_logo")),
        fields = listOf(
            FieldSpec(key = "url", label = "URL", editor = FieldEditor.Text),
            FieldSpec(key = "name", label = "Nombre local", editor = FieldEditor.Text),
            FieldSpec(
                key = "contentScale",
                label = "Escala",
                editor = FieldEditor.Enum,
                options = contentScaleOptions,
            ),
            FieldSpec(key = "contentDescription", label = "Descripción", editor = FieldEditor.Text),
        ),
    ),
    PaletteEntry(
        type = "icon",
        category = Category.Media,
        label = "Icono",
        acceptsChildren = false,
        template = SduiNode(type = "icon", props = props("name" to "home")),
        fields = listOf(
            FieldSpec(key = "name", label = "Nombre", editor = FieldEditor.Text),
        ),
    ),
)
