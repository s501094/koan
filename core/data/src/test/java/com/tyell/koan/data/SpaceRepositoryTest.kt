package com.tyell.koan.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpaceRepositoryTest {

    private lateinit var db: KoanDatabase
    private lateinit var repo: SpaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, KoanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SpaceRepository(context, db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `seeding creates two spaces and is idempotent`() = runTest {
        repo.ensureSeeded()
        repo.ensureSeeded()

        val all = repo.spaces.first()
        assertEquals(2, all.size)
        assertEquals(listOf("Personal", "Work"), all.map { it.name })
        // Distinct cookie jars is the whole point.
        assertEquals(2, all.map { it.contextId }.toSet().size)
    }

    @Test
    fun `active space falls back to the first when nothing is stored`() = runTest {
        repo.ensureSeeded()
        assertEquals("Personal", repo.activeSpace.first()?.name)
    }

    @Test
    fun `active space follows what was set`() = runTest {
        repo.ensureSeeded()
        val work = repo.spaces.first().first { it.name == "Work" }
        repo.setActive(work.id)
        assertEquals("Work", repo.activeSpace.first()?.name)
    }

    @Test
    fun `a stale active id falls back rather than leaving nowhere to be`() = runTest {
        repo.ensureSeeded()
        repo.setActive("does-not-exist")
        assertNotNull(repo.activeSpace.first())
    }

    @Test
    fun `essentials stop at Zen's limit of twelve`() = runTest {
        val space = repo.createSpace("Test", "🧪")
        repeat(EssentialEntity.MAX_PER_SPACE) { i ->
            assertTrue(repo.addEssential(space.id, "https://site$i.example", "Site $i"))
        }
        assertFalse(repo.addEssential(space.id, "https://overflow.example", "Overflow"))
        assertEquals(EssentialEntity.MAX_PER_SPACE, repo.essentials(space.id).first().size)
    }

    @Test
    fun `pinning the same url twice does not duplicate it`() = runTest {
        val space = repo.createSpace("Test", "🧪")
        repo.addEssential(space.id, "https://a.example", "A")
        repo.addEssential(space.id, "https://a.example", "A again")
        assertEquals(1, repo.essentials(space.id).first().size)
    }

    @Test
    fun `essentials are scoped to their space`() = runTest {
        val a = repo.createSpace("A", "🏡")
        val b = repo.createSpace("B", "💼")
        repo.addEssential(a.id, "https://a.example", "A")

        assertEquals(1, repo.essentials(a.id).first().size)
        assertEquals(0, repo.essentials(b.id).first().size)
    }

    @Test
    fun `deleting a space takes its essentials with it`() = runTest {
        val a = repo.createSpace("A", "🏡")
        repo.createSpace("B", "💼")
        repo.addEssential(a.id, "https://a.example", "A")

        assertTrue(repo.deleteSpace(a))
        assertEquals(0, repo.essentials(a.id).first().size)
    }

    @Test
    fun `the last space cannot be deleted`() = runTest {
        val only = repo.createSpace("Only", "🏡")
        assertFalse(repo.deleteSpace(only))
        assertEquals(1, repo.spaces.first().size)
    }

    @Test
    fun `saving a theme persists it on the space`() = runTest {
        val space = repo.createSpace("Test", "🧪")
        val spec = space.toThemeSpec().copy(opacity = 0.9, texture = 0.4, lightness = 33)

        repo.saveTheme(space.id, spec)

        val reloaded = repo.spaces.first().first { it.id == space.id }.toThemeSpec()
        assertEquals(0.9, reloaded.opacity, 1e-9)
        assertEquals(0.4, reloaded.texture, 1e-9)
        assertEquals(33, reloaded.lightness)
    }

    @Test
    fun `new spaces land at the end`() = runTest {
        repo.ensureSeeded()
        val third = repo.createSpace("Third", "📚")
        assertEquals(third.id, repo.spaces.first().last().id)
    }
}
