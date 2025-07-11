package com.fruitflvme.data.repositories

import android.util.Log
import com.fruitflvme.core.utils.Result
import com.fruitflvme.data.local.daos.UserDao
import com.fruitflvme.data.local.entities.UserContact
import com.fruitflvme.data.local.mappers.toDomainUser
import com.fruitflvme.data.local.mappers.toLocalyUser
import com.fruitflvme.data.remote.FirebaseManager
import com.fruitflvme.data.remote.mappers.toLocalUser
import com.fruitflvme.domain.model.User
import com.fruitflvme.domain.repositories.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val firebaseManager: FirebaseManager
) : IUserRepository {

// --- Authentication ---

    override fun observeCurrentUserAuthStatus(): Flow<User?> {
        return firebaseManager.currentUser.map { firebaseUser ->
            if (firebaseUser != null) {
                val remoteUserEntity =
                    firebaseManager.getUserFromFirestore(firebaseUser.uid)
                if (remoteUserEntity != null) {
                    withContext(Dispatchers.IO) {
                        userDao.insertUser(remoteUserEntity) // Сохраняем/обновляем локально
                    }
                    remoteUserEntity.toDomainUser()
                } else {
                    null
                }
            } else {
                null
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun registerUser(
        email: String,
        password: String,
        username: String
    ): Result<User> {
        return when (val firebaseResult = firebaseManager.registerUser(email, password, username)) {
            is Result.Success -> {
                val localEntityUser =
                    firebaseResult.value // firebaseResult.value - это LocalEntityUser
                withContext(Dispatchers.IO) {
                    userDao.insertUser(localEntityUser) // Сохраняем локально после регистрации
                }
                Result.Success(localEntityUser.toDomainUser())
            }

            is Result.Failure ->
                Result.Failure(firebaseResult.exception)
        }
    }


    override suspend fun loginUser(email: String, password: String): Result<User> {
        return when (val firebaseResult = firebaseManager.loginUser(email, password)) {
            is Result.Success -> {
                val localEntityUser =
                    firebaseResult.value // firebaseResult.value - это LocalEntityUser
                withContext(Dispatchers.IO) {
                    userDao.insertUser(localEntityUser) // Сохраняем локально после входа
                }
                Result.Success(localEntityUser.toDomainUser())
            }

            is Result.Failure ->
                Result.Failure(firebaseResult.exception)
        }
    }

    override suspend fun logoutUser(userId: String) {
        firebaseManager.logoutUser()
        withContext(Dispatchers.IO) {
            deleteUserLocally(userId)
        }
    }


    // --- User Data (local & remote sync) ---

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().onEach { localUsers ->
            supervisorScope {
                launch(Dispatchers.IO) {
                    try {
                        // Fetch the latest users from Firebase
                        val firebaseUsers = firebaseManager.getAllUsersFromFirestore()
                        Log.d("UserSync", "Fetched ${firebaseUsers.size} users from Firebase.")

                        // Convert Firebase users to local entities for comparison and insertion
                        val newLocalUserEntities = firebaseUsers

                        // Determine changes (adds, updates, deletes)
                        val usersToAdd = newLocalUserEntities.filter { fbUser ->
                            localUsers.none { localUser -> localUser.userId == fbUser.userId }
                        }

                        val usersToUpdate = newLocalUserEntities.filter { fbUser ->
                            localUsers.any { localUser ->
                                localUser.userId == fbUser.userId && (localUser.username != fbUser.username || localUser.email != fbUser.email || localUser.lastUpdated == fbUser.lastUpdated)
                            }
                        }

                        val usersToDelete = localUsers.filter { localUser ->
                            newLocalUserEntities.none { fbUser -> fbUser.userId == localUser.userId }
                        }

                        if (usersToAdd.isNotEmpty()) {
                            userDao.insertUsers(usersToAdd)
                            Log.d("UserSync", "Added ${usersToAdd.size} users to local DB.")
                        }
                        if (usersToUpdate.isNotEmpty()) {
                            userDao.updateUsers(usersToUpdate)
                        }
                        if (usersToDelete.isNotEmpty()) {
                            userDao.deleteUsers(usersToDelete)
                        }

                    } catch (e: Exception) {
                        Log.e("UserSync", "Error syncing all users from Firebase: ${e.message}", e)
                    }
                }
            }
        }.map { localUsers ->
            localUsers.map { it.toDomainUser() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getUserById(userId: String): Flow<User?> {
        return userDao.getUserById(userId)
            .onEach { localUserEntity ->
                supervisorScope {
                    launch(Dispatchers.IO) {
                        val remoteUserEntity =
                            firebaseManager.getUserFromFirestore(userId)
                        if (remoteUserEntity != null) {
                            if (localUserEntity == null || localUserEntity.lastUpdated < remoteUserEntity.lastUpdated) {
                                userDao.insertUser(remoteUserEntity)
                            }
                        } else {
                            if (localUserEntity != null) {
                                userDao.deleteUser(userId)
                            }
                        }
                    }
                }
            }
            .map { it?.toDomainUser() }
            .flowOn(Dispatchers.IO)
    }


    override suspend fun saveUserLocally(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insertUser(user.toLocalyUser())
        }
    }

    override suspend fun deleteUserLocally(userId: String) {
        withContext(Dispatchers.IO) {
            userDao.deleteUser(userId)
        }
    }

    override suspend fun updateRemoteUser(user: User): Result<Unit> {
        return try {
            firebaseManager.updateUserData(user.toLocalyUser())
            withContext(Dispatchers.IO) {
                userDao.insertUser(
                    user.toLocalyUser().copy(lastUpdated = System.currentTimeMillis())
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // --- Contacts ---

    override fun getUserContacts(userId: String): Flow<List<User>> {
        return userDao.getUserWithContacts(userId).onEach { userWithContacts ->
            userWithContacts?.let { localData ->
                supervisorScope {
                    launch(Dispatchers.IO) {
                        try {
                            Log.d("ContactSync", "Starting contact sync for user: $userId")

                            val firebaseContactData =
                                firebaseManager.getUserContactsFromFirestore(userId)
                            Log.d(
                                "ContactSync",
                                "Fetched ${firebaseContactData.size} contacts from Firebase."
                            )

                            // First, process the actual user data for the contacts:
                            val firebaseContactUsers =
                                firebaseContactData.map { it.toLocalUser() }
                            val existingLocalUsers = userDao.getAllUsers()
                                .first() // Get current users (blocking for one shot)

                            val contactUsersToAdd = firebaseContactUsers.filter { fbUser ->
                                existingLocalUsers.none { localUser -> localUser.userId == fbUser.userId }
                            }
                            val contactUsersToUpdate = firebaseContactUsers.filter { fbUser ->
                                existingLocalUsers.any { localUser ->
                                    localUser.userId == fbUser.userId && (
                                            localUser.username != fbUser.username ||
                                                    localUser.email != fbUser.email ||
                                                    localUser.avatarUrl != fbUser.avatarUrl ||
                                                    localUser.status != fbUser.status ||
                                                    localUser.fcmToken != fbUser.fcmToken ||
                                                    localUser.lastUpdated < fbUser.lastUpdated // Use lastUpdated for conflict resolution
                                            )
                                }
                            }

                            if (contactUsersToAdd.isNotEmpty()) {
                                userDao.insertUsers(contactUsersToAdd)
                                Log.d(
                                    "ContactSync",
                                    "Added ${contactUsersToAdd.size} contact users to local DB."
                                )
                            }
                            if (contactUsersToUpdate.isNotEmpty()) {
                                userDao.updateUsers(contactUsersToUpdate)
                                Log.d(
                                    "ContactSync",
                                    "Updated ${contactUsersToUpdate.size} contact users in local DB."
                                )
                            }

                            // Now, synchronize the UserContact relationship table:
                            val localContactRelations = localData.contacts.map { localContactUser ->
                                UserContact(userId = userId, contactId = localContactUser.userId)
                            }
                            val newContactRelations = firebaseContactData.map { fbContact ->
                                UserContact(userId = userId, contactId = fbContact.userId)
                            }

                            val relationsToAdd = newContactRelations.filter { newRel ->
                                localContactRelations.none { localRel ->
                                    localRel.userId == newRel.userId && localRel.contactId == newRel.contactId
                                }
                            }

                            val relationsToDelete = localContactRelations.filter { localRel ->
                                newContactRelations.none { newRel ->
                                    newRel.userId == localRel.userId && newRel.contactId == localRel.contactId
                                }
                            }

                            if (relationsToAdd.isNotEmpty()) {
                                userDao.insertUserContacts(relationsToAdd)
                                Log.d(
                                    "ContactSync",
                                    "Added ${relationsToAdd.size} contact relationships."
                                )
                            }
                            if (relationsToDelete.isNotEmpty()) {
                                userDao.deleteUserContacts(relationsToDelete)
                                Log.d(
                                    "ContactSync",
                                    "Deleted ${relationsToDelete.size} contact relationships."
                                )
                            }

                            Log.d("ContactSync", "Contact sync for user $userId completed.")

                        } catch (e: Exception) {
                            Log.e(
                                "ContactSync",
                                "Error syncing contacts for user $userId from Firebase: ${e.message}",
                                e
                            )
                        }
                    }
                }
            } ?: Log.d(
                "ContactSync",
                "No local UserWithContacts for $userId, skipping sync for this emission."
            )
        }.map { userWithContacts ->
            userWithContacts?.contacts?.map { contact -> contact.toDomainUser() } ?: emptyList()
        }.flowOn(Dispatchers.IO)
    }


    override suspend fun addContact(currentUserId: String, contactUserId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                userDao.insertUserContact(UserContact(currentUserId, contactUserId))
            }
            firebaseManager.addContactToRemoteUser(currentUserId, contactUserId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun removeContact(currentUserId: String, contactUserId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                userDao.deleteUserContact(currentUserId, contactUserId)
            }
            firebaseManager.removeContactFromRemoteUser(currentUserId, contactUserId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}