package com.fruitflvme.domain.model

data class MessageWithSender(
    val message: Message,
    val sender: User
)
