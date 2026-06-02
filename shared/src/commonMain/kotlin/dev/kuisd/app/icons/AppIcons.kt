package dev.kuisd.app.icons

import dev.kuisd.sdui.icons.IconRegistry
import dev.kuisd.sdui.icons.iconRegistry

/**
 * Override de iconos de la app. Vacío en MVP — demuestra el patrón composicional: la app puede
 * añadir aquí iconos propios (p.ej. branding) y se combinan con `DefaultIconRegistry + appIconsOverride()`.
 */
internal fun appIconsOverride(): IconRegistry = iconRegistry {
    // MVP: vacío. Punto de extensión para iconos propios de la app.
}
