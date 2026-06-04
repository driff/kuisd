package dev.kuisd.builder.catalog

/** Un slot de un contenedor principal, como vista sobre los `children` planos del nodo. */
data class SlotSpec(
    val id: String, // "topBar" | "content" | "bottomBar"
    val label: String, // "Top bar" | "Content" | "Bottom bar"
    val multiple: Boolean, // content = true; topBar/bottomBar = false
    val childTypes: Set<String>, // types que el motor extrae a este slot; vacío ⇒ "el resto" (content)
)

/** Declaración de un contenedor principal (solo-raíz) y sus slots, en orden de render. */
data class MainContainerSpec(
    val type: String,
    val slots: List<SlotSpec>,
) {
    /** El slot "resto" (content): el único con childTypes vacío. */
    val contentSlot: SlotSpec get() = slots.first { it.childTypes.isEmpty() }

    /** Todos los types reservados por slots únicos (topAppBar, bottomBar). */
    val reservedChildTypes: Set<String> get() = slots.flatMapTo(mutableSetOf()) { it.childTypes }

    /** Slot al que pertenece un child de [type]; el de content si no es reservado. */
    fun slotForChildType(type: String): SlotSpec = slots.firstOrNull { type in it.childTypes } ?: contentSlot
}

/**
 * Única fuente de verdad de los contenedores principales del builder. v1: solo `scaffold`.
 * El orden de slots (topBar, content, bottomBar) espeja `partitionScaffoldSlots` del motor.
 */
val mainContainers: Map<String, MainContainerSpec> = mapOf(
    "scaffold" to MainContainerSpec(
        type = "scaffold",
        slots = listOf(
            SlotSpec("topBar", "Top bar", multiple = false, childTypes = setOf("topAppBar")),
            SlotSpec("content", "Content", multiple = true, childTypes = emptySet()),
            SlotSpec("bottomBar", "Bottom bar", multiple = false, childTypes = setOf("bottomBar")),
            SlotSpec("fab", "FAB", multiple = false, childTypes = setOf("fab")),
        ),
    ),
)

/** ¿Es [type] un contenedor principal (solo puede ir en la raíz del documento)? */
fun isMainContainer(type: String): Boolean = type in mainContainers
