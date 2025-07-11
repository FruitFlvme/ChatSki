package com.fruitflvme.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruitflvme.domain.usecases.LogoutUseCase
import com.fruitflvme.domain.usecases.ObserveCurrentUserUseCase
import com.fruitflvme.presentation.auth.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState("Welcome to profile!"))
    val uiState: ProfileUiState get() = _uiState.value
    private val _logoutState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val logoutState: StateFlow<AuthUiState> = _logoutState

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = AuthUiState.Loading
            try {
                observeCurrentUserUseCase.invoke().firstOrNull()?.id?.let { logoutUseCase(it) }
                _logoutState.value = AuthUiState.Logout
            } catch (e: Exception) {
                _logoutState.value = AuthUiState.Error(e.message ?: "Logout failed")
            }
        }
    }
}