package dev.kuisd.builder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kuisd.sdui.core.SduiNode

/** Outline: árbol de nodos del documento; click selecciona, papelera borra (raíz protegida). */
@Composable
internal fun OutlinePane(
    root: SduiNode,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier.verticalScroll(rememberScrollState())) {
        OutlineNode(root, depth = 0, selectedId = selectedId, onSelect = onSelect, onDelete = onDelete)
    }
}

@Composable
private fun OutlineNode(
    node: SduiNode,
    depth: Int,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    val isSelected = node.id != null && node.id == selectedId
    val rowColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .clickable { onSelect(node.id) }
            .padding(start = (depth * 12).dp, top = 2.dp, bottom = 2.dp),
    ) {
        Text(
            text = node.id?.let { "${node.type}  #$it" } ?: node.type,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        val id = node.id
        if (id != null && id != "root") {
            IconButton(onClick = { onDelete(id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar")
            }
        }
    }
    node.children.forEach { child ->
        OutlineNode(child, depth + 1, selectedId, onSelect, onDelete)
    }
}
