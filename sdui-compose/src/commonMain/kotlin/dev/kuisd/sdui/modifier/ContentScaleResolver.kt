package dev.kuisd.sdui.modifier

import androidx.compose.ui.layout.ContentScale

/** Mapea el string de `contentScale` del contrato (spec 012) a `ContentScale` de Compose (default Fit). */
internal fun String.toContentScale(): ContentScale = when (this) {
    "crop" -> ContentScale.Crop
    "fillBounds" -> ContentScale.FillBounds
    "inside" -> ContentScale.Inside
    "none" -> ContentScale.None
    else -> ContentScale.Fit
}
