package com.example.campusgo.core.di

import com.example.campusgo.domain.usecase.*
import org.koin.dsl.module

val useCaseModule = module {
    single { CalculateCartUseCase() }
    single { CreateOrderWithSubordersUseCase() }
    single { RecalculateOrderUseCase() }
    single { GetActiveCampusCatalogUseCase(productRepository = get()) }
    single { UpdateSubOrderStatusUseCase(orderRepository = get()) }
    single { ManageProductStockUseCase(productRepository = get()) }
    single { ProcessSellerApplicationUseCase(adminRepository = get()) }
    single { ModerateIncidentUseCase(adminRepository = get()) }
}
