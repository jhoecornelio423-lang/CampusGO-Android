package com.example.campusgo.domain.repository

import com.example.campusgo.domain.model.BuyerOrderStats
import com.example.campusgo.domain.model.CampusDetailedMetrics
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.CampusMetrics
import com.example.campusgo.domain.model.MetricsPeriod
import com.example.campusgo.domain.model.OrderIncident
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.SellerApplication
import com.example.campusgo.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    fun observeMeetingPoints(): Flow<List<CampusMeetingPoint>>
    suspend fun refreshMeetingPoints()
    suspend fun createMeetingPoint(meetingPoint: CampusMeetingPoint): Result<CampusMeetingPoint>
    suspend fun toggleMeetingPoint(id: String, active: Boolean): Result<CampusMeetingPoint>
    suspend fun deleteMeetingPoint(id: String): Result<Unit>

    fun observeSellerApplications(): Flow<List<SellerApplication>>
    suspend fun refreshSellerApplications()
    suspend fun approveSellerApplication(applicationId: String, adminId: String? = null): Result<Unit>
    suspend fun rejectSellerApplication(applicationId: String, reason: String): Result<Unit>

    fun observeSellers(): Flow<List<UserProfile>>
    suspend fun refreshSellers()
    suspend fun toggleSellerSuspension(sellerId: String, isSuspended: Boolean, reason: String? = null): Result<Unit>

    fun observeBuyers(): Flow<List<UserProfile>>
    suspend fun refreshBuyers()
    suspend fun toggleBuyerSuspension(buyerId: String, isSuspended: Boolean, reason: String? = null): Result<Unit>

    suspend fun issueWarning(profileId: String, reason: String, createdBy: String? = null): Result<Unit>
    suspend fun getProfileWarnings(profileId: String): Result<List<ProfileWarning>>
    fun observeUserStrikes(): Flow<Map<String, Int>>
    suspend fun refreshUserStrikes()
    suspend fun getBuyerOrderStats(buyerId: String): Result<BuyerOrderStats>

    fun observeIncidents(): Flow<List<OrderIncident>>
    suspend fun refreshIncidents()
    suspend fun resolveIncident(incidentId: String, status: String, action: String? = null, adminNotes: String? = null): Result<Unit>
    suspend fun getIncidentsForUser(userId: String): Result<List<OrderIncident>>

    fun observeCampusMetrics(): Flow<CampusMetrics>
    suspend fun getCampusDetailedMetrics(period: MetricsPeriod): Result<CampusDetailedMetrics>
}