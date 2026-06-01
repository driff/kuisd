package dev.kuisd.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.kuisd.app.SduiHost

@Composable
fun PlaceholderApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SduiHost(startRoute = "home")
        }
    }
}
