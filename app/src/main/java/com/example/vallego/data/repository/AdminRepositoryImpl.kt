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
import com.example.vallego.domain.model.BuyerOrderStats
import com.example.vallego.domain.model.ProfileWarning
import com.example.vallego.domain.model.CampusDetailedMetrics
import com.example.vallego.domain.model.MetricsPeriod
import com.example.vallego.domain.model.SellerSalesRanking
import com.example.vallego.domain.model.PaymentMethodBreakdown
import com.example.vallego.domain.model.MeetingPointTraffic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AdminRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val orderRepository: OrderRepository? = null
) : AdminRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonParser = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

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

        private val defaultBuyers = listOf(
            UserProfile(
                id = "buyer-marcos",
                fullName = "Marcos",
                phone = "94814534533",
                role = UserRole.COMPRADOR,
                campus = "UCV - Lima Norte"
            ),
            UserProfile(
                id = "buyer-juan",
                fullName = "Juan Comprador",
                phone = "987654321",
                role = UserRole.COMPRADOR,
                campus = "Los Olivos"
            )
        )
    }

    private val _meetingPointsFlow = MutableStateFlow<List<CampusMeetingPoint>>(defaultMeetingPoints)
    private val _applicationsFlow = MutableStateFlow<List<SellerApplication>>(defaultApplications)
    private val _sellersFlow = MutableStateFlow<List<UserProfile>>(defaultSellers)
    private val _buyersFlow = MutableStateFlow<List<UserProfile>>(defaultBuyers)
    private val _incidentsFlow = MutableStateFlow<List<OrderIncident>>(emptyList())
    private val _userStrikesFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val inMemoryWarnings = mutableListOf<ProfileWarning>()

    init {
        scope.launch {
            refreshAll()
        }
    }

    private suspend fun refreshAll() {
        refreshMeetingPoints()
        refreshSellerApplications()
        refreshSellers()
        refreshBuyers()
        refreshIncidents()
        refreshUserStrikes()
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

    override suspend fun refreshBuyers() = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val profiles = postgrest.from("profiles")
                    .select {
                        filter {
                            or {
                                eq("role", "comprador")
                                eq("role", "suspended_buyer")
                            }
                        }
                    }
                    .decodeList<UserProfile>()
                _buyersFlow.value = profiles
                return@withContext
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al cargar profiles de compradores: ${e.message}", e)
        }

        if (_buyersFlow.value.isEmpty() && postgrest == null) {
            _buyersFlow.value = defaultBuyers
        }
    }

    override suspend fun refreshIncidents(): Unit = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val remoteIncidents = postgrest.from("order_incidents")
                    .select {
                        order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
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
            // Si se reactiva (isSuspended == false), comprobar si tenía 5 o más strikes para resetearlos a 0
            val currentStrikes = _userStrikesFlow.value[sellerId] ?: 0
            if (!isSuspended && currentStrikes >= 5) {
                if (postgrest != null) {
                    try {
                        postgrest.from("profile_warnings").delete {
                            filter { eq("profile_id", sellerId) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AdminRepo", "Error al borrar warnings para reactivacion: ${e.message}")
                    }
                }
                inMemoryWarnings.removeAll { it.profileId == sellerId }
            }

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
                    refreshUserStrikes()
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
                    refreshUserStrikes()
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
            refreshUserStrikes()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeBuyers(): Flow<List<UserProfile>> = _buyersFlow.asStateFlow()

    override suspend fun toggleBuyerSuspension(buyerId: String, isSuspended: Boolean, reason: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Si se reactiva (isSuspended == false), comprobar si tenía 5 o más strikes para resetearlos a 0
            val currentStrikes = _userStrikesFlow.value[buyerId] ?: 0
            if (!isSuspended && currentStrikes >= 5) {
                if (postgrest != null) {
                    try {
                        postgrest.from("profile_warnings").delete {
                            filter { eq("profile_id", buyerId) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AdminRepo", "Error al borrar warnings para reactivacion: ${e.message}")
                    }
                }
                inMemoryWarnings.removeAll { it.profileId == buyerId }
            }

            if (postgrest != null) {
                val targetRole = if (isSuspended) "suspended_buyer" else "comprador"
                postgrest.from("profiles").update(
                    buildJsonObject {
                        put("role", targetRole)
                        if (isSuspended && !reason.isNullOrBlank()) {
                            put("suspension_reason", reason)
                        } else if (!isSuspended) {
                            put("suspension_reason", "")
                        }
                    }
                ) {
                    filter { eq("id", buyerId) }
                }
                refreshBuyers()
                refreshUserStrikes()
                return@withContext Result.success(Unit)
            }

            val current = _buyersFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == buyerId }
            if (index >= 0) {
                val updatedRole = if (isSuspended) UserRole.SUSPENDED_BUYER else UserRole.COMPRADOR
                current[index] = current[index].copy(
                    role = updatedRole,
                    suspensionReason = if (isSuspended) reason else null
                )
                _buyersFlow.value = current
            }
            refreshUserStrikes()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al cambiar suspension de comprador: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeUserStrikes(): Flow<Map<String, Int>> = _userStrikesFlow.asStateFlow()

    override suspend fun refreshUserStrikes() = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val allWarnings = postgrest.from("profile_warnings").select().decodeList<ProfileWarning>()
                val strikesMap = allWarnings.groupBy { it.profileId }.mapValues { it.value.size }
                _userStrikesFlow.value = strikesMap

                // Auto-suspensión inmediata si alguien alcanza 5 o más strikes y aún no está suspendido
                strikesMap.forEach { (profileId, count) ->
                    if (count >= 5) {
                        val currentSeller = _sellersFlow.value.find { it.id == profileId }
                        if (currentSeller != null && currentSeller.role != UserRole.SUSPENDED) {
                            toggleSellerSuspension(
                                sellerId = profileId,
                                isSuspended = true,
                                reason = "Suspensión automática del sistema por acumulación de $count strikes."
                            )
                        }
                        val currentBuyer = _buyersFlow.value.find { it.id == profileId }
                        if (currentBuyer != null && currentBuyer.role != UserRole.SUSPENDED_BUYER && currentBuyer.role != UserRole.SUSPENDED) {
                            toggleBuyerSuspension(
                                buyerId = profileId,
                                isSuspended = true,
                                reason = "Suspensión automática del sistema por acumulación de $count strikes."
                            )
                        }
                    }
                }
                return@withContext
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error al refrescar strikes de usuarios: ${e.message}", e)
        }

        val strikesMap = inMemoryWarnings.groupBy { it.profileId }.mapValues { it.value.size }
        _userStrikesFlow.value = strikesMap
        strikesMap.forEach { (profileId, count) ->
            if (count >= 5) {
                val currentSeller = _sellersFlow.value.find { it.id == profileId }
                if (currentSeller != null && currentSeller.role != UserRole.SUSPENDED) {
                    toggleSellerSuspension(
                        sellerId = profileId,
                        isSuspended = true,
                        reason = "Suspensión automática del sistema por acumulación de $count strikes."
                    )
                }
                val currentBuyer = _buyersFlow.value.find { it.id == profileId }
                if (currentBuyer != null && currentBuyer.role != UserRole.SUSPENDED_BUYER && currentBuyer.role != UserRole.SUSPENDED) {
                    toggleBuyerSuspension(
                        buyerId = profileId,
                        isSuspended = true,
                        reason = "Suspensión automática del sistema por acumulación de $count strikes."
                    )
                }
            }
        }
    }

    override suspend fun issueWarning(profileId: String, reason: String, createdBy: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentStrikes = _userStrikesFlow.value[profileId] ?: 0
            if (currentStrikes >= 5) {
                return@withContext Result.failure(IllegalStateException("El usuario ya alcanzó el tope máximo de 5 strikes y su cuenta está suspendida."))
            }

            val warningId = UUID.randomUUID().toString()
            val newWarning = ProfileWarning(
                id = warningId,
                profileId = profileId,
                reason = reason.trim(),
                createdBy = createdBy,
                createdAt = java.time.Instant.now().toString()
            )
            if (postgrest != null) {
                postgrest.from("profile_warnings").insert(
                    buildJsonObject {
                        put("id", warningId)
                        put("profile_id", profileId)
                        put("reason", reason.trim())
                        createdBy?.let { put("created_by", it) }
                    }
                )
            } else {
                inMemoryWarnings.add(0, newWarning)
            }
            refreshUserStrikes()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al emitir advertencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getProfileWarnings(profileId: String): Result<List<ProfileWarning>> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val warnings = postgrest.from("profile_warnings").select {
                    filter {
                        eq("profile_id", profileId)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<ProfileWarning>()
                return@withContext Result.success(warnings)
            }
            val warnings = inMemoryWarnings.filter { it.profileId == profileId }
            Result.success(warnings)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al obtener advertencias: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getBuyerOrderStats(buyerId: String): Result<BuyerOrderStats> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val orders = postgrest.from("orders").select {
                    filter {
                        eq("buyer_id", buyerId)
                    }
                }.decodeList<AdminOrderDto>()

                val total = orders.size
                val completed = orders.filter { it.status.lowercase() in listOf("completed", "completado", "payment_confirmed", "pago_confirmado") }
                val cancelled = orders.filter { it.status.lowercase() in listOf("cancelled", "cancelado", "rejected", "rechazado", "not_delivered", "no_entregado") }
                val completedCount = completed.size
                val cancelledCount = cancelled.size
                val inProgressCount = (total - completedCount - cancelledCount).coerceAtLeast(0)
                val totalSpent = completed.sumOf { it.totalPrice }

                return@withContext Result.success(
                    BuyerOrderStats(
                        totalOrders = total,
                        completedOrders = completedCount,
                        cancelledOrders = cancelledCount,
                        inProgressOrders = inProgressCount,
                        totalSpent = totalSpent
                    )
                )
            }
            Result.success(BuyerOrderStats(totalOrders = 0, completedOrders = 0, cancelledOrders = 0, inProgressOrders = 0, totalSpent = 0.0))
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al obtener estadisticas de comprador: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeIncidents(): Flow<List<OrderIncident>> = _incidentsFlow.asStateFlow()

    override suspend fun resolveIncident(
        incidentId: String,
        status: String,
        action: String?,
        adminNotes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    postgrest.rpc(
                        function = "resolve_incident_rpc",
                        parameters = buildJsonObject {
                            put("p_incident_id", incidentId)
                            put("p_status", status)
                            action?.let { put("p_action", it) }
                            adminNotes?.let { put("p_admin_notes", it) }
                        }
                    )
                } catch (rpcErr: Exception) {
                    android.util.Log.w("AdminRepo", "RPC resolve_incident_rpc fallo, intentando update directo: ${rpcErr.message}")
                    postgrest.from("order_incidents").update(
                        buildJsonObject {
                            put("status", status)
                            action?.let { put("resolution_action", it) }
                            adminNotes?.let { put("admin_notes", it) }
                        }
                    ) {
                        filter { eq("id", incidentId) }
                    }
                }
                refreshIncidents()
                return@withContext Result.success(Unit)
            }
            val current = _incidentsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == incidentId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(
                    status = status,
                    resolutionAction = action,
                    adminNotes = adminNotes
                )
                _incidentsFlow.value = current
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al resolver incidencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getIncidentsForUser(userId: String): Result<List<OrderIncident>> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val incidents = postgrest.from("order_incidents").select {
                    filter {
                        eq("reported_user_id", userId)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<OrderIncident>()
                return@withContext Result.success(incidents)
            }
            val local = _incidentsFlow.value.filter { it.reportedUserId == userId }
            Result.success(local)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "Error al obtener incidencias de usuario: ${e.message}", e)
            Result.failure(e)
        }
    }

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

    override suspend fun getCampusDetailedMetrics(period: MetricsPeriod): Result<CampusDetailedMetrics> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                try {
                    val rpcResult = postgrest.rpc(
                        function = "get_campus_admin_metrics",
                        parameters = buildJsonObject {
                            put("p_period", period.apiValue)
                        }
                    )
                    val parsed = jsonParser.decodeFromString<CampusDetailedMetrics>(rpcResult.data)
                    return@withContext Result.success(parsed)
                } catch (e: Exception) {
                    android.util.Log.w("AdminRepositoryImpl", "RPC get_campus_admin_metrics fallo, usando cálculo local: ${e.message}", e)
                }
            }

            val sellers = _sellersFlow.value
            val subOrders = try {
                if (postgrest != null) {
                    postgrest.from("sub_orders").select().decodeList<AdminSubOrderDto>()
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }

            val orders = try {
                if (postgrest != null) {
                    postgrest.from("orders").select().decodeList<AdminOrderDto>()
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }

            val computed = computeLocalDetailedMetrics(period, sellers, subOrders, orders)
            Result.success(computed)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepositoryImpl", "Error en getCampusDetailedMetrics: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun computeLocalDetailedMetrics(
        period: MetricsPeriod,
        sellers: List<UserProfile>,
        subOrders: List<AdminSubOrderDto>,
        orders: List<AdminOrderDto>
    ): CampusDetailedMetrics {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        val startOfWeek = now - (7L * 24 * 60 * 60 * 1000)
        val startOfMonth = now - (30L * 24 * 60 * 60 * 1000)

        val filteredSubs = when (period) {
            MetricsPeriod.HOY -> subOrders.filter { parseIsoTime(it.createdAt) >= startOfToday }
            MetricsPeriod.SEMANA -> subOrders.filter { parseIsoTime(it.createdAt) >= startOfWeek }
            MetricsPeriod.MES -> subOrders.filter { parseIsoTime(it.createdAt) >= startOfMonth }
            MetricsPeriod.HISTORICO -> subOrders
        }

        val completed = filteredSubs.filter { it.status.lowercase() in listOf("completed", "pago_confirmado") }
        val cancelled = filteredSubs.filter { it.status.lowercase() in listOf("cancelled", "rejected", "no_entregado") }
        val totalSales = completed.sumOf { it.subtotalAmount }
        val totalOrders = filteredSubs.size
        val completedCount = completed.size
        val cancelledCount = cancelled.size
        val avgTicket = if (completedCount > 0) totalSales / completedCount else 0.0
        val totalAttempts = completedCount + cancelledCount
        val fulfillmentRate = if (totalAttempts > 0) (completedCount.toDouble() * 100.0) / totalAttempts.toDouble() else 100.0

        val sellerMap = sellers.associateBy { it.id }
        val salesBySeller = completed.groupBy { it.sellerId }
        val rankings = salesBySeller.mapNotNull { (sellerId, subs) ->
            val seller = sellerMap[sellerId]
            if (seller == null || (seller.role != UserRole.EMPRENDEDOR && seller.role != UserRole.SUSPENDED) || seller.businessName.isNullOrBlank()) {
                return@mapNotNull null
            }
            val sellerSales = subs.sumOf { it.subtotalAmount }
            val sellerCompleted = subs.size
            val percentage = if (totalSales > 0) ((sellerSales * 100.0) / totalSales) else 0.0
            SellerSalesRanking(
                sellerId = sellerId,
                storeName = seller.displayStoreName,
                ownerName = seller.fullName,
                avatarUrl = seller.avatarUrl,
                totalSales = sellerSales,
                completedOrders = sellerCompleted,
                percentage = percentage
            )
        }.sortedByDescending { it.totalSales }

        val paymentMap = filteredSubs.groupBy { (it.paymentMethod ?: "EFECTIVO").uppercase() }
        val paymentBreakdowns = paymentMap.map { (method, subs) ->
            val count = subs.size
            val amount = subs.sumOf { it.subtotalAmount }
            val pct = if (totalOrders > 0) (count.toDouble() * 100.0) / totalOrders.toDouble() else 0.0
            PaymentMethodBreakdown(
                method = method,
                count = count,
                totalAmount = amount,
                percentage = pct
            )
        }.sortedByDescending { it.count }

        val ordersMap = orders.associateBy { it.id }
        val trafficByPoint = filteredSubs.groupBy { sub ->
            val parent = sub.orderId?.let { ordersMap[it] }
            parent?.meetingPointName ?: parent?.deliveryPlace ?: "Campus General"
        }.map { (ptName, subs) ->
            val count = subs.size
            val pct = if (totalOrders > 0) (count.toDouble() * 100.0) / totalOrders.toDouble() else 0.0
            MeetingPointTraffic(
                pointName = ptName,
                count = count,
                percentage = pct
            )
        }.sortedByDescending { it.count }.take(6)

        return CampusDetailedMetrics(
            period = period.apiValue,
            totalSales = totalSales,
            totalOrders = totalOrders,
            completedOrders = completedCount,
            cancelledOrders = cancelledCount,
            averageTicket = avgTicket,
            fulfillmentRate = fulfillmentRate,
            sellerRankings = rankings,
            paymentMethods = paymentBreakdowns,
            topMeetingPoints = trafficByPoint
        )
    }

    private fun parseIsoTime(isoDate: String?): Long {
        if (isoDate.isNullOrBlank()) return 0L
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
                val d = sdf.parse(isoDate)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return 0L
    }
}

@Serializable
private data class AdminSubOrderDto(
    val id: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("subtotal_amount") val subtotalAmount: Double = 0.0,
    val status: String = "pending",
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class AdminOrderDto(
    val id: String,
    @SerialName("buyer_id") val buyerId: String? = null,
    val status: String = "pending",
    @SerialName("total_price") val totalPrice: Double = 0.0,
    @SerialName("meeting_point_name") val meetingPointName: String? = null,
    @SerialName("delivery_place") val deliveryPlace: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)