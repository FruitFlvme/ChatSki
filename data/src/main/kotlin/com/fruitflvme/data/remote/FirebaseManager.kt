package com.fruitflvme.data.remote

import android.net.Uri
import android.util.Log
import com.fruitflvme.core.utils.Result
import com.fruitflvme.data.local.entities.Message
import com.fruitflvme.data.remote.dtos.FirebaseChatDTO
import com.fruitflvme.data.remote.dtos.FirebaseMessageDTO
import com.fruitflvme.data.remote.dtos.FirebaseUserDTO
import com.fruitflvme.data.remote.mappers.toFirebaseDTO
import com.fruitflvme.data.remote.mappers.toLocalChat
import com.fruitflvme.data.remote.mappers.toLocalMessage
import com.fruitflvme.data.remote.mappers.toLocalUser
import com.fruitflvme.domain.model.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.fruitflvme.data.local.entities.Chat as LocalChat
import com.fruitflvme.data.local.entities.User as LocalUser

class FirebaseManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {

    // --- Authentication ---
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    private fun updateFcmTokenForCurrentUser() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w("FirebaseManager", "Current user is null, cannot update FCM token.")
            return
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            firestore.collection("users").document(currentUserId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FirebaseManager", "FCM token updated in Firestore for user $currentUserId.")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseManager", "Error updating FCM token in Firestore for user $currentUserId: ${e.message}", e)
                }
        }.addOnFailureListener { e ->
            Log.e("FirebaseManager", "Error getting FCM token: ${e.message}", e)
        }
    }

    suspend fun registerUser(
        email: String,
        password: String,
        username: String
    ): Result<LocalUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val newUserDTO = FirebaseUserDTO(
                    userId = firebaseUser.uid,
                    username = username,
                    email = email,
                    avatarUrl = null,
                    status = "online",
                    fcmToken = null,
                    lastUpdated = System.currentTimeMillis()
                )
                firestore.collection("users").document(firebaseUser.uid).set(newUserDTO).await()
                updateFcmTokenForCurrentUser()
                Result.Success(newUserDTO.toLocalUser())
            } else {
                Result.Failure(Exception("Firebase user is null after registration."))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<LocalUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val userData = userDoc.toObject(FirebaseUserDTO::class.java)
            if (userData != null) {
                updateFcmTokenForCurrentUser()
                Result.Success(userData.toLocalUser())
            } else {
                Result.Failure(Exception("User data not found in Firestore"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    fun logoutUser() {
        auth.signOut()
    }

    // --- User Data ---

    suspend fun getAllUsersFromFirestore(): List<LocalUser> {
        return try {
            val querySnapshot = firestore.collection("users").get().await()
            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseUserDTO::class.java)?.toLocalUser()
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching all users: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUserFromFirestore(userId: String): LocalUser? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            doc.toObject(FirebaseUserDTO::class.java)?.toLocalUser()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUserData(user: LocalUser) {
        firestore.collection("users").document(user.userId).set(user.toFirebaseDTO()).await()
    }

    suspend fun updateUserFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
            Log.d("FirebaseManager", "FCM token updated for user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating FCM token for user $userId: ${e.message}", e)
            Result.Failure(e)
        }
    }

    /**
     * Добавляет ID контакта к списку контактов пользователя в Firestore.
     * Использует FieldValue.arrayUnion для атомарного добавления элемента в массив.
     * @param userId ID текущего пользователя.
     * @param contactId ID пользователя, которого нужно добавить в контакты.
     * @return Result.Success(Unit) при успехе, Result.Failure(Exception) при ошибке.
     */
    suspend fun addContactToRemoteUser(userId: String, contactId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("contacts", FieldValue.arrayUnion(contactId)) // Атомарное добавление
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun getUserContactsFromFirestore(userId: String): List<FirebaseUserDTO> {
        return try {
            val querySnapshot =
                firestore.collection("users").document(userId).collection("contacts")
                    .get().await()
            querySnapshot.documents.mapNotNull { it.toObject(FirebaseUserDTO::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching contacts for user $userId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getUsersByIdsFromFirestore(userIds: List<String>): List<FirebaseUserDTO> {
        if (userIds.isEmpty()) return emptyList()
        return try {
            val querySnapshot = firestore
                .collection("users")
                .whereIn("userId", userIds)
                .get().await()
            querySnapshot.documents.mapNotNull { it.toObject(FirebaseUserDTO::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching users by IDs: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Удаляет ID контакта из списка контактов пользователя в Firestore.
     * Использует FieldValue.arrayRemove для атомарного удаления элемента из массива.
     * @param userId ID текущего пользователя.
     * @param contactId ID пользователя, которого нужно удалить из контактов.
     * @return Result.Success(Unit) при успехе, Result.Failure(Exception) при ошибке.
     */
    suspend fun removeContactFromRemoteUser(userId: String, contactId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("contacts", FieldValue.arrayRemove(contactId)) // Атомарное удаление
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    /**
     * Получает список ID контактов для заданного пользователя из Firestore.
     * @param userId ID пользователя.
     * @return Список ID контактов. Возвращает пустой список в случае отсутствия поля или ошибки.
     */
    suspend fun getRemoteUserContactIds(userId: String): List<String> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            // Пытаемся безопасно привести поле "contacts" к List<String>
            val contacts = document.get("contacts") as? List<String> ?: emptyList()
            contacts
        } catch (e: Exception) {
            emptyList() // В случае ошибки или отсутствия документа/поля
        }
    }


    // --- Chat Data ---
    suspend fun createChat(chat: LocalChat, participantIds: List<String>): Result<Unit> {
        return try {
            val allParticipants = if (chat.chatType == "private") {
                participantIds + (auth.currentUser?.uid ?: "")
            } else {
                participantIds
            }.distinct()

            val firebaseChat = chat.toFirebaseDTO(allParticipants)
            firestore.collection("chats").document(firebaseChat.chatId).set(firebaseChat).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }


    fun getChatsForUser(userId: String): Flow<List<LocalChat>?> = callbackFlow {
        val chatsCollection = firestore.collection("chats")
        val listenerRegistration = chatsCollection
            .whereArrayContains(
                "participants",
                userId
            ) // Предполагаем, что это работает для всех типов чатов
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Если произошла ошибка, закрываем Flow с этой ошибкой
                    close(e)
                    return@addSnapshotListener
                }

                // snapshot гарантированно не null здесь, если e == null
                val remoteChats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseChatDTO::class.java)?.toLocalChat()
                }
                trySend(remoteChats) // Всегда отправляем список, даже если он пуст
            }

        // Этот блок выполняется, когда Flow отменяется или завершается
        awaitClose {
            listenerRegistration.remove() // Отписываемся от слушателя Firebase
        }
    }.flowOn(Dispatchers.IO) // Выполняем операции Firestore на IO диспетчере

    fun getChatById(chatId: String): Flow<LocalChat?> = callbackFlow {
        val chatDocument = firestore.collection("chats").document(chatId)
        val listenerRegistration = chatDocument
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val chatEntity = snapshot?.toObject(FirebaseChatDTO::class.java)?.toLocalChat()
                trySend(chatEntity)
            }
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun getChatParticipantIds(chatId: String): List<String> {
        return try {
            val document = firestore.collection("chats").document(chatId).get().await()
            // Маппим документ в FirebaseChatDTO и извлекаем список участников
            document.toObject(FirebaseChatDTO::class.java)?.participants ?: emptyList()
        } catch (e: Exception) {
            // В случае ошибки (например, чат не найден), возвращаем пустой список
            emptyList()
        }
    }

    suspend fun createChat(chat: Chat, participantIds: List<String>): Result<Unit> {
        return try {
            val firebaseChatDTO = chat.toFirebaseDTO(participantIds)
            firestore.collection("chats").document(chat.id).set(firebaseChatDTO).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun updateChat(chatDTO: FirebaseChatDTO): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatDTO.chatId).set(chatDTO, SetOptions.merge())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // --- Message Data ---
    suspend fun sendMessage(messageDTO: FirebaseMessageDTO): Result<Unit> {
        return try {
            firestore.collection("chats").document(messageDTO.chatId)
                .collection("messages").document(messageDTO.messageId)
                .set(messageDTO).await()

            firestore.collection("chats").document(messageDTO.chatId)
                .update(
                    mapOf(
                        "lastMessageText" to messageDTO.textContent,
                        "lastMessageTimestamp" to messageDTO.timestamp,
                        "lastUpdated" to messageDTO.lastUpdated
                    )
                ).await()

            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                messageDTO.textContent?.let {
                    sendNotificationTrigger(
                        chatId = messageDTO.chatId,
                        messageSenderId = currentUserId,
                        messageText = it
                    )
                }
            } else {
                Log.w("FirebaseManager", "Current user is null, cannot send notification trigger.")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private suspend fun sendNotificationTrigger(
        chatId: String,
        messageSenderId: String,
        messageText: String
    ) {
        try {
            val notificationData = mapOf(
                "chatId" to chatId,
                "senderId" to messageSenderId,
                "messageText" to messageText,
                "timestamp" to FieldValue.serverTimestamp() // Используем серверное время
            )
            // Создаем новый документ в коллекции 'notification_triggers'
            // Cloud Function будет слушать эту коллекцию
            firestore.collection("notification_triggers").add(notificationData).await()
            Log.d("FirebaseManager", "Notification trigger sent for chat $chatId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error sending notification trigger: ${e.message}", e)
        }
    }

    /**
     * Обновляет существующее сообщение в Firebase.
     * Использует SetOptions.merge() для обновления только указанных полей.
     * @param messageDTO Объект FirebaseMessageDTO с обновленными данными.
     */
    suspend fun updateMessage(messageDTO: FirebaseMessageDTO): Result<Unit> {
        return try {
            // Путь к сообщению: chats/{chatId}/messages/{messageId}
            firestore.collection("chats").document(messageDTO.chatId)
                .collection("messages").document(messageDTO.messageId)
                .set(messageDTO, SetOptions.merge())
                .await() // Merge обновляет только измененные поля
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    /**
     * Удаляет сообщение из Firebase.
     * @param messageId ID сообщения, которое нужно удалить.
     * @param chatId ID чата, к которому принадлежит сообщение (для построения пути).
     */
    suspend fun deleteMessage(messageId: String, chatId: String): Result<Unit> {
        return try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages").document(messageId)
                .delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>?> = callbackFlow {
        val messagesCollection =
            firestore.collection("chats").document(chatId).collection("messages")
        val listenerRegistration = messagesCollection
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val remoteMessages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseMessageDTO::class.java)?.toLocalMessage()
                }
                trySend(remoteMessages)
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)

    // --- File Storage ---
    suspend fun uploadFile(uri: Uri, path: String): Result<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}
