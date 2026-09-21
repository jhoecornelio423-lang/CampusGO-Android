package com.example.vallego.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vallego.MainActivity
import com.example.vallego.R

object ValleGoNotificationHelper {

    const val CHANNEL_ORDERS = "vallego_orders_channel"
    const val CHANNEL_CHAT = "vallego_chat_channel"
    const val CHANNEL_SERVICE = "vallego_service_channel"
    const val SERVICE_NOTIFICATION_ID = 9001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para notificaciones inmediatas de pedidos (Alta prioridad, sonido y vibración)
            val orderChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Pedidos y Actualizaciones Campus Go",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos pedidos, cambios de estado y entregas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Canal para mensajes de chat (Alta prioridad tipo WhatsApp)
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT,
                "Mensajes de Chat Campus Go",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mensajes de chat entre compradores y vendedores"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Canal para el servicio en segundo plano (Baja prioridad, silencioso)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Servicio en Segundo Plano Campus Go",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la escucha de pedidos en tiempo real"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(orderChannel)
            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun showOrderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        orderId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (orderId != null) {
                putExtra("order_id", orderId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("ValleGoNotification", "Permiso de notificaciones denegado", e)
        }
    }

    fun showChatNotification(
        context: Context,
        notificationId: Int,
        senderName: String,
        message: String,
        subOrderId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (subOrderId != null) {
                putExtra("sub_order_id", subOrderId)
            }
        }

        val resolvedNotifId = if (!subOrderId.isNullOrBlank()) Math.abs(subOrderId.hashCode()) else Math.abs(notificationId)
        val tag = if (!subOrderId.isNullOrBlank()) "chat_$subOrderId" else null

        val pendingIntent = PendingIntent.getActivity(
            context,
            resolvedNotifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message).setSummaryText("Nuevo mensaje"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            if (tag != null) {
                NotificationManagerCompat.from(context).notify(tag, resolvedNotifId, notification)
            } else {
                NotificationManagerCompat.from(context).notify(resolvedNotifId, notification)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("ValleGoNotification", "Permiso de notificaciones denegado", e)
        }
    }

    /**
     * Cancela y descarta las notificaciones de chat de la barra de estado de Android
     * cuando el usuario abre la conversación.
     */
    fun cancelChatNotifications(context: Context, subOrderId: String? = null) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (!subOrderId.isNullOrBlank()) {
                val absId = Math.abs(subOrderId.hashCode())
                val rawId = subOrderId.hashCode()
                notificationManager.cancel(absId)
                notificationManager.cancel(rawId)
                notificationManager.cancel("chat_$subOrderId", absId)
                notificationManager.cancel("chat_$subOrderId", rawId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeList = notificationManager.activeNotifications ?: emptyArray()
                for (statusBarNotif in activeList) {
                    if (statusBarNotif.id == SERVICE_NOTIFICATION_ID) continue

                    val tag = statusBarNotif.tag
                    val id = statusBarNotif.id

                    val isChatChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        statusBarNotif.notification.channelId == CHANNEL_CHAT
                    } else false

                    val matchesSubOrder = !subOrderId.isNullOrBlank() && (
                        tag == "chat_$subOrderId" ||
                        id == Math.abs(subOrderId.hashCode()) ||
                        id == subOrderId.hashCode()
                    )

                    // Al abrir el chat, descartar de la barra de estado cualquier notificación de chat activa
                    if (isChatChannel || matchesSubOrder) {
                        notificationManager.cancel(tag, id)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ValleGoNotification", "Error cancelando notificaciones de chat", e)
        }
    }

    fun getForegroundServiceNotification(
        context: Context,
        title: String = "Campus Go Activo",
        content: String = "Escuchando actualizaciones de pedidos en campus"
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
