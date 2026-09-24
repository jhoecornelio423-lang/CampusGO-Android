package com.example.vallego.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BuyerOrderStats(
    val totalOrders: Int = 0,
    val completedOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val inProgressOrders: Int = 0,
    val totalSpent: Double = 0.0
)
