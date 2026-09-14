package com.example.vallego.features.admin

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.SellerApplication
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCreateMeetingPointDialog: Boolean = false,
    val selectedApplicationForRejection: SellerApplication? = null,
    val selectedSellerForSuspension: UserProfile? = null
)