package dev.kuisd.builder.io

import dev.kuisd.builder.export.decodeEnvelope
import dev.kuisd.builder.export.encodeToJson
import dev.kuisd.sdui.core.SduiEnvelope
import java.io.File

/**
 * E/S de disco del builder: lee/escribe un [File] ↔ [SduiEnvelope] capturando cualquier fallo en
 * [Result] (HU-1.5/HU-2.5), para que la UI degrade a un mensaje en vez de cerrar la app. No abre
 * diálogos: recibe el [File] ya elegido. Lectura/escritura síncrona (archivos pequeños).
 */
internal object BuilderFileStore {
    fun read(file: File): Result<SduiEnvelope> =
        runCatching { decodeEnvelope(file.readText(Charsets.UTF_8)) }

    /** Garantiza extensión `.json`, escribe UTF-8 y devuelve el [File] realmente escrito. */
    fun write(file: File, envelope: SduiEnvelope): Result<File> =
        runCatching {
            withJsonExtension(file).also { it.writeText(envelope.encodeToJson(), Charsets.UTF_8) }
        }

    /** Añade `.json` si el nombre del archivo no termina ya en esa extensión (case-insensitive). */
    fun withJsonExtension(file: File): File =
        if (file.name.endsWith(".json", ignoreCase = true)) file else File("${file.path}.json")
}
