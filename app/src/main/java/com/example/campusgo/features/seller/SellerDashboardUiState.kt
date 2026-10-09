package com.example.campusgo.features.seller

import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SubOrderStatus

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun parseOrderLocalDate(createdAtIso: String?, zoneId: ZoneId = ZoneId.of("America/Lima")): LocalDate {
    if (createdAtIso.isNullOrBlank()) return LocalDate.now(zoneId)
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(createdAtIso)
            if (date != null) {
                return Instant.ofEpochMilli(date.time).atZone(zoneId).toLocalDate()
            }
        } catch (_: Exception) {
            // try next
        }
    }
    return LocalDate.now(zoneId)
}

enum class SellerOrderFilter {
    TODOS,
    PENDIENTES,
    EN_PREPARACION,
    LISTOS,
    COMPLETADOS,
    RECHAZADOS
}

enum class SellerTab {
    PEDIDOS,
    PRODUCTOS,
    ESTADISTICAS,
    CHATS,
    PERFIL,
    MI_PUESTO
}

data class DailyOrderGroup(
    val date: LocalDate,
    val displayTitle: String,
    val isToday: Boolean,
    val totalEarnings: Double,
    val completedCount: Int,
    val cancelledCount: Int,
    val orders: List<SubOrder>
)

data class SellerDashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAcceptingOrders: Boolean = true,
    val selectedTab: SellerTab = SellerTab.PEDIDOS,
    val sellerProfile: com.example.campusgo.domain.model.UserProfile? = null,
    val availableMeetingPoints: List<CampusMeetingPoint> = emptyList(),
    val subOrders: List<SubOrder> = emptyList(),
    val processingSubOrderIds: Set<String> = emptySet(),
    val todayOrders: List<SubOrder> = emptyList(),
    val pastDayGroups: List<DailyOrderGroup> = emptyList(),
    val expandedPastDates: Set<LocalDate> = emptySet(),
    val products: List<com.example.campusgo.domain.model.Product> = emptyList(),
    val categories: List<com.example.campusgo.domain.model.Category> = emptyList(),
    val showAddProductDialog: Boolean = false,
    val selectedProductForEdit: com.example.campusgo.domain.model.Product? = null,
    val isSavingProduct: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isUploadingAsset: Boolean = false,
    val selectedProductForStockEdit: com.example.campusgo.domain.model.Product? = null,
    val selectedFilter: SellerOrderFilter = SellerOrderFilter.TODOS,
    val selectedSubOrderForRejection: SubOrder? = null,
    val selectedSubOrderForDelivery: SubOrder? = null,
    val selectedSubOrderForNoShow: SubOrder? = null,
    val selectedSubOrderForDetail: SubOrder? = null,
    val subOrderToRate: SubOrder? = null,
    val sellerReviewedOrders: Map<String, Int> = emptyMap(),
    val isSubmittingReview: Boolean = false,
    val totalSubOrdersToday: Int = 0,
    val pendingCount: Int = 0,
    val inPreparationCount: Int = 0,
    val readyCount: Int = 0,
    val completedCount: Int = 0,
    val earningsToday: Double = 0.0,
    val statsData: com.example.campusgo.domain.model.SellerDashboardStats? = null,
    val isLoadingStats: Boolean = false,
    val statsTimeRange: String = "all",
    val warnings: List<com.example.campusgo.domain.model.ProfileWarning> = emptyList(),
    val buyerStrikes: Map<String, Int> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(ZoneId.of("America/Lima")),
    val activeSupportTicket: com.example.campusgo.domain.model.SupportTicket? = null,
    val unreadChatCount: Int = 0
) {
    val isViewingToday: Boolean
        get() = selectedDate == LocalDate.now(ZoneId.of("America/Lima"))

    val selectedDateOrders: List<SubOrder>
        get() = if (isViewingToday) {
            todayOrders
        } else {
            subOrders.filter { parseOrderLocalDate(it.createdAt) == selectedDate }
                .sortedByDescending { it.createdAt }
        }

    val displayEarnings: Double
        get() = if (isViewingToday) {
            earningsToday
        } else {
            selectedDateOrders
                .filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
                .sumOf { it.subtotalAmount }
        }

    val displayPendingCount: Int
        get() = if (isViewingToday) pendingCount else selectedDateOrders.count { it.status == SubOrderStatus.PENDIENTE }

    val displayInPrepCount: Int
        get() = if (isViewingToday) inPreparationCount else selectedDateOrders.count { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION }

    val displayReadyCount: Int
        get() = if (isViewingToday) readyCount else selectedDateOrders.count { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }

    val displayCompletedCount: Int
        get() = if (isViewingToday) completedCount else selectedDateOrders.count { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }

    val displayRejectedCount: Int
        get() = selectedDateOrders.count { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }

    val displayTotalOrders: Int
        get() = if (isViewingToday) totalSubOrdersToday else selectedDateOrders.size

    val filteredDisplayOrders: List<SubOrder>
        get() = when (selectedFilter) {
            SellerOrderFilter.TODOS -> selectedDateOrders
            SellerOrderFilter.PENDIENTES -> if (isViewingToday) {
                subOrders.filter { it.status == SubOrderStatus.PENDIENTE }
            } else {
                selectedDateOrders.filter { it.status == SubOrderStatus.PENDIENTE }
            }
            SellerOrderFilter.EN_PREPARACION -> if (isViewingToday) {
                subOrders.filter { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION }
            } else {
                selectedDateOrders.filter { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION }
            }
            SellerOrderFilter.LISTOS -> if (isViewingToday) {
                subOrders.filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
            } else {
                selectedDateOrders.filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
            }
            SellerOrderFilter.COMPLETADOS -> selectedDateOrders.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
            SellerOrderFilter.RECHAZADOS -> selectedDateOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }
        }

    val filteredTodayOrders: List<SubOrder>
        get() = filteredDisplayOrders

    val filteredSubOrders: List<SubOrder>
        get() = when (selectedFilter) {
            SellerOrderFilter.TODOS -> subOrders
            SellerOrderFilter.PENDIENTES -> subOrders.filter { it.status == SubOrderStatus.PENDIENTE }
            SellerOrderFilter.EN_PREPARACION -> subOrders.filter { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION }
            SellerOrderFilter.LISTOS -> subOrders.filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
            SellerOrderFilter.COMPLETADOS -> subOrders.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
            SellerOrderFilter.RECHAZADOS -> subOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }
        }
}