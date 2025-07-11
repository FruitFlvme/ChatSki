package com.fruitflvme.domain.repositories

import com.fruitflvme.core.utils.Result
import com.fruitflvme.domain.model.User
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    // Authentication
    fun observeCurrentUserAuthStatus(): Flow<User?>
    suspend fun registerUser(
        email: String,
        password: String,
        username: String
    ): Result<User>

    suspend fun loginUser(email: String, password: String): Result<User>
    suspend fun logoutUser(userId: String)

    // User Data (local & remote sync)
    fun getAllUsers(): Flow<List<User>>
    suspend fun saveUserLocally(user: User)
    suspend fun deleteUserLocally(userId: String)
    suspend fun updateRemoteUser(user: User): Result<Unit>
    fun getUserById(userId: String): Flow<User?>

    // Contacts
    fun getUserContacts(userId: String): Flow<List<User>>
    suspend fun addContact(currentUserId: String, contactUserId: String): Result<Unit>
    suspend fun removeContact(currentUserId: String, contactUserId: String): Result<Unit>
}