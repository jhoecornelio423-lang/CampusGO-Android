package com.example.vallego.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.MetricsPeriod
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
            adminRepository.observeIncidents().collect { incidents ->
                _uiState.update { it.copy(incidents = incidents) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeCampusMetrics().collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
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
                sellerStats = null
            )
        }
        viewModelScope.launch {
            val prodsResult = productRepository.getProductsBySeller(seller.id)
            val statsResult = orderRepository.getSellerDashboardStatistics(seller.id, "all")
            _uiState.update {
                it.copy(
                    isLoadingSellerProducts = false,
                    sellerProducts = prodsResult.getOrDefault(emptyList()),
                    sellerStats = statsResult.getOrNull()
                )
            }
        }
    }

    fun closeSellerDetail() {
        _uiState.update {
            it.copy(
                selectedSellerDetail = null,
                sellerProducts = emptyList(),
                sellerStats = null
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
                adminRepository.refreshIncidents()
                loadDetailedMetrics(_uiState.value.selectedMetricsPeriod)
                _uiState.value.selectedSellerDetail?.let { seller ->
                    val prods = productRepository.getProductsBySeller(seller.id).getOrDefault(emptyList())
                    val stats = orderRepository.getSellerDashboardStatistics(seller.id, "all").getOrNull()
                    _uiState.update { it.copy(sellerProducts = prods, sellerStats = stats) }
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
        _uiState.update { it.copy(selectedSellerForSuspension = null) }
    }

    fun confirmSellerSuspension(sellerId: String, reason: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = adminRepository.toggleSellerSuspension(sellerId, isSuspended = true, reason = reason)
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
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Puesto reactivado exitosamente." else null,
                    errorMessage = if (result.isFailure) "Error al reactivar: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}