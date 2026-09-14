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
    @SerialName("created_at") val createdAt: String? = null
)
