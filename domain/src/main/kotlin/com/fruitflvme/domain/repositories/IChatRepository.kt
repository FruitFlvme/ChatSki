package com.fruitflvme.domain.repositories

import com.fruitflvme.core.utils.Result
import com.fruitflvme.domain.model.Chat
import com.fruitflvme.domain.model.ChatType
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    // Chat Management
    suspend fun createChat(
        type: ChatType,
        name: String?,
        avatarUrl: String?,
        initialParticipantIds: List<String>, // User IDs of initial participants
        currentUserId: String // ID of the user creating the chat
    ): Result<Chat>

    // Chat Retrieval
    fun getChatsForUser(userId: String): Flow<List<Chat>> // Observe all chats for a user
    fun getChatById(chatId: String): Flow<Chat?> // Observe a specific chat by ID

    // Chat Updates
    suspend fun updateChat(chat: Chat): Result<Unit>
    suspend fun deleteChat(chatId: String): Result<Unit>

    // Other potentially useful methods
    suspend fun addParticipantToChat(
        chatId: String,
        userId: String
    ): Result<Unit>

    suspend fun removeParticipantFromChat(chatId: String, userId: String): Result<Unit>
}