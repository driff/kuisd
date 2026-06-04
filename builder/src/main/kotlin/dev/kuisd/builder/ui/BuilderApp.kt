package dev.kuisd.builder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.app.SduiPreviewEnvironment
import dev.kuisd.builder.export.exportEnvelope
import dev.kuisd.builder.model.BuilderDocument
import dev.kuisd.builder.model.TreeOps
import dev.kuisd.sdui.RenderNode

private const val PREVIEW_WEIGHT = 0.55f
private const val PANEL_WEIGHT = 0.45f
private val EXPORT_PANEL_MAX_HEIGHT = 220.dp

/** App del builder: preview (izquierda) + paleta/outline/inspector/export (columna derecha). */
@Composable
internal fun BuilderApp() {
    val document = remember { BuilderDocument() }
    val handler = remember { LoggingActionHandler() }
    MaterialTheme {
        Surface {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(PREVIEW_WEIGHT).fillMaxHeight().padding(12.dp)) {
                    SduiPreviewEnvironment(handler) { RenderNode(document.root) }
                }
                VerticalDivider()
                RightColumn(document, Modifier.weight(PANEL_WEIGHT).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun RightColumn(document: BuilderDocument, modifier: Modifier) {
    var exported by remember { mutableStateOf<String?>(null) }
    Column(modifier) {
        SectionTitle("Paleta")
        PalettePane(onAdd = { document.insert(it.template) }, Modifier.weight(1f).fillMaxWidth())
        HorizontalDivider()
        SectionTitle("Outline")
        OutlinePane(
            root = document.root,
            selectedId = document.selectedId,
            onSelect = document::select,
            onDelete = document::delete,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        SectionTitle("Inspector")
        val selected = document.selectedId?.let { TreeOps.findById(document.root, it) }
        InspectorPane(
            node = selected,
            onProps = { props -> document.selectedId?.let { document.updateProps(it, props) } },
            onModifier = { mod -> document.selectedId?.let { document.updateModifier(it, mod) } },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        Button(onClick = { exported = exportEnvelope(document.root) }, modifier = Modifier.padding(8.dp)) {
            Text("Exportar JSON")
        }
        exported?.let { json ->
            Box(
                Modifier.heightIn(max = EXPORT_PANEL_MAX_HEIGHT).fillMaxWidth()
                    .padding(8.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(json, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
