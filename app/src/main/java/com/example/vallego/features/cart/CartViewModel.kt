package com.example.vallego.features.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.usecase.CreateOrderWithSubordersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.vallego.domain.repository.AdminRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val createOrderWithSubordersUseCase: CreateOrderWithSubordersUseCase,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        val schedule = generateDeliverySchedule()
        _uiState.update {
            it.copy(
                availableTimeSlots = schedule.slots,
                selectedTimeSlot = schedule.slots.firstOrNull() ?: "Hoy 12:00",
                isCampusClosedNow = schedule.isCampusClosedNow,
                deliveryScheduleNote = schedule.infoMessage
            )
        }
        viewModelScope.launch {
            cartRepository.cartCalculation.collect { calculation ->
                _uiState.update { it.copy(calculation = calculation) }
            }
        }
        viewModelScope.launch {
            adminRepository.refreshMeetingPoints()
            adminRepository.refreshSellers()
        }
        viewModelScope.launch {
            combine(
                cartRepository.items,
                adminRepository.observeMeetingPoints(),
                adminRepository.observeSellers()
            ) { items, allPoints, sellers ->
                val activePoints = allPoints.filter { it.isActive }
                val sellerIdsInCart = items.map { it.product.sellerId }.filter { it.isNotBlank() }.distinct()

                val (filteredPoints, warning) = if (items.isEmpty()) {
                    emptyList<CampusMeetingPoint>() to null
                } else if (sellerIdsInCart.isEmpty()) {
                    emptyList<CampusMeetingPoint>() to "No se pudo identificar el puesto comercial."
                } else {
                    val sellerPointSets = sellerIdsInCart.map { sId ->
                        val seller = sellers.find { it.id == sId }
                        seller?.supportedMeetingPoints?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                    }
                    val commonIds = if (sellerPointSets.isNotEmpty()) {
                        sellerPointSets.reduce { acc, set -> acc.intersect(set) }
                    } else emptySet()

                    val matchingPoints = activePoints.filter { it.id in commonIds }
                    val warn = if (matchingPoints.isEmpty() && sellerIdsInCart.size > 1) {
                        "Los puestos seleccionados no coinciden en un punto de entrega común. Te sugerimos realizar pedidos separados para coordinar cada entrega."
                    } else if (matchingPoints.isEmpty()) {
                        "El puesto seleccionado aún no tiene puntos de entrega autorizados por el vendedor."
                    } else {
                        null
                    }
                    matchingPoints to warn
                }

                // Cálculo de métodos de pago comunes aceptados por los puestos del carrito
                val (filteredPayments, paymentWarn) = if (items.isEmpty() || sellerIdsInCart.isEmpty()) {
                    listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO) to null
                } else {
                    val sellerPaymentSets = sellerIdsInCart.map { sId ->
                        val seller = sellers.find { it.id == sId }
                        seller?.effectivePaymentMethods?.mapNotNull { str ->
                            when (str.uppercase()) {
                                "YAPE" -> PaymentMethod.YAPE
                                "PLIN" -> PaymentMethod.PLIN
                                "EFECTIVO" -> PaymentMethod.EFECTIVO
                                else -> null
                            }
                        }?.toSet() ?: setOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO)
                    }
                    val commonMethods = if (sellerPaymentSets.isNotEmpty()) {
                        sellerPaymentSets.reduce { acc, set -> acc.intersect(set) }
                    } else emptySet()

                    val sortedList = listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO).filter { it in commonMethods }
                    val pWarn = if (sortedList.isEmpty() && sellerIdsInCart.size > 1) {
                        "Los puestos en tu carrito no aceptan un método de pago en común. Te sugerimos realizar pedidos separados."
                    } else if (sortedList.isEmpty()) {
                        "El vendedor no tiene métodos de pago disponibles configurados."
                    } else {
                        null
                    }
                    sortedList to pWarn
                }

                _uiState.update { current ->
                    val updatedSelection = if (current.selectedMeetingPoint != null && filteredPoints.any { it.id == current.selectedMeetingPoint.id }) {
                        current.selectedMeetingPoint
                    } else {
                        filteredPoints.firstOrNull()
                    }
                    val updatedPayment = if (filteredPayments.contains(current.selectedPaymentMethod)) {
                        current.selectedPaymentMethod
                    } else {
                        filteredPayments.firstOrNull() ?: PaymentMethod.EFECTIVO
                    }
                    current.copy(
                        meetingPoints = filteredPoints,
                        selectedMeetingPoint = updatedSelection,
                        meetingPointWarning = warning,
                        availablePaymentMethods = filteredPayments,
                        selectedPaymentMethod = updatedPayment,
                        paymentMethodWarning = paymentWarn
                    )
                }
            }.collect {}
        }
    }

    fun refreshMeetingPoints() {
        viewModelScope.launch {
            adminRepository.refreshMeetingPoints()
            adminRepository.refreshSellers()
        }
    }

    fun incrementItem(productId: String) {
        val currentItems = cartRepository.items.value
        val item = currentItems.firstOrNull { it.product.id == productId } ?: return
        cartRepository.updateQuantity(productId, item.quantity + 1)
    }

    fun decrementItem(productId: String) {
        val currentItems = cartRepository.items.value
        val item = currentItems.firstOrNull { it.product.id == productId } ?: return
        cartRepository.updateQuantity(productId, item.quantity - 1)
    }

    fun removeItem(productId: String) {
        cartRepository.removeFromCart(productId)
    }

    fun clearCart() {
        cartRepository.clearCart()
    }

    fun selectMeetingPoint(point: CampusMeetingPoint) {
        _uiState.update { it.copy(selectedMeetingPoint = point) }
    }

    fun selectTimeSlot(slot: String) {
        _uiState.update { it.copy(selectedTimeSlot = slot) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(orderNotes = notes) }
    }

    fun clearPlacedOrder() {
        _uiState.update { it.copy(placedOrder = null, errorMessage = null) }
    }

    fun confirmOrder(buyerProfile: UserProfile) {
        val state = _uiState.value
        if (!state.canCheckout || state.isSubmitting) return
        val point = state.selectedMeetingPoint ?: return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val order = createOrderWithSubordersUseCase(
                    buyerProfile = buyerProfile,
                    meetingPoint = point,
                    scheduledTime = state.selectedTimeSlot,
                    paymentMethod = state.selectedPaymentMethod,
                    cartResult = state.calculation,
                    notes = state.orderNotes.takeIf { it.isNotBlank() }
                )

                orderRepository.placeOrder(order)
                    .onSuccess { placed ->
                        cartRepository.clearCart()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                placedOrder = placed
                            )
                        }
                    }
                    .onFailure { err ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = err.localizedMessage ?: "Error al confirmar pedido"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage ?: "Error inesperado"
                    )
                }
            }
        }
    }
}
