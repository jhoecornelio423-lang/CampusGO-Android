package com.example.campusgo.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Reinicio detectado ($action). Levantando CampusGoPushService...")
            try {
                CampusGoPushService.start(context)
                PushWatchdogReceiver.scheduleNextWatchdog(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error iniciando CampusGoPushService tras reboot", e)
            }
        }
    }
}
