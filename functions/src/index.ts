import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
// Если вы используете функции v2, импорт Firestore обычно выглядит так:
import {onDocumentCreated} from "firebase-functions/v2/firestore";
// Для типов контекста в v2, если они нужны:
// import { DocumentSnapshot, EventContext } from 'firebase-functions/v2/firestore';

// Инициализация Firebase Admin SDK.
admin.initializeApp();

// Получаем ссылки на базы данных и мессенджер после инициализации admin SDK
const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Cloud Function для отправки push-уведомлений при создании нового документа в коллекции 'notification_triggers'.
 * Использует синтаксис Firebase Functions v2.
 */
// Изменяем вызов функции на новый синтаксис v2
export const sendNotificationOnNewTrigger = onDocumentCreated("notification_triggers/{triggerId}", async (event) => {
  // В v2, snapshot и context теперь находятся внутри объекта 'event'.
  const snapshot = event.data; // snapshot.data() из v1 теперь просто event.data
  // Если вам нужен ID документа, его можно получить из ref:
  const triggerId = event.params.triggerId; // Извлекаем triggerId из параметров события

  // Если snapshot пустой (например, документ был удален слишком быстро), выйти
  if (!snapshot) {
    functions.logger.warn("No data found in snapshot for trigger:", triggerId);
    return null;
  }

  const triggerData = snapshot.data();

  functions.logger.log("New notification trigger created:", triggerId, triggerData);

  // Проверяем наличие необходимых данных
  if (!triggerData || !triggerData.chatId || !triggerData.senderId || !triggerData.messageText) {
    functions.logger.warn("Missing required fields in notification trigger:", triggerData);
    // В v2 доступ к документу для удаления через snapshot.ref
    await snapshot.ref.delete();
    return null;
  }

  const {chatId, senderId, messageText} = triggerData;
  const receiverIds = triggerData.receiverIds;

  const targetReceiverIds: string[] = Array.isArray(receiverIds) ? receiverIds : (receiverIds ? [receiverIds] : []);

  if (targetReceiverIds.length === 0) {
    functions.logger.warn("No receiver IDs specified for notification trigger:", triggerId);
    await snapshot.ref.delete();
    return null;
  }

  try {
    // 1. Получаем FCM токены получателей
    const fcmTokens: string[] = [];
    const usersSnapshot = await db.collection("users")
      .where(admin.firestore.FieldPath.documentId(), "in", targetReceiverIds)
      .get();

    usersSnapshot.forEach((doc) => {
      const userData = doc.data();
      if (userData && userData.fcmToken && doc.id !== senderId) {
        fcmTokens.push(userData.fcmToken);
      }
    });

    if (fcmTokens.length === 0) {
      functions.logger.info("No FCM tokens found for receivers or sender is only receiver:", targetReceiverIds);
      await snapshot.ref.delete();
      return null;
    }

    // 2. Определяем имя отправителя
    let senderName = "Unknown User";
    const senderDoc = await db.collection("users").doc(senderId).get();
    if (senderDoc.exists) {
      senderName = senderDoc.data()?.name || "Unknown User";
    }

    // 3. Формируем сообщение FCM
    // Тип payload теперь явно MulticastMessage
    const payload: admin.messaging.MulticastMessage = {
      notification: {
        title: `${senderName} sent a message`,
        body: messageText,
      },
      // Android-специфичные настройки, включая channelId
      android: {
        notification: {
          channelId: "chat_messages_channel", // channelId теперь здесь
        },
        priority: "high",
      },
      apns: {
        headers: {
          "apns-priority": "10",
        },
      },
      data: {
        chatId: chatId,
        senderId: senderId,
        senderName: senderName,
        messageText: messageText,
      },
      tokens: fcmTokens, // tokens обязателен для MulticastMessage
    };

    // 4. Отправляем уведомление
    const response = await messaging.sendEachForMulticast(payload);
    functions.logger.log("Successfully sent message:", response);

    // Обработка неудачных токенов
    if (response.failureCount > 0) {
      const failedTokens: string[] = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          failedTokens.push(fcmTokens[idx]);
          // Изменено с resp.exception?.message на resp.error?.message
          functions.logger.error(`Failed to send to token ${fcmTokens[idx]}: ${resp.error?.message}`);
          // Здесь можно добавить логику для удаления устаревших токенов из Firestore
        }
      });
      functions.logger.warn("List of failed tokens:", failedTokens);
    }

    // 5. Удаляем триггер из Firestore после успешной обработки
    await snapshot.ref.delete();
    functions.logger.log("Notification trigger deleted:", triggerId);

    return {success: true, message: "Notifications sent and trigger deleted."};
  } catch (error) {
    functions.logger.error("Error sending notification or processing trigger:", error);
    await snapshot.ref.delete();
    return {success: false, error: error instanceof Error ? error.message : String(error)};
  }
});
