package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val savedConfig: Flow<SavedConfigEntity?> = gameDao.observeConfig()
    val savedSkirmishBattle: Flow<SavedSkirmishBattleEntity?> = gameDao.observeSavedBattle()
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

    suspend fun getSavedBattleOnce(): SavedSkirmishBattleEntity? = gameDao.getSavedBattleOnce()

    suspend fun saveSkirmishBattle(battle: SavedSkirmishBattleEntity) {
        gameDao.saveSkirmishBattle(battle)
    }

    suspend fun clearSavedBattle() {
        gameDao.clearSavedBattle()
    }
}
