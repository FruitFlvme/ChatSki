package com.fruitflvme.presentation.chats.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatsListScreen(
    viewModel: ChatsListViewModel = koinViewModel(),
    onChatClick: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState()

    LazyColumn {
        items(chats) { chat ->
            Column {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onChatClicked(chat, onChatClick) },
                    leadingContent = {
                        ChatAvatar(chat)
                    },
                    overlineContent = {
                        Text(chat)
                    },
                    headlineContent = {
                        Text(chat)
                    }
                )
                HorizontalDivider(
                    modifier = Modifier
                        .padding(start = 70.dp)
                )
            }
        }
    }
}

@Composable
fun ChatAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val firstLetter = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = firstLetter,
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}