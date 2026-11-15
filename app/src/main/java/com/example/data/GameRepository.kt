package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val savedConfig: Flow<SavedConfigEntity?> = gameDao.observeConfig()
    val recentBattles: Flow<List<BattleRecordEntity>> = gameDao.observeRecentBattles()

    suspend fun getSavedConfigOnce(): SavedConfigEntity? = gameDao.getConfigOnce()

    suspend fun saveConfig(config: SavedConfigEntity) {
        gameDao.saveConfig(config)
    }

    suspend fun resetConfig() {
        gameDao.clearConfig()
    }

    suspend fun recordBattle(record: BattleRecordEntity) {
        gameDao.insertBattleRecord(record)
    }
}
