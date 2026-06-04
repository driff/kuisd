package dev.kuisd.builder.export

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiEnvelope

/**
 * (De)serialización PURA del documento del builder: único punto de conversión `SduiEnvelope` ↔ `String`
 * usando [DefaultSduiJson] (idéntico en forma al envelope del BFF). Sin E/S ni UI.
 */
internal fun SduiEnvelope.encodeToJson(): String =
    DefaultSduiJson.encodeToString(SduiEnvelope.serializer(), this)

/** Parsea [json] a un [SduiEnvelope]; lanza si el JSON es inválido (lo captura quien llame, p.ej. el store). */
internal fun decodeEnvelope(json: String): SduiEnvelope =
    DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), json)
