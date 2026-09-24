package com.example.vallego.domain.repository

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
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

    fun observeIncidents(): Flow<List<OrderIncident>>
    suspend fun refreshIncidents()

    fun observeCampusMetrics(): Flow<CampusMetrics>
    suspend fun getCampusDetailedMetrics(period: com.example.vallego.domain.model.MetricsPeriod): Result<com.example.vallego.domain.model.CampusDetailedMetrics>
}