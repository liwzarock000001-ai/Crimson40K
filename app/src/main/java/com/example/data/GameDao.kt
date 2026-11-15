package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_config WHERE id = 1")
    fun observeConfig(): Flow<SavedConfigEntity?>

    @Query("SELECT * FROM saved_config WHERE id = 1")
    suspend fun getConfigOnce(): SavedConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SavedConfigEntity)

    @Query("DELETE FROM saved_config")
    suspend fun clearConfig()

    @Query("SELECT * FROM battle_records ORDER BY timestamp DESC LIMIT 10")
    fun observeRecentBattles(): Flow<List<BattleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattleRecord(record: BattleRecordEntity)
}
