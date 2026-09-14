package com.example.vallego.data.repository

import com.example.vallego.domain.model.ApplicationStatus
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AdminRepository
import com.example.vallego.domain.repository.OrderRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class AdminRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val orderRepository: OrderRepository? = null
) : AdminRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _meetingPointsFlow = MutableStateFlow<List<CampusMeetingPoint>>(emptyList())
    private val _applicationsFlow = MutableStateFlow<List<SellerApplication>>(emptyList())
    private val _sellersFlow = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _incidentsFlow = MutableStateFlow<List<OrderIncident>>(emptyList())

    init {
        scope.launch {
            refreshAll()
        }
    }

    private suspend fun refreshAll() {
        refreshMeetingPoints()
        refreshSellerApplications()
        refreshSellers()
        refreshIncidents()
    }

    override suspend fun refreshMeetingPoints() = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val remotePoints = postgrest.from("campus_meeting_points")
                    .select()
                    .decodeList<CampusMeetingPoint>()
                if (remotePoints.isNotEmpty()) {
                    _meetingPointsFlow.value = remotePoints
                    return@withContext
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al cargar campus_meeting_points desde Supabase: ${e.message}", e)
        }

        if (_meetingPointsFlow.value.isEmpty()) {
            _meetingPointsFlow.value = listOf(
                CampusMeetingPoint(
                    id = "mp-1",
                    name = "Biblioteca Central - Puerta Principal",
                    description = "Zona de torniquetes de acceso",
                    pavilion = "Edificio Central",
                    campus = "Los Olivos",
                    isActive = true
                ),
                CampusMeetingPoint(
                    id = "mp-2",
                    name = "Pabellón A - Zona de Bancas",
                    description = "Patio central frente al cafetín",
                    pavilion = "Pabellón A",
                    campus = "Los Olivos",
                    isActive = true
                ),
                CampusMeetingPoint(
                    id = "mp-3",
                    name = "Pabellón C - Explanada",
                    description = "Área techada de mesas de estudio",
                    pavilion = "Pabellón C",
                    campus = "Los Olivos",
                    isActive = true
                ),
                CampusMeetingPoint(
                    id = "mp-4",
                    name = "Cafetería Campus - Terraza",
                    description = "Mesas al aire libre",
                    pavilion = "Pabellón D",
                    campus = "Los Olivos",
                    isActive = true
                )
            )
        }
    }

    private suspend fun refreshSellerApplications() {
        try {
            if (postgrest != null) {
                val remoteApps = postgrest.from("seller_applications")
                    .select()
                    .decodeList<SellerApplication>()
                _applicationsFlow.value = remoteApps
                return
            }
        } catch (_: Exception) {}
    }

    private suspend fun refreshSellers() {
        try {
            if (postgrest != null) {
                val profiles = postgrest.from("profiles")
                    .select {
                        filter {
                            or {
                                eq("role", "emprendedor")
                                eq("role", "suspended")
                            }
                        }
                    }
                    .decodeList<UserProfile>()
                _sellersFlow.value = profiles
            }
        } catch (_: Exception) {}
    }

    private suspend fun refreshIncidents() {
        try {
            if (postgrest != null) {
                val remoteIncidents = postgrest.from("order_incidents")
                    .select()
                    .decodeList<OrderIncident>()
                _incidentsFlow.value = remoteIncidents
            }
        } catch (_: Exception) {}
    }

    override fun observeMeetingPoints(): Flow<List<CampusMeetingPoint>> = _meetingPointsFlow.asStateFlow()

    override suspend fun createMeetingPoint(meetingPoint: CampusMeetingPoint): Result<CampusMeetingPoint> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.from("campus_meeting_points").insert(meetingPoint)
                    refreshMeetingPoints()
                    return@withContext Result.success(meetingPoint)
                } catch (_: Exception) {
                    // Fallback local
                }
            }
            val current = _meetingPointsFlow.value.toMutableList()
            current.add(meetingPoint)
            _meetingPointsFlow.value = current
            Result.success(meetingPoint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleMeetingPoint(id: String, active: Boolean): Result<CampusMeetingPoint> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.from("campus_meeting_points").update(
                        mapOf("is_active" to active)
                    ) {
                        filter { eq("id", id) }
                    }
                    refreshMeetingPoints()
                } catch (_: Exception) {}
            }

            var updated: CampusMeetingPoint? = null
            val current = _meetingPointsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index >= 0) {
                val point = current[index].copy(isActive = active)
                current[index] = point
                _meetingPointsFlow.value = current
                updated = point
            }
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(NoSuchElementException("Punto de encuentro no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSellerApplications(): Flow<List<SellerApplication>> = _applicationsFlow.asStateFlow()

    override suspend fun approveSellerApplication(applicationId: String, adminId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.rpc(
                        function = "approve_seller_application_rpc",
                        parameters = buildJsonObject {
                            put("p_application_id", applicationId)
                            if (!adminId.isNullOrBlank()) {
                                put("p_admin_id", adminId)
                            }
                        }
                    )
                    refreshSellerApplications()
                    refreshSellers()
                    return@withContext Result.success(Unit)
                } catch (_: Exception) {
                    // Fallback: direct updates
                    val targetApp = _applicationsFlow.value.find { it.id == applicationId }
                    postgrest.from("seller_applications").update(
                        mapOf("status" to "APROBADA")
                    ) {
                        filter { eq("id", applicationId) }
                    }
                    if (targetApp != null && targetApp.userId.isNotBlank()) {
                        postgrest.from("profiles").update(
                            mapOf(
                                "role" to "emprendedor",
                                "business_name" to targetApp.storeName,
                                "business_category" to targetApp.category,
                                "business_status" to "ABIERTO",
                                "accepting_orders" to true
                            )
                        ) {
                            filter { eq("id", targetApp.userId) }
                        }
                    }
                    refreshSellerApplications()
                    refreshSellers()
                    return@withContext Result.success(Unit)
                }
            }

            val current = _applicationsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == applicationId }
            if (index >= 0) {
                current[index] = current[index].copy(status = ApplicationStatus.APROBADA)
                _applicationsFlow.value = current
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Solicitud no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectSellerApplication(applicationId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.from("seller_applications").update(
                        mapOf(
                            "status" to "RECHAZADA",
                            "rejection_reason" to reason
                        )
                    ) {
                        filter { eq("id", applicationId) }
                    }
                    refreshSellerApplications()
                    return@withContext Result.success(Unit)
                } catch (_: Exception) {}
            }

            val current = _applicationsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == applicationId }
            if (index >= 0) {
                current[index] = current[index].copy(
                    status = ApplicationStatus.RECHAZADA,
                    rejectionReason = reason
                )
                _applicationsFlow.value = current
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Solicitud no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSellers(): Flow<List<UserProfile>> = _sellersFlow.asStateFlow()

    override suspend fun toggleSellerSuspension(sellerId: String, isSuspended: Boolean, reason: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.rpc(
                        function = "toggle_seller_suspension_rpc",
                        parameters = buildJsonObject {
                            put("target_seller_id", sellerId)
                            put("set_suspended", isSuspended)
                            if (isSuspended && !reason.isNullOrBlank()) {
                                put("suspension_reason_text", reason)
                            }
                        }
                    )
                    refreshSellers()
                    return@withContext Result.success(Unit)
                } catch (e: Exception) {
                    android.util.Log.e("AdminRepo", "RPC toggle_seller_suspension_rpc fallo: ${e.message}")
                }

                try {
                    val targetRole = if (isSuspended) "suspended" else "emprendedor"
                    val updatePayload = mutableMapOf<String, Any?>(
                        "role" to targetRole,
                        "business_status" to if (isSuspended) "CERRADO" else "ABIERTO",
                        "accepting_orders" to !isSuspended
                    )
                    if (isSuspended && !reason.isNullOrBlank()) {
                        updatePayload["suspension_reason"] = reason
                    } else if (!isSuspended) {
                        updatePayload["suspension_reason"] = ""
                    }

                    postgrest.from("profiles").update(updatePayload) {
                        filter { eq("id", sellerId) }
                    }
                    refreshSellers()
                    return@withContext Result.success(Unit)
                } catch (e: Exception) {
                    android.util.Log.e("AdminRepo", "Direct update profiles fallo: ${e.message}")
                }
            }

            val current = _sellersFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == sellerId }
            if (index >= 0) {
                val updatedRole = if (isSuspended) UserRole.SUSPENDED else UserRole.EMPRENDEDOR
                current[index] = current[index].copy(
                    role = updatedRole,
                    businessStatus = if (isSuspended) "CERRADO" else "ABIERTO",
                    acceptingOrders = !isSuspended,
                    suspensionReason = if (isSuspended) reason else null
                )
                _sellersFlow.value = current
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeIncidents(): Flow<List<OrderIncident>> = _incidentsFlow.asStateFlow()

    override fun observeCampusMetrics(): Flow<CampusMetrics> {
        val ordersFlow = if (orderRepository is OrderRepositoryImpl) {
            orderRepository.ordersFlow
        } else {
            MutableStateFlow(emptyList())
        }

        return combine(_meetingPointsFlow, _applicationsFlow, _sellersFlow, _incidentsFlow, ordersFlow) { points, apps, sellers, incidents, orders ->
            val activePoints = points.count { it.isActive }
            val pendingApps = apps.count { it.status == ApplicationStatus.PENDIENTE }
            val activeSellers = sellers.count { it.role == UserRole.EMPRENDEDOR }

            val completedSubOrders = orders.flatMap { it.subOrders }.filter { it.status == SubOrderStatus.COMPLETADO }
            val totalSales = completedSubOrders.sumOf { it.subtotalAmount }

            CampusMetrics(
                totalOrdersToday = orders.size,
                totalSalesToday = totalSales,
                activeSellersCount = if (activeSellers > 0) activeSellers else sellers.size,
                activeMeetingPointsCount = activePoints,
                pendingApplicationsCount = pendingApps
            )
        }
    }
}