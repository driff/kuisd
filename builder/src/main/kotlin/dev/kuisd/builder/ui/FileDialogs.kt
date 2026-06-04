package dev.kuisd.builder.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// Diálogos NATIVOS del SO (AWT FileDialog) para elegir archivo. No testeables (smoke manual): toda la
// lógica de E/S vive en BuilderFileStore. Devuelven `null` cuando el usuario cancela (HU-1.6/HU-2.6).

/** Diálogo de apertura, filtrado a `*.json`. `null` si se cancela. */
internal fun openFileDialog(): File? = fileDialog("Abrir blueprint", FileDialog.LOAD, suggestedName = null)

/** Diálogo de guardado; propone [suggestedName] (con `.json`). `null` si se cancela. */
internal fun saveFileDialog(suggestedName: String): File? =
    fileDialog("Guardar blueprint como…", FileDialog.SAVE, suggestedName)

private fun fileDialog(title: String, mode: Int, suggestedName: String?): File? {
    val dialog = FileDialog(null as Frame?, title, mode).apply {
        file = suggestedName
        setFilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
        isVisible = true
    }
    val dir = dialog.directory
    val name = dialog.file
    return if (dir != null && name != null) File(dir, name) else null
}
