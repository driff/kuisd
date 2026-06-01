package dev.kuisd.sdui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.json.JsonElement

/**
 * Seam de SOLO LECTURA de variables (HU-1.1, HU-1.2). El motor lee bindings contra este seam;
 * la app aporta la implementación (`VariableStore.scope` la cumple). Aquí no hay mutación ni estado
 * mutable — la frontera la mantiene `:sdui-compose`.
 */
fun interface VariableScope {
    /** Valor actual de [name], o `null` si no existe. La lectura es observada por Compose en el impl de la app. */
    fun get(name: String): JsonElement?
}

/**
 * Seam inyectado por el host; default vacío para usar `RenderNode` fuera de un host (HU-1.1).
 * Un cliente sin host pinta los literales con `$` tal cual (degradación visible, no crash).
 */
val LocalVariables: ProvidableCompositionLocal<VariableScope> =
    staticCompositionLocalOf { VariableScope { null } }
