package com.fruitflvme.data.remote.dtos

data class FirebaseChatDTO(
    val chatId: String = "",
    val chatType: String = "",
    val chatName: String? = null,
    val chatAvatarUrl: String? = null,
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Long? = null,
    val participants: List<String> = emptyList(),
    val lastUpdated: Long = 0L
)
