package com.fruitflvme.data.local.daos

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fruitflvme.data.local.entities.Chat
import com.fruitflvme.data.local.entities.ChatParticipant
import com.fruitflvme.data.local.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // --- Операции с Chat ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("SELECT user_id FROM chat_participants WHERE chat_id = :chatId")
    suspend fun getParticipantsIdsForChat(chatId: String): List<String>

    @Query("SELECT * FROM chats WHERE chat_id = :chatId")
    fun getChatById(chatId: String): Flow<Chat?>

    @Query("SELECT * FROM chats ORDER BY last_message_timestamp DESC")
    fun getAllChats(): Flow<List<Chat>> // Для списка чатов в главном окне

    @Query("DELETE FROM chats WHERE chat_id = :chatId")
    suspend fun deleteChat(chatId: String)

    // --- Операции с ChatParticipant (для участников чата) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatParticipant(participant: ChatParticipant)

    @Query("DELETE FROM chat_participants WHERE chat_id = :chatId AND user_id = :userId")
    suspend fun deleteChatParticipant(chatId: String, userId: String)

    // --- Получение связанных данных (участников чата) ---

    // POJO для получения чата со списком его участников
    data class ChatWithParticipants(
        @Embedded val chat: Chat,
        @Relation(
            parentColumn = "chat_id",
            entityColumn = "user_id",
            associateBy = androidx.room.Junction(ChatParticipant::class)
        )
        val participants: List<User> // Список объектов User, которые являются участниками чата
    )

    @Transaction
    @Query("SELECT * FROM chats WHERE chat_id = :chatId")
    fun getChatWithParticipants(chatId: String): Flow<ChatWithParticipants?>

    // Получение всех чатов, в которых участвует данный пользователь
    @Transaction
    @Query(
        """
        SELECT * FROM chats AS c
        INNER JOIN chat_participants AS cp ON c.chat_id = cp.chat_id
        WHERE cp.user_id = :userId
        ORDER BY c.last_message_timestamp DESC
    """
    )
    fun getChatsForUser(userId: String): Flow<List<ChatWithParticipants>>
}