package com.fruitflvme.data.remote.mappers

import com.fruitflvme.data.remote.dtos.FirebaseChatDTO
import com.fruitflvme.data.remote.dtos.FirebaseMessageDTO
import com.fruitflvme.data.remote.dtos.FirebaseUserDTO
import com.fruitflvme.domain.model.ChatType
import com.fruitflvme.domain.model.ContentType
import com.fruitflvme.domain.model.MessageStatus
import com.fruitflvme.data.local.entities.Chat as LocalChat
import com.fruitflvme.data.local.entities.Message as LocalMessage
import com.fruitflvme.data.local.entities.User as LocalUser
import com.fruitflvme.domain.model.Chat as DomainChat
import com.fruitflvme.domain.model.Message as DomainMessage
import com.fruitflvme.domain.model.User as DomainUser

// --- User Mappers ---

fun FirebaseUserDTO.toLocalUser(): LocalUser {
    return LocalUser(
        userId = userId,
        username = username,
        email = email,
        avatarUrl = avatarUrl,
        status = status,
        fcmToken = fcmToken,
        lastUpdated = lastUpdated
    )
}

fun LocalUser.toFirebaseDTO(): FirebaseUserDTO {
    return FirebaseUserDTO(
        userId = userId,
        username = username,
        email = email,
        avatarUrl = avatarUrl,
        status = status,
        fcmToken = fcmToken,
        lastUpdated = lastUpdated
    )
}

// --- FirebaseUserDTO <-> DomainUser (если прямой маппинг нужен, например, при регистрации) ---

fun FirebaseUserDTO.toDomainUser(): DomainUser {
    return DomainUser(
        id = userId,
        name = username,
        email = email,
        avatarUrl = avatarUrl,
        isOnline = status == "online",
        fcmToken = fcmToken
    )
}

fun DomainUser.toFirebaseDTO(): FirebaseUserDTO {
    return FirebaseUserDTO(
        userId = id,
        username = name,
        email = email,
        avatarUrl = avatarUrl,
        status = if (isOnline) "online" else "offline",
        fcmToken = fcmToken,
        lastUpdated = System.currentTimeMillis()
    )
}

// --- Chat Mappers ---

fun FirebaseChatDTO.toLocalChat(): LocalChat {
    return LocalChat(
        chatId = chatId,
        chatType = chatType,
        chatName = chatName,
        chatAvatarUrl = chatAvatarUrl,
        lastMessageText = lastMessageText,
        lastMessageTimestamp = lastMessageTimestamp,
        lastUpdated = lastUpdated
    )
}

fun LocalChat.toFirebaseDTO(participants: List<String>): FirebaseChatDTO {
    return FirebaseChatDTO(
        chatId = chatId,
        chatType = chatType,
        chatName = chatName,
        chatAvatarUrl = chatAvatarUrl,
        lastMessageText = lastMessageText,
        lastMessageTimestamp = lastMessageTimestamp,
        lastUpdated = lastUpdated,
        participants = participants // Участники передаются отдельно
    )
}

fun FirebaseChatDTO.toDomainChat(participants: List<DomainUser>): DomainChat {
    return DomainChat(
        id = chatId,
        type = try {
            ChatType.valueOf(chatType.uppercase())
        } catch (e: IllegalArgumentException) {
            ChatType.PRIVATE
        },
        name = chatName,
        avatarUrl = chatAvatarUrl,
        lastMessage = lastMessageText,
        lastMessageTimestamp = lastMessageTimestamp,
        participants = participants
    )
}

fun DomainChat.toFirebaseDTO(participantIds: List<String>): FirebaseChatDTO {
    return FirebaseChatDTO(
        chatId = id,
        chatType = type.name.lowercase(),
        chatName = name,
        chatAvatarUrl = avatarUrl,
        lastMessageText = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        lastUpdated = System.currentTimeMillis(),
        participants = participantIds
    )
}

// --- Message Mappers ---

fun FirebaseMessageDTO.toLocalMessage(): LocalMessage {
    return LocalMessage(
        messageId = messageId,
        chatId = chatId,
        senderId = senderId,
        textContent = textContent,
        timestamp = timestamp,
        status = status,
        contentType = contentType,
        mediaUrl = mediaUrl,
        lastUpdated = lastUpdated
    )
}

fun LocalMessage.toFirebaseDTO(): FirebaseMessageDTO {
    return FirebaseMessageDTO(
        messageId = messageId,
        chatId = chatId,
        senderId = senderId,
        textContent = textContent,
        timestamp = timestamp,
        status = status,
        contentType = contentType,
        mediaUrl = mediaUrl,
        lastUpdated = lastUpdated
    )
}

fun FirebaseMessageDTO.toDomainMessage(sender: DomainUser): DomainMessage {
    return DomainMessage(
        messageId = messageId,
        chatId = chatId,
        sender = sender,
        content = textContent,
        timestamp = timestamp,
        status = try {
            MessageStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            MessageStatus.PENDING
        },
        contentType = try {
            ContentType.valueOf(contentType.uppercase())
        } catch (e: IllegalArgumentException) {
            ContentType.TEXT
        },
        mediaUrl = mediaUrl
    )
}

fun DomainMessage.toFirebaseDTO(): FirebaseMessageDTO {
    return FirebaseMessageDTO(
        messageId = messageId,
        chatId = chatId,
        senderId = sender.id,
        textContent = content,
        timestamp = timestamp,
        status = status.name.lowercase(),
        contentType = contentType.name.lowercase(),
        mediaUrl = mediaUrl,
        lastUpdated = System.currentTimeMillis()
    )
}

