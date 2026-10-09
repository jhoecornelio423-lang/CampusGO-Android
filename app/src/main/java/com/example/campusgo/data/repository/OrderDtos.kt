package com.example.campusgo.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteOrderDto(
    val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("delivery_place") val deliveryPlace: String? = null,
    @SerialName("meeting_point_id") val meetingPointId: String? = null,
    @SerialName("meeting_point_name") val meetingPointName: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null,
    val notes: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val status: String = "pending",
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteSubOrderDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("subtotal_amount") val subtotalAmount: Double,
    val status: String = "pending",
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("is_payment_confirmed") val isPaymentConfirmed: Boolean = false,
    @SerialName("is_delivery_confirmed") val isDeliveryConfirmed: Boolean = false,
    @SerialName("delivery_code") val deliveryCode: String? = null,
    @SerialName("stock_reserved") val stockReserved: Boolean = true,
    @SerialName("meeting_point_id") val meetingPointId: String? = null,
    @SerialName("meeting_point_name") val meetingPointName: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null,
    @SerialName("buyer_id") val buyerId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteOrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    @SerialName("price_at_sale") val priceAtSale: Double
)

@Serializable
data class ProductBasicDto(
    val id: String,
    val name: String,
    val price: Double? = null,
    val stock: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class ProfileBasicDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("business_description") val businessDescription: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class CheckoutResponseDto(
    val success: Boolean = false,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    val status: String? = null,
    @SerialName("is_duplicate") val isDuplicate: Boolean? = null,
    val message: String? = null
)

@Serializable
data class RpcActionResultDto(
    val success: Boolean = false,
    val message: String? = null,
    @SerialName("expired_count") val expiredCount: Int? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    val status: String? = null
)

@Serializable
data class UpdateSuborderStatusResponseDto(
    val success: Boolean = false,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    val status: String? = null,
    @SerialName("stock_released") val stockReleased: Boolean? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
data class RemoteReviewDto(
    val id: String? = null,
    @SerialName("order_id") val orderId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewee_id") val revieweeId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BuyerProfileCheckDto(
    val id: String,
    val role: String = ""
)
