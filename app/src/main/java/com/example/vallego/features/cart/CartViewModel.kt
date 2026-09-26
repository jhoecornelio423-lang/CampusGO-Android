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

                val sellerMeetingPointsMap = mutableMapOf<String, List<CampusMeetingPoint>>()
                val sellerPaymentMethodsMap = mutableMapOf<String, List<PaymentMethod>>()

                sellerIdsInCart.forEach { sId ->
                    val seller = sellers.find { it.id == sId }
                    val sPointIds = seller?.supportedMeetingPoints?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                    val sPoints = if (sPointIds.isNotEmpty()) {
                        activePoints.filter { it.id in sPointIds }
                    } else {
                        activePoints
                    }
                    sellerMeetingPointsMap[sId] = if (sPoints.isNotEmpty()) sPoints else activePoints

                    val sMethods = seller?.effectivePaymentMethods?.mapNotNull { str ->
                        when (str.uppercase()) {
                            "YAPE" -> PaymentMethod.YAPE
                            "PLIN" -> PaymentMethod.PLIN
                            "EFECTIVO" -> PaymentMethod.EFECTIVO
                            else -> null
                        }
                    }?.takeIf { it.isNotEmpty() } ?: listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO)
                    sellerPaymentMethodsMap[sId] = sMethods
                }

                // Cálculo de puntos de entrega comunes (intersección)
                val (filteredPoints, warning) = if (items.isEmpty()) {
                    emptyList<CampusMeetingPoint>() to null
                } else if (sellerIdsInCart.isEmpty()) {
                    emptyList<CampusMeetingPoint>() to "No se pudo identificar el puesto comercial."
                } else {
                    val sellerPointSets = sellerIdsInCart.map { sId ->
                        sellerMeetingPointsMap[sId]?.map { it.id }?.toSet() ?: emptySet()
                    }
                    val commonIds = if (sellerPointSets.isNotEmpty()) {
                        sellerPointSets.reduce { acc, set -> acc.intersect(set) }
                    } else emptySet()

                    val matchingPoints = activePoints.filter { it.id in commonIds }
                    val warn = if (matchingPoints.isEmpty() && sellerIdsInCart.size > 1) {
                        "Los puestos seleccionados entregan en diferentes puntos del campus. Se ha configurado la entrega independiente por puesto."
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
                        sellerPaymentMethodsMap[sId]?.toSet() ?: setOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO)
                    }
                    val commonMethods = if (sellerPaymentSets.isNotEmpty()) {
                        sellerPaymentSets.reduce { acc, set -> acc.intersect(set) }
                    } else emptySet()

                    val sortedList = listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO).filter { it in commonMethods }
                    val pWarn = if (sortedList.isEmpty() && sellerIdsInCart.size > 1) {
                        "Los puestos aceptan diferentes métodos de pago. Se ha configurado el pago independiente por cada vendedor."
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
                        filteredPoints.firstOrNull() ?: activePoints.firstOrNull()
                    }
                    val updatedPayment = if (filteredPayments.contains(current.selectedPaymentMethod)) {
                        current.selectedPaymentMethod
                    } else {
                        filteredPayments.firstOrNull() ?: PaymentMethod.EFECTIVO
                    }

                    // Actualizar mapa de puntos seleccionados por vendedor
                    val updatedSellerPoints = current.selectedMeetingPointsBySeller.toMutableMap()
                    sellerIdsInCart.forEach { sId ->
                        val validPoints = sellerMeetingPointsMap[sId] ?: activePoints
                        val prev = updatedSellerPoints[sId]
                        if (prev == null || !validPoints.any { it.id == prev.id }) {
                            updatedSellerPoints[sId] = validPoints.firstOrNull() ?: activePoints.firstOrNull() ?: CampusMeetingPoint(id = "mp-default", name = "Campus Principal", pavilion = "", description = null, campus = "", isActive = true)
                        }
                    }

                    // Actualizar mapa de métodos de pago seleccionados por vendedor
                    val updatedSellerPayments = current.selectedPaymentMethodsBySeller.toMutableMap()
                    sellerIdsInCart.forEach { sId ->
                        val validMethods = sellerPaymentMethodsMap[sId] ?: listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO)
                        val prev = updatedSellerPayments[sId]
                        if (prev == null || !validMethods.contains(prev)) {
                            updatedSellerPayments[sId] = validMethods.firstOrNull() ?: PaymentMethod.EFECTIVO
                        }
                    }

                    current.copy(
                        meetingPoints = filteredPoints,
                        selectedMeetingPoint = updatedSelection,
                        meetingPointWarning = warning,
                        availablePaymentMethods = filteredPayments,
                        selectedPaymentMethod = updatedPayment,
                        paymentMethodWarning = paymentWarn,
                        sellerMeetingPoints = sellerMeetingPointsMap,
                        selectedMeetingPointsBySeller = updatedSellerPoints,
                        sellerPaymentMethods = sellerPaymentMethodsMap,
                        selectedPaymentMethodsBySeller = updatedSellerPayments
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
        _uiState.update { current ->
            val updatedMap = current.selectedMeetingPointsBySeller.toMutableMap()
            current.sellerMeetingPoints.forEach { (sId, points) ->
                if (points.any { it.id == point.id }) {
                    updatedMap[sId] = point
                }
            }
            current.copy(selectedMeetingPoint = point, selectedMeetingPointsBySeller = updatedMap)
        }
    }

    fun selectSellerMeetingPoint(sellerId: String, point: CampusMeetingPoint) {
        _uiState.update { current ->
            val updatedMap = current.selectedMeetingPointsBySeller.toMutableMap()
            updatedMap[sellerId] = point
            current.copy(selectedMeetingPointsBySeller = updatedMap)
        }
    }

    fun selectTimeSlot(slot: String) {
        _uiState.update { it.copy(selectedTimeSlot = slot) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { current ->
            val updatedMap = current.selectedPaymentMethodsBySeller.toMutableMap()
            current.sellerPaymentMethods.forEach { (sId, methods) ->
                if (methods.contains(method)) {
                    updatedMap[sId] = method
                }
            }
            current.copy(selectedPaymentMethod = method, selectedPaymentMethodsBySeller = updatedMap)
        }
    }

    fun selectSellerPaymentMethod(sellerId: String, method: PaymentMethod) {
        _uiState.update { current ->
            val updatedMap = current.selectedPaymentMethodsBySeller.toMutableMap()
            updatedMap[sellerId] = method
            current.copy(selectedPaymentMethodsBySeller = updatedMap)
        }
    }

    fun setSplitDeliveryMode(isSplit: Boolean) {
        _uiState.update { it.copy(isSplitDeliveryMode = isSplit) }
    }

    fun setSplitPaymentMode(isSplit: Boolean) {
        _uiState.update { it.copy(isSplitPaymentMode = isSplit) }
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
        val point = state.selectedMeetingPoint ?: state.selectedMeetingPointsBySeller.values.firstOrNull() ?: return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val order = createOrderWithSubordersUseCase(
                    buyerProfile = buyerProfile,
                    meetingPoint = point,
                    scheduledTime = state.selectedTimeSlot,
                    paymentMethod = state.selectedPaymentMethod,
                    cartResult = state.calculation,
                    notes = state.orderNotes.takeIf { it.isNotBlank() },
                    meetingPointsBySeller = state.selectedMeetingPointsBySeller,
                    paymentMethodsBySeller = state.selectedPaymentMethodsBySeller
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
