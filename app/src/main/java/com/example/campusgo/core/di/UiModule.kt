package com.example.campusgo.core.di

import com.example.campusgo.features.admin.AdminViewModel
import com.example.campusgo.features.auth.AuthViewModel
import com.example.campusgo.features.cart.CartViewModel
import com.example.campusgo.features.chat.OrderChatViewModel
import com.example.campusgo.features.seller.SellerDashboardViewModel
import com.example.campusgo.features.tracking.OrderTrackingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::SellerDashboardViewModel)
    viewModelOf(::OrderTrackingViewModel)
    viewModelOf(::AdminViewModel)
    viewModelOf(::OrderChatViewModel)
}
