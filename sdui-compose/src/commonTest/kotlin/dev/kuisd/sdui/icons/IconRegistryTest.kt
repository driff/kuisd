package dev.kuisd.sdui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IconRegistryTest {
    @Test
    fun default_registry_contains_base_icons() {
        assertNotNull(DefaultIconRegistry.get("home"))
        assertNotNull(DefaultIconRegistry.get("search"))
        assertNotNull(DefaultIconRegistry.get("arrowBack"))
        assertNotNull(DefaultIconRegistry.get("settings"))
    }

    @Test
    fun unknown_name_returns_null() {
        assertNull(DefaultIconRegistry.get("does-not-exist"))
    }

    @Test
    fun plus_combines_with_override_semantics_other_wins() {
        val base = iconRegistry { register("home", Icons.Filled.Home) }
        val override = iconRegistry {
            register("home", Icons.Filled.Settings) // override gana
            register("custom", Icons.Filled.Settings)
        }
        val combined = base + override
        assertEquals(Icons.Filled.Settings, combined.get("home"))
        assertEquals(Icons.Filled.Settings, combined.get("custom"))
    }

    @Test
    fun empty_registry_returns_null_for_everything() {
        assertNull(IconRegistry.Empty.get("home"))
    }
}
