package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_config")
data class SavedConfigEntity(
    @PrimaryKey val id: Int = 1,
    val playerFaction: String,
    val pointLimit: Int,
    val marineRosterJson: String,
    val xenosRosterJson: String,
    val customTemplatesJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "battle_records")
data class BattleRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val winnerFaction: String,
    val playerFaction: String,
    val playerWon: Boolean,
    val rounds: Int,
    val playerKills: Int,
    val enemyKills: Int,
    val timestamp: Long = System.currentTimeMillis()
)
