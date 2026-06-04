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
import dev.kuisd.builder.catalog.KEY_ALIGNMENT
import dev.kuisd.builder.catalog.alignHorizontalRefs
import dev.kuisd.builder.catalog.alignVerticalRefs
import dev.kuisd.builder.catalog.catalogByType
import dev.kuisd.builder.catalog.modifierEnumValue
import dev.kuisd.builder.catalog.modifierWeight
import dev.kuisd.builder.catalog.optionLabel
import dev.kuisd.builder.catalog.withModifierEnum
import dev.kuisd.builder.catalog.withModifierWeight
import dev.kuisd.sdui.core.SduiNode
import dev.kuisd.sdui.core.UiModifier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Inspector: edita props/UiModifier del nodo seleccionado según el descriptor de su `type`, más una
 * sección contextual de "Layout en el contenedor" (alignment por-hijo + weight) cuando el nodo es hijo de
 * un `column`/`row` (spec 019); [parentType] es el `type` del contenedor padre (null si no aplica).
 */
@Composable
internal fun InspectorPane(
    node: SduiNode?,
    parentType: String?,
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
        val fields = catalogByType[node.type]?.fields.orEmpty()
        if (fields.isEmpty()) {
            Text("Sin campos editables", style = MaterialTheme.typography.bodySmall)
        } else {
            fields.forEach { field -> FieldRow(node, field, onProps, onModifier) }
        }
        LayoutSection(node, parentType, onProps, onModifier)
    }
}

/** Alignment por-hijo (eje del contenedor padre) + weight; solo si el nodo es hijo de column/row. */
@Composable
private fun LayoutSection(
    node: SduiNode,
    parentType: String?,
    onProps: (JsonObject) -> Unit,
    onModifier: (UiModifier) -> Unit,
) {
    val alignRefs = when (parentType) {
        "column" -> alignHorizontalRefs
        "row" -> alignVerticalRefs
        else -> return
    }
    Text(
        "Layout en el contenedor",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    val alignField = FieldSpec(
        key = KEY_ALIGNMENT,
        label = "Alineación",
        editor = FieldEditor.Enum,
        options = alignRefs,
        target = FieldTarget.Modifier,
    )
    EnumField(node = node, field = alignField, onProps = onProps, onModifier = onModifier)
    WeightField(node, onModifier)
}

@Composable
private fun WeightField(node: SduiNode, onModifier: (UiModifier) -> Unit) {
    // Estado de texto local (sembrado por nodo) para no reformatear mientras se escribe; empuja el Float parseado.
    var text by remember(node.id) { mutableStateOf(modifierWeight(node.modifier)) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onModifier(withModifierWeight(node.modifier, it))
        },
        label = { Text("Weight") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    )
}

@Composable
private fun FieldRow(
    node: SduiNode,
    field: FieldSpec,
    onProps: (JsonObject) -> Unit,
    onModifier: (UiModifier) -> Unit,
) {
    when (field.editor) {
        FieldEditor.Bool -> BoolField(node, field, onProps, onModifier)
        FieldEditor.Enum -> EnumField(node, field, onProps, onModifier)
        FieldEditor.Text -> TextFieldEditor(node, field, onProps)
    }
}

@Composable
private fun TextFieldEditor(node: SduiNode, field: FieldSpec, onProps: (JsonObject) -> Unit) {
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
private fun EnumField(
    node: SduiNode,
    field: FieldSpec,
    onProps: (JsonObject) -> Unit,
    onModifier: (UiModifier) -> Unit,
) {
    val isModifier = field.target == FieldTarget.Modifier
    var expanded by remember { mutableStateOf(false) }
    val rawCurrent = if (isModifier) {
        modifierEnumValue(node.modifier, field.key)
    } else {
        propValue(node, field.key)
    }
    val display = when {
        rawCurrent.isEmpty() -> "(elegir)"
        isModifier -> optionLabel(rawCurrent)
        else -> rawCurrent
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(field.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Box {
            TextButton(onClick = { expanded = true }) { Text(display) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (isModifier) {
                    DropdownMenuItem(
                        text = { Text("(ninguno)") },
                        onClick = {
                            expanded = false
                            onModifier(withModifierEnum(node.modifier, field.key, ""))
                        },
                    )
                }
                field.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (isModifier) optionLabel(option) else option) },
                        onClick = {
                            expanded = false
                            if (isModifier) {
                                onModifier(withModifierEnum(node.modifier, field.key, option))
                            } else {
                                onProps(withProp(node.props, field.key, option))
                            }
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
    onProps: (JsonObject) -> Unit,
    onModifier: (UiModifier) -> Unit,
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
                    onProps(JsonObject(node.props + (field.key to JsonPrimitive(value))))
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
