package com.fruitflvme.domain.usecases

import com.fruitflvme.domain.repositories.IUserRepository

class LogoutUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(userId: String) {
        userRepository.logoutUser(userId)
    }
}