package com.example.vallego.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testCartItemSubtotal() {
        val product = Product(
            id = "prod-1",
            sellerId = "seller-1",
            name = "Hamburguesa Clásica",
            price = 12.50,
            stock = 10
        )
        val cartItem = CartItem(product = product, quantity = 3)
        assertEquals(37.50, cartItem.subtotal, 0.001)
    }

    @Test
    fun testSubOrderStatusFlags() {
        assertTrue(SubOrderStatus.COMPLETADO.isFinal)
        assertTrue(SubOrderStatus.RECHAZADO.isFinal)
        assertTrue(SubOrderStatus.CANCELADO.isFinal)
        assertTrue(SubOrderStatus.CANCELADO.isTerminalCancelled)
        assertFalse(SubOrderStatus.PENDIENTE.isFinal)
        assertFalse(SubOrderStatus.EN_PREPARACION.isFinal)
    }

    @Test
    fun testProductJsonSerialization() {
        val product = Product(
            id = "prod-1",
            sellerId = "seller-1",
            name = "Hamburguesa Clásica",
            price = 12.50,
            stock = 10
        )
        val encoded = json.encodeToString(product)
        assertNotNull(encoded)
        assertTrue(encoded.contains("\"seller_id\""))
        val decodedProduct = json.decodeFromString<Product>(encoded)
        assertEquals(product.sellerId, decodedProduct.sellerId)
        assertEquals(product.price, decodedProduct.price, 0.001)
        assertEquals(product.stock, decodedProduct.stock)
    }

    @Test
    fun testOrderAndSubOrderHierarchySerialization() {
        val papuBurger = SubOrder(
            id = "sub-1",
            orderId = "order-500",
            sellerId = "seller-1",
            sellerName = "Papu Burger",
            subtotalAmount = 23.00,
            status = SubOrderStatus.COMPLETADO
        )
        val dulceValle = SubOrder(
            id = "sub-2",
            orderId = "order-500",
            sellerId = "seller-2",
            sellerName = "Dulce Valle",
            subtotalAmount = 10.00,
            status = SubOrderStatus.PENDIENTE
        )
        val order = Order(
            id = "order-500",
            buyerId = "buyer-123",
            meetingPointId = "mp-1",
            scheduledTime = "13:00",
            totalAmount = 33.00,
            subOrders = listOf(papuBurger, dulceValle)
        )
        val orderJson = json.encodeToString(order)
        val decodedOrder = json.decodeFromString<Order>(orderJson)
        assertEquals(2, decodedOrder.subOrders.size)
        assertEquals(papuBurger.sellerName, decodedOrder.subOrders[0].sellerName)
        assertEquals(papuBurger.subtotalAmount, decodedOrder.subOrders[0].subtotalAmount, 0.001)
        assertEquals(papuBurger.status, decodedOrder.subOrders[0].status)
        assertEquals(dulceValle.sellerName, decodedOrder.subOrders[1].sellerName)
        assertEquals(dulceValle.subtotalAmount, decodedOrder.subOrders[1].subtotalAmount, 0.001)
        assertEquals(dulceValle.status, decodedOrder.subOrders[1].status)
    }

    @Test
    fun testCampusGoMeetingPointAndZoneTypeSerialization() {
        val meetingPoint = CampusMeetingPoint(
            id = "mp-puerta-1",
            name = "Cafetería Central",
            pavilion = "Pabellón A",
            zoneType = "INTERIOR",
            isActive = true
        )
        val jsonStr = json.encodeToString(meetingPoint)
        assertTrue(jsonStr.contains("\"zone_type\""))
        assertTrue(jsonStr.contains("INTERIOR"))

        val decoded = json.decodeFromString<CampusMeetingPoint>(jsonStr)
        assertEquals("INTERIOR", decoded.zoneType)
        assertEquals("Cafetería Central", decoded.name)

        // Fallback al valor por defecto EXTERIOR cuando se omite en el payload
        val jsonWithoutZone = """{"id":"mp-default","name":"Punto Default"}"""
        val decodedDefault = json.decodeFromString<CampusMeetingPoint>(jsonWithoutZone)
        assertEquals("EXTERIOR", decodedDefault.zoneType)
    }

    @Test
    fun testUserProfileSupportedMeetingPointsSerialization() {
        val profile = UserProfile(
            id = "seller-100",
            fullName = "Doña Carmen",
            role = UserRole.EMPRENDEDOR,
            businessName = "Empanadas del Valle",
            supportedMeetingPoints = listOf("mp-1", "mp-2")
        )
        val jsonStr = json.encodeToString(profile)
        assertTrue(jsonStr.contains("\"supported_meeting_points\""))

        val decoded = json.decodeFromString<UserProfile>(jsonStr)
        assertEquals(2, decoded.supportedMeetingPoints.size)
        assertTrue(decoded.supportedMeetingPoints.contains("mp-1"))
        assertTrue(decoded.supportedMeetingPoints.contains("mp-2"))
    }

    @Test
    fun testSubOrderDeliverySnapshotFields() {
        val subOrder = SubOrder(
            id = "sub-10",
            orderId = "order-20",
            sellerId = "seller-5",
            sellerName = "Juguería Campus",
            subtotalAmount = 15.0,
            meetingPointId = "mp-puerta-2",
            meetingPointName = "Puerta 2 - Reja Auxiliar",
            scheduledTime = "14:30",
            buyerName = "Carlos Mendoza",
            buyerPhone = "998877665",
            notes = "Sin azúcar por favor"
        )
        val jsonStr = json.encodeToString(subOrder)
        val decoded = json.decodeFromString<SubOrder>(jsonStr)
        assertEquals("Puerta 2 - Reja Auxiliar", decoded.meetingPointName)
        assertEquals("Carlos Mendoza", decoded.buyerName)
        assertEquals("998877665", decoded.buyerPhone)
        assertEquals("Sin azúcar por favor", decoded.notes)
    }
}