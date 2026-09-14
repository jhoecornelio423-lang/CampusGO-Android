package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class ApplicationStatus {
    PENDIENTE,
    APROBADA,
    RECHAZADA
}

@Serializable
data class SellerApplication(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val applicantName: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("business_name") val storeName: String = "",
    @SerialName("business_category") val category: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("proposed_location") val proposedLocation: String? = null,
    @SerialName("status") val status: ApplicationStatus = ApplicationStatus.PENDIENTE,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val studentEmail: String get() = ""
}