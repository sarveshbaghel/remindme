package com.taskly.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taskly.app.data.entity.TaskHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TaskHistoryEntity): Long

    @Query("SELECT * FROM task_history WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getHistoryForTask(taskId: Long): Flow<List<TaskHistoryEntity>>

    @Query("SELECT * FROM task_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TaskHistoryEntity>>

    @Query("SELECT * FROM task_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getHistorySince(startTime: Long): List<TaskHistoryEntity>
}

