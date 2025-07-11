@file:OptIn(ExperimentalMaterial3Api::class)

package com.fruitflvme.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen(
    initialChatIdFromNotification: String?,
    initialSenderIdFromNotification: String?
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBarVisible = when (currentRoute) {
        Screen.ChatDetail.route, Screen.Auth.route -> false
        else -> true
    }

    Scaffold(
        topBar = {
            if (isBarVisible)
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Screen.ChatDetail.route -> "Чат"
                                Screen.ChatsList.route -> "Список чатов"
                                Screen.Settings.route -> "Настройки"
                                Screen.Profile.route -> "Профиль"
                                else -> "Чатски"
                            }
                        )
                    },
                    navigationIcon = {
                        val canNavigateBack =
                            currentRoute == Screen.ChatDetail.route

                        if (canNavigateBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад"
                                )
                            }
                        }
                    }
                )
        },
        bottomBar = {
            if (isBarVisible)
                BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        AppNavGraph(
            initialChatIdFromNotification = initialChatIdFromNotification,
            initialSenderIdFromNotification = initialSenderIdFromNotification,
            navController = navController,
            paddingValues = paddingValues
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Chats", Screen.ChatsList.route, Icons.Default.Star),
        BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings),
        BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person)
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.ChatsList.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val title: String, val route: String, val icon: ImageVector)

