package com.fruitflvme.domain.model

data class Chat(
    val id: String,
    val type: ChatType, // Enum ChatType
    val name: String?,
    val avatarUrl: String?,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val participants: List<User> // Включает объекты User, а не только их ID
)
