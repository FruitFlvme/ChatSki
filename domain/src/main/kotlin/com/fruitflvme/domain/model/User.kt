package com.fruitflvme.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String?,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val fcmToken: String?
)
