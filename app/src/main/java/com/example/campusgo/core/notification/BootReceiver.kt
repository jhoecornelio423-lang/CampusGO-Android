package com.example.campusgo.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Con Firebase Cloud Messaging nativo, las notificaciones se entregan por GMS sin levantar servicios locales
        PushWatchdogReceiver.cancelWatchdog(context)
        CampusGoPushService.stop(context)
    }
}
