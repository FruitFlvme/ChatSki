package com.fruitflvme.data.di

import com.fruitflvme.data.remote.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.Module
import org.koin.dsl.module

object FirebaseModule {
    val module = module {
        single { FirebaseAuth.getInstance() }
        single { FirebaseFirestore.getInstance() }
        single { FirebaseStorage.getInstance() }
        single {
            FirebaseManager(
                auth = get(),
                firestore = get(),
                storage = get()
            )
        }
    }
}