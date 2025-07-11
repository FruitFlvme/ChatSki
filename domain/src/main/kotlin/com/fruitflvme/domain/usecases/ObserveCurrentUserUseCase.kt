package com.fruitflvme.domain.usecases

import com.fruitflvme.domain.model.User
import com.fruitflvme.domain.repositories.IUserRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentUserUseCase(
    private val userRepository: IUserRepository
) {
    operator fun invoke(): Flow<User?> {
        return userRepository.observeCurrentUserAuthStatus()
    }
}