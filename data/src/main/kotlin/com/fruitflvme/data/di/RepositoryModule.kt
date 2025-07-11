package com.fruitflvme.data.di

import com.fruitflvme.data.repositories.ChatRepositoryImpl
import com.fruitflvme.data.repositories.MessageRepositoryImpl
import com.fruitflvme.data.repositories.UserRepositoryImpl
import com.fruitflvme.domain.repositories.IChatRepository
import com.fruitflvme.domain.repositories.IMessageRepository
import com.fruitflvme.domain.repositories.IUserRepository
import org.koin.dsl.module

object RepositoryModule {
    val module = module {
        single<IUserRepository> { UserRepositoryImpl(get(), get()) }
        single<IChatRepository> { ChatRepositoryImpl(get(), get(), get()) }
        single<IMessageRepository> { MessageRepositoryImpl(get(), get(), get()) }
    }
}