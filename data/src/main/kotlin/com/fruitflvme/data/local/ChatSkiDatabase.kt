package com.fruitflvme.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fruitflvme.data.local.daos.ChatDao
import com.fruitflvme.data.local.daos.MessageDao
import com.fruitflvme.data.local.daos.UserDao
import com.fruitflvme.data.local.entities.Chat
import com.fruitflvme.data.local.entities.ChatParticipant
import com.fruitflvme.data.local.entities.Message
import com.fruitflvme.data.local.entities.User
import com.fruitflvme.data.local.entities.UserContact

@Database(
    entities = [
        User::class,
        UserContact::class,
        Chat::class,
        ChatParticipant::class,
        Message::class
    ],
    version = 1, // Версия базы данных. При изменениях схемы увеличивайте её.
    exportSchema = false // Отключаем экспорт схемы в файл. В реальном проекте, особенно при миграциях, стоит включить.
)

// Если у вас есть кастомные преобразователи типов (например, для List<String>, Date и т.д.),
// их нужно указать здесь:
//@TypeConverters(YourCustomConverters::class)
abstract class ChatSkiDatabase : RoomDatabase() {

    // Абстрактные методы для получения экземпляров наших DAO
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        // Singleton предотвращает создание нескольких экземпляров базы данных
        // и потенциальные проблемы с конкурентным доступом.
        @Volatile // Гарантирует, что изменения видимы для всех потоков
        private var INSTANCE: ChatSkiDatabase? = null

        fun getDatabase(context: Context): ChatSkiDatabase { // Изменил имя на ChatSkiDatabase, как класс
            // Если экземпляр уже существует, возвращаем его
            return INSTANCE
                ?: synchronized(this) { // Защита от создания нескольких экземпляров в разных потоках
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ChatSkiDatabase::class.java, // Имя вашего класса базы данных
                        "chatski_database" // Имя файла базы данных на устройстве
                    )
                        // .addMigrations(MIGRATION_X_Y) // Здесь добавляются миграции при обновлении версии БД
                        .fallbackToDestructiveMigration(false) // Осторожно: удаляет и воссоздает БД при ненайденной миграции
                        .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}