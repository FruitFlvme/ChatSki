package com.fruitflvme.presentation.chats.detail

import com.fruitflvme.domain.model.Message

data class ChatDetailUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
