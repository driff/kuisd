package dev.kuisd.builder.catalog

import dev.kuisd.sdui.core.NavigateBack
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.Tokens
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Categorías de la paleta del builder. */
enum class Category { Texto, Contenedores, Estructura, Media, Presets }

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
    val key: String = type, // identidad de paleta (única); base ⇒ == type
    val isPreset: Boolean = false, // true = preset; no aporta descriptor de inspector
)

/** Construye un [JsonObject] de props desde pares clave→valor string. */
private fun props(vararg pairs: Pair<String, String>): JsonObject =
    JsonObject(pairs.associate { (k, v) -> k to JsonPrimitive(v) })

/** Construye un nodo `bottomBarItem` (icono + etiqueta + valor) para los presets de barra inferior. */
private fun bottomItem(icon: String, label: String, value: String): SduiNode =
    SduiNode(type = "bottomBarItem", props = props("icon" to icon, "label" to label, "value" to value))

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

/** Opciones de `contentDirection` del componente `scaffold`. */
private val contentDirectionOptions: List<String> = listOf("column", "row")

/** Tokens de espacio disponibles para `padding` (4 lados, v1). */
private val spaceOptions = listOf(
    Tokens.Space.Xs.ref,
    Tokens.Space.Sm.ref,
    Tokens.Space.Md.ref,
    Tokens.Space.Lg.ref,
    Tokens.Space.Xl.ref,
)

// Campos de `UiModifier` reutilizados por varias entradas (evita copy-paste de FieldSpec).
private fun fillMaxWidthField() =
    FieldSpec("fillMaxWidth", "Ancho máximo", FieldEditor.Bool, target = FieldTarget.Modifier)

private fun paddingField() =
    FieldSpec("padding", "Padding", FieldEditor.Enum, options = spaceOptions, target = FieldTarget.Modifier)

private fun alignmentField(options: List<String>) =
    FieldSpec("alignment", "Alineación", FieldEditor.Enum, options = options, target = FieldTarget.Modifier)

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
            fillMaxWidthField(),
            paddingField(),
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
            fillMaxWidthField(),
            paddingField(),
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
            fillMaxWidthField(),
            alignmentField(alignHorizontalRefs),
            paddingField(),
        ),
    ),
    PaletteEntry(
        type = "row",
        category = Category.Contenedores,
        label = "Fila",
        acceptsChildren = true,
        template = SduiNode(type = "row"),
        fields = listOf(
            fillMaxWidthField(),
            alignmentField(alignVerticalRefs),
            paddingField(),
        ),
    ),
    PaletteEntry(
        type = "card",
        category = Category.Contenedores,
        label = "Tarjeta",
        acceptsChildren = true,
        template = SduiNode(type = "card"),
        fields = listOf(
            paddingField(),
        ),
    ),
    PaletteEntry(
        type = "surface",
        category = Category.Contenedores,
        label = "Superficie",
        acceptsChildren = true,
        template = SduiNode(type = "surface"),
        fields = listOf(
            paddingField(),
        ),
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
        fields = listOf(
            FieldSpec(
                key = "contentDirection",
                label = "Dirección del contenido",
                editor = FieldEditor.Enum,
                options = contentDirectionOptions,
            ),
            FieldSpec(
                key = "fabPosition",
                label = "Posición FAB",
                editor = FieldEditor.Enum,
                options = listOf("end", "center"),
                target = FieldTarget.Prop,
            ),
            paddingField(),
        ),
    ),
    PaletteEntry(
        type = "topAppBar",
        category = Category.Estructura,
        label = "Barra superior",
        acceptsChildren = false,
        template = SduiNode(type = "topAppBar", props = props("title" to "Título")),
        fields = listOf(
            FieldSpec(key = "title", label = "Título", editor = FieldEditor.Text),
            FieldSpec(
                key = "centered",
                label = "Título centrado",
                editor = FieldEditor.Bool,
                target = FieldTarget.Prop,
            ),
        ),
    ),
    PaletteEntry(
        type = "bottomBar",
        category = Category.Estructura,
        label = "Barra inferior",
        acceptsChildren = true,
        template = SduiNode(type = "bottomBar"),
        fields = listOf(
            FieldSpec(key = "selectedBind", label = "Bind selección", editor = FieldEditor.Text),
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
            fillMaxWidthField(),
            paddingField(),
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
    // Estructura · FAB base
    PaletteEntry(
        type = "fab",
        category = Category.Estructura,
        label = "FAB",
        acceptsChildren = false,
        template = SduiNode(type = "fab", props = props("icon" to "add")),
        fields = listOf(
            FieldSpec(key = "icon", label = "Icono", editor = FieldEditor.Text),
        ),
    ),
    // Presets (no aportan descriptor de inspector; key única; sin campos)
    PaletteEntry(
        type = "topAppBar",
        category = Category.Presets,
        label = "Top Bar · Navegación",
        acceptsChildren = false,
        template = SduiNode(
            type = "topAppBar",
            props = props("title" to "Título", "navigationIcon" to "arrowBack"),
            actions = mapOf("onNavigationClick" to listOf(NavigateBack)),
        ),
        fields = emptyList(),
        key = "preset-topbar-nav",
        isPreset = true,
    ),
    PaletteEntry(
        type = "topAppBar",
        category = Category.Presets,
        label = "Top Bar · Acciones",
        acceptsChildren = false,
        template = SduiNode(
            type = "topAppBar",
            props = props("title" to "Título"),
            children = listOf(
                SduiNode(type = "iconButton", props = props("name" to "search")),
                SduiNode(type = "iconButton", props = props("name" to "moreVert")),
            ),
        ),
        fields = emptyList(),
        key = "preset-topbar-actions",
        isPreset = true,
    ),
    PaletteEntry(
        type = "topAppBar",
        category = Category.Presets,
        label = "Top Bar · Centrado",
        acceptsChildren = false,
        template = SduiNode(
            type = "topAppBar",
            props = JsonObject(
                mapOf("title" to JsonPrimitive("Título"), "centered" to JsonPrimitive(true)),
            ),
        ),
        fields = emptyList(),
        key = "preset-topbar-centered",
        isPreset = true,
    ),
    PaletteEntry(
        type = "bottomBar",
        category = Category.Presets,
        label = "Bottom · Menú (3)",
        acceptsChildren = true,
        template = SduiNode(
            type = "bottomBar",
            children = listOf(
                bottomItem("home", "Inicio", "home"),
                bottomItem("search", "Buscar", "search"),
                bottomItem("settings", "Ajustes", "settings"),
            ),
        ),
        fields = emptyList(),
        key = "preset-bottom-3",
        isPreset = true,
    ),
    PaletteEntry(
        type = "bottomBar",
        category = Category.Presets,
        label = "Bottom · Menú (5)",
        acceptsChildren = true,
        template = SduiNode(
            type = "bottomBar",
            children = listOf(
                bottomItem("home", "Inicio", "home"),
                bottomItem("search", "Buscar", "search"),
                bottomItem("settings", "Ajustes", "settings"),
                bottomItem("favorite", "Favoritos", "favorite"),
                bottomItem("edit", "Editar", "edit"),
            ),
        ),
        fields = emptyList(),
        key = "preset-bottom-5",
        isPreset = true,
    ),
    // El FAB se inserta desde la entrada base `fab` (categoría Estructura); no se duplica como preset.
)

/** Catálogo agrupado por categoría (derivado una vez, evita re-filtrar en cada recomposición). */
val catalogByCategory: Map<Category, List<PaletteEntry>> = builderCatalog.groupBy { it.category }

/** Índice por `type` para el inspector (descriptor del nodo seleccionado). */
val catalogByType: Map<String, PaletteEntry> = builderCatalog.filterNot { it.isPreset }.associateBy { it.type }
