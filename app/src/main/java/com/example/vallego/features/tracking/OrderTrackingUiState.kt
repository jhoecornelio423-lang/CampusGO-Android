package com.example.vallego.features.tracking

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.SubOrder

enum class TrackingTab {
    EN_CURSO,
    HISTORIAL
}

data class OrderTrackingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTab: TrackingTab = TrackingTab.EN_CURSO,
    val orders: List<Order> = emptyList(),
    val isCancelling: Boolean = false,
    val orderToCancel: Order? = null,
    val reviewedOrders: Map<String, Int> = emptyMap(),
    val subOrderToRate: SubOrder? = null,
    val isSubmittingReview: Boolean = false
) {
    val activeOrders: List<Order>
        get() = orders.filter {
            it.status == OrderStatus.PENDIENTE ||
            it.status == OrderStatus.EN_PROCESO ||
            it.status == OrderStatus.PARCIALMENTE_ACEPTADA
        }

    val pastOrders: List<Order>
        get() = orders.filter {
            it.status == OrderStatus.COMPLETADA ||
            it.status == OrderStatus.CANCELADA
        }
}