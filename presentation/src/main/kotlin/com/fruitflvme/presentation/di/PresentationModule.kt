package com.fruitflvme.presentation.di

import com.fruitflvme.presentation.auth.AuthViewModel
import com.fruitflvme.presentation.chats.detail.ChatDetailViewModel
import com.fruitflvme.presentation.chats.list.ChatsListViewModel
import com.fruitflvme.presentation.profile.ProfileViewModel
import com.fruitflvme.presentation.settings.SettingsViewModel
import com.fruitflvme.presentation.splash.SplashViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object PresentationModule {
    val module = module {
        viewModel { SplashViewModel(get()) }
        viewModel { (chatId: String) ->
            ChatDetailViewModel()
        }
        viewModel { ChatsListViewModel() }
        viewModel { ProfileViewModel(get(), get()) }
        viewModel { SettingsViewModel() }
        viewModel { AuthViewModel(get(), get(), get()) }
    }
}