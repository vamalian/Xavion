package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        MemoryRecordEntity::class,
        TaskItemEntity::class,
        NoteEntity::class,
        ToolExecutionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class XavionDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun toolLogDao(): ToolLogDao

    companion object {
        @Volatile
        private var INSTANCE: XavionDatabase? = null

        fun getInstance(context: Context): XavionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XavionDatabase::class.java,
                    "xavion_core.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class ConversationRepository(private val dao: ConversationDao) {
    val conversations: Flow<List<ConversationEntity>> = dao.observeConversations()
    val recentConversations: Flow<List<ConversationEntity>> = dao.observeRecentConversations()

    fun messages(conversationId: Long): Flow<List<ChatMessageEntity>> =
        dao.observeMessages(conversationId)

    suspend fun createConversation(title: String, mode: String): Long =
        dao.insertConversation(ConversationEntity(title = title, mode = mode))

    suspend fun addMessage(
        conversationId: Long,
        sender: String,
        content: String,
        imageUri: String? = null
    ) {
        val now = System.currentTimeMillis()
        dao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                sender = sender,
                content = content,
                imageUri = imageUri,
                timestamp = now
            )
        )
        if (sender == "USER") {
            val preview = content.trim().take(44).ifEmpty { "Voice / Multimodal Session" }
            dao.updateConversationMeta(conversationId, preview, now)
        }
    }

    suspend fun deleteConversation(id: Long) = dao.deleteConversation(id)
    suspend fun clearAll() = dao.deleteAllConversations()
}

class MemoryRepository(private val dao: MemoryDao) {
    val memories: Flow<List<MemoryRecordEntity>> = dao.observeMemories()

    suspend fun getAllOnce(): List<MemoryRecordEntity> = dao.getAllMemoriesOnce()

    suspend fun addOrSave(
        category: String,
        title: String,
        content: String,
        pinned: Boolean = false
    ): Long = dao.insertMemory(
        MemoryRecordEntity(
            category = category,
            title = title,
            content = content,
            pinned = pinned
        )
    )

    suspend fun update(record: MemoryRecordEntity) =
        dao.updateMemory(record.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) = dao.deleteMemory(id)
    suspend fun clearAll() = dao.clearAllMemories()
}

class ProductivityRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val toolLogDao: ToolLogDao
) {
    val tasks: Flow<List<TaskItemEntity>> = taskDao.observeTasks()
    val notes: Flow<List<NoteEntity>> = noteDao.observeNotes()
    val toolLogs: Flow<List<ToolExecutionLogEntity>> = toolLogDao.observeLogs()

    suspend fun getOpenTasksOnce(): List<TaskItemEntity> = taskDao.getOpenTasksOnce()

    suspend fun addTask(
        title: String,
        details: String = "",
        dueLabel: String = "",
        priority: String = "Normal",
        isReminder: Boolean = false
    ): Long = taskDao.insertTask(
        TaskItemEntity(
            title = title,
            details = details,
            dueLabel = dueLabel,
            priority = priority,
            isReminder = isReminder
        )
    )

    suspend fun toggleTask(task: TaskItemEntity) =
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))

    suspend fun deleteTask(id: Long) = taskDao.deleteTask(id)

    suspend fun addNote(title: String, content: String, tag: String = "General"): Long =
        noteDao.insertNote(NoteEntity(title = title, content = content, tag = tag))

    suspend fun updateNote(note: NoteEntity) =
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNote(id: Long) = noteDao.deleteNote(id)

    suspend fun logToolExecution(
        functionName: String,
        argumentsSummary: String,
        status: String,
        resultMessage: String
    ) {
        toolLogDao.insertLog(
            ToolExecutionLogEntity(
                functionName = functionName,
                argumentsSummary = argumentsSummary,
                status = status,
                resultMessage = resultMessage
            )
        )
    }

    suspend fun clearToolLogs() = toolLogDao.clearLogs()
}
