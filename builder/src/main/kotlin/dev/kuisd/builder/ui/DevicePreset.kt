package dev.kuisd.builder.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Presets de dispositivo para enmarcar el preview a tamaño real (1:1). Estado efímero de UI (spec 018). */
internal enum class DevicePreset(
    val label: String,
    val width: Dp,
    val height: Dp,
) {
    Phone("Phone", 360.dp, 800.dp),
    Tablet("Tablet", 800.dp, 1280.dp),
    Desktop("Desktop", 1280.dp, 800.dp),
}

/** Dispositivo por defecto al abrir el builder (HU-1.2). */
internal val DefaultDevice = DevicePreset.Phone
