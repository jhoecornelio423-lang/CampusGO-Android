package com.example.vallego.features.admin

import com.example.vallego.domain.model.CampusDetailedMetrics
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.MetricsPeriod
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.SellerDashboardStats
import com.example.vallego.domain.model.UserProfile

enum class AdminTab {
    MEETING_POINTS,
    SELLER_APPLICATIONS,
    SELLERS_DIRECTORY,
    CAMPUS_METRICS
}

data class AdminUiState(
    val selectedTab: AdminTab = AdminTab.MEETING_POINTS,
    val meetingPoints: List<CampusMeetingPoint> = emptyList(),
    val sellerApplications: List<SellerApplication> = emptyList(),
    val sellers: List<UserProfile> = emptyList(),
    val incidents: List<OrderIncident> = emptyList(),
    val metrics: CampusMetrics = CampusMetrics(),
    val selectedMetricsPeriod: MetricsPeriod = MetricsPeriod.HOY,
    val detailedMetrics: CampusDetailedMetrics = CampusDetailedMetrics(),
    val isLoadingMetrics: Boolean = false,
    val selectedSellerDetail: UserProfile? = null,
    val sellerProducts: List<Product> = emptyList(),
    val isLoadingSellerProducts: Boolean = false,
    val sellerStats: SellerDashboardStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCreateMeetingPointDialog: Boolean = false,
    val selectedApplicationForRejection: SellerApplication? = null,
    val selectedSellerForSuspension: UserProfile? = null,
    val pointToDelete: CampusMeetingPoint? = null,
    val sellerSearchQuery: String = "",
    val applicationFilter: String = "TODAS"
) {
    val filteredSellers: List<UserProfile>
        get() = if (sellerSearchQuery.isBlank()) {
            sellers
        } else {
            val query = sellerSearchQuery.trim().lowercase()
            sellers.filter {
                it.fullName.lowercase().contains(query) ||
                (it.businessName?.lowercase()?.contains(query) == true) ||
                (it.businessCategory?.lowercase()?.contains(query) == true)
            }
        }

    val filteredApplications: List<SellerApplication>
        get() = when (applicationFilter.uppercase()) {
            "PENDIENTE" -> sellerApplications.filter { it.status == com.example.vallego.domain.model.ApplicationStatus.PENDIENTE }
            "APROBADA" -> sellerApplications.filter { it.status == com.example.vallego.domain.model.ApplicationStatus.APROBADA }
            "RECHAZADA" -> sellerApplications.filter { it.status == com.example.vallego.domain.model.ApplicationStatus.RECHAZADA }
            else -> sellerApplications
        }
}