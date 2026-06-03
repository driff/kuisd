package dev.kuisd.sdui.core

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Round-trip de las acciones de overlay (spec 011) por el polimorfismo de `UiAction`. */
class OverlayActionsWireTest {
    @Test
    fun show_dialog_round_trips_with_nested_actions() {
        val original = ShowDialog(
            title = "¿Borrar?",
            text = "Esta acción no se puede deshacer.",
            confirmLabel = "Borrar",
            onConfirm = listOf(SetVar("deleted", JsonPrimitive(true)), ShowSnackbar(message = "Borrado")),
            dismissLabel = "Cancelar",
            onDismiss = listOf(DismissOverlay),
        )
        val encoded = DefaultSduiJson.encodeToString(UiAction.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun show_dialog_decodes_with_defaults() {
        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), """{"type":"showDialog"}""")
        assertTrue(decoded is ShowDialog)
        assertEquals("", decoded.title)
        assertEquals(null, decoded.confirmLabel)
        assertTrue(decoded.onConfirm.isEmpty())
    }

    @Test
    fun show_bottom_sheet_round_trips_with_nested_content() {
        val original = ShowBottomSheet(
            content = listOf(
                SduiNode(
                    type = "column",
                    children = listOf(SduiNode(type = "text")),
                ),
            ),
            onDismiss = listOf(DismissOverlay),
        )
        val encoded = DefaultSduiJson.encodeToString(UiAction.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), encoded)
        assertEquals(original, decoded)
        assertTrue(decoded is ShowBottomSheet)
        assertEquals("column", decoded.content.single().type)
    }

    @Test
    fun show_snackbar_round_trips_and_defaults_duration() {
        val original = ShowSnackbar(
            message = "Guardado",
            messageVar = "resultMsg",
            actionLabel = "Deshacer",
            onAction = listOf(SetVar("undone", JsonPrimitive(true))),
            duration = "long",
        )
        val encoded = DefaultSduiJson.encodeToString(UiAction.serializer(), original)
        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), encoded)
        assertEquals(original, decoded)

        val bare = DefaultSduiJson.decodeFromString(UiAction.serializer(), """{"type":"showSnackbar"}""")
        assertTrue(bare is ShowSnackbar)
        assertEquals("short", bare.duration)
    }

    @Test
    fun dismiss_overlay_is_a_singleton_object() {
        val encoded = DefaultSduiJson.encodeToString(UiAction.serializer(), DismissOverlay)
        val decoded = DefaultSduiJson.decodeFromString(UiAction.serializer(), encoded)
        assertEquals(DismissOverlay, decoded)
    }
}
