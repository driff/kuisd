package dev.kuisd.sdui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.SduiComponent
import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Empareja un [SduiComponent] (type + serializer<P>) con su renderer Compose. Decodifica `node.props`
 * a `P` y delega el render; props inválidas degradan a [UnknownNode] sin crash (HU-1.1, HU-4.2).
 */
class RegisteredComponent<P : Any>(
    val component: SduiComponent<P>,
    val renderer: @Composable RenderScope.(P) -> Unit,
) {
    @Composable
    fun Render(node: SduiNode) {
        val props = remember(node) {
            runCatching { DefaultSduiJson.decodeFromJsonElement(component.serializer, node.props) }.getOrNull()
        }
        if (props == null) {
            UnknownNode(node.type)
            return
        }
        with(RenderScope(node)) { renderer(props) }
    }
}

/** Registro abierto `type → componente`; se combina con [plus] y se resuelve con [rendererFor] (HU-1.1). */
class ComponentRegistry internal constructor(
    private val byType: Map<String, RegisteredComponent<*>>,
) {
    fun rendererFor(type: String): RegisteredComponent<*>? = byType[type]

    operator fun plus(other: ComponentRegistry): ComponentRegistry =
        ComponentRegistry(byType + other.byType)
}

/** Builder del DSL [componentRegistry]; cada `register` empareja un componente con su renderer. */
class ComponentRegistryBuilder {
    @PublishedApi internal val entries = mutableMapOf<String, RegisteredComponent<*>>()

    fun <P : Any> register(
        component: SduiComponent<P>,
        renderer: @Composable RenderScope.(P) -> Unit,
    ) {
        entries[component.type] = RegisteredComponent(component, renderer)
    }
}

fun componentRegistry(block: ComponentRegistryBuilder.() -> Unit): ComponentRegistry =
    ComponentRegistry(ComponentRegistryBuilder().apply(block).entries.toMap())

/** Registro inyectado por el host; default = [CorePack] para usar `RenderNode` fuera de un host (HU-1.3). */
val LocalComponentRegistry: ProvidableCompositionLocal<ComponentRegistry> =
    staticCompositionLocalOf { CorePack }
