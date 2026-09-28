package com.example.campusgo.domain.usecase

import com.example.campusgo.domain.model.CartCalculationResult
import com.example.campusgo.domain.model.CartItem
import com.example.campusgo.domain.model.StoreCartGroup

class CalculateCartUseCase {
    operator fun invoke(items: List<CartItem>, storeNamesMap: Map<String, String> = emptyMap()): CartCalculationResult {
        val groupedItems = items.groupBy { it.product.sellerId }

        val storeCartGroups = groupedItems.map { (sellerId, sellerItems) ->
            val sellerName = storeNamesMap[sellerId] ?: "Emprendedor CampusGO"
            val subtotal = sellerItems.sumOf { it.subtotal }
            StoreCartGroup(
                sellerId = sellerId,
                sellerName = sellerName,
                items = sellerItems,
                subtotal = subtotal
            )
        }

        return CartCalculationResult(storeGroups = storeCartGroups)
    }
}