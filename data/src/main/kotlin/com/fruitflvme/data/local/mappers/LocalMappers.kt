package com.fruitflvme.data.local.mappers

import com.fruitflvme.data.local.daos.UserDao
import com.fruitflvme.domain.model.ChatType
import com.fruitflvme.domain.model.ContentType
import com.fruitflvme.domain.model.MessageStatus
import com.fruitflvme.data.local.daos.MessageDao.MessageWithSender as LocalMessageWithSender
import com.fruitflvme.data.local.entities.Chat as LocalChat
import com.fruitflvme.data.local.entities.Message as LocalMessage
import com.fruitflvme.data.local.entities.User as LocalUser
import com.fruitflvme.domain.model.Chat as DomainChat
import com.fruitflvme.domain.model.Message as DomainMessage
import com.fruitflvme.domain.model.MessageWithSender as DomainMessageWithSender
import com.fruitflvme.domain.model.User as DomainUser

// --- User Mappers ---

fun LocalUser.toDomainUser(): DomainUser {
    return DomainUser(
        id = userId,
        name = username,
        email = email,
        avatarUrl = avatarUrl,
        isOnline = status == "online",
        fcmToken = fcmToken
    )
}

fun DomainUser.toLocalyUser(): LocalUser {
    return LocalUser(
        userId = id,
        username = name,
        email = email,
        avatarUrl = avatarUrl,
        status = if (isOnline) "online" else "offline",
        fcmToken = fcmToken,
        lastUpdated = System.currentTimeMillis()
    )
}

// Маппер для Room-связи UserWithContacts
fun UserDao.UserWithContacts.toDomainUsersList(): List<DomainUser> {
    return contacts.map { it.toDomainUser() }
}

// --- Chat Mappers ---

fun LocalChat.toDomainChat(participants: List<DomainUser>): DomainChat {
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

fun DomainChat.toLocalEntityChat(): LocalChat {
    return LocalChat(
        chatId = id,
        chatType = type.name.lowercase(),
        chatName = name,
        chatAvatarUrl = avatarUrl,
        lastMessageText = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        lastUpdated = System.currentTimeMillis()
    )
}

// --- Message Mappers ---

fun LocalMessage.toDomainMessage(sender: DomainUser): DomainMessage {
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

fun DomainMessage.toLocalMessage(): LocalMessage {
    return LocalMessage(
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

fun LocalMessageWithSender.toDomainMessageWithSender(): DomainMessageWithSender =
    DomainMessageWithSender(
        message = message.toDomainMessage(sender.toDomainUser()),
        sender = sender.toDomainUser()
    )

fun DomainMessageWithSender.toLocalMessageWithSender(): LocalMessageWithSender =
    LocalMessageWithSender(
        message = message.toLocalMessage(),
        sender = sender.toLocalyUser()
    )
