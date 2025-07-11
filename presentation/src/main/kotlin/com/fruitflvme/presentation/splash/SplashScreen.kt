package com.fruitflvme.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.fruitflvme.presentation.navigation.Screen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: SplashViewModel = koinViewModel()
) {
    val startDestination by viewModel.startDestination.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(startDestination) {
        if (startDestination != null && !hasNavigated) {
            hasNavigated = true
            navController.navigate(startDestination!!) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    // Пока ждем — просто показываем индикатор
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}