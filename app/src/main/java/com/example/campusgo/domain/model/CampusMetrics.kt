package com.example.campusgo.domain.model

data class CampusMetrics(
    val totalOrdersToday: Int = 0,
    val totalSalesToday: Double = 0.0,
    val activeSellersCount: Int = 0,
    val activeMeetingPointsCount: Int = 0,
    val pendingApplicationsCount: Int = 0
)