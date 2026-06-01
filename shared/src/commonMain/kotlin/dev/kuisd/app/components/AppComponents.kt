package dev.kuisd.app.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.ComponentRegistry
import dev.kuisd.sdui.CorePack
import dev.kuisd.sdui.componentRegistry

/** Catálogo propio de la app: registra `badge` y lo combina con el [CorePack] del motor (HU-3.1). */
internal val appComponents: ComponentRegistry = componentRegistry {
    register(BadgeComponent) { p ->
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(p.text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

internal val appRegistry: ComponentRegistry = CorePack + appComponents
