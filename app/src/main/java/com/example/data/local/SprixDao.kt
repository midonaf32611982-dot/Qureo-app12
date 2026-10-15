package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivationCodeEntity
import com.example.data.model.ActivityLogEntity
import com.example.data.model.SolverHistoryEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SprixDao {

    // Users
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    fun getTotalUsersCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE isOnline = 1")
    fun getOnlineUsersCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    fun getActiveUsersCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 0")
    fun getSuspendedUsersCountFlow(): Flow<Int>

    // Activation Codes
    @Query("SELECT * FROM activation_codes ORDER BY createdAt DESC")
    fun getAllActivationCodesFlow(): Flow<List<ActivationCodeEntity>>

    @Query("SELECT * FROM activation_codes WHERE code = :code LIMIT 1")
    suspend fun getActivationCode(code: String): ActivationCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivationCode(code: ActivationCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivationCodes(codes: List<ActivationCodeEntity>)

    @Update
    suspend fun updateActivationCode(code: ActivationCodeEntity)

    @Query("DELETE FROM activation_codes WHERE code = :code")
    suspend fun deleteActivationCode(code: String)

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getActivityLogsFlow(): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insertLog(log: ActivityLogEntity)

    // Solver History
    @Query("SELECT * FROM solver_history ORDER BY timestamp DESC LIMIT 50")
    fun getSolverHistoryFlow(): Flow<List<SolverHistoryEntity>>

    @Insert
    suspend fun insertSolverHistory(history: SolverHistoryEntity)
}
