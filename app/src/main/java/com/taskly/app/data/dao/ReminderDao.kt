package com.taskly.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskly.app.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>): List<Long>

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY reminderDateTime ASC")
    fun getRemindersForTask(taskId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY reminderDateTime ASC")
    suspend fun getRemindersForTaskSync(taskId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND isTriggered = 0 ORDER BY reminderDateTime ASC")
    suspend fun getAllPendingActiveReminders(): List<ReminderEntity>

    @Query("UPDATE reminders SET isActive = 0 WHERE taskId = :taskId")
    suspend fun deactivateRemindersForTask(taskId: Long)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}

