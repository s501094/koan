package com.tyell.koan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tyell.koan.theme.ColorDot
import com.tyell.koan.theme.ColorWheel
import com.tyell.koan.theme.DotType
import com.tyell.koan.theme.Harmony
import com.tyell.koan.theme.ThemePresets
import com.tyell.koan.theme.ZenThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("koan_theme")

/**
 * Persists the active theme.
 *
 * Only the primary dot is stored. The companions are always derivable by
 * replaying the harmony, so writing them would be storing the same information
 * twice and inviting the two copies to disagree. This is also the shape a
 * per-Space theme column will take when Spaces land.
 */
class ThemeStore(private val context: Context) {

    private object Keys {
        val x = doublePreferencesKey("x")
        val y = doublePreferencesKey("y")
        val lightness = intPreferencesKey("lightness")
        val harmony = stringPreferencesKey("harmony")
        val dotType = stringPreferencesKey("dot_type")
        val opacity = doublePreferencesKey("opacity")
        val texture = doublePreferencesKey("texture")
    }

    val spec: Flow<ZenThemeSpec> = context.themeDataStore.data.map { prefs ->
        val fallback = ThemePresets.default
        val harmony = prefs[Keys.harmony]?.let(Harmony::fromWire) ?: fallback.harmony
        val type = prefs[Keys.dotType]
            ?.let { wire -> DotType.entries.firstOrNull { it.wireName == wire } }
            ?: fallback.type

        val primary = ColorDot(
            x = prefs[Keys.x] ?: fallback.x,
            y = prefs[Keys.y] ?: fallback.y,
            isPrimary = true,
            type = type,
        )

        ZenThemeSpec(
            dots = ColorWheel.applyHarmony(primary, harmony),
            lightness = prefs[Keys.lightness] ?: fallback.lightness,
            harmony = harmony,
            opacity = prefs[Keys.opacity] ?: 0.5,
            texture = prefs[Keys.texture] ?: 0.0,
        )
    }

    suspend fun save(spec: ZenThemeSpec) {
        val primary = spec.dots.firstOrNull { it.isPrimary } ?: spec.dots.firstOrNull() ?: return
        context.themeDataStore.edit { prefs ->
            prefs[Keys.x] = primary.x
            prefs[Keys.y] = primary.y
            prefs[Keys.lightness] = spec.lightness
            prefs[Keys.harmony] = spec.harmony.wireName
            prefs[Keys.dotType] = primary.type.wireName
            prefs[Keys.opacity] = spec.opacity
            prefs[Keys.texture] = spec.texture
        }
    }
}
