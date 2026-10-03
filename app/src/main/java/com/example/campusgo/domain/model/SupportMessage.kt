package com.example.campusgo.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportMessage(
    @SerialName("id") val id: String = "",
    @SerialName("ticket_id") val ticketId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
