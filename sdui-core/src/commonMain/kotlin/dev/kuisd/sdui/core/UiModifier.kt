package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable

@Serializable
data class UiModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: SpaceToken? = null,
    val height: SpaceToken? = null,
    val padding: PaddingTokens? = null,
    val background: ColorToken? = null,
    val cornerRadius: RadiusToken? = null,
    val elevation: ElevationToken? = null,
    val weight: Float? = null,
    val alignment: AlignmentToken? = null,
)

@Serializable
data class PaddingTokens(
    val l: SpaceToken? = null,
    val t: SpaceToken? = null,
    val r: SpaceToken? = null,
    val b: SpaceToken? = null,
)
