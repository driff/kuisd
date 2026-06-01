package dev.kuisd.sdui

// TODO(spec dispatch de acciones): sustituir por un logger estructurado (p.ej. Kermit / oslog /
// Logcat) cuando se implemente el despacho real de UiAction. De momento centraliza el log del slice.
internal fun sduiLog(message: String) {
    println("[kuisd-sdui] $message")
}
