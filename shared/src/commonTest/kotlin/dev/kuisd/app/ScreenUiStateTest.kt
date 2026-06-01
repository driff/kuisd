package dev.kuisd.app

import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenUiStateTest {
    @Test
    fun loading_is_singleton() {
        assertTrue(ScreenUiState.Loading === ScreenUiState.Loading)
    }

    @Test
    fun error_carries_message() {
        val state = ScreenUiState.Error("boom")
        assertEquals("boom", state.message)
    }

    @Test
    fun content_carries_envelope() {
        val envelope = SduiEnvelope(
            schemaVersion = 1,
            screenId = "home",
            root = SduiNode(type = "column"),
        )
        val state = ScreenUiState.Content(envelope)
        assertEquals("home", state.envelope.screenId)
    }
}
