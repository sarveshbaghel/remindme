package com.taskly.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taskly.app.data.dao.CategoryDao
import com.taskly.app.data.dao.ReminderDao
import com.taskly.app.data.dao.TaskDao
import com.taskly.app.data.dao.TaskHistoryDao
import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.entity.TaskHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        ReminderEntity::class,
        TaskHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TasklyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun taskHistoryDao(): TaskHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TasklyDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TasklyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TasklyDatabase::class.java,
                    "taskly_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultCategories(database.categoryDao())
                }
            }
        }

        private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val defaults = listOf(
                CategoryEntity(name = "Personal", iconName = "Person", colorHex = 0xFF635BFF),
                CategoryEntity(name = "Work", iconName = "Work", colorHex = 0xFF4A90E2),
                CategoryEntity(name = "College", iconName = "School", colorHex = 0xFF9C27B0),
                CategoryEntity(name = "Projects", iconName = "Folder", colorHex = 0xFF009688),
                CategoryEntity(name = "Health", iconName = "FitnessCenter", colorHex = 0xFF22A06B),
                CategoryEntity(name = "Shopping", iconName = "ShoppingCart", colorHex = 0xFFE5A000),
                CategoryEntity(name = "Other", iconName = "MoreHoriz", colorHex = 0xFF777777)
            )
            categoryDao.insertAll(defaults)
        }
    }
}

