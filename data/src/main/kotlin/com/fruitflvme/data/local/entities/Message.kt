package com.fruitflvme.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Chat::class,
            parentColumns = ["chat_id"],
            childColumns = ["chat_id"],
            onDelete = androidx.room.ForeignKey.CASCADE // Удалить сообщения при удалении чата
        ),
        androidx.room.ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["sender_id"],
            onDelete = androidx.room.ForeignKey.CASCADE // Удалить сообщения, если удален отправитель (будьте осторожны с этим)
        )
    ]
)
data class Message(
    @PrimaryKey @ColumnInfo(name = "message_id") val messageId: String, // ID сообщения
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "text_content") val textContent: String?, // Текст сообщения
    @ColumnInfo(name = "timestamp") val timestamp: Long, // Время отправки
    @ColumnInfo(name = "status") val status: String, // "sent", "delivered", "read", "pending"
    @ColumnInfo(name = "content_type") val contentType: String, // "text", "image", "video", "file"
    @ColumnInfo(name = "media_url") val mediaUrl: String?, // URL медиафайла, если есть
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = 0L
)