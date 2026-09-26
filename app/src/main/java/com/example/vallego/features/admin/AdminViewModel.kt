package com.example.vallego.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.MetricsPeriod
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.AdminRepository
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.observeMeetingPoints().collect { points ->
                _uiState.update { it.copy(meetingPoints = points) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeSellerApplications().collect { apps ->
                _uiState.update { it.copy(sellerApplications = apps) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeSellers().collect { sellers ->
                _uiState.update { currentState ->
                    val updatedSelectedSeller = currentState.selectedSellerDetail?.let { selected ->
                        sellers.find { it.id == selected.id } ?: selected
                    }
                    currentState.copy(
                        sellers = sellers,
                        selectedSellerDetail = updatedSelectedSeller
                    )
                }
            }
        }
        viewModelScope.launch {
            adminRepository.observeBuyers().collect { buyers ->
                _uiState.update { currentState ->
                    val updatedSelectedBuyer = currentState.selectedBuyerDetail?.let { selected ->
                        buyers.find { it.id == selected.id } ?: selected
                    }
                    currentState.copy(
                        buyers = buyers,
                        selectedBuyerDetail = updatedSelectedBuyer
                    )
                }
            }
        }
        viewModelScope.launch {
            adminRepository.observeIncidents().collect { incidents ->
                _uiState.update { it.copy(incidents = incidents) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeCampusMetrics().collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeUserStrikes().collect { strikes ->
                _uiState.update { it.copy(userStrikesMap = strikes) }
            }
        }
        loadDetailedMetrics(MetricsPeriod.HOY)
    }

    fun setTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSellerSearchQueryChange(query: String) {
        _uiState.update { it.copy(sellerSearchQuery = query) }
    }

    fun onApplicationFilterChange(filter: String) {
        _uiState.update { it.copy(applicationFilter = filter) }
    }

    fun setMetricsPeriod(period: MetricsPeriod) {
        _uiState.update { it.copy(selectedMetricsPeriod = period) }
        loadDetailedMetrics(period)
    }

    private fun loadDetailedMetrics(period: MetricsPeriod) {
        _uiState.update { it.copy(isLoadingMetrics = true) }
        viewModelScope.launch {
            val result = adminRepository.getCampusDetailedMetrics(period)
            _uiState.update {
                it.copy(
                    isLoadingMetrics = false,
                    detailedMetrics = result.getOrDefault(it.detailedMetrics)
                )
            }
        }
    }

    fun onSelectSeller(seller: UserProfile) {
        _uiState.update {
            it.copy(
                selectedSellerDetail = seller,
                isLoadingSellerProducts = true,
                sellerProducts = emptyList(),
                sellerStats = null,
                userWarnings = emptyList(),
                isLoadingWarnings = true
            )
        }
        viewModelScope.launch {
            val prodsResult = productRepository.getProductsBySeller(seller.id)
            val statsResult = orderRepository.getSellerDashboardStatistics(seller.id, "all")
            val warningsResult = adminRepository.getProfileWarnings(seller.id)
            val incidentsResult = adminRepository.getIncidentsForUser(seller.id)
            _uiState.update {
                it.copy(
                    isLoadingSellerProducts = false,
                    sellerProducts = prodsResult.getOrDefault(emptyList()),
                    sellerStats = statsResult.getOrNull(),
                    userWarnings = warningsResult.getOrDefault(emptyList()),
                    userIncidents = incidentsResult.getOrDefault(emptyList()),
                    isLoadingWarnings = false
                )
            }
        }
    }

    fun closeSellerDetail() {
        _uiState.update {
            it.copy(
                selectedSellerDetail = null,
                sellerProducts = emptyList(),
                sellerStats = null,
                userWarnings = emptyList(),
                userIncidents = emptyList()
            )
        }
    }

    fun onBuyerSearchQueryChange(query: String) {
        _uiState.update { it.copy(buyerSearchQuery = query) }
    }

    fun onSelectBuyer(buyer: UserProfile) {
        _uiState.update {
            it.copy(
                selectedBuyerDetail = buyer,
                isLoadingBuyerDetail = true,
                buyerStats = null,
                userWarnings = emptyList(),
                userIncidents = emptyList(),
                isLoadingWarnings = true
            )
        }
        viewModelScope.launch {
            val statsResult = adminRepository.getBuyerOrderStats(buyer.id)
            val warningsResult = adminRepository.getProfileWarnings(buyer.id)
            val incidentsResult = adminRepository.getIncidentsForUser(buyer.id)
            _uiState.update {
                it.copy(
                    isLoadingBuyerDetail = false,
                    buyerStats = statsResult.getOrNull(),
                    userWarnings = warningsResult.getOrDefault(emptyList()),
                    userIncidents = incidentsResult.getOrDefault(emptyList()),
                    isLoadingWarnings = false
                )
            }
        }
    }

    fun closeBuyerDetail() {
        _uiState.update {
            it.copy(
                selectedBuyerDetail = null,
                buyerStats = null,
                userWarnings = emptyList(),
                userIncidents = emptyList()
            )
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                adminRepository.refreshMeetingPoints()
                adminRepository.refreshSellerApplications()
                adminRepository.refreshSellers()
                adminRepository.refreshBuyers()
                adminRepository.refreshIncidents()
                adminRepository.refreshUserStrikes()
                loadDetailedMetrics(_uiState.value.selectedMetricsPeriod)
                _uiState.value.selectedSellerDetail?.let { seller ->
                    val prods = productRepository.getProductsBySeller(seller.id).getOrDefault(emptyList())
                    val stats = orderRepository.getSellerDashboardStatistics(seller.id, "all").getOrNull()
                    val warns = adminRepository.getProfileWarnings(seller.id).getOrDefault(emptyList())
                    _uiState.update { it.copy(sellerProducts = prods, sellerStats = stats, userWarnings = warns) }
                }
                _uiState.value.selectedBuyerDetail?.let { buyer ->
                    val stats = adminRepository.getBuyerOrderStats(buyer.id).getOrNull()
                    val warns = adminRepository.getProfileWarnings(buyer.id).getOrDefault(emptyList())
                    _uiState.update { it.copy(buyerStats = stats, userWarnings = warns) }
                }
                _uiState.update { it.copy(isLoading = false, successMessage = "Datos actualizados correctamente") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error al actualizar: ${e.message}") }
            }
        }
    }

    fun toggleMeetingPoint(pointId: String, currentActive: Boolean) {
        viewModelScope.launch {
            val result = adminRepository.toggleMeetingPoint(pointId, !currentActive)
            if (result.isSuccess) {
                val stateText = if (!currentActive) "activado" else "desactivado"
                _uiState.update { it.copy(successMessage = "Punto $stateText correctamente.") }
            } else {
                _uiState.update { it.copy(errorMessage = "Error al cambiar estado del punto.") }
            }
        }
    }

    fun openCreateMeetingPointDialog() {
        _uiState.update { it.copy(showCreateMeetingPointDialog = true) }
    }

    fun dismissCreateMeetingPointDialog() {
        _uiState.update { it.copy(showCreateMeetingPointDialog = false) }
    }

    fun createMeetingPoint(name: String, pavilion: String, description: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa el nombre del punto de encuentro") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val point = CampusMeetingPoint(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                pavilion = pavilion.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                isActive = true
            )
            val result = adminRepository.createMeetingPoint(point)
            dismissCreateMeetingPointDialog()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Punto '${point.name}' creado con éxito." else null,
                    errorMessage = if (result.isFailure) "Error al crear: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun openDeleteMeetingPointDialog(point: CampusMeetingPoint) {
        _uiState.update { it.copy(pointToDelete = point) }
    }

    fun dismissDeleteMeetingPointDialog() {
        _uiState.update { it.copy(pointToDelete = null) }
    }

    fun confirmDeleteMeetingPoint() {
        val point = _uiState.value.pointToDelete ?: return
        dismissDeleteMeetingPointDialog()
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.deleteMeetingPoint(point.id)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Punto '${point.name}' eliminado con éxito." else null,
                    errorMessage = if (result.isFailure) "Error al eliminar: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun approveApplication(applicationId: String, adminId: String? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.approveSellerApplication(applicationId, adminId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "¡Solicitud aprobada! El usuario ahora tiene acceso como vendedor." else null,
                    errorMessage = if (result.isFailure) "Error al aprobar solicitud: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun openRejectionDialog(application: SellerApplication) {
        _uiState.update { it.copy(selectedApplicationForRejection = application) }
    }

    fun dismissRejectionDialog() {
        _uiState.update { it.copy(selectedApplicationForRejection = null) }
    }

    fun confirmRejection(applicationId: String, reason: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.rejectSellerApplication(applicationId, reason)
            dismissRejectionDialog()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Solicitud rechazada con motivo registrado." else null,
                    errorMessage = if (result.isFailure) "Error al rechazar: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun openSuspensionDialog(seller: UserProfile) {
        _uiState.update { it.copy(selectedSellerForSuspension = seller) }
    }

    fun dismissSuspensionDialog() {
        _uiState.update { it.copy(selectedSellerForSuspension = null, selectedIncidentForResolution = null) }
    }

    fun confirmSellerSuspension(sellerId: String, reason: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val pendingIncident = _uiState.value.selectedIncidentForResolution
            val result = adminRepository.toggleSellerSuspension(sellerId, isSuspended = true, reason = reason)
            if (pendingIncident != null && result.isSuccess) {
                adminRepository.resolveIncident(
                    incidentId = pendingIncident.id,
                    status = "SANCIONADO",
                    action = "SUSPENDED",
                    adminNotes = "Puesto suspendido por el administrador: $reason"
                )
            }
            dismissSuspensionDialog()
            _uiState.update { currentState ->
                val updatedDetail = if (currentState.selectedSellerDetail?.id == sellerId) {
                    currentState.selectedSellerDetail.copy(
                        role = com.example.vallego.domain.model.UserRole.SUSPENDED,
                        businessStatus = "CERRADO",
                        acceptingOrders = false,
                        suspensionReason = reason
                    )
                } else currentState.selectedSellerDetail
                currentState.copy(
                    selectedSellerDetail = updatedDetail,
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Puesto suspendido temporalmente." else null,
                    errorMessage = if (result.isFailure) "Error al suspender: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun reactivateSeller(sellerId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.toggleSellerSuspension(sellerId, isSuspended = false)
            val updatedWarnings = adminRepository.getProfileWarnings(sellerId).getOrDefault(emptyList())
            _uiState.update { currentState ->
                val updatedDetail = if (currentState.selectedSellerDetail?.id == sellerId) {
                    currentState.selectedSellerDetail.copy(
                        role = com.example.vallego.domain.model.UserRole.EMPRENDEDOR,
                        businessStatus = "ABIERTO",
                        acceptingOrders = true,
                        suspensionReason = null
                    )
                } else currentState.selectedSellerDetail
                currentState.copy(
                    selectedSellerDetail = updatedDetail,
                    userWarnings = if (currentState.selectedSellerDetail?.id == sellerId) updatedWarnings else currentState.userWarnings,
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Puesto reactivado exitosamente." else null,
                    errorMessage = if (result.isFailure) "Error al reactivar: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun openBuyerSuspensionDialog(buyer: UserProfile) {
        _uiState.update { it.copy(selectedBuyerForSuspension = buyer) }
    }

    fun dismissBuyerSuspensionDialog() {
        _uiState.update { it.copy(selectedBuyerForSuspension = null, selectedIncidentForResolution = null) }
    }

    fun confirmBuyerSuspension(buyerId: String, reason: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val pendingIncident = _uiState.value.selectedIncidentForResolution
            val result = adminRepository.toggleBuyerSuspension(buyerId, isSuspended = true, reason = reason)
            if (pendingIncident != null && result.isSuccess) {
                adminRepository.resolveIncident(
                    incidentId = pendingIncident.id,
                    status = "SANCIONADO",
                    action = "SUSPENDED",
                    adminNotes = "Comprador suspendido por el administrador: $reason"
                )
            }
            dismissBuyerSuspensionDialog()
            _uiState.update { currentState ->
                val updatedDetail = if (currentState.selectedBuyerDetail?.id == buyerId) {
                    currentState.selectedBuyerDetail.copy(
                        role = com.example.vallego.domain.model.UserRole.SUSPENDED_BUYER,
                        suspensionReason = reason
                    )
                } else currentState.selectedBuyerDetail
                currentState.copy(
                    selectedBuyerDetail = updatedDetail,
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Comprador suspendido temporalmente." else null,
                    errorMessage = if (result.isFailure) "Error al suspender comprador: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun reactivateBuyer(buyerId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.toggleBuyerSuspension(buyerId, isSuspended = false)
            val updatedWarnings = adminRepository.getProfileWarnings(buyerId).getOrDefault(emptyList())
            _uiState.update { currentState ->
                val updatedDetail = if (currentState.selectedBuyerDetail?.id == buyerId) {
                    currentState.selectedBuyerDetail.copy(
                        role = com.example.vallego.domain.model.UserRole.COMPRADOR,
                        suspensionReason = null
                    )
                } else currentState.selectedBuyerDetail
                currentState.copy(
                    selectedBuyerDetail = updatedDetail,
                    userWarnings = if (currentState.selectedBuyerDetail?.id == buyerId) updatedWarnings else currentState.userWarnings,
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Comprador reactivado exitosamente." else null,
                    errorMessage = if (result.isFailure) "Error al reactivar comprador: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun openWarningDialog(user: UserProfile) {
        val currentStrikes = _uiState.value.userStrikesMap[user.id] ?: 0
        if (currentStrikes >= 5) {
            _uiState.update { it.copy(errorMessage = "El usuario ya alcanzó el tope máximo de 5 strikes y está suspendido.") }
            return
        }
        _uiState.update { it.copy(selectedUserForWarning = user) }
    }

    fun dismissWarningDialog() {
        _uiState.update { it.copy(selectedUserForWarning = null, selectedIncidentForResolution = null) }
    }

    fun openWarningDialogForIncident(incident: OrderIncident, reportedUser: UserProfile) {
        val currentStrikes = _uiState.value.userStrikesMap[reportedUser.id] ?: 0
        if (currentStrikes >= 5) {
            _uiState.update { it.copy(errorMessage = "El usuario ya alcanzó el tope máximo de 5 strikes y está suspendido.") }
            return
        }
        _uiState.update {
            it.copy(
                selectedUserForWarning = reportedUser,
                selectedIncidentForResolution = incident
            )
        }
    }

    fun openSuspensionDialogForIncident(incident: OrderIncident, reportedUser: UserProfile) {
        if (reportedUser.role == com.example.vallego.domain.model.UserRole.COMPRADOR ||
            reportedUser.role == com.example.vallego.domain.model.UserRole.SUSPENDED_BUYER) {
            _uiState.update {
                it.copy(
                    selectedBuyerForSuspension = reportedUser,
                    selectedIncidentForResolution = incident
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedSellerForSuspension = reportedUser,
                    selectedIncidentForResolution = incident
                )
            }
        }
    }

    fun onIncidentFilterChange(filter: String) {
        _uiState.update { it.copy(incidentFilter = filter) }
    }

    fun resolveIncident(incidentId: String, status: String, action: String? = null, adminNotes: String? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.resolveIncident(incidentId, status, action, adminNotes)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Incidencia actualizada a $status." else null,
                    errorMessage = if (result.isFailure) "Error al resolver incidencia: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun confirmIssueWarning(userId: String, reason: String, adminId: String? = null) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes indicar el motivo de la llamada de atención") }
            return
        }
        val currentStrikes = _uiState.value.userStrikesMap[userId] ?: 0
        if (currentStrikes >= 5) {
            dismissWarningDialog()
            _uiState.update { it.copy(errorMessage = "El usuario ya alcanzó el tope máximo de 5 strikes.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val pendingIncident = _uiState.value.selectedIncidentForResolution
            val result = adminRepository.issueWarning(userId, reason, adminId)
            if (pendingIncident != null && result.isSuccess) {
                adminRepository.resolveIncident(
                    incidentId = pendingIncident.id,
                    status = "SANCIONADO",
                    action = "WARNING_ISSUED",
                    adminNotes = "Llamada de atención aplicada: $reason"
                )
            }
            dismissWarningDialog()
            if (result.isSuccess) {
                val warnings = adminRepository.getProfileWarnings(userId).getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userWarnings = warnings,
                        successMessage = "Llamada de atención registrada con éxito."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al registrar llamada de atención: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}