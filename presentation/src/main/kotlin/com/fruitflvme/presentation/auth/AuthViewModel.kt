package com.fruitflvme.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruitflvme.core.utils.Result
import com.fruitflvme.domain.usecases.ObserveCurrentUserUseCase
import com.fruitflvme.domain.usecases.SignInUseCase
import com.fruitflvme.domain.usecases.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        observeAuthStatus()
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            observeCurrentUserUseCase()
                .catch { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
                }
                .collect { user ->
                    _uiState.value = if (user != null) {
                        AuthUiState.Success
                    } else {
                        AuthUiState.Idle
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password must not be empty")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            when (val result = signInUseCase(email, password)) {
                is Result.Success -> _uiState.value = AuthUiState.Success
                is Result.Failure -> _uiState.value =
                    AuthUiState.Error(result.exception.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _uiState.value = AuthUiState.Error("All fields must be filled")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            when (val result = signUpUseCase(email, password, username)) {
                is Result.Success -> _uiState.value = AuthUiState.Success
                is Result.Failure -> _uiState.value =
                    AuthUiState.Error(result.exception.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}