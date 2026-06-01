package dev.kuisd.app.components

import dev.kuisd.sdui.core.sduiComponent
import kotlinx.serialization.Serializable

@Serializable
data class BadgeProps(
    val text: String = "",
)

internal val BadgeComponent = sduiComponent<BadgeProps>("badge")
