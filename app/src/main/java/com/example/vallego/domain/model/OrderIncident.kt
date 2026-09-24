package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class OrderIncident(
    @SerialName("id") val id: String = "",
    @SerialName("sub_order_id") val subOrderId: String? = null,
    @SerialName("reporter_id") val reporterId: String? = null,
    @SerialName("reported_user_id") val reportedUserId: String? = null,
    @SerialName("incident_type") val incidentType: String = "NO_SHOW_BUYER",
    @SerialName("details") val details: String? = null,
    @SerialName("status") val status: String = "PENDIENTE",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("resolved_by") val resolvedBy: String? = null,
    @SerialName("resolution_action") val resolutionAction: String? = null,
    @SerialName("admin_notes") val adminNotes: String? = null
) {
    val isPending: Boolean
        get() = status.equals("PENDIENTE", ignoreCase = true)

    val isResolved: Boolean
        get() = status.equals("RESUELTO", ignoreCase = true) ||
                status.equals("SANCIONADO", ignoreCase = true) ||
                status.equals("DESCARTADO", ignoreCase = true)

    val displayIncidentTitle: String
        get() = when (incidentType) {
            "NO_SHOW_BUYER" -> "Comprador no se presentó al punto"
            "NO_SHOW_SELLER" -> "Vendedor no se presentó al punto"
            "WRONG_DAMAGED_PRODUCT" -> "Producto vencido o en mal estado"
            "UNAUTHORIZED_CHARGE" -> "Cobro indebido o alteración de precio"
            "INAPPROPRIATE_BEHAVIOR" -> "Conducta inapropiada / falta de respeto"
            "STORE_UNAVAILABLE" -> "Puesto cerrado / no atiende pedidos"
            "SCAM_SUSPICION" -> "Sospecha de estafa o suplantación"
            "CANCELADO_VENDEDOR" -> "Cancelado unilateralmente por vendedor"
            else -> incidentType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
}

