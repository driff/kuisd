package dev.kuisd.builder.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kuisd.app.SduiPreviewEnvironment
import dev.kuisd.builder.model.BuilderDocument
import dev.kuisd.sdui.RenderNode
import dev.kuisd.sdui.SduiActionHandler

/**
 * Preview multi-dispositivo (spec 018): selector de dispositivo + lienzo dimensionado (dp) a tamaño real
 * con scroll cuando excede el panel. El estado de dispositivo es efímero (no se guarda en el documento) y
 * solo cambia el `size` del lienzo; el mismo [SduiPreviewEnvironment] renderiza el mismo árbol.
 */
@Composable
internal fun PreviewPane(
    document: BuilderDocument,
    handler: SduiActionHandler,
    modifier: Modifier = Modifier,
) {
    var device by remember { mutableStateOf(DefaultDevice) }
    Column(modifier) {
        DeviceSelector(selected = device, onSelect = { device = it }) // HU-1
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Surface( // lienzo a tamaño real (1:1), distinguible (borde + sombra) — HU-2.1
                modifier = Modifier.size(device.width, device.height),
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                SduiPreviewEnvironment(handler) { RenderNode(document.root) } // HU-2.2 (mismo árbol)
            }
        }
    }
}

/** Fila de chips seleccionables para los presets; resalta el seleccionado (HU-1.1). */
@Composable
private fun DeviceSelector(selected: DevicePreset, onSelect: (DevicePreset) -> Unit) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        DevicePreset.entries.forEachIndexed { index, preset ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(preset.label) },
            )
        }
    }
}
