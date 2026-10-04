package com.example.campusgo.core.notification

import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AppNotificationPayload {
    data class OrderChat(val subOrderId: String) : AppNotificationPayload
    data class SupportChat(val ticketId: String) : AppNotificationPayload
    data class Warning(val profileId: String? = null) : AppNotificationPayload
    data class AdminIncident(val incidentId: String) : AppNotificationPayload
    data class OrderTracking(val orderId: String) : AppNotificationPayload
}

object AppNotificationRouter {
    private const val TAG = "AppNotificationRouter"

    private val _pendingRoute = MutableStateFlow<AppNotificationPayload?>(null)
    val pendingRoute: StateFlow<AppNotificationPayload?> = _pendingRoute.asStateFlow()

    fun onNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val route = extractRoute(intent)
        if (route != null) {
            Log.i(TAG, "Notificación detectada para redirección inmediata: $route")
            _pendingRoute.value = route
        }
    }

    fun extractRoute(intent: Intent): AppNotificationPayload? {
        val subOrderId = intent.getStringExtra("sub_order_id")
        if (!subOrderId.isNullOrBlank()) {
            return AppNotificationPayload.OrderChat(subOrderId)
        }

        val ticketId = intent.getStringExtra("support_ticket_id")
        if (!ticketId.isNullOrBlank()) {
            return AppNotificationPayload.SupportChat(ticketId)
        }

        val incidentId = intent.getStringExtra("incident_id")
        if (!incidentId.isNullOrBlank()) {
            return AppNotificationPayload.AdminIncident(incidentId)
        }

        val isWarning = intent.getBooleanExtra("show_warnings", false) ||
                intent.hasExtra("warning_id") ||
                intent.hasExtra("is_warning")
        if (isWarning) {
            val profileId = intent.getStringExtra("profile_id")
            return AppNotificationPayload.Warning(profileId)
        }

        val orderId = intent.getStringExtra("order_id")
        if (!orderId.isNullOrBlank()) {
            return AppNotificationPayload.OrderTracking(orderId)
        }

        return null
    }

    fun clearRoute() {
        Log.i(TAG, "Ruta de notificación consumida y limpiada")
        _pendingRoute.value = null
    }
}
