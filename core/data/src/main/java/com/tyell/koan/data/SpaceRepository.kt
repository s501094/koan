package com.tyell.koan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tyell.koan.theme.ThemePresets
import com.tyell.koan.theme.ZenThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.spacesDataStore: DataStore<Preferences> by preferencesDataStore("koan_spaces")

/**
 * Spaces and their Essentials.
 *
 * Which Space is active is a UI preference rather than a record, so it lives in
 * DataStore next to the database rather than as a column somebody has to
 * remember to keep singular.
 */
class SpaceRepository(
    private val context: Context,
    private val db: KoanDatabase,
) {
    private val activeKey = stringPreferencesKey("active_space")

    val spaces: Flow<List<SpaceEntity>> = db.spaces().observeAll()

    private val activeId: Flow<String?> =
        context.spacesDataStore.data.map { it[activeKey] }

    /**
     * The active Space, falling back to the first one if the stored id is
     * stale — which happens whenever the active Space is the one you delete.
     */
    val activeSpace: Flow<SpaceEntity?> = combine(spaces, activeId) { all, id ->
        all.firstOrNull { it.id == id } ?: all.firstOrNull()
    }

    fun essentials(spaceId: String): Flow<List<EssentialEntity>> =
        db.essentials().observeForSpace(spaceId)

    /** The Boost for a hostname, or null if the site has none. */
    fun boostFor(pattern: String): Flow<BoostEntity?> =
        db.boosts().observeForPattern(pattern)

    /** Creates the starter Spaces on first run. Idempotent. */
    suspend fun ensureSeeded() {
        if (db.spaces().count() > 0) return
        listOf(
            Triple("Personal", "🏡", "Nocturne"),
            Triple("Work", "💼", "Basalt"),
        ).forEachIndexed { index, (name, icon, presetName) ->
            val preset = ThemePresets.everything.firstOrNull { it.name == presetName }
                ?: ThemePresets.default
            db.spaces().upsert(
                SpaceEntity.create(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    icon = icon,
                    position = index,
                    preset = preset,
                ),
            )
        }
    }

    suspend fun setActive(spaceId: String) {
        context.spacesDataStore.edit { it[activeKey] = spaceId }
    }

    suspend fun createSpace(name: String, icon: String): SpaceEntity {
        val space = SpaceEntity.create(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = icon,
            position = db.spaces().nextPosition(),
        )
        db.spaces().upsert(space)
        return space
    }

    suspend fun rename(space: SpaceEntity, name: String, icon: String) {
        db.spaces().update(space.copy(name = name, icon = icon))
    }

    suspend fun saveTheme(spaceId: String, spec: ZenThemeSpec) {
        val space = db.spaces().get(spaceId) ?: return
        db.spaces().update(space.withTheme(spec))
    }

    /** Refuses to delete the last Space — there has to be somewhere to be. */
    suspend fun deleteSpace(space: SpaceEntity): Boolean {
        if (db.spaces().count() <= 1) return false
        db.spaces().delete(space)
        return true
    }

    /**
     * Adds an Essential, or returns false if the Space is already at Zen's
     * limit of twelve. Adding a URL that is already an Essential is a no-op
     * rather than a duplicate.
     */
    suspend fun addEssential(spaceId: String, url: String, title: String): Boolean {
        if (db.essentials().find(spaceId, url) != null) return true
        if (db.essentials().countForSpace(spaceId) >= EssentialEntity.MAX_PER_SPACE) return false
        db.essentials().upsert(
            EssentialEntity(
                id = UUID.randomUUID().toString(),
                spaceId = spaceId,
                url = url,
                title = title,
                position = db.essentials().nextPosition(spaceId),
            ),
        )
        return true
    }

    suspend fun removeEssential(essential: EssentialEntity) {
        db.essentials().delete(essential)
    }

    suspend fun isEssential(spaceId: String, url: String): Boolean =
        db.essentials().find(spaceId, url) != null
}
