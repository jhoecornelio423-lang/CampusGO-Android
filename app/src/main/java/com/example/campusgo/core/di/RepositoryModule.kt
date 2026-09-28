package com.example.campusgo.core.di

import com.example.campusgo.data.repository.AdminRepositoryImpl
import com.example.campusgo.data.repository.AuthRepositoryImpl
import com.example.campusgo.data.repository.CartRepositoryImpl
import com.example.campusgo.data.repository.OrderRepositoryImpl
import com.example.campusgo.domain.repository.AdminRepository
import com.example.campusgo.domain.repository.AuthRepository
import com.example.campusgo.domain.repository.CartRepository
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.domain.usecase.CalculateCartUseCase
import com.example.campusgo.domain.usecase.CreateOrderWithSubordersUseCase
import com.example.campusgo.domain.usecase.RecalculateOrderUseCase
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> {
        AuthRepositoryImpl(
            auth = get(),
            postgrest = get()
        )
    }

    single { CalculateCartUseCase() }
    single { CreateOrderWithSubordersUseCase() }
    single { RecalculateOrderUseCase() }

    single<CartRepository> {
        CartRepositoryImpl(
            calculateCartUseCase = get()
        )
    }

    single<OrderRepository> {
        OrderRepositoryImpl(
            postgrest = get(),
            recalculateOrderUseCase = get()
        )
    }

    single<AdminRepository> {
        AdminRepositoryImpl(
            postgrest = get(),
            orderRepository = get()
        )
    }

    single<com.example.campusgo.domain.repository.ProductRepository> {
        com.example.campusgo.data.repository.ProductRepositoryImpl(
            postgrest = get(),
            auth = get(),
            storage = getOrNull()
        )
    }

    single<com.example.campusgo.domain.repository.ChatRepository> {
        com.example.campusgo.data.repository.ChatRepositoryImpl(
            postgrest = get(),
            realtime = getOrNull()
        )
    }
}
