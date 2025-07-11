package com.fruitflvme.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String, // Идентификатор пользователя из Firebase Auth
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String?, // Может быть null, если не используется email-аутентификация
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?, // URL аватара пользователя
    @ColumnInfo(name = "status") val status: String? = "offline", // Онлайн/оффлайн
    @ColumnInfo(name = "fcm_token") val fcmToken: String?, // Токен для Firebase Cloud Messaging
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)

// Дополнительная сущность для хранения контактов (многие ко многим)
// Можно реализовать как отдельную таблицу связи или как список ID в User,
// но для Room лучше использовать отдельную таблицу для отношений многие ко многим.
@Entity(
    tableName = "user_contacts",
    primaryKeys = ["user_id", "contact_id"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
        androidx.room.ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["contact_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class UserContact(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "contact_id") val contactId: String
)