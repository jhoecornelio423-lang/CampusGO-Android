package com.example.vallego.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MetricsPeriod(val label: String, val apiValue: String) {
    HOY("Hoy", "today"),
    SEMANA("Esta Semana", "week"),
    MES("Este Mes", "month"),
    HISTORICO("Histórico Total", "all")
}

@Serializable
data class SellerSalesRanking(
    @SerialName("seller_id") val sellerId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("total_sales") val totalSales: Double = 0.0,
    @SerialName("completed_orders") val completedOrders: Int = 0,
    @SerialName("percentage") val percentage: Double = 0.0
)

@Serializable
data class PaymentMethodBreakdown(
    @SerialName("method") val method: String,
    @SerialName("count") val count: Int = 0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("percentage") val percentage: Double = 0.0
)

@Serializable
data class MeetingPointTraffic(
    @SerialName("point_name") val pointName: String,
    @SerialName("count") val count: Int = 0,
    @SerialName("percentage") val percentage: Double = 0.0
)

@Serializable
data class CampusDetailedMetrics(
    @SerialName("period") val period: String = "today",
    @SerialName("total_sales") val totalSales: Double = 0.0,
    @SerialName("total_orders") val totalOrders: Int = 0,
    @SerialName("completed_orders") val completedOrders: Int = 0,
    @SerialName("cancelled_orders") val cancelledOrders: Int = 0,
    @SerialName("average_ticket") val averageTicket: Double = 0.0,
    @SerialName("fulfillment_rate") val fulfillmentRate: Double = 100.0,
    @SerialName("seller_rankings") val sellerRankings: List<SellerSalesRanking> = emptyList(),
    @SerialName("payment_methods") val paymentMethods: List<PaymentMethodBreakdown> = emptyList(),
    @SerialName("top_meeting_points") val topMeetingPoints: List<MeetingPointTraffic> = emptyList()
)
