package dev.kuisd.sdui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.sduiComponent
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

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

/**
 * Props del input two-way (spec 007). API state-based: el `TextFieldState` posee el texto y la
 * selección; el sync hacia el store es vía `snapshotFlow` colectado en un `LaunchedEffect`.
 *
 * - `bind`: nombre **directo** de la variable a enlazar (sin `$`); `""` => uncontrolled (sin emisión).
 * - `placeholder`: texto Material3 mostrado cuando el campo está vacío.
 * - `label`: etiqueta opcional; resuelta por `bind(label)` (admite `$ref` por la regla 005).
 */
@Serializable
data class TextFieldProps(
    val bind: String = "",
    val placeholder: String = "",
    val label: String? = null,
)

/** Catálogo base del motor: column/row/text/button/textField con sus props tipadas. */
val CorePack: ComponentRegistry = componentRegistry {
    register(sduiComponent<ColumnProps>("column")) { Column { renderChildren() } }
    register(sduiComponent<RowProps>("row")) { Row { renderChildren() } }
    register(sduiComponent<TextProps>("text")) { p -> Text(bind(p.text)) }
    register(sduiComponent<ButtonProps>("button")) { p ->
        val handler = LocalSduiActionHandler.current
        Button(onClick = { handler.handle(node.actions["onClick"].orEmpty()) }) { Text(bind(p.label)) }
    }
    register(sduiComponent<TextFieldProps>("textField")) { p -> TextFieldRenderer(p) }
}

/**
 * Renderer del `textField`. Acota el `@OptIn(ExperimentalMaterial3Api::class)` a esta función para
 * no contaminar todo `CorePack`.
 *
 * Semilla puntual: `vars.get(p.bind)?.asDisplayString()` se calcula UNA SOLA VEZ por `p.bind` vía
 * `remember(p.bind)` — documenta la semántica "lectura única" del docs state-based y evita ejecutar
 * la coerción en cada recomposición.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderScope.TextFieldRenderer(p: TextFieldProps) {
    val handler = LocalSduiActionHandler.current
    val vars = LocalVariables.current
    val initial = remember(p.bind) { vars.get(p.bind)?.asDisplayString().orEmpty() }
    val state = rememberTextFieldState(initial)

    if (p.bind.isNotEmpty()) {
        LaunchedEffect(state, p.bind) {
            snapshotFlow { state.text.toString() }
                .drop(1)
                .collect { newText ->
                    handler.handle(listOf(SetVar(p.bind, JsonPrimitive(newText))))
                }
        }
    }

    OutlinedTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(p.placeholder) },
        label = p.label?.let { { Text(bind(it)) } },
    )
}
