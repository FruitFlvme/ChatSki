package com.fruitflvme.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fruitflvme.presentation.auth.AuthScreen
import com.fruitflvme.presentation.chats.detail.ChatDetailScreen
import com.fruitflvme.presentation.chats.list.ChatsListScreen
import com.fruitflvme.presentation.profile.ProfileScreen
import com.fruitflvme.presentation.settings.SettingsScreen
import com.fruitflvme.presentation.splash.SplashScreen

@Composable
fun AppNavGraph(
    initialChatIdFromNotification: String?,
    initialSenderIdFromNotification: String?,
    navController: NavHostController,
    paddingValues: PaddingValues
) {

    LaunchedEffect(initialChatIdFromNotification) {
//        if (!initialChatIdFromNotification.isNullOrEmpty()) {
//            navController.navigate("chat_detail/${initialChatIdFromNotification}") {
//                popUpTo(navController.graph.startDestinationId) { inclusive = false }
//            }
//        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.ChatsList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ChatsList.route) {
            ChatsListScreen(
                onChatClick = { chatId ->
                    navController.navigate(Screen.ChatDetail.withId(chatId))
                }
            )
        }

        composable(Screen.ChatDetail.route) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(chatId)
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
    }
}