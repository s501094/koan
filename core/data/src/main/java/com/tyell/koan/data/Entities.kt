package com.tyell.koan.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tyell.koan.theme.ColorDot
import com.tyell.koan.theme.ColorWheel
import com.tyell.koan.theme.DotType
import com.tyell.koan.theme.Harmony
import com.tyell.koan.theme.ThemePresets
import com.tyell.koan.theme.ZenThemeSpec

/**
 * A Space.
 *
 * Note what is *not* here: any record of which tabs belong to it. A tab's
 * Space is its `contextId`, which Gecko already stores as the cookie-jar
 * partition key and Android Components already persists through session
 * restore. Keeping a second copy here would be two sources of truth for one
 * fact, and they would eventually disagree.
 *
 * The theme is stored inline rather than in its own table. It is exactly one
 * row's worth of scalars — only the primary dot is kept, since the companions
 * are replayable from the harmony.
 */
@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val position: Int,

    val themeX: Double,
    val themeY: Double,
    val themeLightness: Int,
    val themeHarmony: String,
    val themeDotType: String,
    val themeOpacity: Double,
    val themeTexture: Double,
) {
    /** Gecko's storage partition key. Same string doubles as the Space tag. */
    val contextId: String get() = "koan-space-$id"

    fun toThemeSpec(): ZenThemeSpec {
        val harmony = Harmony.fromWire(themeHarmony)
        val type = DotType.entries.firstOrNull { it.wireName == themeDotType }
            ?: DotType.ExplicitLightness
        val primary = ColorDot(themeX, themeY, isPrimary = true, type = type)
        return ZenThemeSpec(
            dots = ColorWheel.applyHarmony(primary, harmony),
            lightness = themeLightness,
            harmony = harmony,
            opacity = themeOpacity,
            texture = themeTexture,
        )
    }

    fun withTheme(spec: ZenThemeSpec): SpaceEntity {
        val primary = spec.dots.firstOrNull { it.isPrimary }
            ?: spec.dots.firstOrNull()
            ?: return this
        return copy(
            themeX = primary.x,
            themeY = primary.y,
            themeLightness = spec.lightness,
            themeHarmony = spec.harmony.wireName,
            themeDotType = primary.type.wireName,
            themeOpacity = spec.opacity,
            themeTexture = spec.texture,
        )
    }

    companion object {
        fun create(
            id: String,
            name: String,
            icon: String,
            position: Int,
            preset: ThemePresets.Preset = ThemePresets.default,
        ) = SpaceEntity(
            id = id,
            name = name,
            icon = icon,
            position = position,
            themeX = preset.x,
            themeY = preset.y,
            themeLightness = preset.lightness,
            themeHarmony = preset.harmony.wireName,
            themeDotType = preset.type.wireName,
            themeOpacity = 0.5,
            themeTexture = 0.0,
        )
    }
}

/**
 * An Essential — a favourite pinned to a Space.
 *
 * Zen caps these at twelve (`zen.tabs.essentials.max`) and resets them to the
 * pinned URL rather than wherever you navigated. Storing the URL rather than a
 * live tab is what makes the reset behaviour fall out for free.
 */
@Entity(
    tableName = "essentials",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("spaceId")],
)
data class EssentialEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val url: String,
    val title: String,
    val position: Int,
) {
    companion object {
        /** `zen.tabs.essentials.max` */
        const val MAX_PER_SPACE = 12
    }
}
