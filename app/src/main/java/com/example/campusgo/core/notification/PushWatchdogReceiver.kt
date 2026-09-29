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
 * Garantiza que ValleGoPushService se mantenga activo o se reinicie de forma limpia
 * tras el arranque del sistema, actualizaciones de la app, eliminación de la app de recientes
 * o periodos prolongados en reposo profundo (Doze Mode).
 */
class PushWatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ACTION_WATCHDOG_TICK
        Log.d(TAG, "Broadcast recibido ($action). Asegurando estado de ValleGoPushService...")

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ValleGo:PushWatchdogWakeLock")

        try {
            wakeLock?.acquire(5000L)
            ValleGoPushService.start(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar ValleGoPushService desde watchdog", e)
        } finally {
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
            } catch (_: Exception) {}

            // Programar el siguiente latido preventivo
            scheduleNextWatchdog(context)
        }
    }

    companion object {
        private const val TAG = "PushWatchdog"
        const val ACTION_WATCHDOG_TICK = "com.example.campusgo.action.WATCHDOG_TICK"
        const val ACTION_RESTART_SERVICE = "com.example.campusgo.action.RESTART_SERVICE"
        private const val WATCHDOG_INTERVAL_MS = 300_000L // Latido de rescate cada 5 min para Doze Mode

        fun scheduleNextWatchdog(context: Context, delayMillis: Long = WATCHDOG_INTERVAL_MS) {
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
                val triggerAt = System.currentTimeMillis() + delayMillis

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager?.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error programando siguiente latido de watchdog", e)
            }
        }
    }
}
