package dev.kuisd.builder

import androidx.compose.ui.window.singleWindowApplication
import dev.kuisd.builder.ui.BuilderApp

fun main() = singleWindowApplication(title = "kuisd builder") {
    BuilderApp()
}
