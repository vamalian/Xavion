package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT 5")
    fun observeRecentConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationMeta(id: Long, title: String, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_records ORDER BY pinned DESC, updatedAt DESC")
    fun observeMemories(): Flow<List<MemoryRecordEntity>>

    @Query("SELECT * FROM memory_records ORDER BY pinned DESC, updatedAt DESC")
    suspend fun getAllMemoriesOnce(): List<MemoryRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(record: MemoryRecordEntity): Long

    @Update
    suspend fun updateMemory(record: MemoryRecordEntity)

    @Query("DELETE FROM memory_records WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memory_records")
    suspend fun clearAllMemories()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_items ORDER BY isCompleted ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskItemEntity>>

    @Query("SELECT * FROM task_items WHERE isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getOpenTasksOnce(): List<TaskItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItemEntity): Long

    @Update
    suspend fun updateTask(task: TaskItemEntity)

    @Query("DELETE FROM task_items WHERE id = :id")
    suspend fun deleteTask(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}

@Dao
interface ToolLogDao {
    @Query("SELECT * FROM tool_execution_logs ORDER BY timestamp DESC LIMIT 50")
    fun observeLogs(): Flow<List<ToolExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ToolExecutionLogEntity): Long

    @Query("DELETE FROM tool_execution_logs")
    suspend fun clearLogs()
}
