package com.example.campusgo.domain.usecase

import com.example.campusgo.domain.repository.AdminRepository

class ModerateIncidentUseCase(
    private val adminRepository: AdminRepository
) {
    suspend fun resolve(
        incidentId: String,
        status: String = "RESUELTO",
        action: String? = null,
        adminNotes: String? = null
    ): Result<Unit> {
        if (incidentId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de incidencia inválido."))
        }
        return adminRepository.resolveIncident(
            incidentId = incidentId,
            status = status,
            action = action,
            adminNotes = adminNotes
        )
    }

    suspend fun issueWarning(
        userId: String,
        reason: String,
        createdBy: String? = null
    ): Result<Unit> {
        if (userId.isBlank() || reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de advertencia incompletos."))
        }
        return adminRepository.issueWarning(userId, reason.trim(), createdBy)
    }

    suspend fun suspendUser(
        userId: String,
        isSeller: Boolean,
        reason: String? = null
    ): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de usuario inválido."))
        }
        return if (isSeller) {
            adminRepository.toggleSellerSuspension(userId, isSuspended = true, reason = reason)
        } else {
            adminRepository.toggleBuyerSuspension(userId, isSuspended = true, reason = reason)
        }
    }
}
