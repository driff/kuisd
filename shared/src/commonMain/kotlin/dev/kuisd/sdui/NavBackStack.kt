package dev.kuisd.sdui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Una entrada de la pila de navegación: ruta del BFF + args opcionales (HU-2). */
data class NavEntry(
    val route: String,
    val args: Map<String, String> = emptyMap(),
)

/** Pila de navegación en memoria (snapshot-state). Único consumidor: la composición de [SduiHost] (HU-2, HU-4.1). */
@Stable
class NavBackStack(
    initial: NavEntry,
) {
    var entries: List<NavEntry> by mutableStateOf(listOf(initial))
        private set

    val current: NavEntry get() = entries.last()
    val canGoBack: Boolean get() = entries.size > 1

    fun push(entry: NavEntry) {
        entries = entries + entry
    }

    /** Desapila el tope. No desapila la raíz (HU-2.3): devuelve `false` si solo queda una entrada. */
    fun pop(): Boolean {
        if (entries.size <= 1) return false
        entries = entries.dropLast(1)
        return true
    }
}
