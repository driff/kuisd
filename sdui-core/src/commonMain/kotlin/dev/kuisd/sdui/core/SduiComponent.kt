package dev.kuisd.sdui.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

interface SduiComponent<P : Any> {
    val type: String
    val serializer: KSerializer<P>
}

inline fun <reified P : Any> sduiComponent(type: String): SduiComponent<P> =
    object : SduiComponent<P> {
        override val type = type
        override val serializer = serializer<P>()
    }
