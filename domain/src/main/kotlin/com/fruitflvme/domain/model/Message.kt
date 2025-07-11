package com.fruitflvme.domain.model

data class Message(
    val messageId: String, // Firebase ID
    val chatId: String,
    val sender: User, // Ссылка на объект отправителя
    val content: String?,
    val timestamp: Long,
    val status: MessageStatus, // Enum MessageStatus
    val contentType: ContentType,
    val mediaUrl: String?
)
