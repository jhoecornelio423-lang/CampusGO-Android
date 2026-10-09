package com.example.campusgo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.campusgo.core.notification.AppNotificationPayload
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.seller.SellerDashboardScreen

@Composable
fun SellerNavGraph(
    profile: UserProfile,
    onSignOut: () -> Unit,
    pendingSubOrderId: String? = null,
    onClearPendingSubOrder: () -> Unit = {},
    pendingRoute: AppNotificationPayload? = null,
    onClearPendingRoute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SellerDashboardScreen(
        profile = profile,
        onSignOut = onSignOut,
        pendingSubOrderId = pendingSubOrderId,
        onClearPendingSubOrder = onClearPendingSubOrder,
        pendingRoute = pendingRoute,
        onClearPendingRoute = onClearPendingRoute,
        modifier = modifier.fillMaxSize()
    )
}
