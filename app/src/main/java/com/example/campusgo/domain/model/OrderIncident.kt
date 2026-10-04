package com.example.campusgo.domain.model

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
    @SerialName("admin_notes") val adminNotes: String? = null,
    @SerialName("evidence_url") val evidenceUrl: String? = null
) {
    val isPending: Boolean
        get() = status.equals("PENDIENTE", ignoreCase = true)

    val isResolved: Boolean
        get() = status.equals("RESUELTO", ignoreCase = true) ||
                status.equals("SANCIONADO", ignoreCase = true) ||
                status.equals("DESCARTADO", ignoreCase = true)

    val displayIncidentTitle: String
        get() = formatIncidentType(incidentType)
}

fun formatIncidentType(rawType: String?): String {
    if (rawType.isNullOrBlank()) return "Incidencia de mediación"
    var clean = rawType.trim()
    while (clean.contains(":") && (
            clean.startsWith("Incidencia", ignoreCase = true) ||
            clean.startsWith("Reclamo", ignoreCase = true) ||
            clean.startsWith("Caso", ignoreCase = true) ||
            clean.startsWith("Reporte", ignoreCase = true))) {
        clean = clean.substringAfter(":").trim()
    }
    clean = clean.replace(Regex("Caso\\s*#?\\d+", RegexOption.IGNORE_CASE), "").trim()
    clean = clean.trim('-', ':', '•', ' ')

    val normalized = clean.uppercase().replace(" ", "_")
    return when {
        normalized.contains("NO_SHOW_BUYER") || normalized.contains("COMPRADOR_NO_SE_PRESENT") -> "Comprador ausente en punto de entrega"
        normalized.contains("NO_SHOW_SELLER") || normalized.contains("VENDEDOR_NO_SE_PRESENT") -> "Vendedor ausente en punto de entrega"
        normalized.contains("WRONG_DAMAGED_PRODUCT") || normalized.contains("MAL_ESTADO") || normalized.contains("VENCIDO") -> "Producto vencido o en mal estado"
        normalized.contains("UNAUTHORIZED_CHARGE") || normalized.contains("COBRO_INDEBIDO") || normalized.contains("ALTERACION_PRECIO") -> "Cobro indebido o alteración de precio"
        normalized.contains("INAPPROPRIATE_BEHAVIOR") || normalized.contains("CONDUCTA_INAPROPIADA") -> "Conducta inapropiada / falta de respeto"
        normalized.contains("STORE_UNAVAILABLE") || normalized.contains("PUESTO_CERRADO") || normalized.contains("NO_ATIENDE") -> "Puesto inactivo / no atiende pedidos"
        normalized.contains("SCAM_SUSPICION") || normalized.contains("ESTAFA") -> "Sospecha de estafa o suplantación"
        normalized.contains("CANCELADO_VENDEDOR") -> "Cancelado unilateralmente por vendedor"
        else -> {
            if (clean.any { it.isLowerCase() }) clean
            else clean.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

