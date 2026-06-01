package dev.kuisd.app.variables

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.kuisd.sdui.VariableScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Estado reactivo de variables locales de la pantalla actual (HU-3). Vive en la app, no en el motor.
 * Expone una vista de SOLO LECTURA (`scope: VariableScope`) que cumple el seam del motor; toda mutación
 * pasa por los métodos públicos que invoca el [VariableActionHandler].
 */
@Stable
class VariableStore {
    internal var vars: Map<String, JsonElement> by mutableStateOf(emptyMap())
        private set

    /** Vista de solo lectura observada por Compose: el motor lee bindings por aquí. */
    val scope: VariableScope = VariableScope { name -> vars[name] }

    /**
     * Siembra idempotente (merge no-overwrite) al cargar el envelope (HU-3.1 / spec 007 HU-6.5).
     *
     * - Claves nuevas: se añaden.
     * - Claves ya presentes: **se preservan** (no se sobrescribe el valor actual).
     *
     * Defensa contra re-emisión del envelope dentro de la misma entrada del back stack: protege a
     * los nodos que leen `$bind` de rebotar al valor inicial mientras el usuario edita esa misma
     * variable. La 1ª siembra (mapa vacío) se comporta igual que la versión anterior.
     */
    fun seed(initial: Map<String, JsonElement>) {
        val current = vars
        var changed = false
        val merged = current.toMutableMap()
        initial.forEach { (k, v) ->
            if (k !in current) {
                merged[k] = v
                changed = true
            }
        }
        if (changed) vars = merged.toMap()
    }

    /** Fija [name] a [value] (HU-3.2). */
    fun set(name: String, value: JsonElement) {
        vars = vars + (name to value)
    }

    /** Invierte el booleano de [name] (ausente/no-booleano → `false` → pasa a `true`) (HU-3.3, HU-5.3). */
    fun toggle(name: String) {
        val curr = (vars[name] as? JsonPrimitive)?.booleanOrNull ?: false
        vars = vars + (name to JsonPrimitive(!curr))
    }

    /** Suma [by] al entero de [name] (ausente/no-entero parte de 0) y aplica clamp `[min, max]` (HU-3.4, HU-3.5). */
    fun increment(name: String, by: Int, min: Int?, max: Int?) {
        val curr = (vars[name] as? JsonPrimitive)?.intOrNull ?: 0
        var next = curr + by
        if (min != null) next = maxOf(next, min)
        if (max != null) next = minOf(next, max)
        vars = vars + (name to JsonPrimitive(next))
    }
}
