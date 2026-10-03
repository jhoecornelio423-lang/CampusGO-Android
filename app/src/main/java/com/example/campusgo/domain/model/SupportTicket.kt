package com.example.campusgo.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportTicket(
    @SerialName("id") val id: String = "",
    @SerialName("ticket_number") val ticketNumber: Int = 0,
    @SerialName("user_id") val userId: String = "",
    @SerialName("incident_id") val incidentId: String? = null,
    @SerialName("subject") val subject: String = "",
    @SerialName("status") val status: String = "ABIERTO",
    @SerialName("assigned_admin_id") val assignedAdminId: String? = null,
    @SerialName("admin_notes") val adminNotes: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isOpen: Boolean
        get() = !status.equals("RESUELTO", ignoreCase = true) && !status.equals("CERRADO", ignoreCase = true)

    val displayStatusText: String
        get() = when (status.uppercase()) {
            "ABIERTO" -> "En Revisión"
            "EN_PROCESO" -> "En Diálogo"
            "RESUELTO" -> "Resuelto"
            "CERRADO" -> "Cerrado"
            else -> status
        }
}
