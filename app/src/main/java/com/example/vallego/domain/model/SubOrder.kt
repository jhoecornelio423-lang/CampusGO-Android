package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SubOrderItem(
    @SerialName("id") val id: String,
    @SerialName("sub_order_id") val subOrderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("unit_price") val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)

@Serializable
data class SubOrder(
    @SerialName("id") val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("items") val items: List<SubOrderItem> = emptyList(),
    @SerialName("subtotal_amount") val subtotalAmount: Double = 0.0,
    @SerialName("status") val status: SubOrderStatus = SubOrderStatus.PENDIENTE,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("payment_method") val paymentMethod: PaymentMethod? = null,
    @SerialName("meeting_point_id") val meetingPointId: String? = null,
    @SerialName("meeting_point_name") val meetingPointName: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null,
    @SerialName("buyer_id") val buyerId: String? = null,
    @SerialName("buyer_name") val buyerName: String? = null,
    @SerialName("buyer_phone") val buyerPhone: String? = null,
    @SerialName("buyer_avatar_url") val buyerAvatarUrl: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("is_payment_confirmed") val isPaymentConfirmed: Boolean = false,
    @SerialName("is_delivery_confirmed") val isDeliveryConfirmed: Boolean = false,
    @SerialName("delivery_code") val deliveryCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Código de 4 dígitos de verificación para entrega segura en campus.
 * Si no está grabado en la base de datos, se genera de forma determinística y consistente
 * a partir del identificador único del subpedido (1000..9999).
 */
val SubOrder.verificationCode: String
    get() = deliveryCode?.takeIf { it.isNotBlank() }
        ?: (kotlin.math.abs(id.hashCode()) % 9000 + 1000).toString()