package dev.kuisd.sdui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import dev.kuisd.sdui.core.sduiComponent
import kotlinx.serialization.Serializable

@Serializable
class ColumnProps

@Serializable
class RowProps

@Serializable
data class TextProps(
    val text: String = "",
    val style: String? = null,
)

@Serializable
data class ButtonProps(
    val label: String = "",
)

/** Catálogo base del motor: column/row/text/button con sus props tipadas (HU-2). */
val CorePack: ComponentRegistry = componentRegistry {
    register(sduiComponent<ColumnProps>("column")) { Column { renderChildren() } }
    register(sduiComponent<RowProps>("row")) { Row { renderChildren() } }
    register(sduiComponent<TextProps>("text")) { p -> Text(bind(p.text)) }
    register(sduiComponent<ButtonProps>("button")) { p ->
        val handler = LocalSduiActionHandler.current
        Button(onClick = { handler.handle(node.actions["onClick"].orEmpty()) }) { Text(bind(p.label)) }
    }
}
