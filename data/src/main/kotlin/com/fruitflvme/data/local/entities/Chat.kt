package com.fruitflvme.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey @ColumnInfo(name = "chat_id") val chatId: String, // Идентификатор чата
    @ColumnInfo(name = "chat_type") val chatType: String, // "private" или "group"
    @ColumnInfo(name = "chat_name") val chatName: String?, // Название для групповых чатов
    @ColumnInfo(name = "chat_avatar_url") val chatAvatarUrl: String?, // URL аватара чата для групповых
    @ColumnInfo(name = "last_message_text") val lastMessageText: String?, // Текст последнего сообщения
    @ColumnInfo(name = "last_message_timestamp") val lastMessageTimestamp: Long?, // Время последнего сообщения
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = 0L
)

// Сущность для связи Chat и User (кто участвует в чате)
// Отношение многие ко многим между Chat и User
@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chat_id", "user_id"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Chat::class,
            parentColumns = ["chat_id"],
            childColumns = ["chat_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
        androidx.room.ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class ChatParticipant(
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "user_id") val userId: String
)