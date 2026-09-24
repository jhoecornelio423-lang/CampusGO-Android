package com.example.vallego.features.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.vallego.domain.repository.AdminRepository
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SellerDashboardViewModel(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val adminRepository: AdminRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private var currentSellerId: String = ""
    private var currentPickupLocation: String = "Campus Los Olivos"

    fun initialize(sellerId: String, initialAcceptingOrders: Boolean, businessLocation: String? = null) {
        currentSellerId = sellerId
        if (!businessLocation.isNullOrBlank()) {
            currentPickupLocation = businessLocation
        }
        _uiState.update { it.copy(isAcceptingOrders = initialAcceptingOrders) }
        loadProducts()
        loadSellerProfile()

        if (adminRepository != null) {
            viewModelScope.launch {
                adminRepository.refreshMeetingPoints()
            }
            viewModelScope.launch {
                adminRepository.observeMeetingPoints().collect { points ->
                    _uiState.update { it.copy(availableMeetingPoints = points.filter { p -> p.isActive }) }
                }
            }
        }
        viewModelScope.launch {
            orderRepository.observeSubOrdersForSeller(sellerId).collect { orders ->
                val today = LocalDate.now(limaZone)
                val activeStatuses = setOf(
                    SubOrderStatus.PENDIENTE,
                    SubOrderStatus.ACEPTADO,
                    SubOrderStatus.EN_PREPARACION,
                    SubOrderStatus.LISTO,
                    SubOrderStatus.ESPERANDO_ENTREGA
                )

                val activeOrders = orders.filter { it.status in activeStatuses }
                val closedOrders = orders.filter { it.status !in activeStatuses }
                val closedGroupedByDate = closedOrders.groupBy { parseOrderLocalDate(it.createdAt) }

                val todayClosed = closedGroupedByDate[today].orEmpty()
                val todayCompleted = todayClosed.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
                val todayEarnings = todayCompleted.sumOf { it.subtotalAmount }

                val todayOrders = (activeOrders + todayClosed)
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt }

                val pastDates = closedGroupedByDate.keys.filter { it.isBefore(today) }.sortedDescending()
                val pastGroups = pastDates.map { date ->
                    val dayOrders = closedGroupedByDate[date].orEmpty()
                    val completed = dayOrders.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
                    val cancelled = dayOrders.filter {
                        it.status == SubOrderStatus.RECHAZADO ||
                        it.status == SubOrderStatus.CANCELADO ||
                        it.status == SubOrderStatus.NO_ENTREGADO
                    }
                    DailyOrderGroup(
                        date = date,
                        displayTitle = formatDayTitle(date, today),
                        isToday = false,
                        totalEarnings = completed.sumOf { it.subtotalAmount },
                        completedCount = completed.size,
                        cancelledCount = cancelled.size,
                        orders = dayOrders
                    )
                }

                _uiState.update {
                    it.copy(
                        subOrders = orders,
                        todayOrders = todayOrders,
                        pastDayGroups = pastGroups,
                        totalSubOrdersToday = todayOrders.size,
                        pendingCount = orders.count { s -> s.status == SubOrderStatus.PENDIENTE },
                        inPreparationCount = orders.count { s -> s.status == SubOrderStatus.ACEPTADO || s.status == SubOrderStatus.EN_PREPARACION },
                        readyCount = orders.count { s -> s.status == SubOrderStatus.LISTO || s.status == SubOrderStatus.ESPERANDO_ENTREGA },
                        completedCount = todayCompleted.size,
                        earningsToday = todayEarnings,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadSellerProfile() {
        if (currentSellerId.isBlank()) return
        viewModelScope.launch {
            productRepository.getSellerProfiles().onSuccess { profiles ->
                val myProfile = profiles.find { it.id == currentSellerId }
                if (myProfile != null) {
                    val currentMethods = _uiState.value.sellerProfile?.supportedPaymentMethods.orEmpty()
                    val resolvedMethods = if (myProfile.supportedPaymentMethods.isNotEmpty()) {
                        myProfile.supportedPaymentMethods
                    } else if (currentMethods.isNotEmpty()) {
                        currentMethods
                    } else {
                        emptyList()
                    }
                    val finalProfile = myProfile.copy(supportedPaymentMethods = resolvedMethods)
                    _uiState.update {
                        it.copy(
                            sellerProfile = finalProfile,
                            isAcceptingOrders = finalProfile.acceptingOrders
                        )
                    }
                }
            }
        }
    }

    fun loadProducts() {
        if (currentSellerId.isBlank()) return
        viewModelScope.launch {
            productRepository.getProductsBySeller(currentSellerId).onSuccess { prods ->
                _uiState.update { it.copy(products = prods) }
            }
            if (_uiState.value.categories.isEmpty()) {
                productRepository.getCategories().onSuccess { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }
        }
    }

    fun setSelectedTab(tab: SellerTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            SellerTab.PRODUCTOS -> loadProducts()
            SellerTab.MI_PUESTO -> loadSellerProfile()
            SellerTab.PEDIDOS -> {}
            SellerTab.ESTADISTICAS -> loadStatistics()
        }
    }

    fun loadStatistics(range: String = _uiState.value.statsTimeRange) {
        _uiState.update { it.copy(isLoadingStats = true, statsTimeRange = range) }
        viewModelScope.launch {
            val sellerId = currentSellerId.ifBlank {
                _uiState.value.sellerProfile?.id ?: return@launch
            }
            val res = orderRepository.getSellerDashboardStatistics(sellerId, range)
            res.onSuccess { stats ->
                val localSubOrders = _uiState.value.subOrders
                val resolvedStats = if (stats.totalOrdersCount == 0 && localSubOrders.isNotEmpty()) {
                    null
                } else {
                    stats
                }
                _uiState.update { it.copy(statsData = resolvedStats, isLoadingStats = false) }
            }.onFailure {
                _uiState.update { it.copy(statsData = null, isLoadingStats = false) }
            }
        }
    }

    fun openAddProductDialog() {
        _uiState.update { it.copy(showAddProductDialog = true) }
    }

    fun dismissAddProductDialog() {
        _uiState.update { it.copy(showAddProductDialog = false) }
    }

    fun openEditStockDialog(product: Product) {
        _uiState.update { it.copy(selectedProductForStockEdit = product) }
    }

    fun dismissEditStockDialog() {
        _uiState.update { it.copy(selectedProductForStockEdit = null) }
    }

    fun updateStock(productId: String, newStock: Int) {
        if (newStock < 0) return
        viewModelScope.launch {
            val result = productRepository.updateProductStock(productId, newStock)
            if (result.isSuccess) {
                val updated = _uiState.value.products.map {
                    if (it.id == productId) {
                        it.copy(
                            stock = newStock,
                            isActive = if (newStock == 0) false else it.isActive
                        )
                    } else it
                }
                _uiState.update {
                    it.copy(
                        products = updated,
                        selectedProductForStockEdit = null,
                        successMessage = "Stock actualizado correctamente."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Error al actualizar stock en la nube.")
                }
            }
        }
    }

    fun createProduct(
        name: String,
        price: Double,
        stock: Int,
        categoryId: String?,
        description: String?,
        imageUrl: String? = null
    ) {
        if (name.isBlank() || price <= 0 || stock < 0) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa nombre, precio y stock válidos.") }
            return
        }
        _uiState.update { it.copy(isSavingProduct = true) }
        viewModelScope.launch {
            val fallbackCatId = categoryId?.takeIf { it.isNotBlank() }
                ?: _uiState.value.categories.firstOrNull()?.id
                ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a"
            val fallbackDesc = description?.takeIf { it.isNotBlank() } ?: name.trim()

            val newProduct = Product(
                id = UUID.randomUUID().toString(),
                sellerId = currentSellerId,
                categoryId = fallbackCatId,
                name = name.trim(),
                description = fallbackDesc,
                price = price,
                stock = stock,
                imageUrl = imageUrl?.takeIf { it.isNotBlank() },
                isActive = (stock > 0),
                pickupLocation = currentPickupLocation
            )
            val result = productRepository.createProduct(newProduct)
            if (result.isSuccess) {
                val created = result.getOrNull() ?: newProduct
                val updated = _uiState.value.products.toMutableList()
                updated.add(0, created)
                _uiState.update {
                    it.copy(
                        products = updated,
                        isSavingProduct = false,
                        showAddProductDialog = false,
                        errorMessage = null,
                        successMessage = "¡Producto agregado con éxito!"
                    )
                }
                loadProducts()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al guardar el producto en la nube."
                _uiState.update {
                    it.copy(
                        isSavingProduct = false,
                        errorMessage = "No se pudo guardar el producto: $err"
                    )
                }
            }
        }
    }

    fun toggleProductActive(productId: String, isActive: Boolean) {
        val product = _uiState.value.products.find { it.id == productId }
        if (isActive && (product == null || product.stock <= 0)) {
            _uiState.update { it.copy(errorMessage = "No puedes activar un producto sin stock disponible. Actualiza el stock primero.") }
            return
        }
        viewModelScope.launch {
            productRepository.toggleProductActive(productId, isActive)
            val updated = _uiState.value.products.map {
                if (it.id == productId) it.copy(isActive = isActive) else it
            }
            _uiState.update { it.copy(products = updated) }
        }
    }

    fun setFilter(filter: SellerOrderFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleAcceptingOrders(accepting: Boolean) {
        _uiState.update { it.copy(isAcceptingOrders = accepting) }
        viewModelScope.launch {
            val result = productRepository.updateSellerAcceptingOrders(currentSellerId, accepting)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = if (accepting) "Puesto Abierto: Ahora estás visible en el catálogo de campus." else "Puesto Cerrado: Tu catálogo ha sido pausado."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "No se pudo actualizar el estado del puesto en el servidor.")
                }
            }
        }
    }

    fun acceptSubOrder(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.ACEPTADO)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al aceptar el pedido.") }
            } else {
                loadProducts()
            }
        }
    }

    fun startPreparation(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.EN_PREPARACION)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar preparación.") }
            }
        }
    }

    fun markReady(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.LISTO)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al marcar pedido listo.") }
            }
        }
    }

    fun openRejectionDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForRejection = subOrder) }
    }

    fun dismissRejectionDialog() {
        _uiState.update { it.copy(selectedSubOrderForRejection = null) }
    }

    fun confirmRejection(subOrderId: String, reason: String) {
        viewModelScope.launch {
            val curSub = _uiState.value.subOrders.firstOrNull { it.id == subOrderId }
            val targetStatus = if (curSub != null && curSub.status != SubOrderStatus.PENDIENTE) {
                SubOrderStatus.CANCELADO
            } else {
                SubOrderStatus.RECHAZADO
            }
            val result = orderRepository.updateSubOrderStatus(subOrderId, targetStatus, reason)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al procesar la cancelación.") }
            } else {
                dismissRejectionDialog()
                loadProducts()
                _uiState.update { it.copy(successMessage = if (targetStatus == SubOrderStatus.CANCELADO) "Pedido cancelado correctamente. Stock devuelto a tu puesto." else "Subpedido rechazado.") }
            }
        }
    }

    fun openDeliveryDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForDelivery = subOrder) }
    }

    fun dismissDeliveryDialog() {
        _uiState.update { it.copy(selectedSubOrderForDelivery = null) }
    }

    fun confirmDeliveryAndPayment(subOrderId: String) {
        viewModelScope.launch {
            val result = orderRepository.updateSubOrderStatus(subOrderId, SubOrderStatus.COMPLETADO)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al confirmar entrega y pago.") }
            } else {
                dismissDeliveryDialog()
                _uiState.update { it.copy(successMessage = "¡Venta y entrega registrada correctamente!") }
            }
        }
    }

    fun openEditProductDialog(product: Product) {
        _uiState.update { it.copy(selectedProductForEdit = product) }
    }

    fun dismissEditProductDialog() {
        _uiState.update { it.copy(selectedProductForEdit = null) }
    }

    fun updateProduct(
        productId: String,
        name: String,
        price: Double,
        stock: Int,
        categoryId: String?,
        description: String?,
        imageUrl: String?
    ) {
        if (name.isBlank() || price <= 0 || stock < 0) {
            _uiState.update { it.copy(errorMessage = "Ingresa nombre, precio y stock válidos.") }
            return
        }
        val current = _uiState.value.products.find { it.id == productId } ?: return
        val updatedProd = current.copy(
            name = name.trim(),
            price = price,
            stock = stock,
            categoryId = categoryId?.takeIf { it.isNotBlank() } ?: current.categoryId,
            description = description?.trim()?.takeIf { it.isNotBlank() } ?: current.description,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() } ?: current.imageUrl,
            isActive = (stock > 0)
        )
        _uiState.update { it.copy(isSavingProduct = true) }
        viewModelScope.launch {
            val result = productRepository.updateProduct(updatedProd)
            if (result.isSuccess) {
                val updatedList = _uiState.value.products.map { if (it.id == productId) updatedProd else it }
                _uiState.update {
                    it.copy(
                        products = updatedList,
                        selectedProductForEdit = null,
                        isSavingProduct = false,
                        successMessage = "¡Producto actualizado exitosamente!"
                    )
                }
                loadProducts()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al actualizar el producto."
                _uiState.update { it.copy(isSavingProduct = false, errorMessage = err) }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val result = productRepository.deleteProduct(productId)
            if (result.isSuccess) {
                val updatedList = _uiState.value.products.filter { it.id != productId }
                _uiState.update {
                    it.copy(
                        products = updatedList,
                        selectedProductForEdit = null,
                        successMessage = "Producto eliminado con éxito."
                    )
                }
                loadProducts()
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar el producto.") }
            }
        }
    }

    fun updateBusinessProfile(
        businessName: String,
        businessStatus: String,
        businessDescription: String?,
        businessCategory: String?,
        businessLocation: String?,
        openTime: String?,
        closeTime: String?,
        bannerUrl: String?,
        avatarUrl: String?,
        acceptingOrders: Boolean,
        supportedMeetingPoints: List<String> = emptyList(),
        supportedPaymentMethods: List<String> = listOf("EFECTIVO", "YAPE", "PLIN")
    ) {
        val currentProfile = _uiState.value.sellerProfile
        val profileToSave = (currentProfile ?: com.example.vallego.domain.model.UserProfile(
            id = currentSellerId,
            fullName = businessName.ifBlank { "Emprendedor" }
        )).copy(
            id = currentSellerId,
            businessName = businessName.trim().takeIf { it.isNotBlank() },
            businessStatus = businessStatus,
            businessDescription = businessDescription?.trim()?.takeIf { it.isNotBlank() },
            businessCategory = businessCategory?.trim()?.takeIf { it.isNotBlank() },
            businessLocation = businessLocation?.trim()?.takeIf { it.isNotBlank() },
            openTime = openTime?.trim()?.takeIf { it.isNotBlank() },
            closeTime = closeTime?.trim()?.takeIf { it.isNotBlank() },
            bannerUrl = bannerUrl?.trim()?.takeIf { it.isNotBlank() },
            avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() },
            acceptingOrders = acceptingOrders,
            supportedMeetingPoints = supportedMeetingPoints,
            supportedPaymentMethods = supportedPaymentMethods
        )

        _uiState.update {
            it.copy(
                sellerProfile = profileToSave,
                isAcceptingOrders = profileToSave.acceptingOrders,
                isSavingProfile = true
            )
        }
        viewModelScope.launch {
            val result = productRepository.updateBusinessProfile(profileToSave)
            if (result.isSuccess) {
                val updated = result.getOrNull() ?: profileToSave
                _uiState.update {
                    it.copy(
                        sellerProfile = updated,
                        isAcceptingOrders = updated.acceptingOrders,
                        isSavingProfile = false,
                        successMessage = "¡Puesto actualizado con éxito!"
                    )
                }
            } else {
                val rawErr = result.exceptionOrNull()?.message ?: "Error al guardar el puesto."
                val friendlyErr = when {
                    rawErr.contains("schema cache", ignoreCase = true) || rawErr.contains("supported_meeting_points", ignoreCase = true) ->
                        "No se pudo guardar la información del puesto. Por favor, intenta de nuevo más tarde."
                    rawErr.contains("network", ignoreCase = true) || rawErr.contains("connect", ignoreCase = true) || rawErr.contains("timeout", ignoreCase = true) ->
                        "Error de conexión. Verifica tu acceso a internet e intenta nuevamente."
                    rawErr.contains("URL:", ignoreCase = true) || rawErr.contains("Headers:", ignoreCase = true) ->
                        "No se pudo sincronizar el puesto con el servidor en este momento."
                    else -> rawErr
                }
                _uiState.update { it.copy(isSavingProfile = false, errorMessage = friendlyErr) }
            }
        }
    }

    fun openSubOrderDetail(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForDetail = subOrder) }
    }

    fun dismissSubOrderDetail() {
        _uiState.update { it.copy(selectedSubOrderForDetail = null) }
    }

    fun openNoShowDialog(subOrder: SubOrder) {
        _uiState.update { it.copy(selectedSubOrderForNoShow = subOrder) }
    }

    fun dismissNoShowDialog() {
        _uiState.update { it.copy(selectedSubOrderForNoShow = null) }
    }

    fun confirmBuyerNoShow(subOrderId: String, reason: String) {
        viewModelScope.launch {
            val result = orderRepository.markBuyerNoShow(subOrderId, reason.ifBlank { "Comprador no se presentó al punto" })
            if (result.isSuccess) {
                dismissNoShowDialog()
                _uiState.update { it.copy(successMessage = "Subpedido marcado como NO entregado (stock devuelto).") }
                loadProducts()
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al reportar inasistencia.") }
            }
        }
    }

    fun onSubOrderExpired(subOrderId: String) {
        viewModelScope.launch {
            orderRepository.expirePendingSuborders()
            _uiState.update { it.copy(errorMessage = "El subpedido ha expirado tras 15 minutos sin ser aceptado.") }
        }
    }

    fun uploadAsset(bucket: String, path: String, bytes: ByteArray, onUploaded: (String) -> Unit) {
        _uiState.update { it.copy(isUploadingAsset = true) }
        viewModelScope.launch {
            val result = productRepository.uploadImage(bucket, path, bytes)
            _uiState.update { it.copy(isUploadingAsset = false) }
            result.onSuccess { url ->
                onUploaded(url)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = "Error al subir imagen: ${err.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun togglePastDayExpanded(date: LocalDate) {
        _uiState.update { state ->
            val set = state.expandedPastDates
            val nextSet = if (set.contains(date)) set - date else set + date
            state.copy(expandedPastDates = nextSet)
        }
    }

    private val limaZone: ZoneId = ZoneId.of("America/Lima")

    private fun parseOrderLocalDate(createdAtIso: String?): LocalDate {
        if (createdAtIso.isNullOrBlank()) return LocalDate.now(limaZone)
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(createdAtIso)
                if (date != null) {
                    return Instant.ofEpochMilli(date.time).atZone(limaZone).toLocalDate()
                }
            } catch (_: Exception) {
                // try next
            }
        }
        return LocalDate.now(limaZone)
    }

    private fun formatDayTitle(date: LocalDate, today: LocalDate): String {
        return when (date) {
            today -> "Hoy"
            today.minusDays(1) -> "Ayer"
            else -> {
                val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                    .replaceFirstChar { it.uppercase() }
                val month = date.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                "$dayOfWeek, ${date.dayOfMonth} de $month"
            }
        }
    }
}