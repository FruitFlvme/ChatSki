package com.fruitflvme.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fruitflvme.data.local.entities.User
import com.fruitflvme.data.local.entities.UserContact
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(usersToAdd: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Update
    suspend fun updateUsers(usersToUpdate: List<User>)

    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserById(userId: String): Flow<User?> // Flow для отслеживания изменений в реальном времени

    @Query("SELECT * FROM users WHERE user_id IN (:userIds)")
    fun getUsersByIds(userIds: List<String>): Flow<List<User>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>> // Для отображения списка всех зарегистрированных пользователей

    @Query("DELETE FROM users WHERE user_id = :userId")
    suspend fun deleteUser(userId: String)

    @Delete
    suspend fun deleteUsers(usersToDelete: List<User>)

    // --- Операции с UserContact (для контактов) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserContact(contact: UserContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserContacts(userContacts: List<UserContact>)

    @Query("DELETE FROM user_contacts WHERE user_id = :userId AND contact_id = :contactId")
    suspend fun deleteUserContact(userId: String, contactId: String)

    @Delete
    suspend fun deleteUserContacts(userContacts: List<UserContact>)

    @Query("DELETE FROM user_contacts WHERE user_id = :userId AND contact_id IN (:contactIds)")
    suspend fun deleteUserContactsByIds(userId: String, contactIds: List<String>)

    @Query("DELETE FROM user_contacts WHERE user_id = :userId")
    suspend fun deleteAllContactsForUser(userId: String)

    // POJO для получения пользователя со списком его контактов
    data class UserWithContacts(
        @Embedded val user: User,
        @Relation(
            parentColumn = "user_id",
            entityColumn = "user_id",
            associateBy = Junction( // This is crucial for many-to-many relationships
                value = UserContact::class, // The join table
                parentColumn = "user_id",    // Column in UserContact that refers to the parent (User)
                entityColumn = "contact_id"  // Column in UserContact that refers to the child (contact User)
            )
        )
        val contacts: List<User> // The contacts are also User objects
    )

    @Transaction // Транзакция нужна для операций с @Relation
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithContacts(userId: String): Flow<UserWithContacts?>

}