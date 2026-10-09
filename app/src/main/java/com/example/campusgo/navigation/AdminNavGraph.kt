package com.example.campusgo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.campusgo.core.notification.AppNotificationPayload
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.admin.AdminHomeScreen

@Composable
fun AdminNavGraph(
    profile: UserProfile,
    onSignOut: () -> Unit,
    pendingRoute: AppNotificationPayload? = null,
    onClearPendingRoute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AdminHomeScreen(
        profile = profile,
        onSignOut = onSignOut,
        pendingRoute = pendingRoute,
        onClearPendingRoute = onClearPendingRoute,
        modifier = modifier.fillMaxSize()
    )
}
