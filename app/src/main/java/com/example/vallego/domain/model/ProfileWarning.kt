package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ProfileWarning(
    @SerialName("id") val id: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("reason") val reason: String,
    @SerialName("ticket_id") val ticketId: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val formattedDate: String
        get() {
            if (createdAt.isNullOrBlank()) return "Reciente"
            return try {
                createdAt.substringBefore("T")
            } catch (e: Exception) {
                "Reciente"
            }
        }
}
