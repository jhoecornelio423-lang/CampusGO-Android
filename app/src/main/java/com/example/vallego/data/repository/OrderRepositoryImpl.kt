package com.example.vallego.data.repository

import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderItem
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.ProfileWarning
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import com.example.vallego.domain.model.HourlyDemandStat
import com.example.vallego.domain.model.MeetingPointStat
import com.example.vallego.domain.model.SellerDashboardStats
import com.example.vallego.domain.model.TopProductStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class RemoteOrderDto(
    val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("delivery_place") val deliveryPlace: String? = null,
    @SerialName("meeting_point_id") val meetingPointId: String? = null,
    @SerialName("meeting_point_name") val meetingPointName: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null,
    val notes: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val status: String = "pending",
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteSubOrderDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("subtotal_amount") val subtotalAmount: Double,
    val status: String = "pending",
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("is_payment_confirmed") val isPaymentConfirmed: Boolean = false,
    @SerialName("is_delivery_confirmed") val isDeliveryConfirmed: Boolean = false,
    @SerialName("delivery_code") val deliveryCode: String? = null,
    @SerialName("stock_reserved") val stockReserved: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RemoteOrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    @SerialName("price_at_sale") val priceAtSale: Double
)

@Serializable
data class ProductBasicDto(
    val id: String,
    val name: String,
    val price: Double? = null,
    val stock: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class ProfileBasicDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("business_description") val businessDescription: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class CheckoutResponseDto(
    val success: Boolean = false,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    val status: String? = null,
    @SerialName("is_duplicate") val isDuplicate: Boolean? = null,
    val message: String? = null
)

@Serializable
data class RpcActionResultDto(
    val success: Boolean = false,
    val message: String? = null,
    @SerialName("expired_count") val expiredCount: Int? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    val status: String? = null
)

@Serializable
data class UpdateSuborderStatusResponseDto(
    val success: Boolean = false,
    @SerialName("sub_order_id") val subOrderId: String? = null,
    val status: String? = null,
    @SerialName("stock_released") val stockReleased: Boolean? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
data class RemoteReviewDto(
    val id: String? = null,
    @SerialName("order_id") val orderId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewee_id") val revieweeId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

class OrderRepositoryImpl(
    private val postgrest: Postgrest? = null,
    private val recalculateOrderUseCase: RecalculateOrderUseCase = RecalculateOrderUseCase()
) : OrderRepository {

    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val ordersFlow = _ordersFlow.asStateFlow()

    private val buyerReviewsMap = ConcurrentHashMap<String, Int>()
    private val productNameCache = ConcurrentHashMap<String, String>()
    private val profileNameCache = ConcurrentHashMap<String, String>()
    private val profilePhoneCache = ConcurrentHashMap<String, String>()
    private val profileAvatarCache = ConcurrentHashMap<String, String>()
    private val sellerSubOrdersCache = ConcurrentHashMap<String, List<SubOrder>>()
    private val cachedOrderItemsByOrder = ConcurrentHashMap<String, List<RemoteOrderItemDto>>()
    private val cachedSubOrdersByOrder = ConcurrentHashMap<String, List<RemoteSubOrderDto>>()
    private val cachedOrderUpdatedAt = ConcurrentHashMap<String, String>()
    private val cachedParentOrders = ConcurrentHashMap<String, RemoteOrderDto>()
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    override fun clearCache() {
        _ordersFlow.value = emptyList()
        buyerReviewsMap.clear()
        sellerSubOrdersCache.clear()
        cachedOrderItemsByOrder.clear()
        cachedSubOrdersByOrder.clear()
        cachedOrderUpdatedAt.clear()
        cachedParentOrders.clear()
    }

    override suspend fun placeOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            if (order.subOrders.isEmpty()) {
                return@withContext Result.failure(Exception("El pedido no contiene ningún producto."))
            }

            val enrichedSubOrders = order.subOrders.map { sub ->
                sub.copy(
                    meetingPointId = sub.meetingPointId ?: order.meetingPointId,
                    meetingPointName = sub.meetingPointName ?: order.meetingPointName,
                    scheduledTime = sub.scheduledTime ?: order.scheduledTime,
                    buyerName = sub.buyerName ?: order.buyerName,
                    notes = sub.notes ?: order.notes
                )
            }

            if (postgrest != null) {

                val subordersArray = buildJsonArray {
                    for (sub in enrichedSubOrders) {
                        val subId = if (isValidUUID(sub.id)) sub.id else UUID.randomUUID().toString()
                        val subPm = (sub.paymentMethod ?: PaymentMethod.EFECTIVO).name
                        add(buildJsonObject {
                            put("id", subId)
                            put("seller_id", sub.sellerId)
                            put("payment_method", subPm)
                            put("items", buildJsonArray {
                                for (item in sub.items) {
                                    val itemId = if (isValidUUID(item.id)) item.id else UUID.randomUUID().toString()
                                    val prodId = if (isValidUUID(item.productId)) item.productId else UUID.randomUUID().toString()
                                    add(buildJsonObject {
                                        put("id", itemId)
                                        put("product_id", prodId)
                                        put("quantity", item.quantity)
                                        put("unit_price", item.unitPrice)
                                    })
                                }
                            })
                        })
                    }
                }

                val orderId = if (isValidUUID(order.id)) order.id else UUID.randomUUID().toString()
                val paymentMethodName = (order.paymentMethod ?: order.subOrders.firstOrNull()?.paymentMethod ?: PaymentMethod.EFECTIVO).name

                val rpcResult = postgrest.rpc(
                    function = "checkout_order_atomic",
                    parameters = buildJsonObject {
                        put("p_order_id", orderId)
                        put("p_meeting_point_id", order.meetingPointId)
                        put("p_meeting_point_name", order.meetingPointName)
                        put("p_scheduled_time", order.scheduledTime)
                        put("p_payment_method", paymentMethodName)
                        put("p_notes", order.notes)
                        put("p_suborders", subordersArray)
                    }
                )
                val response = jsonParser.decodeFromString<CheckoutResponseDto>(rpcResult.data)

                if (!response.success) {
                    return@withContext Result.failure(Exception(response.message ?: "No se pudo confirmar el pedido en el servidor."))
                }

                val confirmedOrder = order.copy(
                    id = response.orderId ?: order.id,
                    totalAmount = response.totalAmount ?: order.totalAmount,
                    status = OrderStatus.PENDIENTE,
                    subOrders = enrichedSubOrders
                )

                val currentList = _ordersFlow.value.toMutableList()
                currentList.removeAll { it.id == confirmedOrder.id }
                currentList.add(0, confirmedOrder)
                _ordersFlow.value = currentList

                Result.success(confirmedOrder)
            } else {
                val localOrder = order.copy(subOrders = enrichedSubOrders)
                val currentList = _ordersFlow.value.toMutableList()
                currentList.removeAll { it.id == localOrder.id }
                currentList.add(0, localOrder)
                _ordersFlow.value = currentList
                Result.success(localOrder)
            }
        } catch (e: Exception) {
            val friendlyMsg = mapExceptionToUserFriendlyMessage(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    private fun mapExceptionToUserFriendlyMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("INSUFFICIENT_STOCK", ignoreCase = true) -> {
                val detail = msg.substringAfter("INSUFFICIENT_STOCK:").substringBefore("\n").trim()
                if (detail.isNotBlank()) detail else "Stock insuficiente para uno de los productos seleccionados."
            }
            msg.contains("SELLER_CLOSED", ignoreCase = true) -> {
                "Uno de los puestos del carrito no está aceptando pedidos en este momento."
            }
            msg.contains("SELF_PURCHASE", ignoreCase = true) -> {
                "No puedes realizar un pedido a tu propio emprendimiento."
            }
            msg.contains("PRODUCT_UNAVAILABLE", ignoreCase = true) -> {
                "Uno de los productos seleccionados ya no se encuentra disponible."
            }
            msg.contains("UNAUTHENTICATED", ignoreCase = true) -> {
                "Sesión no autenticada. Por favor vuelve a iniciar sesión."
            }
            msg.contains("INVALID_DATA", ignoreCase = true) -> {
                msg.substringAfter("INVALID_DATA:").substringBefore("\n").trim()
            }
            else -> e.localizedMessage ?: "Error de conexión al confirmar el pedido. Por favor reintenta."
        }
    }

    override suspend fun getOrdersForBuyer(buyerId: String): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val matching = _ordersFlow.value.filter { it.buyerId == buyerId }
            Result.success(matching)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubOrdersForSeller(sellerId: String): Result<List<SubOrder>> = withContext(Dispatchers.IO) {
        try {
            val cached = sellerSubOrdersCache[sellerId]
            if (!cached.isNullOrEmpty()) {
                return@withContext Result.success(cached)
            }
            val subOrders = _ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId }
            Result.success(subOrders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeOrdersForBuyer(buyerId: String): Flow<List<Order>> = flow {
        val initialCached = _ordersFlow.value.filter { it.buyerId == buyerId }
        emit(initialCached)
        var previousEmitted: List<Order>? = initialCached.takeIf { it.isNotEmpty() }

        while (true) {
            try {
                if (postgrest != null && isValidUUID(buyerId)) {
                    val remoteOrders = postgrest.from("orders")
                        .select {
                            filter {
                                eq("buyer_id", buyerId)
                            }
                        }
                        .decodeList<RemoteOrderDto>()
                        .sortedByDescending { it.createdAt }

                    if (remoteOrders.isNotEmpty()) {
                        val orderIds = remoteOrders.map { it.id }

                        // Optimización 1: Consultar sub_orders si no están en caché, si cambió updatedAt, o si los subpedidos aún no son finales (evita congelar estados como 'ready')
                        val orderIdsNeedingSubs = remoteOrders.filter { ro ->
                            val cachedSubs = cachedSubOrdersByOrder[ro.id]
                            val lastKnownUpdated = cachedOrderUpdatedAt[ro.id]
                            val wasUpdated = lastKnownUpdated != ro.updatedAt
                            val isAllFinal = !cachedSubs.isNullOrEmpty() && cachedSubs.all {
                                it.status in listOf("completed", "cancelled", "rejected", "not_delivered")
                            }
                            !isAllFinal || wasUpdated
                        }.map { it.id }

                        if (orderIdsNeedingSubs.isNotEmpty()) {
                            val freshlyFetchedSubs = try {
                                postgrest.from("sub_orders")
                                    .select {
                                        filter {
                                            isIn("order_id", orderIdsNeedingSubs)
                                        }
                                    }
                                    .decodeList<RemoteSubOrderDto>()
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val groupedSubs = freshlyFetchedSubs.groupBy { it.orderId }
                            orderIdsNeedingSubs.forEach { ordId ->
                                cachedSubOrdersByOrder[ordId] = groupedSubs[ordId] ?: emptyList()
                                val ro = remoteOrders.firstOrNull { it.id == ordId }
                                if (ro?.updatedAt != null) {
                                    cachedOrderUpdatedAt[ordId] = ro.updatedAt
                                }
                            }
                        }

                        val remoteSubOrders = orderIds.flatMap { ordId ->
                            cachedSubOrdersByOrder[ordId] ?: emptyList()
                        }

                        // Optimización 2: Solo consultar order_items para órdenes activas o no cacheadas
                        val orderIdsNeedingItems = remoteOrders.filter { ro ->
                            ro.status in listOf("pending", "accepted", "preparing", "ready") || !cachedOrderItemsByOrder.containsKey(ro.id)
                        }.map { it.id }

                        if (orderIdsNeedingItems.isNotEmpty()) {
                            val freshlyFetchedItems = try {
                                postgrest.from("order_items")
                                    .select {
                                        filter {
                                            isIn("order_id", orderIdsNeedingItems)
                                        }
                                    }
                                    .decodeList<RemoteOrderItemDto>()
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val groupedItems = freshlyFetchedItems.groupBy { it.orderId }
                            orderIdsNeedingItems.forEach { ordId ->
                                cachedOrderItemsByOrder[ordId] = groupedItems[ordId] ?: emptyList()
                            }
                        }

                        val remoteItems = orderIds.flatMap { ordId ->
                            cachedOrderItemsByOrder[ordId] ?: emptyList()
                        }

                        val missingProdIds = remoteItems.map { it.productId }
                            .filter { isValidUUID(it) && !productNameCache.containsKey(it) }
                            .distinct()
                        if (missingProdIds.isNotEmpty()) {
                            try {
                                val prods = postgrest.from("products")
                                    .select { filter { isIn("id", missingProdIds) } }
                                    .decodeList<ProductBasicDto>()
                                for (p in prods) {
                                    productNameCache[p.id] = p.name
                                }
                            } catch (_: Exception) {}
                        }

                        val missingSellerIds = remoteSubOrders.map { it.sellerId }
                            .filter { isValidUUID(it) && !profileNameCache.containsKey(it) }
                            .distinct()
                        if (missingSellerIds.isNotEmpty()) {
                            try {
                                val profiles = postgrest.from("profiles")
                                    .select { filter { isIn("id", missingSellerIds) } }
                                    .decodeList<ProfileBasicDto>()
                                for (pr in profiles) {
                                    profileNameCache[pr.id] = pr.fullName ?: "Emprendedor"
                                    pr.avatarUrl?.let { profileAvatarCache[pr.id] = it }
                                }
                            } catch (_: Exception) {}
                        }

                        val itemsBySubOrder = remoteItems.groupBy { it.subOrderId ?: it.orderId }
                        val subOrdersByOrder = remoteSubOrders.groupBy { it.orderId }

                        val mappedOrders = remoteOrders.map { ro ->
                            val subsForOrder = subOrdersByOrder[ro.id] ?: emptyList()
                            val domainSubOrders = if (subsForOrder.isNotEmpty()) {
                                subsForOrder.map { rso ->
                                    val sItems = (itemsBySubOrder[rso.id] ?: emptyList()).map { oi ->
                                        SubOrderItem(
                                            id = oi.id,
                                            subOrderId = rso.id,
                                            productId = oi.productId,
                                            productName = productNameCache[oi.productId] ?: "Producto",
                                            quantity = oi.quantity,
                                            unitPrice = oi.priceAtSale,
                                            subtotal = oi.priceAtSale * oi.quantity
                                        )
                                    }
                                    val subStatus = mapRemoteStatusToSubOrderStatus(rso.status)
                                    val meetingPlace = ro.meetingPointName ?: ro.deliveryPlace ?: "Campus Universitario"
                                    val schedule = ro.scheduledTime ?: extractScheduleFromDeliveryPlace(ro.deliveryPlace)
                                    SubOrder(
                                        id = rso.id,
                                        orderId = ro.id,
                                        sellerId = rso.sellerId,
                                        sellerName = profileNameCache[rso.sellerId] ?: "Emprendimiento",
                                        items = sItems,
                                        subtotalAmount = rso.subtotalAmount,
                                        status = subStatus,
                                        rejectionReason = rso.rejectionReason,
                                        paymentMethod = parsePaymentMethod(rso.paymentMethod),
                                        meetingPointId = ro.meetingPointId,
                                        meetingPointName = meetingPlace,
                                        scheduledTime = schedule,
                                        buyerId = ro.buyerId,
                                        buyerName = profileNameCache[ro.buyerId] ?: "Comprador",
                                        buyerPhone = profilePhoneCache[ro.buyerId] ?: "",
                                        buyerAvatarUrl = ro.buyerId?.let { profileAvatarCache[it] },
                                        notes = ro.notes,
                                        isPaymentConfirmed = rso.isPaymentConfirmed,
                                        isDeliveryConfirmed = rso.isDeliveryConfirmed,
                                        deliveryCode = rso.deliveryCode,
                                        createdAt = rso.createdAt,
                                        updatedAt = rso.updatedAt
                                    )
                                }
                            } else {
                                val legacyItems = (itemsBySubOrder[ro.id] ?: emptyList()).map { oi ->
                                    SubOrderItem(
                                        id = oi.id,
                                        subOrderId = ro.id,
                                        productId = oi.productId,
                                        productName = productNameCache[oi.productId] ?: "Producto",
                                        quantity = oi.quantity,
                                        unitPrice = oi.priceAtSale,
                                        subtotal = oi.priceAtSale * oi.quantity
                                    )
                                }
                                val subStatus = mapRemoteStatusToSubOrderStatus(ro.status)
                                val meetingPlace = ro.meetingPointName ?: ro.deliveryPlace ?: "Campus Universitario"
                                val schedule = ro.scheduledTime ?: extractScheduleFromDeliveryPlace(ro.deliveryPlace)
                                listOf(
                                    SubOrder(
                                        id = ro.id,
                                        orderId = ro.id,
                                        sellerId = ro.sellerId ?: "",
                                        sellerName = ro.sellerId?.let { profileNameCache[it] } ?: "Emprendimiento",
                                        items = legacyItems,
                                        subtotalAmount = ro.totalPrice,
                                        status = subStatus,
                                        paymentMethod = parsePaymentMethod(ro.paymentMethod),
                                        meetingPointId = ro.meetingPointId,
                                        meetingPointName = meetingPlace,
                                        scheduledTime = schedule,
                                        buyerId = ro.buyerId,
                                        buyerName = profileNameCache[ro.buyerId] ?: "Comprador",
                                        buyerPhone = profilePhoneCache[ro.buyerId] ?: "",
                                        buyerAvatarUrl = ro.buyerId?.let { profileAvatarCache[it] },
                                        notes = ro.notes,
                                        isPaymentConfirmed = (subStatus == SubOrderStatus.COMPLETADO),
                                        isDeliveryConfirmed = (subStatus == SubOrderStatus.COMPLETADO),
                                        createdAt = ro.createdAt,
                                        updatedAt = ro.updatedAt
                                    )
                                )
                            }

                            val activeSubs = domainSubOrders.filter {
                                it.status != SubOrderStatus.RECHAZADO && it.status != SubOrderStatus.CANCELADO
                            }
                            val effectiveTotal = if (activeSubs.isNotEmpty()) {
                                activeSubs.sumOf { it.subtotalAmount }
                            } else {
                                if (domainSubOrders.isNotEmpty()) 0.0 else ro.totalPrice
                            }

                            val computedOrderStatus = when {
                                domainSubOrders.isEmpty() -> mapRemoteStatusToOrderStatus(ro.status)
                                domainSubOrders.all { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO } -> OrderStatus.CANCELADA
                                domainSubOrders.all { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO } -> OrderStatus.COMPLETADA
                                domainSubOrders.any { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO } &&
                                domainSubOrders.any { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION || it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.COMPLETADO } -> OrderStatus.PARCIALMENTE_ACEPTADA
                                domainSubOrders.any { it.status == SubOrderStatus.ACEPTADO || it.status == SubOrderStatus.EN_PREPARACION || it.status == SubOrderStatus.LISTO } -> OrderStatus.EN_PROCESO
                                else -> mapRemoteStatusToOrderStatus(ro.status)
                            }

                            val meetingPlace = ro.meetingPointName ?: ro.deliveryPlace ?: "Campus Universitario"
                            val schedule = ro.scheduledTime ?: extractScheduleFromDeliveryPlace(ro.deliveryPlace)

                            Order(
                                id = ro.id,
                                buyerId = ro.buyerId,
                                buyerName = "Comprador",
                                meetingPointId = ro.meetingPointId ?: "mp-default",
                                meetingPointName = meetingPlace,
                                scheduledTime = schedule,
                                totalAmount = effectiveTotal,
                                status = computedOrderStatus,
                                subOrders = domainSubOrders,
                                paymentMethod = parsePaymentMethod(ro.paymentMethod),
                                notes = ro.notes,
                                createdAt = ro.createdAt,
                                updatedAt = ro.updatedAt
                            )
                        }

                        if (mappedOrders != previousEmitted) {
                            previousEmitted = mappedOrders
                            _ordersFlow.value = mappedOrders
                            emit(mappedOrders)
                        }
                    } else if (previousEmitted != null && previousEmitted.isNotEmpty()) {
                        previousEmitted = emptyList()
                        _ordersFlow.value = emptyList()
                        emit(emptyList())
                    }
                }
            } catch (_: Exception) {
            }

            val hasActiveOrders = previousEmitted?.any { o ->
                o.status in listOf(OrderStatus.PENDIENTE, OrderStatus.EN_PROCESO, OrderStatus.PARCIALMENTE_ACEPTADA)
            } ?: false
            delay(if (hasActiveOrders) 3000L else 15000L)
        }
    }.flowOn(Dispatchers.IO)

    override fun observeSubOrdersForSeller(sellerId: String): Flow<List<SubOrder>> = flow {
        var previousEmitted: List<SubOrder>? = null
        val initialCached = sellerSubOrdersCache[sellerId]
        if (!initialCached.isNullOrEmpty()) {
            previousEmitted = initialCached
            emit(initialCached)
        } else {
            val fallback = _ordersFlow.value.flatMap { it.subOrders }.filter { it.sellerId == sellerId }
            if (fallback.isNotEmpty()) {
                previousEmitted = fallback
                emit(fallback)
            }
        }

        while (true) {
            try {
                if (postgrest != null && isValidUUID(sellerId)) {
                    val remoteSubOrders = try {
                        postgrest.from("sub_orders")
                            .select {
                                filter {
                                    eq("seller_id", sellerId)
                                }
                            }
                            .decodeList<RemoteSubOrderDto>()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val remoteLegacyOrders = try {
                        postgrest.from("orders")
                            .select {
                                filter {
                                    eq("seller_id", sellerId)
                                }
                            }
                            .decodeList<RemoteOrderDto>()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val existingSubOrderOrderIds = remoteSubOrders.map { it.orderId }.toSet()
                    val missingFromSubOrders = remoteLegacyOrders.filter { it.id !in existingSubOrderOrderIds }
                    val synthesizedSubs = missingFromSubOrders.map { ro ->
                        RemoteSubOrderDto(
                            id = ro.id,
                            orderId = ro.id,
                            sellerId = ro.sellerId ?: sellerId,
                            subtotalAmount = ro.totalPrice,
                            status = ro.status,
                            paymentMethod = ro.paymentMethod,
                            createdAt = ro.createdAt,
                            updatedAt = ro.updatedAt
                        )
                    }

                    val combinedSubOrders = (remoteSubOrders + synthesizedSubs).sortedByDescending { it.createdAt }

                    if (combinedSubOrders.isNotEmpty()) {
                        val parentOrderIds = combinedSubOrders.map { it.orderId }.distinct()

                        // Optimización 1: Solo consultar order_items para subpedidos activos o no cacheados
                        val orderIdsNeedingItems = combinedSubOrders.filter { rso ->
                            rso.status in listOf("pending", "accepted", "preparing", "ready") || !cachedOrderItemsByOrder.containsKey(rso.orderId)
                        }.map { it.orderId }.distinct()

                        if (orderIdsNeedingItems.isNotEmpty()) {
                            val freshlyFetchedItems = try {
                                postgrest.from("order_items")
                                    .select { filter { isIn("order_id", orderIdsNeedingItems) } }
                                    .decodeList<RemoteOrderItemDto>()
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val groupedItems = freshlyFetchedItems.groupBy { it.orderId }
                            orderIdsNeedingItems.forEach { ordId ->
                                cachedOrderItemsByOrder[ordId] = groupedItems[ordId] ?: emptyList()
                            }
                        }

                        val remoteItems = parentOrderIds.flatMap { ordId ->
                            cachedOrderItemsByOrder[ordId] ?: emptyList()
                        }

                        val missingProdIds = remoteItems.map { it.productId }
                            .filter { isValidUUID(it) && !productNameCache.containsKey(it) }
                            .distinct()
                        if (missingProdIds.isNotEmpty()) {
                            try {
                                val prods = postgrest.from("products")
                                    .select { filter { isIn("id", missingProdIds) } }
                                    .decodeList<ProductBasicDto>()
                                for (p in prods) {
                                    productNameCache[p.id] = p.name
                                }
                            } catch (_: Exception) {}
                        }

                        if (!profileNameCache.containsKey(sellerId)) {
                            try {
                                val pr = postgrest.from("profiles")
                                    .select { filter { eq("id", sellerId) } }
                                    .decodeSingleOrNull<ProfileBasicDto>()
                                if (pr != null) {
                                    profileNameCache[sellerId] = pr.fullName ?: "Mi Emprendimiento"
                                }
                            } catch (_: Exception) {}
                        }

                        // Optimización 2: Cachear pedidos padre para evitar reconsultar órdenes completadas
                        val orderIdsNeedingParent = parentOrderIds.filter { ordId ->
                            val cached = cachedParentOrders[ordId]
                            cached == null || cached.status in listOf("pending", "accepted", "preparing", "ready")
                        }

                        if (orderIdsNeedingParent.isNotEmpty()) {
                            val freshlyFetchedParentOrders = try {
                                postgrest.from("orders")
                                    .select { filter { isIn("id", orderIdsNeedingParent) } }
                                    .decodeList<RemoteOrderDto>()
                            } catch (_: Exception) {
                                emptyList()
                            }
                            for (po in freshlyFetchedParentOrders) {
                                cachedParentOrders[po.id] = po
                            }
                        }

                        val remoteParentOrders = parentOrderIds.mapNotNull { cachedParentOrders[it] }
                        val parentOrdersMap = remoteParentOrders.associateBy { it.id }

                        val missingBuyerIds = remoteParentOrders.map { it.buyerId }
                            .filter { isValidUUID(it) && (!profileNameCache.containsKey(it) || !profilePhoneCache.containsKey(it) || !profileAvatarCache.containsKey(it)) }
                            .distinct()
                        if (missingBuyerIds.isNotEmpty()) {
                            try {
                                val buyers = postgrest.from("profiles")
                                    .select { filter { isIn("id", missingBuyerIds) } }
                                    .decodeList<ProfileBasicDto>()
                                for (b in buyers) {
                                    b.fullName?.let { profileNameCache[b.id] = it }
                                    b.phone?.let { profilePhoneCache[b.id] = it }
                                    b.avatarUrl?.let { profileAvatarCache[b.id] = it }
                                }
                            } catch (_: Exception) {}
                        }

                        val itemsBySubOrder = remoteItems.groupBy { it.subOrderId ?: it.orderId }

                        val mappedSubOrders = combinedSubOrders.map { rso ->
                            val sItems = (itemsBySubOrder[rso.id] ?: itemsBySubOrder[rso.orderId] ?: emptyList()).map { oi ->
                                SubOrderItem(
                                    id = oi.id,
                                    subOrderId = rso.id,
                                    productId = oi.productId,
                                    productName = productNameCache[oi.productId] ?: "Producto",
                                    quantity = oi.quantity,
                                    unitPrice = oi.priceAtSale,
                                    subtotal = oi.priceAtSale * oi.quantity
                                )
                            }
                            val subStatus = mapRemoteStatusToSubOrderStatus(rso.status)
                            val parentOrder = parentOrdersMap[rso.orderId]
                            val buyerId = parentOrder?.buyerId
                            val buyerName = buyerId?.let { profileNameCache[it] } ?: ""
                            val buyerPhone = buyerId?.let { profilePhoneCache[it] } ?: ""
                            val buyerAvatar = buyerId?.let { profileAvatarCache[it] }
                            val meetingPlace = parentOrder?.meetingPointName?.takeIf { it.isNotBlank() } ?: parentOrder?.deliveryPlace ?: "Punto de encuentro"
                            val scheduledTime = parentOrder?.scheduledTime ?: extractScheduleFromDeliveryPlace(parentOrder?.deliveryPlace)

                            SubOrder(
                                id = rso.id,
                                orderId = rso.orderId,
                                sellerId = rso.sellerId,
                                sellerName = profileNameCache[rso.sellerId] ?: "Mi Emprendimiento",
                                items = sItems,
                                subtotalAmount = rso.subtotalAmount,
                                status = subStatus,
                                rejectionReason = rso.rejectionReason,
                                paymentMethod = parsePaymentMethod(rso.paymentMethod),
                                meetingPointId = parentOrder?.meetingPointId,
                                meetingPointName = meetingPlace,
                                scheduledTime = scheduledTime,
                                buyerId = buyerId,
                                buyerName = buyerName,
                                buyerPhone = buyerPhone,
                                buyerAvatarUrl = buyerAvatar,
                                notes = parentOrder?.notes,
                                isPaymentConfirmed = rso.isPaymentConfirmed,
                                isDeliveryConfirmed = rso.isDeliveryConfirmed,
                                deliveryCode = rso.deliveryCode,
                                createdAt = rso.createdAt,
                                updatedAt = rso.updatedAt
                            )
                        }

                        // Optimización 3: Deduplicación de emisiones para evitar recomposiciones cíclicas
                        if (mappedSubOrders != previousEmitted) {
                            previousEmitted = mappedSubOrders
                            sellerSubOrdersCache[sellerId] = mappedSubOrders
                            emit(mappedSubOrders)
                        }
                    } else if (previousEmitted != null && previousEmitted.isNotEmpty()) {
                        previousEmitted = emptyList()
                        sellerSubOrdersCache[sellerId] = emptyList()
                        emit(emptyList())
                    }
                }
            } catch (_: Exception) {
            }

            val hasActiveSubOrders = previousEmitted?.any { so ->
                so.status in listOf(SubOrderStatus.PENDIENTE, SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION, SubOrderStatus.LISTO)
            } ?: false
            delay(if (hasActiveSubOrders) 3000L else 15000L)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateSubOrderStatus(
        subOrderId: String,
        newStatus: SubOrderStatus,
        rejectionReason: String?
    ): Result<SubOrder> = withContext(Dispatchers.IO) {
        val previousOrders = _ordersFlow.value
        val previousSellerCache = sellerSubOrdersCache.toMap()
        try {
            val currentSub = _ordersFlow.value.flatMap { it.subOrders }.firstOrNull { it.id == subOrderId }
            val effectiveStatus = if (newStatus == SubOrderStatus.RECHAZADO && currentSub != null && currentSub.status != SubOrderStatus.PENDIENTE) {
                SubOrderStatus.CANCELADO
            } else {
                newStatus
            }

            // Actualización optimista local en memoria (0ms latencia percibida)
            var updated: SubOrder? = null
            val currentOrders = _ordersFlow.value.toMutableList()
            for (i in currentOrders.indices) {
                val order = currentOrders[i]
                val subIndex = order.subOrders.indexOfFirst { it.id == subOrderId }
                if (subIndex >= 0) {
                    val curSub = order.subOrders[subIndex]
                    val isPayConfirmed = if (effectiveStatus == SubOrderStatus.COMPLETADO || effectiveStatus == SubOrderStatus.PAGO_CONFIRMADO) true else curSub.isPaymentConfirmed
                    val isDelivConfirmed = if (effectiveStatus == SubOrderStatus.COMPLETADO) true else curSub.isDeliveryConfirmed
                    val newSub = curSub.copy(
                        status = effectiveStatus,
                        rejectionReason = rejectionReason ?: curSub.rejectionReason,
                        isPaymentConfirmed = isPayConfirmed,
                        isDeliveryConfirmed = isDelivConfirmed
                    )
                    val recalculated = recalculateOrderUseCase(order, newSub)
                    currentOrders[i] = recalculated
                    updated = newSub
                    break
                }
            }

            if (updated != null) {
                _ordersFlow.value = currentOrders
                sellerSubOrdersCache.forEach { (sid, list) ->
                    sellerSubOrdersCache[sid] = list.map { if (it.id == subOrderId) updated else it }
                }
                cachedSubOrdersByOrder.remove(updated.orderId)
                cachedOrderUpdatedAt.remove(updated.orderId)
            }

            if (postgrest != null && isValidUUID(subOrderId)) {
                val remoteStatusStr = mapLocalStatusToRemote(effectiveStatus)

                val rpcResult = postgrest.rpc(
                    function = "update_suborder_status_atomic",
                    parameters = buildJsonObject {
                        put("p_sub_order_id", subOrderId)
                        put("p_new_status", remoteStatusStr)
                        if (rejectionReason != null) {
                            put("p_rejection_reason", rejectionReason)
                        }
                    }
                )
                val response = jsonParser.decodeFromString<UpdateSuborderStatusResponseDto>(rpcResult.data)

                if (!response.success) {
                    // Rollback atómico en caso de fallo remoto
                    _ordersFlow.value = previousOrders
                    sellerSubOrdersCache.clear()
                    sellerSubOrdersCache.putAll(previousSellerCache)
                    return@withContext Result.failure(Exception("No se pudo actualizar el estado del subpedido en el servidor."))
                }
            }

            if (updated != null) {
                Result.success(updated)
            } else {
                val fallbackSub = SubOrder(
                    id = subOrderId,
                    orderId = subOrderId,
                    sellerId = "",
                    sellerName = "Subpedido",
                    status = newStatus,
                    rejectionReason = rejectionReason
                )
                Result.success(fallbackSub)
            }
        } catch (e: Exception) {
            // Rollback atómico en caso de excepción
            _ordersFlow.value = previousOrders
            sellerSubOrdersCache.clear()
            sellerSubOrdersCache.putAll(previousSellerCache)

            val friendlyMsg = when {
                e.message.orEmpty().contains("INVALID_TRANSITION", ignoreCase = true) -> {
                    e.message?.substringAfter("INVALID_TRANSITION:")?.substringBefore("\n")?.trim() ?: "Transición de estado no permitida."
                }
                e.message.orEmpty().contains("FORBIDDEN", ignoreCase = true) -> {
                    "No tienes permisos para modificar este subpedido."
                }
                else -> e.localizedMessage ?: "Error al actualizar estado del subpedido."
            }
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    override suspend fun cancelOrderByBuyer(orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val previousOrders = _ordersFlow.value
        val previousSellerCache = sellerSubOrdersCache.toMap()
        try {
            // Actualización optimista local inmediata
            val currentOrders = _ordersFlow.value.toMutableList()
            val orderIdx = currentOrders.indexOfFirst { it.id == orderId }
            if (orderIdx >= 0) {
                val ord = currentOrders[orderIdx]
                val cancelledSubs = ord.subOrders.map { sub ->
                    if (sub.status == SubOrderStatus.PENDIENTE) sub.copy(status = SubOrderStatus.CANCELADO) else sub
                }
                currentOrders[orderIdx] = ord.copy(
                    status = OrderStatus.CANCELADA,
                    subOrders = cancelledSubs
                )
                _ordersFlow.value = currentOrders
                sellerSubOrdersCache.forEach { (sid, list) ->
                    sellerSubOrdersCache[sid] = list.map { sub ->
                        if (sub.orderId == orderId && sub.status == SubOrderStatus.PENDIENTE) {
                            sub.copy(status = SubOrderStatus.CANCELADO)
                        } else sub
                    }
                }
            }

            if (postgrest != null && isValidUUID(orderId)) {
                val rpcResult = postgrest.rpc(
                    function = "cancel_order_by_buyer_atomic",
                    parameters = buildJsonObject {
                        put("p_order_id", orderId)
                    }
                )
                val response = jsonParser.decodeFromString<RpcActionResultDto>(rpcResult.data)
                if (!response.success) {
                    // Rollback atómico si el servidor rechaza la cancelación
                    _ordersFlow.value = previousOrders
                    sellerSubOrdersCache.clear()
                    sellerSubOrdersCache.putAll(previousSellerCache)
                    return@withContext Result.failure(Exception(response.message ?: "No se pudo cancelar el pedido."))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _ordersFlow.value = previousOrders
            sellerSubOrdersCache.clear()
            sellerSubOrdersCache.putAll(previousSellerCache)
            val friendlyMsg = when {
                e.message.orEmpty().contains("ORDER_ALREADY_PROCESSED", ignoreCase = true) ->
                    "El pedido ya fue aceptado o procesado y no puede cancelarse."
                e.message.orEmpty().contains("ORDER_NOT_FOUND", ignoreCase = true) ->
                    "El pedido no fue encontrado."
                else -> e.localizedMessage ?: "Error al cancelar el pedido."
            }
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    override suspend fun markBuyerNoShow(subOrderId: String, reason: String?): Result<SubOrder> = withContext(Dispatchers.IO) {
        val previousOrders = _ordersFlow.value
        val previousSellerCache = sellerSubOrdersCache.toMap()
        try {
            // Actualización optimista local
            var updated: SubOrder? = null
            val currentOrders = _ordersFlow.value.toMutableList()
            for (i in currentOrders.indices) {
                val order = currentOrders[i]
                val subIndex = order.subOrders.indexOfFirst { it.id == subOrderId }
                if (subIndex >= 0) {
                    val curSub = order.subOrders[subIndex]
                    val newSub = curSub.copy(
                        status = SubOrderStatus.NO_ENTREGADO,
                        rejectionReason = reason ?: "Comprador no se presentó"
                    )
                    val recalculated = recalculateOrderUseCase(order, newSub)
                    currentOrders[i] = recalculated
                    updated = newSub
                    break
                }
            }

            if (updated != null) {
                _ordersFlow.value = currentOrders
                sellerSubOrdersCache.forEach { (sid, list) ->
                    sellerSubOrdersCache[sid] = list.map { if (it.id == subOrderId) updated else it }
                }
            }

            if (postgrest != null && isValidUUID(subOrderId)) {
                val rpcResult = postgrest.rpc(
                    function = "mark_suborder_no_show_atomic",
                    parameters = buildJsonObject {
                        put("p_sub_order_id", subOrderId)
                        put("p_reported_by_seller", true)
                        put("p_reason", reason ?: "Comprador no se presentó al punto de entrega")
                    }
                )
                val response = jsonParser.decodeFromString<RpcActionResultDto>(rpcResult.data)
                if (!response.success) {
                    _ordersFlow.value = previousOrders
                    sellerSubOrdersCache.clear()
                    sellerSubOrdersCache.putAll(previousSellerCache)
                    return@withContext Result.failure(Exception(response.message ?: "No se pudo reportar la ausencia del comprador."))
                }
                try {
                    postgrest.from("order_incidents").insert(
                        mapOf(
                            "sub_order_id" to subOrderId,
                            "incident_type" to "NO_SHOW_BUYER",
                            "details" to (reason ?: "Comprador no se presentó al punto de entrega"),
                            "status" to "PENDIENTE"
                        )
                    )
                } catch (_: Exception) {}
            }

            if (updated != null) {
                Result.success(updated)
            } else {
                val fallbackSub = SubOrder(
                    id = subOrderId,
                    orderId = subOrderId,
                    sellerId = "",
                    sellerName = "Subpedido",
                    status = SubOrderStatus.NO_ENTREGADO,
                    rejectionReason = reason ?: "Comprador no se presentó"
                )
                Result.success(fallbackSub)
            }
        } catch (e: Exception) {
            _ordersFlow.value = previousOrders
            sellerSubOrdersCache.clear()
            sellerSubOrdersCache.putAll(previousSellerCache)
            Result.failure(e)
        }
    }

    override suspend fun expirePendingSuborders(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            if (postgrest != null) {
                val rpcResult = postgrest.rpc(
                    function = "expire_unanswered_suborders_atomic"
                )
                val response = jsonParser.decodeFromString<RpcActionResultDto>(rpcResult.data)
                count = response.expiredCount ?: 0
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapRemoteStatusToSubOrderStatus(status: String): SubOrderStatus = when (status.lowercase()) {
        "pending" -> SubOrderStatus.PENDIENTE
        "accepted" -> SubOrderStatus.ACEPTADO
        "preparing" -> SubOrderStatus.EN_PREPARACION
        "ready", "waiting_delivery" -> SubOrderStatus.LISTO
        "completed", "payment_confirmed" -> SubOrderStatus.COMPLETADO
        "rejected" -> SubOrderStatus.RECHAZADO
        "cancelled" -> SubOrderStatus.CANCELADO
        "not_delivered" -> SubOrderStatus.NO_ENTREGADO
        else -> SubOrderStatus.PENDIENTE
    }

    private fun mapLocalStatusToRemote(status: SubOrderStatus): String = when (status) {
        SubOrderStatus.PENDIENTE -> "pending"
        SubOrderStatus.ACEPTADO -> "accepted"
        SubOrderStatus.EN_PREPARACION -> "preparing"
        SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> "ready"
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> "completed"
        SubOrderStatus.RECHAZADO -> "rejected"
        SubOrderStatus.CANCELADO -> "cancelled"
        SubOrderStatus.NO_ENTREGADO -> "not_delivered"
    }

    private fun mapRemoteStatusToOrderStatus(status: String): OrderStatus = when (status.lowercase()) {
        "pending" -> OrderStatus.PENDIENTE
        "accepted", "preparing", "ready", "in_progress" -> OrderStatus.EN_PROCESO
        "partially_accepted" -> OrderStatus.PARCIALMENTE_ACEPTADA
        "completed" -> OrderStatus.COMPLETADA
        "cancelled", "rejected" -> OrderStatus.CANCELADA
        else -> OrderStatus.PENDIENTE
    }

    private fun parsePaymentMethod(method: String?): PaymentMethod = when (method?.uppercase()) {
        "YAPE" -> PaymentMethod.YAPE
        "PLIN" -> PaymentMethod.PLIN
        "TRANSFERENCIA" -> PaymentMethod.TRANSFERENCIA
        "OTRO" -> PaymentMethod.OTRO
        else -> PaymentMethod.EFECTIVO
    }

    private fun extractScheduleFromDeliveryPlace(deliveryPlace: String?): String {
        if (deliveryPlace == null) return "Turno seleccionado"
        val openParen = deliveryPlace.indexOf('(')
        val closeParen = deliveryPlace.indexOf(')')
        return if (openParen in 0 until closeParen) {
            deliveryPlace.substring(openParen + 1, closeParen).trim()
        } else "Turno seleccionado"
    }

    override suspend fun submitSellerReview(
        orderId: String,
        buyerId: String,
        sellerId: String,
        rating: Int,
        comment: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val clampedRating = rating.coerceIn(1, 5)
            val compoundKey = "$orderId-$sellerId"
            buyerReviewsMap[compoundKey] = clampedRating
            buyerReviewsMap[orderId] = clampedRating

            if (postgrest != null && isValidUUID(orderId) && isValidUUID(buyerId) && isValidUUID(sellerId)) {
                try {
                    val payload = buildJsonObject {
                        put("order_id", orderId)
                        put("reviewer_id", buyerId)
                        put("reviewee_id", sellerId)
                        put("rating", clampedRating)
                        if (!comment.isNullOrBlank()) {
                            put("comment", comment.trim())
                        }
                    }
                    postgrest.from("reviews").insert(payload)
                } catch (e: Exception) {
                    // Si falla por duplicidad o RLS no disponible, mantenemos la calificación local
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBuyerReviews(buyerId: String): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null && isValidUUID(buyerId)) {
                try {
                    val remoteReviews = postgrest.from("reviews")
                        .select {
                            filter {
                                eq("reviewer_id", buyerId)
                            }
                        }
                        .decodeList<RemoteReviewDto>()
                    for (rev in remoteReviews) {
                        buyerReviewsMap["${rev.orderId}-${rev.revieweeId}"] = rev.rating
                        buyerReviewsMap[rev.orderId] = rev.rating
                    }
                } catch (_: Exception) {}
            }
            Result.success(buyerReviewsMap.toMap())
        } catch (e: Exception) {
            Result.success(buyerReviewsMap.toMap())
        }
    }

    override suspend fun getSellerDashboardStatistics(sellerId: String, range: String): Result<SellerDashboardStats> = withContext(Dispatchers.IO) {
        try {
            val dbRange = when (range.lowercase()) {
                "hoy", "today" -> "today"
                "semana", "week" -> "week"
                else -> "all"
            }

            if (postgrest != null && isValidUUID(sellerId)) {
                try {
                    val rpcParams = buildJsonObject {
                        put("p_seller_id", sellerId)
                        put("p_time_range", dbRange)
                    }
                    val jsonElement = postgrest.rpc("get_seller_dashboard_statistics", rpcParams).decodeAs<JsonObject>()

                    val totalEarnings = jsonElement["total_earnings"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val completedCount = jsonElement["completed_count"]?.jsonPrimitive?.intOrNull ?: 0
                    val cancelledCount = jsonElement["cancelled_count"]?.jsonPrimitive?.intOrNull ?: 0
                    val inProgressCount = jsonElement["in_progress_count"]?.jsonPrimitive?.intOrNull ?: 0
                    val totalOrdersCount = jsonElement["total_orders_count"]?.jsonPrimitive?.intOrNull ?: 0
                    val avgTicket = jsonElement["average_ticket"]?.jsonPrimitive?.doubleOrNull ?: 0.0

                    val topProducts = jsonElement["top_products"]?.jsonArray?.mapNotNull { el ->
                        val obj = el as? JsonObject ?: return@mapNotNull null
                        val name = obj["product_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val units = obj["units_sold"]?.jsonPrimitive?.intOrNull ?: 0
                        val amt = obj["total_amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        TopProductStat(name, units, amt)
                    } ?: emptyList()

                    val hourlyDist = jsonElement["hourly_distribution"]?.jsonArray?.mapNotNull { el ->
                        val obj = el as? JsonObject ?: return@mapNotNull null
                        val slot = obj["slot"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val count = obj["order_count"]?.jsonPrimitive?.intOrNull ?: 0
                        HourlyDemandStat(slot, count)
                    } ?: emptyList()

                    val topMeetingPoints = jsonElement["top_meeting_points"]?.jsonArray?.mapNotNull { el ->
                        val obj = el as? JsonObject ?: return@mapNotNull null
                        val ptName = obj["point_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val count = obj["delivery_count"]?.jsonPrimitive?.intOrNull ?: 0
                        MeetingPointStat(ptName, count)
                    } ?: emptyList()

                    val stats = SellerDashboardStats(
                        totalEarnings = totalEarnings,
                        completedCount = completedCount,
                        cancelledCount = cancelledCount,
                        inProgressCount = inProgressCount,
                        totalOrdersCount = totalOrdersCount,
                        averageTicket = avgTicket,
                        topProducts = topProducts,
                        hourlyDistribution = hourlyDist,
                        topMeetingPoints = topMeetingPoints
                    )

                    val cachedCount = sellerSubOrdersCache[sellerId]?.size ?: 0
                    if (totalOrdersCount > 0 || cachedCount == 0) {
                        val finalStats = if (topProducts.isEmpty() && completedCount > 0) {
                            val cachedOrders = sellerSubOrdersCache[sellerId] ?: emptyList()
                            val localStats = computeLocalSellerStats(cachedOrders, dbRange)
                            if (localStats.topProducts.isNotEmpty()) {
                                stats.copy(topProducts = localStats.topProducts)
                            } else {
                                stats
                            }
                        } else {
                            stats
                        }
                        return@withContext Result.success(finalStats)
                    }
                } catch (_: Exception) {
                    // Si RPC no está desplegado o falla la red, procedemos al cálculo con caché local
                }
            }

            // Fallback: cálculo local reactivo usando subpedidos en memoria/caché
            val cachedOrders = sellerSubOrdersCache[sellerId]
            val subOrders = if (!cachedOrders.isNullOrEmpty()) {
                cachedOrders
            } else {
                getSubOrdersForSeller(sellerId).getOrDefault(emptyList())
            }
            val stats = computeLocalSellerStats(subOrders, dbRange)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun computeLocalSellerStats(subOrders: List<SubOrder>, dbRange: String): SellerDashboardStats {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        val startOfWeek = startOfToday - (6L * 24 * 60 * 60 * 1000)

        val filtered = when (dbRange) {
            "today" -> subOrders.filter { parseIso(it.createdAt) >= startOfToday }
            "week" -> subOrders.filter { parseIso(it.createdAt) >= startOfWeek }
            else -> subOrders
        }

        val completed = filtered.filter { it.status == SubOrderStatus.COMPLETADO || it.status == SubOrderStatus.PAGO_CONFIRMADO }
        val cancelled = filtered.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }
        val inProgress = filtered.filter {
            it.status == SubOrderStatus.PENDIENTE ||
            it.status == SubOrderStatus.ACEPTADO ||
            it.status == SubOrderStatus.EN_PREPARACION ||
            it.status == SubOrderStatus.LISTO ||
            it.status == SubOrderStatus.ESPERANDO_ENTREGA
        }

        val totalEarnings = completed.sumOf { it.subtotalAmount }
        val completedCount = completed.size
        val avgTicket = if (completedCount > 0) totalEarnings / completedCount else 0.0

        val productCounts = mutableMapOf<String, Pair<Int, Double>>()
        completed.forEach { order ->
            order.items.forEach { item ->
                val cur = productCounts.getOrDefault(item.productName, Pair(0, 0.0))
                productCounts[item.productName] = Pair(cur.first + item.quantity, cur.second + (item.unitPrice * item.quantity))
            }
        }
        val topProducts = productCounts.entries
            .map { (name, pair) -> TopProductStat(name, pair.first, pair.second) }
            .sortedByDescending { it.totalAmount }
            .take(5)

        val pointCounts = mutableMapOf<String, Int>()
        filtered.forEach { order ->
            val pt = order.meetingPointName?.trim().takeIf { !it.isNullOrBlank() } ?: "Punto por acordar"
            pointCounts[pt] = pointCounts.getOrDefault(pt, 0) + 1
        }
        val topPoints = pointCounts.entries
            .map { MeetingPointStat(it.key, it.value) }
            .sortedByDescending { it.deliveryCount }
            .take(4)

        var morning = 0
        var lunch = 0
        var afternoon = 0
        var night = 0
        filtered.forEach { order ->
            val hour = getHourOfDay(order.createdAt)
            when (hour) {
                in 8..11 -> morning++
                in 12..14 -> lunch++
                in 15..17 -> afternoon++
                else -> night++
            }
        }
        val hourly = listOf(
            HourlyDemandStat("morning", morning),
            HourlyDemandStat("lunch", lunch),
            HourlyDemandStat("afternoon", afternoon),
            HourlyDemandStat("night", night)
        )

        return SellerDashboardStats(
            totalEarnings = totalEarnings,
            completedCount = completedCount,
            cancelledCount = cancelled.size,
            inProgressCount = inProgress.size,
            totalOrdersCount = filtered.size,
            averageTicket = avgTicket,
            topProducts = topProducts,
            hourlyDistribution = hourly,
            topMeetingPoints = topPoints
        )
    }

    private fun parseIso(isoDate: String?): Long {
        if (isoDate.isNullOrBlank()) return 0L
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
                val d = sdf.parse(isoDate)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun getHourOfDay(isoDate: String?): Int {
        val t = parseIso(isoDate)
        if (t <= 0L) return 12
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply { timeInMillis = t }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun isValidUUID(value: String): Boolean = try {
        UUID.fromString(value)
        true
    } catch (_: Exception) {
        false
    }

    override suspend fun reportIncident(
        subOrderId: String?,
        reporterId: String?,
        reportedUserId: String?,
        incidentType: String,
        details: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                postgrest.from("order_incidents").insert(
                    buildJsonObject {
                        put("id", UUID.randomUUID().toString())
                        subOrderId?.let { put("sub_order_id", it) }
                        reporterId?.let { put("reporter_id", it) }
                        reportedUserId?.let { put("reported_user_id", it) }
                        put("incident_type", incidentType)
                        put("details", details.trim())
                        put("status", "PENDIENTE")
                    }
                )
                return@withContext Result.success(Unit)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("OrderRepo", "Error al registrar reporte/incidencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserWarnings(userId: String): Result<List<ProfileWarning>> = withContext(Dispatchers.IO) {
        try {
            if (postgrest != null) {
                val warnings = postgrest.from("profile_warnings").select {
                    filter {
                        eq("profile_id", userId)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<ProfileWarning>()
                return@withContext Result.success(warnings)
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            android.util.Log.e("OrderRepo", "Error al obtener advertencias de usuario: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeUserWarnings(userId: String): Flow<List<ProfileWarning>> = flow {
        while (true) {
            val res = getUserWarnings(userId)
            emit(res.getOrDefault(emptyList()))
            delay(15000)
        }
    }.flowOn(Dispatchers.IO)
}
