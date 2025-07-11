package com.fruitflvme.domain.di

import com.fruitflvme.domain.usecases.LogoutUseCase
import com.fruitflvme.domain.usecases.ObserveCurrentUserUseCase
import com.fruitflvme.domain.usecases.SignInUseCase
import com.fruitflvme.domain.usecases.SignUpUseCase
import org.koin.dsl.module

object DomainModule {
    val module = module {
        single { SignInUseCase(get()) }
        single { SignUpUseCase(get()) }
        single { LogoutUseCase(get()) }
        single { ObserveCurrentUserUseCase(get()) }
    }
}