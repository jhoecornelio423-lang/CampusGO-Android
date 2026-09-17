package com.example.vallego.domain.usecase

import com.example.vallego.data.repository.CartRepositoryImpl
import com.example.vallego.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartAndSuborderTest {

    private val calculateCartUseCase = CalculateCartUseCase()
    private val createOrderUseCase = CreateOrderWithSubordersUseCase()
    private val recalculateOrderUseCase = RecalculateOrderUseCase()

    private val burger = Product(
        id = "p-burger",
        sellerId = "seller-papu",
        name = "Hamburguesa Clásica",
        price = 10.0,
        stock = 20
    )

    private val soda = Product(
        id = "p-soda",
        sellerId = "seller-papu",
        name = "Gaseosa 500ml",
        price = 3.0,
        stock = 50
    )

    private val brownie = Product(
        id = "p-brownie",
        sellerId = "seller-dulce",
        name = "Brownie con Helado",
        price = 5.0,
        stock = 15
    )

    private val coffee = Product(
        id = "p-coffee",
        sellerId = "seller-coffee",
        name = "Café Americano",
        price = 6.0,
        stock = 30
    )

    @Test
    fun testMultiVendorCartGroupingAndSubtotals() {
        val items = listOf(
            CartItem(product = burger, quantity = 2), // 20.0
            CartItem(product = soda, quantity = 1),   // 3.0 (Papu subtotal = 23.0)
            CartItem(product = brownie, quantity = 2),// 10.0 (Dulce subtotal = 10.0)
            CartItem(product = coffee, quantity = 1)  // 6.0  (Coffee subtotal = 6.0)
        )

        val storeNames = mapOf(
            "seller-papu" to "Papu Burger",
            "seller-dulce" to "Dulce Valle",
            "seller-coffee" to "Coffee Campus"
        )

        val result = calculateCartUseCase(items, storeNames)

        assertEquals(3, result.storeGroups.size)
        assertEquals(39.0, result.grandTotal, 0.001)
        assertEquals(6, result.totalItemCount)

        val papuGroup = result.storeGroups.first { it.sellerId == "seller-papu" }
        assertEquals("Papu Burger", papuGroup.sellerName)
        assertEquals(23.0, papuGroup.subtotal, 0.001)
        assertEquals(2, papuGroup.items.size)

        val dulceGroup = result.storeGroups.first { it.sellerId == "seller-dulce" }
        assertEquals("Dulce Valle", dulceGroup.sellerName)
        assertEquals(10.0, dulceGroup.subtotal, 0.001)

        val coffeeGroup = result.storeGroups.first { it.sellerId == "seller-coffee" }
        assertEquals("Coffee Campus", coffeeGroup.sellerName)
        assertEquals(6.0, coffeeGroup.subtotal, 0.001)
    }

    @Test
    fun testCartRepositoryMutations() {
        val cartRepo = CartRepositoryImpl(calculateCartUseCase)
        cartRepo.setStoreName("seller-papu", "Papu Burger")

        cartRepo.addToCart(burger, quantity = 2)
        assertEquals(1, cartRepo.items.value.size)
        assertEquals(20.0, cartRepo.cartCalculation.value.grandTotal, 0.001)

        // Add more of same product
        cartRepo.addToCart(burger, quantity = 1)
        assertEquals(3, cartRepo.items.value.first().quantity)
        assertEquals(30.0, cartRepo.cartCalculation.value.grandTotal, 0.001)

        // Update quantity
        cartRepo.updateQuantity("p-burger", quantity = 1)
        assertEquals(1, cartRepo.items.value.first().quantity)
        assertEquals(10.0, cartRepo.cartCalculation.value.grandTotal, 0.001)

        // Remove
        cartRepo.removeFromCart("p-burger")
        assertTrue(cartRepo.items.value.isEmpty())
        assertEquals(0.0, cartRepo.cartCalculation.value.grandTotal, 0.001)
    }

    @Test
    fun testCreateOrderWithSubordersAtomicHierarchy() {
        val items = listOf(
            CartItem(product = burger, quantity = 2),
            CartItem(product = brownie, quantity = 2)
        )
        val cartResult = calculateCartUseCase(items, mapOf("seller-papu" to "Papu Burger", "seller-dulce" to "Dulce Valle"))

        val buyer = UserProfile(
            id = "buyer-123",
            fullName = "Juan Pérez",
            phone = "987654321",
            role = UserRole.COMPRADOR
        )

        val meetingPoint = CampusMeetingPoint(
            id = "mp-biblio",
            name = "Biblioteca - Puerta Principal",
            pavilion = "Central"
        )

        val order = createOrderUseCase(
            buyerProfile = buyer,
            meetingPoint = meetingPoint,
            scheduledTime = "13:00",
            paymentMethod = PaymentMethod.YAPE,
            cartResult = cartResult,
            notes = "Por favor llamar al llegar"
        )

        assertEquals("buyer-123", order.buyerId)
        assertEquals("Juan Pérez", order.buyerName)
        assertEquals("Biblioteca - Puerta Principal", order.meetingPointName)
        assertEquals("13:00", order.scheduledTime)
        assertEquals(30.0, order.totalAmount, 0.001)
        assertEquals(OrderStatus.PENDIENTE, order.status)
        assertEquals(2, order.subOrders.size)

        val subOrderPapu = order.subOrders.first { it.sellerId == "seller-papu" }
        assertEquals(20.0, subOrderPapu.subtotalAmount, 0.001)
        assertEquals(PaymentMethod.YAPE, subOrderPapu.paymentMethod)
        assertEquals(order.id, subOrderPapu.orderId)
    }

    @Test
    fun testRecalculateOrderWhenSellerRejects() {
        // Orden original de S/39: Papu (S/23), Dulce (S/10), Coffee (S/6)
        val sub1 = SubOrder(
            id = "sub-1",
            orderId = "ord-500",
            sellerId = "seller-papu",
            sellerName = "Papu Burger",
            subtotalAmount = 23.0,
            status = SubOrderStatus.ACEPTADO
        )
        val sub2 = SubOrder(
            id = "sub-2",
            orderId = "ord-500",
            sellerId = "seller-dulce",
            sellerName = "Dulce Valle",
            subtotalAmount = 10.0,
            status = SubOrderStatus.ACEPTADO
        )
        val sub3 = SubOrder(
            id = "sub-3",
            orderId = "ord-500",
            sellerId = "seller-coffee",
            sellerName = "Coffee Campus",
            subtotalAmount = 6.0,
            status = SubOrderStatus.PENDIENTE
        )

        val initialOrder = Order(
            id = "ord-500",
            buyerId = "buyer-1",
            meetingPointId = "mp-1",
            scheduledTime = "13:00",
            totalAmount = 39.0,
            status = OrderStatus.PENDIENTE,
            subOrders = listOf(sub1, sub2, sub3)
        )

        // Coffee Campus rechaza su subpedido
        val rejectedSub3 = sub3.copy(status = SubOrderStatus.RECHAZADO, rejectionReason = "Sin stock de café")
        val recalculatedOrder = recalculateOrderUseCase(initialOrder, rejectedSub3)

        // El nuevo total debe excluir los S/6 rechazados -> S/33.0
        assertEquals(33.0, recalculatedOrder.totalAmount, 0.001)
        assertEquals(OrderStatus.PARCIALMENTE_ACEPTADA, recalculatedOrder.status)
    }

    @Test
    fun testMeetingPointIntersectionLogicMultiVendor() {
        val mp1 = CampusMeetingPoint(id = "mp-1", name = "Puerta 1 - Exterior", zoneType = "EXTERIOR")
        val mp2 = CampusMeetingPoint(id = "mp-2", name = "Puerta 2 - Panamericana", zoneType = "EXTERIOR")
        val mp3 = CampusMeetingPoint(id = "mp-3", name = "Cafetería Central", zoneType = "INTERIOR")
        val activePoints = listOf(mp1, mp2, mp3)

        val seller1 = UserProfile(
            id = "s-1",
            fullName = "Vendedor A",
            role = UserRole.EMPRENDEDOR,
            supportedMeetingPoints = listOf("mp-1", "mp-2")
        )
        val seller2 = UserProfile(
            id = "s-2",
            fullName = "Vendedor B",
            role = UserRole.EMPRENDEDOR,
            supportedMeetingPoints = listOf("mp-2", "mp-3")
        )
        val seller3WithoutRestrictions = UserProfile(
            id = "s-3",
            fullName = "Vendedor C",
            role = UserRole.EMPRENDEDOR,
            supportedMeetingPoints = emptyList() // Atiende todos los puntos activos por defecto
        )

        val sellers = listOf(seller1, seller2, seller3WithoutRestrictions)

        // Caso 1: Solo seller1 -> mp1, mp2
        val seller1Points = listOf("s-1").map { sId ->
            val s = sellers.first { it.id == sId }
            if (s.supportedMeetingPoints.isNotEmpty()) s.supportedMeetingPoints.toSet() else activePoints.map { it.id }.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertEquals(setOf("mp-1", "mp-2"), seller1Points)

        // Caso 2: Intersección seller1 y seller2 -> solo mp-2 en común
        val common1And2 = listOf("s-1", "s-2").map { sId ->
            val s = sellers.first { it.id == sId }
            if (s.supportedMeetingPoints.isNotEmpty()) s.supportedMeetingPoints.toSet() else activePoints.map { it.id }.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertEquals(setOf("mp-2"), common1And2)

        // Caso 3: Intersección seller1 y seller3 (seller3 sin restricciones) -> mp-1 y mp-2
        val common1And3 = listOf("s-1", "s-3").map { sId ->
            val s = sellers.first { it.id == sId }
            if (s.supportedMeetingPoints.isNotEmpty()) s.supportedMeetingPoints.toSet() else activePoints.map { it.id }.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertEquals(setOf("mp-1", "mp-2"), common1And3)

        // Caso 4: Vendedor con puntos disjuntos -> intersección vacía
        val sellerDisjoint = UserProfile(
            id = "s-4",
            fullName = "Vendedor D",
            role = UserRole.EMPRENDEDOR,
            supportedMeetingPoints = listOf("mp-3")
        )
        val disjointCommon = listOf(seller1, sellerDisjoint).map { s ->
            if (s.supportedMeetingPoints.isNotEmpty()) s.supportedMeetingPoints.toSet() else activePoints.map { it.id }.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertTrue(disjointCommon.isEmpty())
    }

    @Test
    fun testPaymentMethodIntersectionLogicMultiVendor() {
        val seller1 = UserProfile(
            id = "s-1",
            fullName = "Vendedor A",
            role = UserRole.EMPRENDEDOR,
            supportedPaymentMethods = listOf("EFECTIVO", "YAPE")
        )
        val seller2 = UserProfile(
            id = "s-2",
            fullName = "Vendedor B",
            role = UserRole.EMPRENDEDOR,
            supportedPaymentMethods = listOf("YAPE", "PLIN")
        )
        val sellerDefault = UserProfile(
            id = "s-3",
            fullName = "Vendedor C",
            role = UserRole.EMPRENDEDOR,
            supportedPaymentMethods = emptyList() // effective = EFECTIVO, YAPE, PLIN
        )

        // Caso 1: Vendedor A sólo acepta Efectivo y Yape
        assertEquals(listOf("EFECTIVO", "YAPE"), seller1.effectivePaymentMethods)

        // Caso 2: Intersección entre Vendedor A y Vendedor B -> Solo YAPE en común
        val common1And2 = listOf(seller1, seller2).map { s ->
            s.effectivePaymentMethods.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertEquals(setOf("YAPE"), common1And2)

        // Caso 3: Intersección entre Vendedor A y Vendedor C (por defecto) -> EFECTIVO, YAPE
        val common1And3 = listOf(seller1, sellerDefault).map { s ->
            s.effectivePaymentMethods.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertEquals(setOf("EFECTIVO", "YAPE"), common1And3)

        // Caso 4: Vendedor sólo Efectivo vs Vendedor sólo Plin -> Intersección vacía
        val sellerCashOnly = UserProfile(id = "s-4", fullName = "Vendedor D", supportedPaymentMethods = listOf("EFECTIVO"))
        val sellerPlinOnly = UserProfile(id = "s-5", fullName = "Vendedor E", supportedPaymentMethods = listOf("PLIN"))
        val disjoint = listOf(sellerCashOnly, sellerPlinOnly).map { s ->
            s.effectivePaymentMethods.toSet()
        }.reduce { acc, set -> acc.intersect(set) }
        assertTrue(disjoint.isEmpty())
    }

    @Test
    fun testSellerPaymentMethodsStorageFallbackAndEnrichment() {
        val storage = com.example.vallego.data.repository.SellerPaymentMethodsStorage
        val sellerId = "seller-yape-only"

        // 1. Vendedor guarda solo "YAPE"
        storage.saveMethods(sellerId, listOf("YAPE"))
        assertEquals(listOf("YAPE"), storage.getMethods(sellerId))

        // 2. Simular respuesta de Supabase sin columna (supportedPaymentMethods = emptyList)
        val profileFromDbWithoutColumn = UserProfile(
            id = sellerId,
            fullName = "Mi Tienda",
            role = UserRole.EMPRENDEDOR,
            supportedPaymentMethods = emptyList()
        )

        // 3. Al enriquecer el perfil, debe recuperar "YAPE" de la caché local y no reestablecer a todos
        val enrichedProfile = storage.enrichProfile(profileFromDbWithoutColumn)
        assertEquals(listOf("YAPE"), enrichedProfile.supportedPaymentMethods)
        assertEquals(listOf("YAPE"), enrichedProfile.effectivePaymentMethods)

        // 4. Si luego Supabase ya devuelve columna con "YAPE", se mantiene
        val profileWithDbData = UserProfile(
            id = sellerId,
            fullName = "Mi Tienda",
            role = UserRole.EMPRENDEDOR,
            supportedPaymentMethods = listOf("YAPE")
        )
        val verifiedProfile = storage.enrichProfile(profileWithDbData)
        assertEquals(listOf("YAPE"), verifiedProfile.supportedPaymentMethods)
    }
}

