package dev.kuisd.desktop

import androidx.compose.ui.window.singleWindowApplication
import dev.kuisd.presentation.PlaceholderApp

fun main() = singleWindowApplication(title = "kuisd") {
    PlaceholderApp()
}
