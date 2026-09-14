package com.example.vallego.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AdminViewModel(
    private val adminRepository: AdminRepository
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
                _uiState.update { it.copy(sellers = sellers) }
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
    }

    fun setTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleMeetingPoint(pointId: String, currentActive: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleMeetingPoint(pointId, !currentActive)
        }
    }

    fun openCreateMeetingPointDialog() {
        _uiState.update { it.copy(showCreateMeetingPointDialog = true) }
    }

    fun dismissCreateMeetingPointDialog() {
        _uiState.update { it.copy(showCreateMeetingPointDialog = false) }
    }

    fun createMeetingPoint(name: String, pavilion: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val point = CampusMeetingPoint(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                pavilion = pavilion.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                isActive = true
            )
            adminRepository.createMeetingPoint(point)
            dismissCreateMeetingPointDialog()
        }
    }

    fun approveApplication(applicationId: String, adminId: String? = null) {
        viewModelScope.launch {
            adminRepository.approveSellerApplication(applicationId, adminId)
            _uiState.update { it.copy(successMessage = "Solicitud aprobada con éxito. El usuario ahora es emprendedor.") }
        }
    }

    fun openRejectionDialog(application: SellerApplication) {
        _uiState.update { it.copy(selectedApplicationForRejection = application) }
    }

    fun dismissRejectionDialog() {
        _uiState.update { it.copy(selectedApplicationForRejection = null) }
    }

    fun confirmRejection(applicationId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.rejectSellerApplication(applicationId, reason)
            dismissRejectionDialog()
            _uiState.update { it.copy(successMessage = "Solicitud rechazada con motivo registrado.") }
        }
    }

    fun openSuspensionDialog(seller: UserProfile) {
        _uiState.update { it.copy(selectedSellerForSuspension = seller) }
    }

    fun dismissSuspensionDialog() {
        _uiState.update { it.copy(selectedSellerForSuspension = null) }
    }

    fun confirmSellerSuspension(sellerId: String, reason: String) {
        viewModelScope.launch {
            val result = adminRepository.toggleSellerSuspension(sellerId, isSuspended = true, reason = reason)
            dismissSuspensionDialog()
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "Puesto suspendido temporalmente.") }
            } else {
                _uiState.update { it.copy(errorMessage = "Error al suspender: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun reactivateSeller(sellerId: String) {
        viewModelScope.launch {
            val result = adminRepository.toggleSellerSuspension(sellerId, isSuspended = false)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "Puesto reactivado exitosamente.") }
            } else {
                _uiState.update { it.copy(errorMessage = "Error al reactivar: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}