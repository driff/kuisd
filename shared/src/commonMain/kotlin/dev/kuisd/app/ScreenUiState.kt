package dev.kuisd.app

import dev.kuisd.sdui.core.SduiEnvelope

/** Estado local de una pantalla SDUI (HU-3). */
sealed interface ScreenUiState {
    data object Loading : ScreenUiState

    data class Error(
        val message: String,
    ) : ScreenUiState

    data class Content(
        val envelope: SduiEnvelope,
    ) : ScreenUiState
}
