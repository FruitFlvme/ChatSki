package com.fruitflvme.data.remote.dtos

data class FirebaseMessageDTO(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val textContent: String? = null,
    val timestamp: Long = 0,
    val status: String = "pending", // "sent", "delivered", "read", "pending"
    val contentType: String = "text",
    val mediaUrl: String? = null,
    val lastUpdated: Long = 0L
)