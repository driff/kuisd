package dev.kuisd.builder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import dev.kuisd.builder.catalog.isMainContainer
import dev.kuisd.builder.catalog.mainContainers
import dev.kuisd.builder.model.SlotOps
import dev.kuisd.sdui.core.SduiNode

/**
 * Outline: árbol de nodos del documento; click selecciona, papelera borra (raíz protegida).
 *
 * Si la raíz es un contenedor principal, los `children` planos se presentan como slots explícitos
 * (Top bar / Content / Bottom bar), cada uno seleccionable como destino de inserción.
 */
@Composable
internal fun OutlinePane(
    root: SduiNode,
    selectedId: String?,
    selectedSlotId: String?,
    onSelect: (String?) -> Unit,
    onSelectSlot: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        if (isMainContainer(root.type)) {
            // La raíz (protegida de borrado) es seleccionable por id; su id puede no ser el literal "root".
            OutlineNode(
                node = root,
                depth = 0,
                rootId = root.id,
                selectedId = selectedId,
                onSelect = onSelect,
                onDelete = onDelete,
                renderChildren = false,
            )
            val spec = mainContainers.getValue(root.type)
            val slots = SlotOps.project(root, spec)
            spec.slots.forEach { slot ->
                SlotHeader(
                    label = slot.label,
                    selected = selectedSlotId == slot.id,
                    onClick = { onSelectSlot(slot.id) },
                )
                slots[slot.id].orEmpty().forEach { child ->
                    OutlineNode(
                        node = child,
                        depth = 2,
                        rootId = root.id,
                        selectedId = selectedId,
                        onSelect = onSelect,
                        onDelete = onDelete,
                    )
                }
            }
        } else {
            // Defensivo: tras la validación de carga la raíz siempre es contenedor principal.
            OutlineNode(
                node = root,
                depth = 0,
                rootId = root.id,
                selectedId = selectedId,
                onSelect = onSelect,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun SlotHeader(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(start = (1 * 12).dp, top = 2.dp, bottom = 2.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun OutlineNode(
    node: SduiNode,
    depth: Int,
    rootId: String?,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
    renderChildren: Boolean = true,
) {
    val isSelected = node.id != null && node.id == selectedId
    val rowColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            // Solo selecciona nodos con id (todos lo tienen tras ensureUniqueTree); evita deseleccionar.
            .clickable(enabled = node.id != null) { node.id?.let(onSelect) }
            .padding(start = (depth * 12).dp, top = 2.dp, bottom = 2.dp),
    ) {
        Text(
            text = node.id?.let { "${node.type}  #$it" } ?: node.type,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        val id = node.id
        if (id != null && id != rootId) {
            IconButton(onClick = { onDelete(id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar")
            }
        }
    }
    if (renderChildren) {
        node.children.forEach { child ->
            OutlineNode(child, depth + 1, rootId, selectedId, onSelect, onDelete)
        }
    }
}
