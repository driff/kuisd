package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ImageRegistryTest {
    // Factories @Composable que NUNCA se invocan: el registry solo almacena/recupera presencia.
    private val factoryA: @Composable () -> Painter = { error("not invoked") }
    private val factoryB: @Composable () -> Painter = { error("not invoked") }

    @Test
    fun empty_registry_returns_null() {
        assertNull(ImageRegistry.Empty.get("logo"))
    }

    @Test
    fun register_makes_get_non_null() {
        val registry = imageRegistry { register("logo", factoryA) }
        assertNotNull(registry.get("logo"))
        assertNull(registry.get("missing"))
    }

    @Test
    fun plus_combines_with_override_semantics_other_wins() {
        val base = imageRegistry {
            register("logo", factoryA)
            register("brand", factoryA)
        }
        val override = imageRegistry { register("logo", factoryB) } // override gana
        val combined = base + override

        assertSame(factoryB, combined.get("logo"))
        assertSame(factoryA, combined.get("brand"))
    }
}
