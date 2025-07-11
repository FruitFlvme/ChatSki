package com.fruitflvme.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fruitflvme.presentation.auth.AuthUiState
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val logoutState by viewModel.logoutState.collectAsState()
    val state = viewModel.uiState

    LaunchedEffect(logoutState) {
        if (logoutState is AuthUiState.Logout) {
            onLogout()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp, 16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = state.profile
        )
        Button(
            onClick = {
                viewModel.logout()
            }
        ) {
            Text("Выйти")
        }
    }
}