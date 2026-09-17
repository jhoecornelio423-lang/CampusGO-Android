package com.example.vallego.features.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.repository.CartRepository

class OrderTrackingViewModel(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState = _uiState.asStateFlow()

    fun initialize(buyerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            orderRepository.observeOrdersForBuyer(buyerId).collect { buyerOrders ->
                _uiState.update {
                    it.copy(
                        orders = buyerOrders,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            val reviewsResult = orderRepository.getBuyerReviews(buyerId)
            reviewsResult.onSuccess { reviewsMap ->
                _uiState.update { it.copy(reviewedOrders = reviewsMap) }
            }
        }
    }

    fun setSelectedTab(tab: TrackingTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openCancelDialog(order: Order) {
        _uiState.update { it.copy(orderToCancel = order) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(orderToCancel = null) }
    }

    fun confirmCancelOrder(orderId: String) {
        _uiState.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            val result = orderRepository.cancelOrderByBuyer(orderId)
            _uiState.update { it.copy(isCancelling = false, orderToCancel = null) }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(successMessage = "Pedido cancelado con éxito. El stock fue liberado.")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al cancelar el pedido.")
                }
            }
        }
    }

    fun repeatOrder(order: Order, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            order.subOrders.forEach { subOrder ->
                if (subOrder.sellerName.isNotBlank()) {
                    cartRepository.setStoreName(subOrder.sellerId, subOrder.sellerName)
                }
                subOrder.items.forEach { item ->
                    val product = Product(
                        id = item.productId,
                        sellerId = subOrder.sellerId,
                        name = item.productName,
                        price = item.unitPrice,
                        stock = 99
                    )
                    cartRepository.addToCart(product, item.quantity)
                }
            }
            _uiState.update { it.copy(successMessage = "¡Productos agregados al carrito de compras!") }
            onCompleted()
        }
    }

    fun expirePendingOrders() {
        viewModelScope.launch {
            orderRepository.expirePendingSuborders()
        }
    }

    fun openRateDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(subOrderToRate = subOrder) }
    }

    fun dismissRateDialog() {
        _uiState.update { it.copy(subOrderToRate = null) }
    }

    fun submitReview(
        buyerId: String,
        orderId: String,
        sellerId: String,
        rating: Int,
        comment: String? = null
    ) {
        _uiState.update { it.copy(isSubmittingReview = true) }
        viewModelScope.launch {
            val result = orderRepository.submitSellerReview(
                orderId = orderId,
                buyerId = buyerId,
                sellerId = sellerId,
                rating = rating,
                comment = comment
            )
            _uiState.update { current ->
                val updatedMap = current.reviewedOrders.toMutableMap()
                val sub = current.subOrderToRate
                updatedMap["$orderId-$sellerId"] = rating
                updatedMap[orderId] = rating
                if (sub != null) {
                    updatedMap[sub.id] = rating
                }
                current.copy(
                    isSubmittingReview = false,
                    subOrderToRate = null,
                    reviewedOrders = updatedMap,
                    successMessage = if (result.isSuccess) "¡Gracias por calificar al vendedor!" else "Calificación guardada."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}