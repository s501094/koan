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

@Database(
    entities = [SpaceEntity::class, EssentialEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class KoanDatabase : RoomDatabase() {
    abstract fun spaces(): SpaceDao
    abstract fun essentials(): EssentialDao

    companion object {
        fun create(context: Context): KoanDatabase =
            Room.databaseBuilder(context, KoanDatabase::class.java, "koan.db").build()
    }
}
