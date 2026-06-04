package dev.kuisd.builder.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.kuisd.builder.catalog.Category
import dev.kuisd.builder.catalog.PaletteEntry
import dev.kuisd.builder.catalog.builderCatalog

/** Paleta de componentes agrupada por categoría; al pulsar una entrada se inserta su plantilla. */
@Composable
internal fun PalettePane(onAdd: (PaletteEntry) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier) {
        Category.entries.forEach { category ->
            val entries = builderCatalog.filter { it.category == category }
            if (entries.isEmpty()) return@forEach
            item(key = "cat-${category.name}") {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            items(entries, key = { it.type }) { entry ->
                TextButton(onClick = { onAdd(entry) }, modifier = Modifier.fillMaxWidth()) {
                    Text(entry.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }
        }
    }
}
