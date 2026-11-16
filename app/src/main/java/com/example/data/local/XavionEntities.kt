package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mode: String, // "LIVE_VOICE", "TEXT_CHAT", "VISION"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val sender: String, // "USER", "XAVION", "SYSTEM", "TOOL"
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_records")
data class MemoryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Preference", "Fact", "Instruction", "Context"
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_items")
data class TaskItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val details: String = "",
    val dueLabel: String = "",
    val priority: String = "Normal", // "Low", "Normal", "High"
    val isReminder: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String = "General",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_execution_logs")
data class ToolExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val functionName: String,
    val argumentsSummary: String,
    val status: String, // "EXECUTED", "DENIED", "FAILED", "PENDING"
    val resultMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)
