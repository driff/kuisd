package dev.kuisd.sdui

import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SduiUiStateTest {
    @Test
    fun loading_is_singleton() {
        assertTrue(SduiUiState.Loading === SduiUiState.Loading)
    }

    @Test
    fun error_carries_message() {
        val state = SduiUiState.Error("boom")
        assertEquals("boom", state.message)
    }

    @Test
    fun content_carries_envelope() {
        val envelope = SduiEnvelope(
            schemaVersion = 1,
            screenId = "home",
            root = SduiNode(type = "column"),
        )
        val state = SduiUiState.Content(envelope)
        assertEquals("home", state.envelope.screenId)
    }
}
