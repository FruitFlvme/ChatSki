package com.fruitflvme.data.repositories

import android.net.Uri
import com.fruitflvme.core.utils.Result
import com.fruitflvme.data.local.daos.MessageDao
import com.fruitflvme.data.local.daos.UserDao
import com.fruitflvme.data.local.mappers.toDomainMessageWithSender
import com.fruitflvme.data.local.mappers.toDomainUser
import com.fruitflvme.data.local.mappers.toLocalMessage
import com.fruitflvme.data.remote.FirebaseManager
import com.fruitflvme.data.remote.mappers.toFirebaseDTO
import com.fruitflvme.domain.model.ContentType
import com.fruitflvme.domain.model.Message
import com.fruitflvme.domain.model.MessageStatus
import com.fruitflvme.domain.model.MessageWithSender
import com.fruitflvme.domain.repositories.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val firebaseManager: FirebaseManager
) : IMessageRepository {

    override suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        text: String
    ): Result<Unit> {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val sender = userDao.getUserById(senderId).firstOrNull()?.toDomainUser()
            ?: return Result.Failure(IllegalStateException("Sender not found locally for ID: $senderId"))

        val initialMessage = Message(
            messageId = messageId,
            chatId = chatId,
            sender = sender,
            content = text,
            timestamp = timestamp,
            status = MessageStatus.PENDING,
            contentType = ContentType.TEXT,
            mediaUrl = null
        )

        // 1. Сохраняем сообщение локально со статусом PENDING
        messageDao.insertMessage(initialMessage.toLocalMessage())

        // 2. Отправляем в Firebase
        return when (val firebaseResult =
            firebaseManager.sendMessage(initialMessage.toFirebaseDTO())) {
            is Result.Success -> {
                // 3. Если успешно, обновляем статус локально на SENT
                messageDao.updateMessage(
                    initialMessage.toLocalMessage()
                        .copy(status = MessageStatus.SENT.name.lowercase())
                )
                Result.Success(Unit)
            }

            is Result.Failure -> {
                // 4. Если ошибка, обновляем статус локально на FAILED
                messageDao.updateMessage(
                    initialMessage.toLocalMessage()
                        .copy(status = MessageStatus.FAILED.name.lowercase())
                )
                Result.Failure(firebaseResult.exception)
            }
        }
    }

    override suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUri: Uri
    ): Result<Unit> {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val storagePath = "chat_media/${chatId}/${UUID.randomUUID()}.jpg"
        val sender = userDao.getUserById(senderId).firstOrNull()?.toDomainUser()
            ?: return Result.Failure(IllegalStateException("Sender not found locally for ID: $senderId"))

        val initialMessage = Message(
            messageId = messageId,
            chatId = chatId,
            sender = sender,
            content = "Image", // Или имя файла
            timestamp = timestamp,
            status = MessageStatus.UPLOADING, // Статус для отображения загрузки
            contentType = ContentType.IMAGE,
            mediaUrl = null
        )

        messageDao.insertMessage(initialMessage.toLocalMessage()) // Сохраняем локально со статусом UPLOADING

        return when (val uploadResult = firebaseManager.uploadFile(imageUri, storagePath)) {
            is Result.Success -> {
                val imageUrl = uploadResult.value
                val messageWithMedia = initialMessage.copy(
                    mediaUrl = imageUrl,
                    status = MessageStatus.PENDING
                ) // Меняем на PENDING после загрузки

                messageDao.updateMessage(messageWithMedia.toLocalMessage()) // Обновляем локально с URL

                when (val firebaseSendResult =
                    firebaseManager.sendMessage(messageWithMedia.toFirebaseDTO())) {
                    is Result.Success -> {
                        messageDao.updateMessage(
                            messageWithMedia.toLocalMessage()
                                .copy(status = MessageStatus.SENT.name.lowercase())
                        )
                        Result.Success(Unit)
                    }

                    is Result.Failure -> {
                        messageDao.updateMessage(
                            messageWithMedia.toLocalMessage()
                                .copy(status = MessageStatus.FAILED.name.lowercase())
                        )
                        Result.Failure(firebaseSendResult.exception)
                    }
                }
            }

            is Result.Failure -> {
                messageDao.updateMessage(
                    initialMessage.toLocalMessage()
                        .copy(status = MessageStatus.FAILED.name.lowercase())
                )
                Result.Failure(uploadResult.exception)
            }
        }
    }

    override fun getMessagesForChat(chatId: String): Flow<List<MessageWithSender>> {
        return combine(
            messageDao.getMessagesForChat(chatId), // Flow<List<LocalEntityMessage>> из Room
            firebaseManager.getMessagesForChat(chatId) // Flow<List<LocalEntityMessage>> из FirebaseManager
        ) { localMessages, remoteMessages ->
            coroutineScope {
                val localMessageMap = localMessages.associateBy { it.messageId }

                // 1. Синхронизация сообщений из Firebase в локальную БД
                val syncMessageJobs = remoteMessages?.map { remoteMessageEntity ->
                    async {
                        val existingLocalMessage = localMessageMap[remoteMessageEntity.messageId]

                        val localLastUpdated = existingLocalMessage?.lastUpdated ?: 0L
                        val remoteLastUpdated = remoteMessageEntity.lastUpdated

                        // Если удаленное сообщение новее или локального нет
                        if (existingLocalMessage == null || localLastUpdated < remoteLastUpdated) {
                            messageDao.insertMessage(remoteMessageEntity)

                            // Если отправителя нет локально, синхронизируем его (если удаленный пользователь новее)
                            val remoteSenderUser =
                                firebaseManager.getUserFromFirestore(remoteMessageEntity.senderId)
                            if (remoteSenderUser != null) {
                                val localSenderUser =
                                    userDao.getUserById(remoteSenderUser.userId).firstOrNull()
                                val localSenderLastUpdated = localSenderUser?.lastUpdated ?: 0L
                                val remoteSenderLastUpdated = remoteSenderUser.lastUpdated

                                if (localSenderUser == null || localSenderLastUpdated < remoteSenderLastUpdated) {
                                    userDao.insertUser(remoteSenderUser)
                                }
                            }
                        }
                    }
                }
                syncMessageJobs?.forEach { it.await() } // Ждем завершения всех синхронизаций сообщений и отправителей

                // 2. Удаляем сообщения, которые больше не существуют в Firebase
                val remoteMessageIds = remoteMessages?.map { it.messageId }
                val messageIdsToDelete = localMessageMap.keys.toSet() - (remoteMessageIds?.toSet()
                    ?: emptySet()).toSet()
                messageIdsToDelete.forEach { messageId ->
                    messageDao.deleteMessage(messageId)
                }

                // 3. Возвращаем сообщения из локальной БД, маппим их в Domain Models
                // Это гарантирует, что мы всегда маппим самые свежие данные из Room
                messageDao.getMessagesWithSenderForChat(chatId).firstOrNull()
                    ?.map { messageWithSender ->
                        messageWithSender.toDomainMessageWithSender()
                    } ?: emptyList()
            }
        }.flowOn(Dispatchers.IO) // Выполняем combine и синхронизацию на IO диспетчере
    }

    override suspend fun changeMessageStatus(
        messageId: String,
        messageStatus: String,
        chatId: String
    ): Result<Unit> {
        return try {
            val lastUpdated = System.currentTimeMillis()
            // 1. Обновить статус в локальной БД
            messageDao.updateMessageStatus(messageId, messageStatus, lastUpdated)

            // 2. Обновить статус в Firebase
            val message = messageDao.getMessageById(messageId) // Получаем сообщение для его DTO
            if (message != null) {
                val updatedMessageDTO =
                    message.copy(status = messageStatus, lastUpdated = lastUpdated).toFirebaseDTO()
                firebaseManager.updateMessage(updatedMessageDTO)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String, chatId: String): Result<Unit> {
        return try {
            messageDao.deleteMessage(messageId)
            firebaseManager.deleteMessage(messageId, chatId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}