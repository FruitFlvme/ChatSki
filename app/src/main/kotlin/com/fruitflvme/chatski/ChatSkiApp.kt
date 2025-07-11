package com.fruitflvme.chatski

import android.app.Application
import com.fruitflvme.chatski.di.initKoin

class ChatSkiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}