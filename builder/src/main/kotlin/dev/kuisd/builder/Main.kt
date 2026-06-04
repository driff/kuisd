package dev.kuisd.builder

import androidx.compose.material3.Text
import androidx.compose.ui.window.singleWindowApplication

// T1: andamiaje mínimo. La UI real (BuilderApp) llega en T5/T6.
fun main() = singleWindowApplication(title = "kuisd builder") {
    Text("kuisd builder")
}
