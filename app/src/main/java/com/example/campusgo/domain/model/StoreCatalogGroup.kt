package com.example.campusgo.domain.model

data class StoreCatalogGroup(
    val sellerId: String,
    val sellerName: String,
    val location: String?,
    val bannerUrl: String?,
    val avatarUrl: String?,
    val businessStatus: String,
    val openTime: String?,
    val closeTime: String?,
    val description: String?,
    val acceptingOrders: Boolean,
    val products: List<Product>,
    val sellerProfile: UserProfile? = null,
    val businessCategory: String? = null,
    val phone: String = "",
    val ratingAverage: Double = 5.0,
    val supportedMeetingPoints: List<String> = emptyList(),
    val supportedPaymentMethods: List<String> = listOf("EFECTIVO", "YAPE", "PLIN")
)
