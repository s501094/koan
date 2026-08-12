package com.tyell.koan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY position ASC")
    fun observeAll(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces ORDER BY position ASC")
    suspend fun getAll(): List<SpaceEntity>

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun get(id: String): SpaceEntity?

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(space: SpaceEntity)

    @Update
    suspend fun update(space: SpaceEntity)

    @Delete
    suspend fun delete(space: SpaceEntity)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM spaces")
    suspend fun nextPosition(): Int
}

@Dao
interface EssentialDao {
    @Query("SELECT * FROM essentials WHERE spaceId = :spaceId ORDER BY position ASC")
    fun observeForSpace(spaceId: String): Flow<List<EssentialEntity>>

    @Query("SELECT COUNT(*) FROM essentials WHERE spaceId = :spaceId")
    suspend fun countForSpace(spaceId: String): Int

    @Query("SELECT * FROM essentials WHERE spaceId = :spaceId AND url = :url LIMIT 1")
    suspend fun find(spaceId: String, url: String): EssentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(essential: EssentialEntity)

    @Delete
    suspend fun delete(essential: EssentialEntity)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM essentials WHERE spaceId = :spaceId")
    suspend fun nextPosition(spaceId: String): Int
}

@Dao
interface BoostDao {
    @Query("SELECT * FROM boosts")
    fun observeAll(): Flow<List<BoostEntity>>

    @Query("SELECT * FROM boosts")
    suspend fun getAll(): List<BoostEntity>

    @Query("SELECT * FROM boosts WHERE pattern = :pattern LIMIT 1")
    suspend fun forPattern(pattern: String): BoostEntity?

    @Query("SELECT * FROM boosts WHERE pattern = :pattern LIMIT 1")
    fun observeForPattern(pattern: String): Flow<BoostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(boost: BoostEntity)

    @Delete
    suspend fun delete(boost: BoostEntity)
}

@Database(
    entities = [SpaceEntity::class, EssentialEntity::class, BoostEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class KoanDatabase : RoomDatabase() {
    abstract fun spaces(): SpaceDao
    abstract fun essentials(): EssentialDao
    abstract fun boosts(): BoostDao

    companion object {
        fun create(context: Context): KoanDatabase =
            Room.databaseBuilder(context, KoanDatabase::class.java, "koan.db")
                // v1 -> v2 added the boosts table. Nothing to preserve in it,
                // and Spaces/Essentials are untouched, so a plain create is
                // the whole migration.
                .addMigrations(MIGRATION_1_2)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `boosts` (
                        `id` TEXT NOT NULL,
                        `pattern` TEXT NOT NULL,
                        `css` TEXT NOT NULL,
                        `js` TEXT NOT NULL,
                        `zapSelectors` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_boosts_pattern` ON `boosts` (`pattern`)",
                )
            }
        }
    }
}
