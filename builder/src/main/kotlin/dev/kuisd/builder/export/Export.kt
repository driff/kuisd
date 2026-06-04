package dev.kuisd.builder.export

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode

/**
 * Exporta el árbol [root] como un envelope JSON idéntico al del BFF (HU-6): mismo `DefaultSduiJson`,
 * `schemaVersion = 1` y `screenId = "builder"`.
 */
internal fun exportEnvelope(root: SduiNode): String =
    DefaultSduiJson.encodeToString(
        SduiEnvelope.serializer(),
        SduiEnvelope(schemaVersion = 1, screenId = "builder", root = root),
    )
