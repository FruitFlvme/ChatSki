package com.fruitflvme.presentation.chats.list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatsListViewModel : ViewModel() {
    private val _chats = MutableStateFlow(listOf("General", "Random", "Support"))
    val chats: StateFlow<List<String>> = _chats.asStateFlow()

    fun onChatClicked(chatName: String, onNavigate: (String) -> Unit) {
        onNavigate(chatName)
    }
}