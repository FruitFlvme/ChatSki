package com.fruitflvme.data.repositories

import com.fruitflvme.core.utils.Result
import com.fruitflvme.data.local.daos.ChatDao
import com.fruitflvme.data.local.daos.UserDao
import com.fruitflvme.data.local.entities.ChatParticipant
import com.fruitflvme.data.local.mappers.toDomainChat
import com.fruitflvme.data.local.mappers.toDomainUser
import com.fruitflvme.data.local.mappers.toLocalEntityChat
import com.fruitflvme.data.remote.FirebaseManager
import com.fruitflvme.data.remote.mappers.toFirebaseDTO
import com.fruitflvme.domain.model.Chat
import com.fruitflvme.domain.model.ChatType
import com.fruitflvme.domain.repositories.IChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import java.util.UUID
import com.fruitflvme.data.local.entities.Chat as LocalChat

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val firebaseManager: FirebaseManager
) : IChatRepository {

    override suspend fun createChat(
        type: ChatType,
        name: String?,
        avatarUrl: String?,
        initialParticipantIds: List<String>,
        currentUserId: String
    ): Result<Chat> {
        val chatId = UUID.randomUUID().toString()
        val chatEntity = LocalChat(
            chatId = chatId,
            chatType = type.name.lowercase(),
            chatName = name,
            chatAvatarUrl = avatarUrl,
            lastMessageText = null,
            lastMessageTimestamp = null,
            lastUpdated = System.currentTimeMillis()
        )

        val allParticipantIds = (initialParticipantIds + currentUserId).distinct()

        return when (val firebaseResult =
            firebaseManager.createChat(chatEntity, allParticipantIds)) {
            is Result.Success -> {
                // Сохраняем в локальную БД
                chatDao.insertChat(chatEntity)
                allParticipantIds.forEach { userId ->
                    chatDao.insertChatParticipant(ChatParticipant(chatId, userId))
                }
                // Получаем DomainChat для возврата
                val participants = allParticipantIds.mapNotNull {
                    userDao.getUserById(it).firstOrNull()?.toDomainUser()
                }
                Result.Success(chatEntity.toDomainChat(participants))
            }

            is Result.Failure ->
                Result.Failure(firebaseResult.exception)
        }
    }

    override fun getChatsForUser(userId: String): Flow<List<Chat>> {
        return combine(
            chatDao.getChatsForUser(userId), // Flow<List<ChatWithParticipants>> из Room
            firebaseManager.getChatsForUser(userId) // Flow<List<LocalEntityChat>> из FirebaseManager
        ) { localChatsWithParticipants, remoteChatsEntities ->

            coroutineScope {
                val localChatMap = localChatsWithParticipants.associateBy { it.chat.chatId }
                val remoteChatMap = remoteChatsEntities?.associateBy { it.chatId }

                // 1. Синхронизация чатов (ChatEntity)
                remoteChatsEntities?.forEach { remoteChatEntity ->
                    val existingLocalChat = localChatMap[remoteChatEntity.chatId]?.chat

                    // Используем lastUpdated для определения необходимости обновления
                    val localLastUpdated = existingLocalChat?.lastUpdated ?: 0L
                    val remoteLastUpdated =
                        remoteChatEntity.lastUpdated // Предполагаем, что оно не null в LocalEntityChat

                    if (existingLocalChat == null || localLastUpdated < remoteLastUpdated) {
                        chatDao.insertChat(remoteChatEntity) // Вставляем/обновляем чат
                    }
                }

                // Удаление чатов, которые больше не существуют в Firebase
                val chatIdsToDelete =
                    localChatMap.keys.toSet() - (remoteChatMap?.keys?.toSet() ?: emptySet()).toSet()
                chatIdsToDelete.forEach { chatId ->
                    chatDao.deleteChat(chatId) // Удаляем чат из локальной БД
                }

                // 2. Синхронизация участников и пользователей для каждого чата
                val syncJobs = remoteChatsEntities?.map { remoteChatEntity ->
                    async {
                        val existingParticipants =
                            chatDao.getParticipantsIdsForChat(remoteChatEntity.chatId)
                                .map { it }.toSet()
                        val firebaseParticipantIds =
                            firebaseManager.getChatParticipantIds(remoteChatEntity.chatId) // Получаем участников из Firebase

                        // Добавляем новых участников
                        (firebaseParticipantIds - existingParticipants).forEach { newParticipantId ->
                            chatDao.insertChatParticipant(
                                ChatParticipant(
                                    remoteChatEntity.chatId,
                                    newParticipantId
                                )
                            )
                            // Также синхронизируем данные самого участника (пользователя)
                            val remoteUserEntity =
                                firebaseManager.getUserFromFirestore(newParticipantId)
                            if (remoteUserEntity != null) {
                                val localUserEntity = userDao.getUserById(newParticipantId)
                                    .firstOrNull() // Получаем текущего локального пользователя
                                val localUserLastUpdated = localUserEntity?.lastUpdated ?: 0L
                                val remoteUserLastUpdated =
                                    remoteUserEntity.lastUpdated // remoteUserEntity - это LocalEntityUser, lastUpdated non-nullable

                                if (localUserEntity == null || localUserLastUpdated < remoteUserLastUpdated) {
                                    userDao.insertUser(remoteUserEntity)
                                }
                            }
                        }

                        // Удаляем отсутствующих участников
                        (existingParticipants - firebaseParticipantIds).forEach { removedParticipantId ->
                            chatDao.deleteChatParticipant(
                                remoteChatEntity.chatId,
                                removedParticipantId
                            )
                        }
                    }
                }
                syncJobs?.forEach { it.await() } // Ждем завершения всех задач синхронизации

                // 3. Возвращаем данные из локальной БД, маппим их в Domain Models
                chatDao.getChatsForUser(userId).firstOrNull()?.map { chatWithParticipants ->
                    val participants = chatWithParticipants.participants.map { it.toDomainUser() }
                    chatWithParticipants.chat.toDomainChat(participants)
                } ?: emptyList()
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getChatById(chatId: String): Flow<Chat?> {
        return combine(
            chatDao.getChatWithParticipants(chatId),
            firebaseManager.getChatById(chatId)
        ) { localChatWithParticipants, remoteChatEntity ->
            coroutineScope {
                if (remoteChatEntity != null) {
                    val existingChat = localChatWithParticipants?.chat
                    val localLastUpdated = existingChat?.lastUpdated ?: 0L
                    val remoteLastUpdated = remoteChatEntity.lastUpdated

                    if (existingChat == null || localLastUpdated < remoteLastUpdated) {
                        chatDao.insertChat(remoteChatEntity)
                    }

                    // ИСХОДНАЯ СТРОКА С ОШИБКОЙ: chatDao.getParticipantsForChat(remoteChatEntity.chatId) - здесь тоже поправим
                    val existingParticipantIds =
                        chatDao.getParticipantsIdsForChat(remoteChatEntity.chatId).toSet()
                    val remoteParticipantIds =
                        firebaseManager.getChatParticipantIds(remoteChatEntity.chatId)

                    (remoteParticipantIds - existingParticipantIds).forEach { newParticipantId ->
                        async {
                            chatDao.insertChatParticipant(ChatParticipant(chatId, newParticipantId))
                            val remoteUserEntity =
                                firebaseManager.getUserFromFirestore(newParticipantId)
                            if (remoteUserEntity != null) {
                                val localUserEntity =
                                    userDao.getUserById(newParticipantId).firstOrNull()
                                val localUserLastUpdated = localUserEntity?.lastUpdated ?: 0L
                                val remoteUserLastUpdated = remoteUserEntity.lastUpdated
                                if (localUserEntity == null || localUserLastUpdated < remoteUserLastUpdated) {
                                    userDao.insertUser(remoteUserEntity)
                                }
                            }
                        }.await()
                    }
                    (existingParticipantIds - remoteParticipantIds.toSet()).forEach { removedParticipantId ->
                        chatDao.deleteChatParticipant(chatId, removedParticipantId)
                    }
                }
                chatDao.getChatWithParticipants(chatId).firstOrNull()?.let { chatWithParticipants ->
                    val participants = chatWithParticipants.participants.map { it.toDomainUser() }
                    chatWithParticipants.chat.toDomainChat(participants)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateChat(chat: Chat): Result<Unit> {
        return try {
            val localChat = chat.toLocalEntityChat().copy(lastUpdated = System.currentTimeMillis())
            chatDao.updateChat(localChat)
            val participantIds =
                chatDao.getParticipantsIdsForChat(chat.id) // Используем новый метод
            firebaseManager.updateChat(localChat.toFirebaseDTO(participantIds))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            chatDao.deleteChat(chatId)
            firebaseManager.deleteChat(chatId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun addParticipantToChat(chatId: String, userId: String): Result<Unit> {
        return try {
            chatDao.insertChatParticipant(ChatParticipant(chatId, userId))
            val chat = chatDao.getChatById(chatId).firstOrNull()
            val participantIds = chatDao.getParticipantsIdsForChat(chatId)
            val updatedChat = chat?.copy(lastUpdated = System.currentTimeMillis())
            updatedChat?.let { firebaseManager.updateChat(it.toFirebaseDTO(participantIds)) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun removeParticipantFromChat(chatId: String, userId: String): Result<Unit> {
        return try {
            chatDao.deleteChatParticipant(chatId, userId)
            val chat = chatDao.getChatById(chatId).firstOrNull()
            val participantIds = chatDao.getParticipantsIdsForChat(chatId)
            val updatedChat = chat?.copy(lastUpdated = System.currentTimeMillis())
            updatedChat?.let { firebaseManager.updateChat(it.toFirebaseDTO(participantIds)) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}