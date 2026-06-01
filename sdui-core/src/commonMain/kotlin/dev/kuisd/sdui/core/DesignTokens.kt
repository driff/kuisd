package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable @JvmInline
value class ColorToken(
    val ref: String,
)

@Serializable @JvmInline
value class TypeToken(
    val ref: String,
)

@Serializable @JvmInline
value class SpaceToken(
    val ref: String,
)

@Serializable @JvmInline
value class RadiusToken(
    val ref: String,
)

@Serializable @JvmInline
value class ElevationToken(
    val ref: String,
)

@Serializable @JvmInline
value class AlignmentToken(
    val ref: String,
)

object Tokens {
    object Color {
        val Primary = ColorToken("color.primary")
        val OnPrimary = ColorToken("color.onPrimary")
        val Surface = ColorToken("color.surface")
        val OnSurface = ColorToken("color.onSurface")
        val Error = ColorToken("color.error")
        val Outline = ColorToken("color.outline")
    }

    object Type {
        val Display = TypeToken("type.display")
        val Title = TypeToken("type.title")
        val Body = TypeToken("type.body")
        val Label = TypeToken("type.label")
        val Button = TypeToken("type.button")
    }

    object Space {
        val Xs = SpaceToken("space.xs")
        val Sm = SpaceToken("space.sm")
        val Md = SpaceToken("space.md")
        val Lg = SpaceToken("space.lg")
        val Xl = SpaceToken("space.xl")
    }

    object Radius {
        val None = RadiusToken("radius.none")
        val Sm = RadiusToken("radius.sm")
        val Md = RadiusToken("radius.md")
        val Lg = RadiusToken("radius.lg")
        val Xl = RadiusToken("radius.xl")
        val Card = RadiusToken("radius.card")
        val Pill = RadiusToken("radius.pill")
    }

    object Elevation {
        val None = ElevationToken("elevation.none")
        val Sm = ElevationToken("elevation.sm")
        val Md = ElevationToken("elevation.md")
        val Lg = ElevationToken("elevation.lg")
    }

    object Alignment {
        val Start = AlignmentToken("alignment.start")
        val Center = AlignmentToken("alignment.center")
        val End = AlignmentToken("alignment.end")
        val Top = AlignmentToken("alignment.top")
        val Bottom = AlignmentToken("alignment.bottom")
        val CenterHorizontally = AlignmentToken("alignment.centerH")
        val CenterVertically = AlignmentToken("alignment.centerV")
    }
}
