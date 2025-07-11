package com.fruitflvme.data.local.daos

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fruitflvme.data.local.entities.Message
import com.fruitflvme.data.local.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("DELETE FROM messages WHERE message_id= :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chat_id = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Transaction
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<Message>>

    @Transaction
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    fun getMessagesWithSenderForChat(chatId: String): Flow<List<MessageWithSender>>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp DESC LIMIT :limit")
    fun getLastMessagesForChat(chatId: String, limit: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE message_id = :messageId")
    suspend fun getMessageById(messageId: String): Message? // Для получения одного сообщения

    @Query("UPDATE messages SET status = :status, last_updated = :lastUpdated WHERE message_id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String, lastUpdated: Long)

    // POJO для получения сообщения с информацией об отправителе
    data class MessageWithSender(
        @Embedded val message: Message,
        @Relation(
            parentColumn = "sender_id",
            entityColumn = "user_id"
        )
        val sender: User
    )
}