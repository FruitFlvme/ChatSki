package com.fruitflvme.presentation.auth

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data object Logout : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}