package com.fruitflvme.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object ChatsList : Screen("chats_list")
    data object ChatDetail : Screen("chat_detail/{chatId}") {
        fun withId(chatId: String) = "chat_detail/$chatId"
    }

    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
}