package com.taskly.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.model.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskWithDetails(taskId: Long): Flow<TaskWithDetails?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithDetailsSync(taskId: Long): TaskWithDetails?

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, 
            dueDate ASC, 
            dueTime ASC, 
            id DESC
    """)
    fun getAllActiveTasks(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 AND status = 'PENDING' AND dueDate >= :startOfDay AND dueDate <= :endOfDay 
        ORDER BY 
            CASE WHEN dueTime IS NULL THEN 1 ELSE 0 END, 
            dueTime ASC, 
            priority DESC
    """)
    fun getTodayPendingTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 AND status = 'COMPLETED' AND completedAt >= :startOfDay AND completedAt <= :endOfDay 
        ORDER BY completedAt DESC
    """)
    fun getTodayCompletedTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 AND status = 'PENDING' AND dueDate < :startOfDay 
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getOverdueTasks(startOfDay: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 AND status = 'PENDING' AND dueDate > :endOfDay 
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getUpcomingTasks(endOfDay: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 AND dueDate >= :startDate AND dueDate <= :endDate 
        ORDER BY dueDate ASC, dueTime ASC
    """)
    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE archived = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY id DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE archived = 1 ORDER BY updatedAt DESC")
    fun getArchivedTasks(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getAllCompletedTasks(): Flow<List<TaskWithDetails>>

    @Query("DELETE FROM tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedTasks()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT COUNT(*) FROM tasks WHERE archived = 0")
    suspend fun getTotalTaskCount(): Int
}

