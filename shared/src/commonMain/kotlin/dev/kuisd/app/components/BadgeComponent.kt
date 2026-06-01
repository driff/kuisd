package dev.kuisd.app.components

import dev.kuisd.sdui.core.sduiComponent
import kotlinx.serialization.Serializable

@Serializable
internal data class BadgeProps(
    val text: String = "",
)

internal val BadgeComponent = sduiComponent<BadgeProps>("badge")
