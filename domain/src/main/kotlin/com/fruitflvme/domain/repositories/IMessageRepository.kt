package com.fruitflvme.domain.repositories

import android.net.Uri
import com.fruitflvme.core.utils.Result
import com.fruitflvme.domain.model.MessageWithSender
import kotlinx.coroutines.flow.Flow

interface IMessageRepository {
    // Sending Messages
    suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        text: String
    ): Result<Unit>

    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUri: Uri
    ): Result<Unit>
    // Add other media types as needed

    // Retrieving Messages
    fun getMessagesForChat(chatId: String): Flow<List<MessageWithSender>>

    // Message Actions
    suspend fun changeMessageStatus(
        messageId: String,
        messageStatus: String,
        chatId: String
    ): Result<Unit>

    suspend fun deleteMessage(messageId: String, chatId: String): Result<Unit>
}