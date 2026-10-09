package com.example.campusgo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.OrderStatus
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.domain.model.SubOrder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val buyerId: String,
    val buyerName: String,
    val meetingPointId: String,
    val meetingPointName: String,
    val scheduledTime: String,
    val totalAmount: Double,
    val status: String,
    val paymentMethod: String?,
    val notes: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val subOrdersJson: String = "[]"
) {
    fun toDomain(): Order {
        val parsedSubOrders = try {
            Json.decodeFromString<List<SubOrder>>(subOrdersJson)
        } catch (_: Exception) {
            emptyList()
        }
        val parsedStatus = try {
            OrderStatus.valueOf(status)
        } catch (_: Exception) {
            OrderStatus.PENDIENTE
        }
        val parsedPayment = paymentMethod?.let {
            try {
                PaymentMethod.valueOf(it)
            } catch (_: Exception) {
                PaymentMethod.EFECTIVO
            }
        }

        return Order(
            id = id,
            buyerId = buyerId,
            buyerName = buyerName,
            meetingPointId = meetingPointId,
            meetingPointName = meetingPointName,
            scheduledTime = scheduledTime,
            totalAmount = totalAmount,
            status = parsedStatus,
            subOrders = parsedSubOrders,
            paymentMethod = parsedPayment,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

fun Order.toEntity(): OrderEntity {
    val json = try {
        Json.encodeToString(subOrders)
    } catch (_: Exception) {
        "[]"
    }

    return OrderEntity(
        id = id,
        buyerId = buyerId,
        buyerName = buyerName,
        meetingPointId = meetingPointId,
        meetingPointName = meetingPointName,
        scheduledTime = scheduledTime,
        totalAmount = totalAmount,
        status = status.name,
        paymentMethod = paymentMethod?.name,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        subOrdersJson = json
    )
}
