package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val role: String, // "ADMIN" or "USER"
    val isActive: Boolean = true,
    val isOnline: Boolean = false,
    val maxSessions: Int = 3,
    val currentSessions: Int = 1,
    val maxServers: Int = 4,
    val planName: String = "Pro Solver",
    val planExpiryDate: String = "2026-12-31",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activation_codes")
data class ActivationCodeEntity(
    @PrimaryKey val code: String,
    val planName: String, // "Basic", "Pro Solver", "Elite VIP", "Lifetime"
    val durationDays: Int,
    val maxUses: Int = 1,
    val usedCount: Int = 0,
    val isRevoked: Boolean = false,
    val createdBy: String = "admin",
    val createdAt: Long = System.currentTimeMillis(),
    val redeemedBy: String? = null
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val targetUser: String,
    val details: String,
    val level: String = "INFO" // "INFO", "WARNING", "SUCCESS", "ERROR"
)

@Entity(tableName = "solver_history")
data class SolverHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolType: String, // "SPRIX", "QUREO", "WISO"
    val title: String,
    val subjectOrStage: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED"
)
