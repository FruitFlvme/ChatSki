package com.fruitflvme.chatski.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

fun initKoin(app: Application) {
    startKoin {
        androidContext(app)
        modules(appModules)
        printLogger(Level.DEBUG)
    }
}