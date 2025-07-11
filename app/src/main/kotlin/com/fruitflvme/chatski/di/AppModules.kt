package com.fruitflvme.chatski.di

import com.fruitflvme.data.di.FirebaseModule
import com.fruitflvme.data.di.LocalModule
import com.fruitflvme.data.di.RepositoryModule
import com.fruitflvme.domain.di.DomainModule
import com.fruitflvme.presentation.di.PresentationModule

val appModules = listOf(
    PresentationModule.module,
    LocalModule.module,
    FirebaseModule.module,
    DomainModule.module,
    RepositoryModule.module
)