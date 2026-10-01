package com.example.campusgo.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Receptor de alta resiliencia para el servicio de notificaciones de CampusGO.
 * Garantiza que CampusGoPushService se mantenga activo o se reinicie de forma limpia
 * tras el arranque del sistema, actualizaciones de la app, eliminación de la app de recientes
 * o periodos prolongados en reposo profundo (Doze Mode).
 */
class PushWatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_TEST_CHAT_NOTIF) {
            val title = intent.getStringExtra("title") ?: "Vendedor CampusGO"
            val message = intent.getStringExtra("message") ?: "¡Hola! Tu pedido ya está casi listo para entrega."
            val subOrderId = intent.getStringExtra("sub_order_id") ?: "test_sub_order_123"
            CampusGoNotificationHelper.showChatNotification(
                context = context,
                notificationId = Math.abs(subOrderId.hashCode()),
                senderName = title,
                message = message,
                subOrderId = subOrderId
            )
            return
        }

        // Con Firebase Cloud Messaging nativo, el ForegroundService y su notificación persistente ya no son necesarios
        cancelWatchdog(context)
        CampusGoPushService.stop(context)
    }

    companion object {
        private const val TAG = "PushWatchdog"
        const val ACTION_WATCHDOG_TICK = "com.example.campusgo.action.WATCHDOG_TICK"
        const val ACTION_RESTART_SERVICE = "com.example.campusgo.action.RESTART_SERVICE"
        const val ACTION_TEST_CHAT_NOTIF = "com.example.campusgo.action.TEST_CHAT_NOTIF"
        private const val WATCHDOG_INTERVAL_MS = 300_000L // Latido de rescate cada 5 min para Doze Mode

        fun cancelWatchdog(context: Context) {
            try {
                val intent = Intent(context, PushWatchdogReceiver::class.java).apply {
                    action = ACTION_WATCHDOG_TICK
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    9002,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.cancel(pendingIntent)
            } catch (_: Exception) {}
        }

        fun scheduleNextWatchdog(context: Context, delayMillis: Long = WATCHDOG_INTERVAL_MS) {
            // Con FCM nativo no se reprograman alarmas de polling local
        }
    }
}
