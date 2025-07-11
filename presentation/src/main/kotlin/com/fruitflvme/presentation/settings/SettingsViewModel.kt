package com.fruitflvme.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState("Welcome to settings!"))
    val uiState: SettingsUiState get() = _uiState.value
}