package com.example.campusgo.domain.repository

import com.example.campusgo.domain.model.CartCalculationResult
import com.example.campusgo.domain.model.CartItem
import com.example.campusgo.domain.model.Product
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val items: StateFlow<List<CartItem>>
    val cartCalculation: StateFlow<CartCalculationResult>

    fun addToCart(product: Product, quantity: Int = 1)
    fun updateQuantity(productId: String, quantity: Int)
    fun removeFromCart(productId: String)
    fun clearCart()
    fun setStoreName(sellerId: String, name: String)
}