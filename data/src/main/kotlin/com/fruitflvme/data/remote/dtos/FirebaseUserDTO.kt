package com.fruitflvme.data.remote.dtos

data class FirebaseUserDTO(
    val userId: String = "",
    val username: String = "",
    val email: String? = null,
    val avatarUrl: String? = null,
    val status: String? = "offline",
    val fcmToken: String? = null,
    val lastUpdated: Long = 0L
)
