package com.example.campusgo.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Dispositivo reiniciado. Iniciando sincronización en segundo plano...")
        CampusGoPushService.start(context)
        PushWatchdogReceiver.scheduleNextWatchdog(context)
    }
}
