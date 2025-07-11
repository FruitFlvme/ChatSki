package com.fruitflvme.domain.usecases

import com.fruitflvme.core.utils.Result
import com.fruitflvme.domain.model.User
import com.fruitflvme.domain.repositories.IUserRepository

class SignInUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return userRepository.loginUser(email, password)
    }
}