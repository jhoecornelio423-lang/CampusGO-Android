package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ChatMessage(
    @SerialName("id") val id: String,
    @SerialName("sub_order_id") val subOrderId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    val isFromMe: Boolean = false
)
