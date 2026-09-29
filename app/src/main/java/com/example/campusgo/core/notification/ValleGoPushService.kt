package com.example.campusgo.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.campusgo.data.repository.RemoteOrderDto
import com.example.campusgo.data.repository.RemoteOrderItemDto
import com.example.campusgo.data.repository.RemoteSubOrderDto
import com.example.campusgo.data.repository.RemoteOrderMessageDto
import com.example.campusgo.data.repository.ProfileBasicDto
import com.example.campusgo.data.repository.ProductBasicDto
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.chat.ActiveChatSessionManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ValleGoPushService : Service(), KoinComponent {

    private val postgrest: Postgrest by inject()
    private val auth: Auth by inject()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var monitoringJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val seenSellerOrderIds = mutableSetOf<String>()
    private var isSellerFirstRun = true

    private val lastKnownBuyerStatuses = mutableMapOf<String, String>()
    private val lastKnownBuyerSubOrderStatuses = mutableMapOf<String, String>()
    private var isBuyerFirstRun = true
    private val productNameCache = ConcurrentHashMap<String, String>()
    private val sellerNameCache = ConcurrentHashMap<String, String>()
    private val profileNameCache = ConcurrentHashMap<String, String>()
    private val orderItemsSummaryCache = ConcurrentHashMap<String, String>()
    private var cachedUserRole: Pair<String, String>? = null
    private var userRoleCachedAt: Long = 0L
    private var lastSessionRefreshTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        ValleGoNotificationHelper.createNotificationChannels(this)
        val ongoingNotification = ValleGoNotificationHelper.getForegroundServiceNotification(
            context = this,
            title = "CampusGO",
            content = "Monitoreando pedidos y notificaciones en campus"
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    ValleGoNotificationHelper.SERVICE_NOTIFICATION_ID,
                    ongoingNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    ValleGoNotificationHelper.SERVICE_NOTIFICATION_ID,
                    ongoingNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(ValleGoNotificationHelper.SERVICE_NOTIFICATION_ID, ongoingNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando servicio en primer plano", e)
        }

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ValleGo:PushServiceWakeLock")?.apply {
            setReferenceCounted(false)
        }

        PushWatchdogReceiver.scheduleNextWatchdog(applicationContext)
        Log.d(TAG, "ValleGoPushService iniciado en primer plano.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startOrderMonitoringLoop()
        PushWatchdogReceiver.scheduleNextWatchdog(applicationContext)
        return START_STICKY
    }

    private suspend fun ensureValidSession() {
        val now = System.currentTimeMillis()
        // Supabase JWT expira a los 60 min. Refrescar proactivamente cada 40 minutos en segundo plano
        if (now - lastSessionRefreshTime > 40 * 60 * 1000L) {
            try {
                if (auth.currentSessionOrNull() != null) {
                    auth.refreshCurrentSession()
                    lastSessionRefreshTime = now
                    Log.d(TAG, "Sesión de Supabase refrescada automáticamente en segundo plano.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso al refrescar sesión preventivamente: ${e.message}")
            }
        }
    }

    private fun isRecent(dateStr: String?, maxAgeMinutes: Long = 30): Boolean {
        if (dateStr.isNullOrBlank()) return true
        return try {
            val cleanStr = dateStr.substringBefore(".").substringBefore("+").substringBefore("Z")
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(cleanStr) ?: return true
            (System.currentTimeMillis() - date.time) < (maxAgeMinutes * 60 * 1000L)
        } catch (_: Exception) {
            true
        }
    }

    private fun isAlreadyNotified(eventKey: String): Boolean {
        val prefs = getSharedPreferences("vallego_notifs_cache", Context.MODE_PRIVATE)
        return prefs.getBoolean(eventKey, false)
    }

    private fun markAsNotified(eventKey: String) {
        val prefs = getSharedPreferences("vallego_notifs_cache", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(eventKey, true).apply()
    }

    private suspend fun getOrderItemsSummary(orderId: String, subOrderId: String? = null): String {
        val cacheKey = subOrderId ?: orderId
        val cached = orderItemsSummaryCache[cacheKey]
        if (!cached.isNullOrBlank()) return cached

        return try {
            val items = if (subOrderId != null) {
                val subItems = try {
                    postgrest.from("order_items")
                        .select { filter { eq("sub_order_id", subOrderId) } }
                        .decodeList<RemoteOrderItemDto>()
                } catch (_: Exception) {
                    emptyList()
                }
                if (subItems.isNotEmpty()) subItems else {
                    try {
                        postgrest.from("order_items")
                            .select { filter { eq("order_id", orderId) } }
                            .decodeList<RemoteOrderItemDto>()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            } else {
                try {
                    postgrest.from("order_items")
                        .select { filter { eq("order_id", orderId) } }
                        .decodeList<RemoteOrderItemDto>()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (items.isEmpty()) return ""

            val missingProductIds = items.map { it.productId }
                .filter { it.isNotBlank() && !productNameCache.containsKey(it) }
                .distinct()

            if (missingProductIds.isNotEmpty()) {
                try {
                    val prods = postgrest.from("products")
                        .select { filter { isIn("id", missingProductIds) } }
                        .decodeList<ProductBasicDto>()
                    for (p in prods) {
                        productNameCache[p.id] = p.name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val summary = items.joinToString(", ") { item ->
                val name = productNameCache[item.productId] ?: "Producto"
                "$name (x${item.quantity})"
            }
            if (summary.isNotBlank()) {
                orderItemsSummaryCache[cacheKey] = summary
            }
            summary
        } catch (e: Exception) {
            ""
        }
    }

    private fun startOrderMonitoringLoop() {
        if (monitoringJob?.isActive == true) {
            Log.d(TAG, "Bucle de monitoreo ya activo. Se omite duplicación.")
            return
        }
        monitoringJob = serviceScope.launch {
            Log.d(TAG, "Iniciando bucle de monitoreo de pedidos en segundo plano...")
            while (isActive) {
                try {
                    try { wakeLock?.acquire(3000L) } catch (_: Exception) {}
                    val user = auth.currentUserOrNull()
                    if (user != null) {
                        val userId = user.id
                        // Verificar y refrescar proactivamente la sesión para evitar JWT expired en segundo plano
                        ensureValidSession()

                        // Determinar rol con cache TTL de 5 minutos para no saturar Supabase con 15 requests/min
                        val roleStr = if (cachedUserRole?.first == userId && (System.currentTimeMillis() - userRoleCachedAt < 5 * 60 * 1000L)) {
                            cachedUserRole!!.second
                        } else {
                            val profile = runCatching {
                                postgrest.from("profiles")
                                    .select {
                                        filter { eq("id", userId) }
                                    }
                                    .decodeSingleOrNull<UserProfile>()
                            }.getOrNull()
                            val resolved = profile?.role?.name?.lowercase() ?: "comprador"
                            cachedUserRole = Pair(userId, resolved)
                            userRoleCachedAt = System.currentTimeMillis()
                            resolved
                        }

                        if (roleStr == "emprendedor" || roleStr == "admin") {
                            monitorSellerOrders(userId)
                        } else {
                            monitorBuyerOrders(userId)
                        }
                        monitorChatMessages(userId)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error durante chequeo de pedidos: ${e.message}")
                    if (e.message?.contains("JWT", ignoreCase = true) == true ||
                        e.message?.contains("expired", ignoreCase = true) == true ||
                        e.message?.contains("401") == true) {
                        runCatching { auth.refreshCurrentSession() }
                    }
                } finally {
                    if (wakeLock?.isHeld == true) {
                        try { wakeLock?.release() } catch (_: Exception) {}
                    }
                }
                delay(4000)
            }
        }
    }

    private suspend fun monitorSellerOrders(sellerId: String) {
        val subOrders = try {
            postgrest.from("sub_orders")
                .select {
                    filter { eq("seller_id", sellerId) }
                }
                .decodeList<RemoteSubOrderDto>()
        } catch (e: Exception) {
            if (e.message?.contains("JWT", ignoreCase = true) == true || e.message?.contains("expired", ignoreCase = true) == true) {
                runCatching { auth.refreshCurrentSession() }
            }
            emptyList()
        }

        val legacyOrders = try {
            postgrest.from("orders")
                .select {
                    filter { eq("seller_id", sellerId) }
                }
                .decodeList<RemoteOrderDto>()
        } catch (e: Exception) {
            if (e.message?.contains("JWT", ignoreCase = true) == true || e.message?.contains("expired", ignoreCase = true) == true) {
                runCatching { auth.refreshCurrentSession() }
            }
            emptyList()
        }

        val existingSubOrderOrderIds = subOrders.map { it.orderId }.toSet()
        val missingFromSubOrders = legacyOrders.filter { it.id !in existingSubOrderOrderIds }
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

        val allSubs = (subOrders + synthesizedSubs).sortedByDescending { it.createdAt }

        if (isSellerFirstRun) {
            allSubs.forEach { sub ->
                val isTerminal = sub.status.lowercase() in listOf("completed", "completado", "rejected", "rechazado", "cancelled", "cancelado")
                val isOld = !isRecent(sub.createdAt, maxAgeMinutes = 30)
                if (isTerminal || isOld) {
                    seenSellerOrderIds.add(sub.id)
                    markAsNotified("seller_sub_${sub.id}")
                }
            }
            isSellerFirstRun = false
        }

        for (sub in allSubs) {
            val eventKey = "seller_sub_${sub.id}"
            if (isAlreadyNotified(eventKey)) {
                seenSellerOrderIds.add(sub.id)
                continue
            }

            val isNew = !seenSellerOrderIds.contains(sub.id)
            val isPending = sub.status.lowercase() in listOf("pending", "pendiente")

            if (isPending && (isNew || !isAlreadyNotified(eventKey))) {
                seenSellerOrderIds.add(sub.id)
                markAsNotified(eventKey)
                val itemsSummary = getOrderItemsSummary(orderId = sub.orderId, subOrderId = sub.id)
                val totalStr = String.format(java.util.Locale.US, "%.2f", sub.subtotalAmount)
                val messageText = if (itemsSummary.isNotBlank()) {
                    "Has recibido un pedido de $itemsSummary por S/. $totalStr. Toca para atenderlo."
                } else {
                    "Has recibido un nuevo pedido por S/. $totalStr. Toca para atenderlo."
                }
                ValleGoNotificationHelper.showOrderNotification(
                    context = this@ValleGoPushService,
                    notificationId = sub.id.hashCode(),
                    title = "¡Nuevo pedido recibido!",
                    message = messageText,
                    orderId = sub.orderId
                )
            } else {
                seenSellerOrderIds.add(sub.id)
            }
        }
    }

    private suspend fun monitorBuyerOrders(buyerId: String) {
        val rawOrders = try {
            postgrest.from("orders")
                .select {
                    filter { eq("buyer_id", buyerId) }
                }
                .decodeList<RemoteOrderDto>()
        } catch (e: Exception) {
            if (e.message?.contains("JWT", ignoreCase = true) == true || e.message?.contains("expired", ignoreCase = true) == true) {
                runCatching { auth.refreshCurrentSession() }
            }
            emptyList()
        }

        // Optimización: Limitar el escaneo a las últimas 20 órdenes para no saturar memoria ni red
        val orders = rawOrders.sortedByDescending { it.createdAt }.take(20)

        val orderMap = orders.associateBy { it.id }
        val orderIds = orders.map { it.id }

        val subOrders = if (orderIds.isNotEmpty()) {
            try {
                postgrest.from("sub_orders")
                    .select {
                        filter { isIn("order_id", orderIds) }
                    }
                    .decodeList<RemoteSubOrderDto>()
            } catch (e: Exception) {
                if (e.message?.contains("JWT", ignoreCase = true) == true || e.message?.contains("expired", ignoreCase = true) == true) {
                    runCatching { auth.refreshCurrentSession() }
                }
                emptyList()
            }
        } else {
            emptyList()
        }

        // Cachear nombres de vendedores que atienden los subpedidos
        val missingSellerIds = subOrders.map { it.sellerId }
            .filter { it.isNotBlank() && !sellerNameCache.containsKey(it) }
            .distinct()
        if (missingSellerIds.isNotEmpty()) {
            try {
                val sellers = postgrest.from("profiles")
                    .select {
                        filter { isIn("id", missingSellerIds) }
                    }
                    .decodeList<UserProfile>()
                for (s in sellers) {
                    sellerNameCache[s.id] = s.displayStoreName
                }
            } catch (_: Exception) {}
        }

        if (isBuyerFirstRun) {
            orders.forEach { o ->
                val isTerminal = o.status.lowercase() in listOf("completed", "completado", "rejected", "rechazado", "cancelled", "cancelado")
                val isOld = !isRecent(o.updatedAt ?: o.createdAt, maxAgeMinutes = 30)
                if (isTerminal || isOld || isAlreadyNotified("buyer_order_${o.id}_${o.status.lowercase()}")) {
                    lastKnownBuyerStatuses[o.id] = o.status.lowercase()
                    markAsNotified("buyer_order_${o.id}_${o.status.lowercase()}")
                }
            }
            subOrders.forEach { sub ->
                val isTerminal = sub.status.lowercase() in listOf("completed", "completado", "rejected", "rechazado", "cancelled", "cancelado")
                val isOld = !isRecent(sub.updatedAt ?: sub.createdAt, maxAgeMinutes = 30)
                if (isTerminal || isOld || isAlreadyNotified("buyer_sub_${sub.id}_${sub.status.lowercase()}")) {
                    lastKnownBuyerSubOrderStatuses[sub.id] = sub.status.lowercase()
                    markAsNotified("buyer_sub_${sub.id}_${sub.status.lowercase()}")
                }
            }
            isBuyerFirstRun = false
        }

        // 1. Monitoreo reactivo de subpedidos (Notificaciones granulares por puesto)
        for (sub in subOrders) {
            val currentSubStatus = sub.status.lowercase()
            val previousSubStatus = lastKnownBuyerSubOrderStatuses[sub.id]
            val eventKey = "buyer_sub_${sub.id}_$currentSubStatus"

            val shouldNotify = ((previousSubStatus != null && previousSubStatus != currentSubStatus) ||
                               (previousSubStatus == null && !isAlreadyNotified(eventKey) && isRecent(sub.updatedAt ?: sub.createdAt, 30))) &&
                               currentSubStatus !in listOf("pending", "pendiente")

            if (shouldNotify) {
                if (!isAlreadyNotified(eventKey)) {
                    markAsNotified(eventKey)
                    val parentOrder = orderMap[sub.orderId]
                    val storeName = sellerNameCache[sub.sellerId] ?: "El emprendedor"
                    val meetingPoint = parentOrder?.meetingPointName ?: parentOrder?.deliveryPlace ?: "Campus Universitario"
                    val schedule = parentOrder?.scheduledTime?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                    val itemsSummary = getOrderItemsSummary(orderId = sub.orderId, subOrderId = sub.id)
                    val summaryPart = if (itemsSummary.isNotBlank()) " ($itemsSummary)" else ""

                    when (currentSubStatus) {
                        "accepted", "aceptado" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = sub.id.hashCode(),
                                title = "Pedido aceptado",
                                message = "$storeName aceptó tu pedido$summaryPart.",
                                orderId = sub.orderId
                            )
                        }
                        "preparing", "in_preparation", "en_preparacion" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = sub.id.hashCode(),
                                title = "Pedido en preparación",
                                message = "$storeName comenzó a preparar tu pedido$summaryPart.",
                                orderId = sub.orderId
                            )
                        }
                        "ready", "listo", "esperando_entrega" -> {
                            val pinCode = sub.deliveryCode?.takeIf { it.isNotBlank() } ?: run {
                                val hash = (sub.orderId + sub.id).hashCode()
                                String.format(java.util.Locale.US, "%04d", kotlin.math.abs(hash % 10000))
                            }
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = sub.id.hashCode(),
                                title = "¡Tu pedido está listo!",
                                message = "Tu pedido de $storeName está listo. Acércate al punto de encuentro: $meetingPoint$schedule con tu PIN #$pinCode.",
                                orderId = sub.orderId
                            )
                        }
                        "completed", "completado" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = sub.id.hashCode(),
                                title = "¡Pedido entregado!",
                                message = "Tu entrega con $storeName fue completada exitosamente.",
                                orderId = sub.orderId
                            )
                        }
                        "rejected", "rechazado", "cancelled", "cancelado" -> {
                            val reasonPart = if (!sub.rejectionReason.isNullOrBlank()) ": ${sub.rejectionReason}" else "."
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = sub.id.hashCode(),
                                title = "Pedido cancelado/rechazado",
                                message = "$storeName no pudo atender tu pedido$reasonPart",
                                orderId = sub.orderId
                            )
                        }
                    }
                }
            }
            lastKnownBuyerSubOrderStatuses[sub.id] = currentSubStatus
        }

        // 2. Monitoreo de órdenes globales (para órdenes legacy)
        for (order in orders) {
            val currentStatus = order.status.lowercase()
            val previousStatus = lastKnownBuyerStatuses[order.id]
            val eventKey = "buyer_order_${order.id}_$currentStatus"

            val shouldNotifyOrder = ((previousStatus != null && previousStatus != currentStatus) ||
                                    (previousStatus == null && !isAlreadyNotified(eventKey) && isRecent(order.updatedAt ?: order.createdAt, 30))) &&
                                    currentStatus !in listOf("pending", "pendiente")

            if (shouldNotifyOrder) {
                if (!isAlreadyNotified(eventKey)) {
                    markAsNotified(eventKey)
                    val itemsSummary = getOrderItemsSummary(order.id)
                    val summaryPart = if (itemsSummary.isNotBlank()) " ($itemsSummary)" else ""
                    val meetingPoint = order.meetingPointName ?: order.deliveryPlace ?: "Campus Universitario"
                    val schedule = order.scheduledTime?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""

                    when (currentStatus) {
                        "accepted", "aceptado", "in_preparation", "en_preparacion" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "Pedido en preparación",
                                message = "El vendedor comenzó a preparar tu pedido$summaryPart.",
                                orderId = order.id
                            )
                        }
                        "ready", "listo", "esperando_entrega" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "¡Tu pedido está listo!",
                                message = "Tu pedido está listo. Acércate al punto de encuentro: $meetingPoint$schedule.",
                                orderId = order.id
                            )
                        }
                        "completed", "completado" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "¡Pedido entregado!",
                                message = "Tu pedido$summaryPart ha sido completado exitosamente. ¡Buen provecho!",
                                orderId = order.id
                            )
                        }
                        "cancelled", "cancelado", "rejected", "rechazado" -> {
                            ValleGoNotificationHelper.showOrderNotification(
                                context = this@ValleGoPushService,
                                notificationId = order.id.hashCode(),
                                title = "Pedido cancelado",
                                message = "Tu pedido$summaryPart fue cancelado.",
                                orderId = order.id
                            )
                        }
                    }
                }
            }
            lastKnownBuyerStatuses[order.id] = currentStatus
        }
    }

    private suspend fun monitorChatMessages(currentUserId: String) {
        try {
            val unreadRemote = postgrest.from("order_messages")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("is_read", false)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(15)
                }
                .decodeList<RemoteOrderMessageDto>()

            if (unreadRemote.isEmpty()) return

            for (msg in unreadRemote) {
                val eventKey = "chat_msg_${msg.id}"
                if (isAlreadyNotified(eventKey)) continue

                // REGLA CLAVE: Si el usuario tiene la interfaz de conversación abierta con este remitente / subpedido,
                // silenciar la notificación local del sistema.
                val isChatOpenWithSender = ActiveChatSessionManager.isChatActiveWith(
                    subOrderId = msg.subOrderId,
                    senderId = msg.senderId
                )

                if (isChatOpenWithSender) {
                    markAsNotified(eventKey)
                    ValleGoNotificationHelper.cancelChatNotifications(this@ValleGoPushService, msg.subOrderId)
                    continue
                }

                val senderName = getSenderName(msg.senderId)
                markAsNotified(eventKey)

                ValleGoNotificationHelper.showChatNotification(
                    context = this@ValleGoPushService,
                    notificationId = msg.id.hashCode(),
                    senderName = senderName,
                    message = msg.content,
                    subOrderId = msg.subOrderId
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Chequeo de mensajes de chat omitido: ${e.message}")
            if (e.message?.contains("JWT", ignoreCase = true) == true || e.message?.contains("expired", ignoreCase = true) == true) {
                runCatching { auth.refreshCurrentSession() }
            }
        }
    }

    private suspend fun getSenderName(senderId: String): String {
        val cached = profileNameCache[senderId]
        if (!cached.isNullOrBlank()) return cached

        return try {
            val profile = postgrest.from("profiles")
                .select { filter { eq("id", senderId) } }
                .decodeSingleOrNull<ProfileBasicDto>()
            val name = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Usuario de CampusGO"
            profileNameCache[senderId] = name
            name
        } catch (_: Exception) {
            "Usuario de CampusGO"
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved invocado. Activando PushWatchdog para mantener servicio...")
        PushWatchdogReceiver.scheduleNextWatchdog(applicationContext, 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (_: Exception) {}
        }
        Log.d(TAG, "ValleGoPushService destruido. Reprogramando rescate mediante PushWatchdog...")
        PushWatchdogReceiver.scheduleNextWatchdog(applicationContext, 1000L)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ValleGoPushService"

        fun start(context: Context) {
            try {
                val intent = Intent(context, ValleGoPushService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar ValleGoPushService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ValleGoPushService::class.java)
            context.stopService(intent)
        }
    }
}
