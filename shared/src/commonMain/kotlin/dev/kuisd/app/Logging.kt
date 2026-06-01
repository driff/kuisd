package dev.kuisd.app

// TODO(spec dispatch de acciones): sustituir por un logger estructurado (p.ej. Kermit / oslog /
// Logcat) cuando se implemente el despacho real de UiAction. De momento centraliza el log de la app
// (la capa app no usa el logger interno del motor `:sdui-compose`).
internal fun appLog(message: String) {
    println("[kuisd-app] $message")
}
