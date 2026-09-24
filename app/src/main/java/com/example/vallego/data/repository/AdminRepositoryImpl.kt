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

    companion object {
        private val defaultMeetingPoints = listOf(
            CampusMeetingPoint(
                id = "mp-1",
                name = "Puerta Principal - Acceso Exterior",
                description = "Zona de torniquetes y vereda de ingreso",
                pavilion = "Acceso Exterior",
                campus = "Los Olivos",
                zoneType = "EXTERIOR",
                isActive = true
            ),
            CampusMeetingPoint(
                id = "mp-2",
                name = "Puerta 2 - Reja Auxiliar",
                description = "Frente al paradero de transporte",
                pavilion = "Acceso Exterior",
                campus = "Los Olivos",
                zoneType = "EXTERIOR",
                isActive = true
            ),
            CampusMeetingPoint(
                id = "mp-3",
                name = "Pabellón C - Explanada Central",
                description = "Área techada de mesas de estudio",
                pavilion = "Pabellón C",
                campus = "Los Olivos",
                zoneType = "INTERIOR",
                isActive = true
            ),
            CampusMeetingPoint(
                id = "mp-4",
                name = "Cafetería Campus - Terraza",
                description = "Mesas al aire libre",
                pavilion = "Pabellón D",
                campus = "Los Olivos",
                zoneType = "INTERIOR",
                isActive = true
            )
        )

        private val defaultApplications = listOf(
            SellerApplication(
                id = "app-1",
                userId = "user-carlos",
                applicantName = "Carlos Mendoza Ramos",
                phone = "987654321",
                storeName = "Postres UCV",
                category = "Repostería",
                description = "Venta de queques, brownies y pies de limón caseros para recreo.",
                proposedLocation = "Pabellón B piso 1",
                status = ApplicationStatus.PENDIENTE
            ),
            SellerApplication(
                id = "app-2",
                userId = "user-maria",
                applicantName = "María Fernanda Torres",
                phone = "912345678",
                storeName = "Sándwiches Vallejo",
                category = "Comida Rápida",
                description = "Triples, empanadas y tostadas mixtas para el desayuno.",
                proposedLocation = "Pabellón C entrada",
                status = ApplicationStatus.PENDIENTE
            )
        )

        private val defaultSellers = listOf(
            UserProfile(
                id = "seller-papu",
                fullName = "Papu Burger",
                role = UserRole.EMPRENDEDOR,
                businessName = "Papu Burger",
                businessCategory = "Comida Rápida",
                businessStatus = "ABIERTO",
                acceptingOrders = true,
                supportedMeetingPoints = listOf("mp-1", "mp-2")
            ),
            UserProfile(
                id = "seller-dulce",
                fullName = "Dulce Tentación",
                role = UserRole.EMPRENDEDOR,
                businessName = "Dulce Tentación",
                businessCategory = "Repostería",
                businessStatus = "ABIERTO",
                acceptingOrders = true,
                supportedMeetingPoints = listOf("mp-1", "mp-2", "mp-3", "mp-4")
            )
        )
    }

    private val _meetingPointsFlow = MutableStateFlow<List<CampusMeetingPoint>>(defaultMeetingPoints)
    private val _applicationsFlow = MutableStateFlow<List<SellerApplication>>(defaultApplications)
    private val _sellersFlow = MutableStateFlow<List<UserProfile>>(defaultSellers)
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
                _meetingPointsFlow.value = remotePoints
                return@withContext
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al cargar campus_meeting_points desde Supabase: ${e.message}", e)
        }

        if (_meetingPointsFlow.value.isEmpty() && postgrest == null) {
            _meetingPointsFlow.value = defaultMeetingPoints
        }
    }

    override suspend fun refreshSellerApplications() = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val remoteApps = postgrest.from("seller_applications")
                    .select()
                    .decodeList<SellerApplication>()
                _applicationsFlow.value = remoteApps
                return@withContext
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al cargar seller_applications: ${e.message}", e)
        }

        if (_applicationsFlow.value.isEmpty() && postgrest == null) {
            _applicationsFlow.value = defaultApplications
        }
    }

    override suspend fun refreshSellers() = withContext(Dispatchers.IO) {
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
                _sellersFlow.value = profiles.map { SellerPaymentMethodsStorage.enrichProfile(it) }
                return@withContext
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al cargar profiles de vendedores: ${e.message}", e)
        }

        if (_sellersFlow.value.isEmpty() && postgrest == null) {
            _sellersFlow.value = defaultSellers
        }
    }

    override suspend fun refreshIncidents(): Unit = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val remoteIncidents = postgrest.from("order_incidents")
                    .select()
                    .decodeList<OrderIncident>()
                _incidentsFlow.value = remoteIncidents
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al cargar order_incidents: ${e.message}", e)
        }
    }

    override fun observeMeetingPoints(): Flow<List<CampusMeetingPoint>> = _meetingPointsFlow.asStateFlow()

    override suspend fun createMeetingPoint(meetingPoint: CampusMeetingPoint): Result<CampusMeetingPoint> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                postgrest.from("campus_meeting_points").insert(
                    buildJsonObject {
                        put("id", meetingPoint.id)
                        put("name", meetingPoint.name)
                        meetingPoint.pavilion?.let { put("pavilion", it) }
                        meetingPoint.description?.let { put("description", it) }
                        put("campus", meetingPoint.campus ?: "Los Olivos")
                        put("zone_type", meetingPoint.zoneType)
                        put("is_active", meetingPoint.isActive)
                    }
                )
                refreshMeetingPoints()
                return@withContext Result.success(meetingPoint)
            }
            val current = _meetingPointsFlow.value.toMutableList()
            current.add(meetingPoint)
            _meetingPointsFlow.value = current
            Result.success(meetingPoint)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al crear punto: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleMeetingPoint(id: String, active: Boolean): Result<CampusMeetingPoint> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.from("campus_meeting_points").update(
                        buildJsonObject {
                            put("is_active", active)
                        }
                    ) {
                        filter { eq("id", id) }
                    }
                    refreshMeetingPoints()
                } catch (e: Exception) {
                    android.util.Log.e("AdminRepo", "Error al cambiar estado de punto: ${e.message}", e)
                }
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

    override suspend fun deleteMeetingPoint(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                postgrest.from("campus_meeting_points").delete {
                    filter { eq("id", id) }
                }
                refreshMeetingPoints()
                return@withContext Result.success(Unit)
            }
            val current = _meetingPointsFlow.value.toMutableList()
            current.removeAll { it.id == id }
            _meetingPointsFlow.value = current
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al eliminar punto: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeSellerApplications(): Flow<List<SellerApplication>> = _applicationsFlow.asStateFlow()

    override suspend fun approveSellerApplication(applicationId: String, adminId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val targetApp = _applicationsFlow.value.find { it.id == applicationId }
                    ?: try {
                        postgrest.from("seller_applications").select { filter { eq("id", applicationId) } }.decodeSingleOrNull<SellerApplication>()
                    } catch (_: Exception) { null }

                postgrest.from("seller_applications").update(
                    buildJsonObject {
                        put("status", "approved")
                        if (!adminId.isNullOrBlank()) {
                            put("reviewed_by", adminId)
                        }
                    }
                ) {
                    filter { eq("id", applicationId) }
                }

                val userId = targetApp?.userId
                if (!userId.isNullOrBlank()) {
                    postgrest.from("profiles").update(
                        buildJsonObject {
                            put("role", "emprendedor")
                            put("business_name", targetApp.storeName.ifBlank { "Mi Tienda" })
                            if (targetApp.category.isNotBlank()) {
                                put("business_category", targetApp.category)
                            }
                            if (targetApp.description.isNotBlank()) {
                                put("business_description", targetApp.description)
                            }
                            if (targetApp.phone.isNotBlank()) {
                                put("phone", targetApp.phone)
                            }
                            if (!targetApp.proposedLocation.isNullOrBlank()) {
                                put("business_location", targetApp.proposedLocation)
                            }
                            put("business_status", "ABIERTO")
                            put("accepting_orders", true)
                        }
                    ) {
                        filter { eq("id", userId) }
                    }
                }
                refreshSellerApplications()
                refreshSellers()
                return@withContext Result.success(Unit)
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
            android.util.Log.e("AdminRepo", "Error al aprobar solicitud: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun rejectSellerApplication(applicationId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val targetApp = _applicationsFlow.value.find { it.id == applicationId }
                    ?: try {
                        postgrest.from("seller_applications").select { filter { eq("id", applicationId) } }.decodeSingleOrNull<SellerApplication>()
                    } catch (_: Exception) { null }

                postgrest.from("seller_applications").update(
                    buildJsonObject {
                        put("status", "rejected")
                        put("rejection_reason", reason)
                    }
                ) {
                    filter { eq("id", applicationId) }
                }

                val userId = targetApp?.userId
                if (!userId.isNullOrBlank()) {
                    postgrest.from("profiles").update(
                        buildJsonObject {
                            put("business_status", "RECHAZADO")
                            put("accepting_orders", false)
                            put("suspension_reason", reason)
                        }
                    ) {
                        filter { eq("id", userId) }
                    }
                }
                refreshSellerApplications()
                refreshSellers()
                return@withContext Result.success(Unit)
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
            android.util.Log.e("AdminRepo", "Error al rechazar solicitud: ${e.message}", e)
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
                    postgrest.from("profiles").update(
                        buildJsonObject {
                            put("role", targetRole)
                            put("business_status", if (isSuspended) "CERRADO" else "ABIERTO")
                            put("accepting_orders", !isSuspended)
                            if (isSuspended && !reason.isNullOrBlank()) {
                                put("suspension_reason_text", reason)
                                put("suspension_reason", reason)
                            } else if (!isSuspended) {
                                put("suspension_reason", "")
                            }
                        }
                    ) {
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