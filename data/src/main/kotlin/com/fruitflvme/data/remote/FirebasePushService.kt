package com.fruitflvme.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fruitflvme.chatski.core.R
import com.fruitflvme.core.utils.Constants
import com.fruitflvme.core.utils.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

class FirebasePushService : FirebaseMessagingService(), KoinComponent {

    // Внедряем FirebaseManager с помощью Koin
    private val firebaseManager: FirebaseManager by inject()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Вызывается, когда FCM выдает новый токен для данного приложения/устройства.
     * Здесь мы должны обновить токен в Firestore для текущего пользователя.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            scope.launch {
                when (firebaseManager.updateUserFcmToken(currentUserId, token)) {
                    is Result.Success -> Log.d(
                        TAG,
                        "FCM token updated successfully in Firestore via Koin."
                    )

                    is Result.Failure -> Log.e(
                        TAG,
                        "Failed to update FCM token in Firestore via Koin."
                    )
                }
            }
        } else {
            Log.w(TAG, "User not logged in, FCM token not saved to Firestore immediately.")
            // Если пользователь не залогинен, токен будет сохранен при следующем логине
            // через updateFcmTokenForCurrentUser() в FirebaseManager.
        }
    }

    /**
     * Вызывается при получении уведомления.
     * Обрабатывает "data messages" всегда.
     * Обрабатывает "notification messages" только когда приложение на переднем плане.
     * Когда приложение в фоне/закрыто, "notification" часть обрабатывается системой Android.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 1. Обработка DATA-сообщений (всегда)
        if (remoteMessage.data.isNotEmpty()) {
            val chatId = remoteMessage.data["chatId"] ?: "unknown_chat"
            val senderId = remoteMessage.data["senderId"] ?: "unknown_sender"
            val senderName = remoteMessage.data["senderName"] ?: "Unknown User"
            val messageText = remoteMessage.data["messageText"] ?: "New message"

            // Build a more descriptive title/body
            val notificationTitle = "$senderName in $chatId" // Maybe get chat name later
            val notificationBody = messageText

            // Pass all relevant data to showNotification
            showNotification(notificationTitle, notificationBody, remoteMessage.data)
        }

        // 2. Обработка NOTIFICATION-сообщений
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            showNotification(
                title = notification.title ?: "Новое сообщение",
                body = notification.body ?: "Вам пришло новое сообщение.",
                data = remoteMessage.data // Передаем данные из data-payload
            )
        }
    }

    // --- Вспомогательные функции ---

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = "chat_notifications_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            getString(R.string.chat_notifications_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.chat_notifications_channel_description)
            enableLights(true)
            lightColor = getColor(android.R.color.holo_blue_light)
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        }
        notificationManager.createNotificationChannel(channel)

        // СОЗДАЕМ INTENT С ДЕЙСТВИЕМ
        val intent = Intent(Constants.ACTION_OPEN_CHAT_FROM_NOTIFICATION).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // Очищает стек и открывает Activity поверх
            // Передаем данные из data-payload в Intent
            data.forEach { (key, value) -> putExtra(key, value) }
            // Добавим специфические данные, например, ID чата и отправителя
            putExtra(Constants.EXTRA_CHAT_ID, data["chatId"])
            putExtra(Constants.EXTRA_SENDER_ID, data["senderId"])
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // Уникальный request code для каждого уведомления
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Отменяем корутины при уничтожении сервиса
    }

    companion object {
        private const val TAG = "FirebasePushService"
    }
}