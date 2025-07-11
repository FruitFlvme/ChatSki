package com.fruitflvme.data.di

import androidx.room.Room
import com.fruitflvme.data.local.ChatSkiDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object LocalModule {
    val module = module {
        single {
            Room.databaseBuilder(
                androidApplication(),
                ChatSkiDatabase::class.java,
                "chatski_database"
            )
                .fallbackToDestructiveMigration(false)
                .build()
        }

        single { get<ChatSkiDatabase>().userDao() }
        single { get<ChatSkiDatabase>().chatDao() }
        single { get<ChatSkiDatabase>().messageDao() }
    }
}