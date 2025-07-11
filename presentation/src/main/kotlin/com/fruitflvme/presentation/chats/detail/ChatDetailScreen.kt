@file:OptIn(ExperimentalMaterial3Api::class)

package com.fruitflvme.presentation.chats.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(chatId) })
) {

}
