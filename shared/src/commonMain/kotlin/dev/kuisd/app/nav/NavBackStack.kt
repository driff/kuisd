package dev.kuisd.app.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Una entrada de la pila de navegación: `id` estable monotónico + ruta del BFF + args opcionales (HU-4.1). */
data class NavEntry(
    val id: Long,
    val route: String,
    val args: Map<String, String> = emptyMap(),
)

/** Pila de navegación en memoria (snapshot-state). Único consumidor: la composición de `SduiHost` (HU-4.1). */
@Stable
class NavBackStack(
    startRoute: String,
    startArgs: Map<String, String> = emptyMap(),
) {
    private var nextId = 0L

    var entries: List<NavEntry> by mutableStateOf(listOf(NavEntry(nextId++, startRoute, startArgs)))
        private set

    val current: NavEntry get() = entries.last()
    val canGoBack: Boolean get() = entries.size > 1

    fun push(route: String, args: Map<String, String> = emptyMap()) {
        entries = entries + NavEntry(nextId++, route, args)
    }

    /** Desapila el tope. No desapila la raíz (HU-4): devuelve `false` si solo queda una entrada. */
    fun pop(): Boolean {
        if (entries.size <= 1) return false
        entries = entries.dropLast(1)
        return true
    }
}
