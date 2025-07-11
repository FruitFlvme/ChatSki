package com.fruitflvme.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruitflvme.domain.usecases.ObserveCurrentUserUseCase
import com.fruitflvme.presentation.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SplashViewModel(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            observeCurrentUserUseCase()
                .firstOrNull()
                .let { user ->
                    _startDestination.value =
                        if (user != null) Screen.ChatsList.route else Screen.Auth.route
                }
        }
    }
}
