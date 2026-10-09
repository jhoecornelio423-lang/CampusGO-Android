package com.example.campusgo.domain.usecase

import com.example.campusgo.domain.repository.AdminRepository

class ProcessSellerApplicationUseCase(
    private val adminRepository: AdminRepository
) {
    suspend fun approve(
        applicationId: String,
        adminId: String? = null,
        category: String? = null,
        addToGlobalCategories: Boolean = false
    ): Result<Unit> {
        if (applicationId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de solicitud inválido."))
        }
        return adminRepository.approveSellerApplication(
            applicationId = applicationId,
            adminId = adminId,
            category = category,
            addToGlobalCategories = addToGlobalCategories
        )
    }

    suspend fun reject(applicationId: String, reason: String): Result<Unit> {
        if (applicationId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de solicitud inválido."))
        }
        val trimmedReason = reason.trim()
        if (trimmedReason.isBlank()) {
            return Result.failure(IllegalArgumentException("Debe indicar un motivo de rechazo."))
        }
        return adminRepository.rejectSellerApplication(applicationId, trimmedReason)
    }
}
