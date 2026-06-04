package dev.kuisd.builder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.builder.catalog.FieldEditor
import dev.kuisd.builder.catalog.FieldSpec
import dev.kuisd.builder.catalog.FieldTarget
import dev.kuisd.builder.catalog.builderCatalog
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Inspector: edita props/UiModifier del nodo seleccionado según el descriptor de su `type`. */
@Composable
internal fun InspectorPane(
    node: SduiNode?,
    onProps: (JsonObject) -> Unit,
    onModifier: (UiModifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(8.dp)) {
        if (node == null) {
            Text("Nada seleccionado", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        Text("Inspector · ${node.type}", style = MaterialTheme.typography.titleSmall)
        val entry = builderCatalog.firstOrNull { it.type == node.type }
        if (entry == null || entry.fields.isEmpty()) {
            Text("Sin campos editables", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        entry.fields.forEach { field ->
            FieldRow(node, field, onProps, onModifier)
        }
    }
}

@Composable
private fun FieldRow(
    node: SduiNode,
    field: FieldSpec,
    onProps: (JsonObject) -> Unit,
    onModifier: (UiModifier) -> Unit,
) {
    when (field.editor) {
        FieldEditor.Bool -> BoolField(node, field, onModifier, onProps)
        FieldEditor.Enum -> EnumField(node, field, onProps)
        FieldEditor.Text -> TextField(node, field, onProps)
    }
}

@Composable
private fun TextField(node: SduiNode, field: FieldSpec, onProps: (JsonObject) -> Unit) {
    val current = propValue(node, field.key)
    OutlinedTextField(
        value = current,
        onValueChange = { onProps(withProp(node.props, field.key, it)) },
        label = { Text(field.label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    )
}

@Composable
private fun EnumField(node: SduiNode, field: FieldSpec, onProps: (JsonObject) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = propValue(node, field.key).ifEmpty { "(elegir)" }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(field.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Box {
            TextButton(onClick = { expanded = true }) { Text(current) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                field.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onProps(withProp(node.props, field.key, option))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoolField(
    node: SduiNode,
    field: FieldSpec,
    onModifier: (UiModifier) -> Unit,
    onProps: (JsonObject) -> Unit,
) {
    val checked = if (field.target == FieldTarget.Modifier) {
        modifierBool(node.modifier, field.key)
    } else {
        propValue(node, field.key).toBooleanStrictOrNull() ?: false
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(field.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Checkbox(
            checked = checked,
            onCheckedChange = { value ->
                if (field.target == FieldTarget.Modifier) {
                    onModifier(withModifierBool(node.modifier, field.key, value))
                } else {
                    onProps(withProp(node.props, field.key, value.toString()))
                }
            },
        )
    }
}

private fun propValue(node: SduiNode, key: String): String =
    (node.props[key] as? JsonPrimitive)?.content ?: ""

private fun withProp(props: JsonObject, key: String, value: String): JsonObject =
    JsonObject(props + (key to JsonPrimitive(value)))

private fun modifierBool(modifier: UiModifier, key: String): Boolean = when (key) {
    "fillMaxWidth" -> modifier.fillMaxWidth
    "fillMaxHeight" -> modifier.fillMaxHeight
    else -> false
}

private fun withModifierBool(modifier: UiModifier, key: String, value: Boolean): UiModifier = when (key) {
    "fillMaxWidth" -> modifier.copy(fillMaxWidth = value)
    "fillMaxHeight" -> modifier.copy(fillMaxHeight = value)
    else -> modifier
}
