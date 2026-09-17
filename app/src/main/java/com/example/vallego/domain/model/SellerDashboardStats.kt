package com.example.vallego.domain.model

data class TopProductStat(
    val productName: String,
    val unitsSold: Int,
    val totalAmount: Double
)

data class HourlyDemandStat(
    val slot: String,
    val orderCount: Int
)

data class MeetingPointStat(
    val pointName: String,
    val deliveryCount: Int
)

data class SellerDashboardStats(
    val totalEarnings: Double = 0.0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0,
    val inProgressCount: Int = 0,
    val totalOrdersCount: Int = 0,
    val averageTicket: Double = 0.0,
    val topProducts: List<TopProductStat> = emptyList(),
    val hourlyDistribution: List<HourlyDemandStat> = emptyList(),
    val topMeetingPoints: List<MeetingPointStat> = emptyList()
)
