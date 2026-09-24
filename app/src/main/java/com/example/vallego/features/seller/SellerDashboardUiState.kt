package com.example.vallego.features.seller

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus

import java.time.LocalDate

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
    val sellerProfile: com.example.vallego.domain.model.UserProfile? = null,
    val availableMeetingPoints: List<CampusMeetingPoint> = emptyList(),
    val subOrders: List<SubOrder> = emptyList(),
    val todayOrders: List<SubOrder> = emptyList(),
    val pastDayGroups: List<DailyOrderGroup> = emptyList(),
    val expandedPastDates: Set<LocalDate> = emptySet(),
    val products: List<com.example.vallego.domain.model.Product> = emptyList(),
    val categories: List<com.example.vallego.domain.model.Category> = emptyList(),
    val showAddProductDialog: Boolean = false,
    val selectedProductForEdit: com.example.vallego.domain.model.Product? = null,
    val isSavingProduct: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isUploadingAsset: Boolean = false,
    val selectedProductForStockEdit: com.example.vallego.domain.model.Product? = null,
    val selectedFilter: SellerOrderFilter = SellerOrderFilter.TODOS,
    val selectedSubOrderForRejection: SubOrder? = null,
    val selectedSubOrderForDelivery: SubOrder? = null,
    val selectedSubOrderForNoShow: SubOrder? = null,
    val selectedSubOrderForDetail: SubOrder? = null,
    val totalSubOrdersToday: Int = 0,
    val pendingCount: Int = 0,
    val inPreparationCount: Int = 0,
    val readyCount: Int = 0,
    val completedCount: Int = 0,
    val earningsToday: Double = 0.0,
    val statsData: com.example.vallego.domain.model.SellerDashboardStats? = null,
    val isLoadingStats: Boolean = false,
    val statsTimeRange: String = "all",
    val warnings: List<com.example.vallego.domain.model.ProfileWarning> = emptyList()
) {
    val filteredTodayOrders: List<SubOrder>
        get() = when (selectedFilter) {
            SellerOrderFilter.TODOS -> todayOrders
            SellerOrderFilter.PENDIENTES -> subOrders.filter { it.status == SubOrderStatus.PENDIENTE }
            SellerOrderFilter.EN_PREPARACION -> subOrders.filter { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION }
            SellerOrderFilter.LISTOS -> subOrders.filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
            SellerOrderFilter.COMPLETADOS -> todayOrders.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
            SellerOrderFilter.RECHAZADOS -> todayOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }
        }

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