package com.fruitflvme.chatski

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.fruitflvme.core.utils.Constants
import com.fruitflvme.presentation.navigation.MainScreen
import com.fruitflvme.theme.ChatSkiTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Разрешение на уведомления получено!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Разрешение на уведомления отклонено.", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запрашиваем разрешение на уведомления при запуске приложения (только для Android 13+)
        // Лучше делать это здесь или в начале жизненного цикла приложения.
        askNotificationPermission()

        setContent {
            ChatSkiTheme {
                // Используем rememberSaveable для сохранения состояния при пересоздании Activity
                var initialChatIdFromNotification by rememberSaveable { mutableStateOf<String?>(null) }
                var initialSenderIdFromNotification by rememberSaveable {
                    mutableStateOf<String?>(
                        null
                    )
                }

                // LaunchedEffect для обработки Intent, когда Activity только создается
                LaunchedEffect(Unit) {
                    handleIntent(intent) { chatId, senderId ->
                        initialChatIdFromNotification = chatId
                        initialSenderIdFromNotification = senderId
                    }
                }

                // Передаем данные из Intent в MainScreen
                MainScreen(
                    initialChatIdFromNotification = initialChatIdFromNotification,
                    initialSenderIdFromNotification = initialSenderIdFromNotification
                )
            }
        }
    }

    // Этот метод вызывается, если Activity уже запущена, и приходит новый Intent (например, от уведомления)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent) { chatId, senderId ->
            // Здесь, если MainScreen уже отображается, нужно как-то уведомить его
            // Например, обновить StateFlow в ViewModel, которую слушает MainScreen.
            // Для этого примера, мы пока просто логируем:
            Log.d("MainActivity", "onNewIntent: ChatId: $chatId, SenderId: $senderId")
            // В реальном приложении вы, возможно, захотите перенаправить пользователя
            // к нужному чату сразу, используя навигацию NavHost.
        }
    }

    // Вспомогательная функция для обработки Intent'а
    private fun handleIntent(
        intent: Intent?,
        onIntentHandled: (chatId: String?, senderId: String?) -> Unit
    ) {
        intent?.let {
            if (it.action == Constants.ACTION_OPEN_CHAT_FROM_NOTIFICATION) {
                val chatId = it.getStringExtra(Constants.EXTRA_CHAT_ID)
                val senderId = it.getStringExtra(Constants.EXTRA_SENDER_ID)
                Log.d("MainActivity", "Intent обработан: ChatId: $chatId, SenderId: $senderId")
                onIntentHandled(chatId, senderId)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("Permission", "Разрешение на уведомления уже есть")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(
                    this,
                    "Для получения уведомлений необходимо разрешение.",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}