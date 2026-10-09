package com.example.campusgo.features.buyer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.campusgo.features.buyer.components.BuyerProductGridCard
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import android.content.Context
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import com.example.campusgo.features.buyer.components.BuyerBottomNavTab
import com.example.campusgo.features.buyer.components.CampusGoBottomNavBar
import com.example.campusgo.features.buyer.components.BuyerFavoritesView
import com.example.campusgo.features.buyer.components.BuyerNotificationsDialog
import com.example.campusgo.features.buyer.components.CampusFlyerCarousel
import com.example.campusgo.features.seller.components.SellerNotificationsBottomSheet
import com.example.campusgo.ui.components.OfficialWarningBanner
import com.example.campusgo.domain.model.Category
import com.example.campusgo.domain.model.Product
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.model.verificationCode
import com.example.campusgo.R
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.domain.model.StoreCatalogGroup
import com.example.campusgo.features.buyer.BuyerHomeViewModel
import org.koin.androidx.compose.koinViewModel
import com.example.campusgo.features.cart.CartScreen
import com.example.campusgo.features.tracking.OrderTrackingScreen
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.StoreStatusBadge
import com.example.campusgo.ui.components.CampusGoBusinessAvatar
import com.example.campusgo.ui.components.CampusGoBusinessBanner
import com.example.campusgo.ui.components.CampusGoProductImage
import com.example.campusgo.ui.components.CampusGoUserAvatar
import com.example.campusgo.ui.components.compressImageUri
import com.example.campusgo.ui.components.formatAccountCreationDate
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.features.chat.SupportChatBottomSheet
import com.example.campusgo.ui.components.ActiveSupportTicketBanner
import com.example.campusgo.features.chat.ActiveChatSummary
import com.example.campusgo.features.chat.ActiveChatsSheet
import com.example.campusgo.features.chat.OrderChatBottomSheet
import com.example.campusgo.features.chat.OrderChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.material.icons.automirrored.filled.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    pendingSubOrderId: String? = null,
    onClearPendingSubOrder: () -> Unit = {},
    pendingRoute: com.example.campusgo.core.notification.AppNotificationPayload? = null,
    onClearPendingRoute: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BuyerHomeViewModel = koinViewModel()
) {
    var currentProfile by remember { mutableStateOf(profile) }

    LaunchedEffect(currentProfile.campus, currentProfile.id) {
        viewModel.initialize(currentProfile.campus, currentProfile.id)
    }

    val uiState by viewModel.uiState.collectAsState()
    val allMeetingPoints by viewModel.meetingPoints.collectAsState()
    val cartCalculation by viewModel.cartCalculation.collectAsState()
    val unreadChatCount by remember(currentProfile.id) {
        viewModel.observeUnreadCount(currentProfile.id)
    }.collectAsState(initial = 0)
    val buyerOrders by remember(currentProfile.id) {
        viewModel.observeOrders(currentProfile.id)
    }.collectAsState(initial = emptyList())
    val buyerWarnings by remember(currentProfile.id) {
        viewModel.observeWarnings(currentProfile.id)
    }.collectAsState(initial = emptyList())

    val buyerActiveTicket = uiState.buyerActiveTicket
    var activeSupportTicket by remember { mutableStateOf<SupportTicket?>(null) }

    var currentTab by rememberSaveable { mutableStateOf(BuyerBottomNavTab.INICIO) }
    var ordersNavKey by remember { mutableStateOf(0) }
    val favoriteProductIds = uiState.favoriteProductIds
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showStrikesBottomSheet by remember { mutableStateOf(false) }
    var selectedStoreForProfile by remember { mutableStateOf<StoreCatalogGroup?>(null) }
    val cartScale = remember { Animatable(1f) }
    var prevCartCount by remember { mutableStateOf(cartCalculation.totalItemCount) }

    LaunchedEffect(cartCalculation.totalItemCount) {
        if (cartCalculation.totalItemCount > prevCartCount) {
            cartScale.animateTo(
                targetValue = 1.18f,
                animationSpec = tween(durationMillis = 150)
            )
            cartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        prevCartCount = cartCalculation.totalItemCount
    }
    val readyOrdersInfo = remember(buyerOrders) {
        buyerOrders.flatMap { order ->
            order.subOrders
                .filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
                .map { sub -> Triple(order, sub, "${order.id}_${sub.id}") }
        }
    }
    val preparingOrdersInfo = remember(buyerOrders) {
        buyerOrders.flatMap { order ->
            order.subOrders
                .filter { it.status == SubOrderStatus.EN_PREPARACION }
                .map { sub -> Triple(order, sub, "${order.id}_${sub.id}") }
        }
    }
    var readNotificationIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val currentNotificationIds = remember(readyOrdersInfo, preparingOrdersInfo) {
        val ids = mutableListOf<String>()
        readyOrdersInfo.forEach { ids.add("ready_${it.third}") }
        preparingOrdersInfo.forEach { ids.add("prep_${it.third}") }
        ids.toSet()
    }
    val hasPendingNotifications = remember(currentNotificationIds, readNotificationIds) {
        currentNotificationIds.any { it !in readNotificationIds }
    }

    LaunchedEffect(showNotificationsDialog) {
        if (showNotificationsDialog && currentNotificationIds.isNotEmpty()) {
            readNotificationIds = readNotificationIds + currentNotificationIds
        }
    }
    var dismissedAlertSignatures by remember { mutableStateOf(setOf<String>()) }

    val currentNotificationData = remember(readyOrdersInfo, preparingOrdersInfo, dismissedAlertSignatures) {
        val readyKey = if (readyOrdersInfo.isNotEmpty()) {
            "ready_${readyOrdersInfo.size}_${readyOrdersInfo.map { it.third }.sorted().joinToString(",")}"
        } else null

        val prepKey = if (preparingOrdersInfo.isNotEmpty()) {
            "prep_${preparingOrdersInfo.size}_${preparingOrdersInfo.map { it.third }.sorted().joinToString(",")}"
        } else null

        when {
            readyKey != null && readyKey !in dismissedAlertSignatures -> {
                val count = readyOrdersInfo.size
                val text = if (count == 1) "Tienes 1 pedido Listo" else "Tienes $count pedidos listos"
                Triple(readyKey, text, true) // true = isReady
            }
            prepKey != null && prepKey !in dismissedAlertSignatures -> {
                val count = preparingOrdersInfo.size
                val text = if (count == 1) "Tienes un pedido en preparacion" else "Tienes $count pedidos en preparación"
                Triple(prepKey, text, false) // false = isPreparing
            }
            else -> null
        }
    }

    val timerProgress = remember { Animatable(1f) }
    var isNotificationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentNotificationData?.first) {
        val sig = currentNotificationData?.first
        if (sig != null) {
            isNotificationVisible = true
            timerProgress.snapTo(1f)
            timerProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            isNotificationVisible = false
            dismissedAlertSignatures = dismissedAlertSignatures + sig
        } else {
            isNotificationVisible = false
        }
    }

    val alarmTransition = rememberInfiniteTransition(label = "alarm_shake_transition")
    val bellRotation by alarmTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0
                -18f at 100
                18f at 200
                -14f at 300
                14f at 400
                -10f at 500
                10f at 600
                -5f at 700
                5f at 800
                0f at 900
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bell_rotation"
    )
    val bellScale by alarmTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1f at 0
                1.14f at 200
                1.14f at 600
                1f at 900
                1f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bell_scale"
    )
    var showCartScreen by rememberSaveable { mutableStateOf(false) }
    var activeChatSummary by remember { mutableStateOf<ActiveChatSummary?>(null) }
    val chatViewModel: OrderChatViewModel = koinInject()

    val realStoresWithProducts = uiState.stores

    val activeBuyerChats = remember(buyerOrders, realStoresWithProducts) {
        buyerOrders.flatMap { order ->
            order.subOrders
                .filter { !it.status.isFinal }
                .map { sub ->
                    val store = realStoresWithProducts.find { it.sellerId == sub.sellerId }
                    val sellerAvatar = store?.avatarUrl
                    ActiveChatSummary(
                        subOrderId = sub.id,
                        otherUserId = sub.sellerId,
                        otherUserName = sub.sellerName.ifBlank { store?.sellerName ?: "Vendedor Campus Go" },
                        meetingPoint = sub.meetingPointName ?: "Punto por convenir",
                        status = sub.status,
                        subtotal = sub.subtotalAmount,
                        itemsSummary = sub.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                        deliveryCode = sub.verificationCode,
                        isBuyerPerspective = true,
                        otherUserAvatarUrl = sellerAvatar
                    )
                }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("TODOS") }
    var onlyOpenStores by remember { mutableStateOf(false) }
    var selectedStoreId by remember { mutableStateOf<String?>(null) }

    var selectedProductForDetail by remember { mutableStateOf<Pair<Product, StoreCatalogGroup>?>(null) }

    var hasDismissedWarningBanner by remember { mutableStateOf(false) }
    var lastWarningCount by remember { mutableStateOf(-1) }
    var showWarningBanner by remember { mutableStateOf(false) }
    val warningTimerProgress = remember { Animatable(1f) }

    LaunchedEffect(buyerWarnings) {
        if (buyerWarnings.isNotEmpty()) {
            val isNewStrike = lastWarningCount != -1 && buyerWarnings.size > lastWarningCount
            val shouldShow = !hasDismissedWarningBanner || isNewStrike

            lastWarningCount = buyerWarnings.size

            if (shouldShow) {
                hasDismissedWarningBanner = true
                showWarningBanner = true
                warningTimerProgress.snapTo(1f)
                warningTimerProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                )
                showWarningBanner = false
            }
        }
    }

    // Manejo nativo del botón / gesto Atrás de Android
    BackHandler(enabled = activeSupportTicket != null) {
        activeSupportTicket = null
    }
    BackHandler(enabled = activeChatSummary != null) {
        activeChatSummary = null
        chatViewModel.clearChat()
    }
    BackHandler(enabled = activeChatSummary == null && showCartScreen) {
        showCartScreen = false
    }
    BackHandler(enabled = activeChatSummary == null && !showCartScreen && selectedProductForDetail != null) {
        selectedProductForDetail = null
    }
    BackHandler(enabled = activeChatSummary == null && !showCartScreen && selectedProductForDetail == null && selectedStoreForProfile != null) {
        selectedStoreForProfile = null
    }
    BackHandler(enabled = activeChatSummary == null && !showCartScreen && selectedProductForDetail == null && selectedStoreForProfile == null && currentTab != BuyerBottomNavTab.INICIO) {
        currentTab = BuyerBottomNavTab.INICIO
    }

    val categoriesList = uiState.categories
    val isLoadingCatalog = uiState.isLoadingCatalog
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pendingRoute, pendingSubOrderId, buyerOrders, realStoresWithProducts) {
        val effectiveSubOrderId = when (val route = pendingRoute) {
            is com.example.campusgo.core.notification.AppNotificationPayload.OrderChat -> route.subOrderId
            else -> pendingSubOrderId
        }

        if (!effectiveSubOrderId.isNullOrBlank()) {
            val subId = effectiveSubOrderId
            val existing = buyerOrders.flatMap { it.subOrders }.find { it.id == subId }
            if (existing != null) {
                val store = realStoresWithProducts.find { it.sellerId == existing.sellerId }
                val sellerAvatar = store?.avatarUrl
                val meetingPt = existing.meetingPointName ?: "Punto por convenir"
                val sellerName = existing.sellerName.ifBlank { store?.sellerName ?: "Vendedor Campus Go" }
                val chatSummary = ActiveChatSummary(
                    subOrderId = existing.id,
                    otherUserId = existing.sellerId,
                    otherUserName = sellerName,
                    meetingPoint = meetingPt,
                    status = existing.status,
                    subtotal = existing.subtotalAmount,
                    itemsSummary = existing.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                    deliveryCode = existing.verificationCode,
                    isBuyerPerspective = true,
                    otherUserAvatarUrl = sellerAvatar
                )
                activeChatSummary = chatSummary
                chatViewModel.initChat(
                    subOrderId = existing.id,
                    currentUserId = currentProfile.id,
                    otherUserId = existing.sellerId,
                    otherUserName = sellerName,
                    meetingPoint = meetingPt,
                    subOrderStatus = existing.status,
                    otherUserAvatarUrl = sellerAvatar,
                    deliveryCode = existing.verificationCode
                )
                onClearPendingSubOrder()
                onClearPendingRoute()
            } else {
                coroutineScope.launch {
                    val fetched = viewModel.getSubOrderById(subId)
                    if (fetched != null) {
                        val store = realStoresWithProducts.find { it.sellerId == fetched.sellerId }
                        val sellerAvatar = store?.avatarUrl
                        val meetingPt = fetched.meetingPointName ?: "Punto por convenir"
                        val sellerName = fetched.sellerName.ifBlank { store?.sellerName ?: "Vendedor Campus Go" }
                        val chatSummary = ActiveChatSummary(
                            subOrderId = fetched.id,
                            otherUserId = fetched.sellerId,
                            otherUserName = sellerName,
                            meetingPoint = meetingPt,
                            status = fetched.status,
                            subtotal = fetched.subtotalAmount,
                            itemsSummary = fetched.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                            deliveryCode = fetched.verificationCode,
                            isBuyerPerspective = true,
                            otherUserAvatarUrl = sellerAvatar
                        )
                        activeChatSummary = chatSummary
                        chatViewModel.initChat(
                            subOrderId = fetched.id,
                            currentUserId = currentProfile.id,
                            otherUserId = fetched.sellerId,
                            otherUserName = sellerName,
                            meetingPoint = meetingPt,
                            subOrderStatus = fetched.status,
                            otherUserAvatarUrl = sellerAvatar,
                            deliveryCode = fetched.verificationCode
                        )
                        onClearPendingSubOrder()
                        onClearPendingRoute()
                    }
                }
            }
        }

        when (val route = pendingRoute) {
            is com.example.campusgo.core.notification.AppNotificationPayload.SupportChat -> {
                coroutineScope.launch {
                    val ticket = viewModel.getTicketById(route.ticketId, currentProfile.id)
                    if (ticket != null) {
                        activeSupportTicket = ticket
                        onClearPendingRoute()
                    }
                }
            }
            is com.example.campusgo.core.notification.AppNotificationPayload.Warning -> {
                showStrikesBottomSheet = true
                onClearPendingRoute()
            }
            is com.example.campusgo.core.notification.AppNotificationPayload.OrderTracking -> {
                ordersNavKey++
                currentTab = BuyerBottomNavTab.PEDIDOS
                onClearPendingRoute()
            }
            else -> {}
        }
    }

    val onToggleFavoriteAction: (String) -> Unit = { prodId ->
        viewModel.toggleFavorite(currentProfile.id, prodId)
    }

    if (activeChatSummary != null) {
        OrderChatBottomSheet(
            viewModel = chatViewModel,
            onDismiss = {
                activeChatSummary = null
                chatViewModel.clearChat()
            }
        )
        return
    }

    activeSupportTicket?.let { ticket ->
        SupportChatBottomSheet(
            ticket = ticket,
            currentUserId = currentProfile.id,
            isAdmin = false,
            onDismiss = {
                activeSupportTicket = null
                coroutineScope.launch {
                    viewModel.refreshActiveTicket(currentProfile.id)
                }
            }
        )
        return
    }

    // Pantalla completa de Carrito de Compras (oculta barra inferior y muestra flecha de volver)
    if (showCartScreen) {
        CartScreen(
            buyerProfile = currentProfile,
            onNavigateBack = { showCartScreen = false },
            onNavigateToTracking = {
                showCartScreen = false
                ordersNavKey++
                currentTab = BuyerBottomNavTab.PEDIDOS
            },
            onExploreStalls = {
                showCartScreen = false
                currentTab = BuyerBottomNavTab.INICIO
            },
            onOpenChatForOrder = { order, subOrder ->
                showCartScreen = false
                val store = realStoresWithProducts.find { it.sellerId == subOrder.sellerId }
                val sellerAvatar = store?.avatarUrl
                val meetingPt = subOrder.meetingPointName ?: order.meetingPointName.ifBlank { "Punto por convenir" }
                val sellerName = subOrder.sellerName.ifBlank { store?.sellerName ?: "Vendedor Campus Go" }
                val chatSummary = ActiveChatSummary(
                    subOrderId = subOrder.id,
                    otherUserId = subOrder.sellerId,
                    otherUserName = sellerName,
                    meetingPoint = meetingPt,
                    status = subOrder.status,
                    subtotal = subOrder.subtotalAmount,
                    itemsSummary = subOrder.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                    deliveryCode = subOrder.verificationCode,
                    isBuyerPerspective = true,
                    otherUserAvatarUrl = sellerAvatar
                )
                activeChatSummary = chatSummary
                chatViewModel.initChat(
                    subOrderId = subOrder.id,
                    currentUserId = currentProfile.id,
                    otherUserId = subOrder.sellerId,
                    otherUserName = sellerName,
                    meetingPoint = meetingPt,
                    subOrderStatus = subOrder.status,
                    otherUserAvatarUrl = sellerAvatar,
                    deliveryCode = subOrder.verificationCode
                )
            },
            modifier = modifier
        )
        return
    }

    // Pantalla completa de Perfil del Vendedor / Puesto Comercial
    if (selectedStoreForProfile != null) {
        BuyerSellerProfileScreen(
            store = selectedStoreForProfile!!,
            meetingPoints = allMeetingPoints,
            cartCalculation = cartCalculation,
            categoriesList = categoriesList,
            onNavigateBack = {
                selectedProductForDetail = null
                selectedStoreForProfile = null
            },
            onProductClick = { product ->
                selectedProductForDetail = Pair(product, selectedStoreForProfile!!)
            },
            onAddToCart = { product ->
                viewModel.addToCart(selectedStoreForProfile!!.sellerId, selectedStoreForProfile!!.sellerName, product, 1)
            },
            onNavigateToCart = {
                selectedProductForDetail = null
                selectedStoreForProfile = null
                showCartScreen = true
            },
            modifier = modifier
        )

        // Modal de Detalle de Producto al estilo Rappi dentro del perfil del vendedor
        if (selectedProductForDetail != null) {
            val (prod, store) = selectedProductForDetail!!
            val catName = categoriesList.find { it.id == prod.categoryId }?.name
            val isStoreAvail = store.acceptingOrders &&
                    !store.businessStatus.equals("PAUSADO", ignoreCase = true) &&
                    !store.businessStatus.equals("CERRADO", ignoreCase = true)
            ProductDetailBottomSheet(
                product = prod,
                storeName = store.sellerName,
                categoryName = catName,
                isStoreAvailable = isStoreAvail,
                storeStatus = store.businessStatus,
                onDismiss = { selectedProductForDetail = null },
                onStoreClick = {
                    selectedProductForDetail = null
                },
                onAddToCart = { product, quantity, instructions ->
                    viewModel.addToCart(store.sellerId, store.sellerName, product, quantity)
                }
            )
        }
        return
    }

    // Modal de Detalle de Producto al estilo Rappi
    if (selectedProductForDetail != null) {
        val (prod, store) = selectedProductForDetail!!
        val catName = categoriesList.find { it.id == prod.categoryId }?.name
        val isStoreAvail = store.acceptingOrders &&
                !store.businessStatus.equals("PAUSADO", ignoreCase = true) &&
                !store.businessStatus.equals("CERRADO", ignoreCase = true)
        ProductDetailBottomSheet(
            product = prod,
            storeName = store.sellerName,
            categoryName = catName,
            isStoreAvailable = isStoreAvail,
            storeStatus = store.businessStatus,
            onDismiss = { selectedProductForDetail = null },
            onStoreClick = {
                selectedProductForDetail = null
                selectedStoreForProfile = store
            },
            onAddToCart = { product, quantity, instructions ->
                viewModel.addToCart(store.sellerId, store.sellerName, product, quantity)
            }
        )
    }


    // Filtrado reactivo en tiempo real
    val filteredStores = remember(realStoresWithProducts, searchQuery, selectedCategoryFilter, onlyOpenStores, categoriesList) {
        val q = searchQuery.trim().lowercase()
        realStoresWithProducts.mapNotNull { store ->
            if (onlyOpenStores && (!store.acceptingOrders || store.businessStatus.equals("CERRADO", ignoreCase = true))) {
                return@mapNotNull null
            }
            val storeMatches = q.isEmpty() || store.sellerName.lowercase().contains(q) || (store.description?.lowercase()?.contains(q) == true)
            val matchingProducts = store.products.filter { prod ->
                val matchesSearch = q.isEmpty() || prod.name.lowercase().contains(q) || (prod.description?.lowercase()?.contains(q) == true) || storeMatches
                val catName = categoriesList.find { it.id == prod.categoryId }?.name?.uppercase().orEmpty()
                val prodNameLower = prod.name.lowercase()
                val prodDescLower = prod.description?.lowercase().orEmpty()
                val storeCatLower = store.businessCategory?.lowercase().orEmpty()
                val isApparelOrAccessory = catName.contains("ROPA") || catName.contains("ACCESORIO") || catName.contains("TEXTIL") ||
                        catName.contains("MANUALIDAD") || catName.contains("REGALO") || catName.contains("SERVICIO") || catName.contains("ASESOR") ||
                        prodNameLower.contains("polo") || prodNameLower.contains("polera") || prodNameLower.contains("camisa")

                val matchesCategory = when (selectedCategoryFilter) {
                    "TODOS" -> true
                    "COMIDAS" -> {
                        if (isApparelOrAccessory) {
                            false
                        } else {
                            val isBurger = catName.contains("HAMBURGUESA") || prodNameLower.contains("hamburguesa") || prodNameLower.contains("burger")
                            val isDrink = catName.contains("BEBIDA") || catName.contains("JUGO") || catName.contains("REFRESCO")
                            val isDessert = catName.contains("POSTRE") || catName.contains("DULCE") || catName.contains("REPOSTER")
                            !isBurger && !isDrink && !isDessert && (
                                catName.contains("COMIDA") || catName.contains("ALMUERZO") || catName.contains("MENÚ") || catName.contains("MENU") ||
                                prodNameLower.contains("almuerzo") || prodNameLower.contains("menú") || prodNameLower.contains("menu") ||
                                prodNameLower.contains("chaufa") || prodNameLower.contains("lomo") ||
                                prodNameLower.contains("arroz") || prodNameLower.contains("tallarin") || prodNameLower.contains("sopa") ||
                                prodNameLower.contains("segundo") || prodNameLower.contains("milanesa") || prodDescLower.contains("segundo") ||
                                (prodNameLower.contains("pollo") && !prodNameLower.contains("polo"))
                            )
                        }
                    }
                    "HAMBURGUESAS" -> {
                        if (isApparelOrAccessory) {
                            false
                        } else {
                            catName.contains("HAMBURGUESA") || catName.contains("FAST") ||
                                prodNameLower.contains("hamburguesa") || prodNameLower.contains("burger") || prodNameLower.contains("salchipapa") ||
                                prodNameLower.contains("broaster") || prodNameLower.contains("alitas") || prodNameLower.contains("nugget") ||
                                prodNameLower.contains("hot dog") || prodNameLower.contains("papas fritas")
                        }
                    }
                    "BEBIDAS" -> {
                        if (isApparelOrAccessory) {
                            false
                        } else {
                            catName.contains("BEBIDA") || catName.contains("JUGO") || catName.contains("REFRESCO") ||
                                prodNameLower.contains("bebida") || prodNameLower.contains("jugo") || prodNameLower.contains("chicha") ||
                                prodNameLower.contains("maracuyá") || prodNameLower.contains("maracuya") || prodNameLower.contains("café") ||
                                prodNameLower.contains("cafe") || prodNameLower.contains("gaseosa") || prodNameLower.contains("agua") ||
                                prodNameLower.contains("smoothie") || prodNameLower.contains("frappe") || prodNameLower.contains("infusion")
                        }
                    }
                    "POSTRES" -> {
                        if (isApparelOrAccessory) {
                            false
                        } else {
                            catName.contains("POSTRE") || catName.contains("DULCE") || catName.contains("REPOSTER") ||
                                prodNameLower.contains("postre") || prodNameLower.contains("queque") || prodNameLower.contains("torta") ||
                                prodNameLower.contains("alfajor") || prodNameLower.contains("pie") || prodNameLower.contains("brownie") ||
                                prodNameLower.contains("dulce") || prodNameLower.contains("pastel") || prodNameLower.contains("galleta") ||
                                prodNameLower.contains("trufa") || prodNameLower.contains("cupcake") || prodNameLower.contains("waffle")
                        }
                    }
                    "ACCESORIOS" -> {
                        isApparelOrAccessory || catName.contains("PAPEL") || catName.contains("UTIL") || catName.contains("VARIEDAD") ||
                            prodNameLower.contains("accesorio") || prodNameLower.contains("cuaderno") || prodNameLower.contains("lapicero") ||
                            prodNameLower.contains("pulsera") || prodNameLower.contains("joya") || prodNameLower.contains("mochila") ||
                            prodNameLower.contains("cartuchera") || prodNameLower.contains("sticker") || prodNameLower.contains("llavero") ||
                            prodNameLower.contains("aretes") || prodNameLower.contains("collar") || prodNameLower.contains("funda")
                    }
                    else -> true
                }
                matchesSearch && matchesCategory
            }

            if (matchingProducts.isNotEmpty()) {
                store.copy(products = matchingProducts)
            } else {
                null
            }
        }
    }

    // Listado plano de productos coincidentes para cuadrícula directa de 2 columnas (Propuesta A)
    val allMatchingProducts = remember(filteredStores, selectedStoreId) {
        filteredStores
            .filter { store -> selectedStoreId == null || store.sellerId == selectedStoreId }
            .flatMap { store -> store.products.map { prod -> Pair(prod, store) } }
    }
    val isDark = LocalDarkTheme.current
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentTab == BuyerBottomNavTab.INICIO) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                spotColor = if (isDark) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                ambientColor = if (isDark) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                clip = false
                            ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Fila 1: Perfil del Comprador (Foto e Información) y botones de acción a la derecha (solo Chat y Notificaciones)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar e Información del comprador unificados en un solo botón que abre su perfil completo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { currentTab = BuyerBottomNavTab.PERFIL }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    CampusGoUserAvatar(
                                        avatarUrl = currentProfile.avatarUrl,
                                        name = currentProfile.fullName,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentProfile.fullName.ifBlank { "Comprador" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val buyerSubtitle = if (!currentProfile.studentCode.isNullOrBlank()) {
                                             "${currentProfile.studentCode} • Campus ${currentProfile.campus}"
                                        } else {
                                            "Estudiante • Campus ${currentProfile.campus}"
                                        }
                                        Text(
                                            text = buyerSubtitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Botones de acción a la derecha (Avisos/Strikes, Notificaciones y Carrito)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 0. Botón de Advertencia / Strikes (solo visible si tiene al menos 1 aviso activo)
                                    if (buyerWarnings.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    showStrikesBottomSheet = true
                                                },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                                                shadowElevation = 1.dp,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.WarningAmber,
                                                        contentDescription = "Avisos y Moderación",
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Badge con cantidad de strikes activos
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 2.dp, y = (-2).dp)
                                            ) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ) {
                                                    Text(
                                                        text = "${buyerWarnings.size}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 1. Notificaciones (Lado Izquierdo) con punto rojo perfectamente posicionado
                                    Box(
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                readNotificationIds = readNotificationIds + currentNotificationIds
                                                showNotificationsDialog = true
                                            },
                                            shape = CircleShape,
                                            color = if (hasPendingNotifications) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (hasPendingNotifications) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Notifications,
                                                    contentDescription = "Notificaciones",
                                                    tint = if (hasPendingNotifications) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Punto rojo indicador en la esquina superior derecha con borde semántico
                                        if (hasPendingNotifications) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = (-2).dp, y = 2.dp)
                                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                            )
                                        }
                                    }

                                    // 2. Carrito de Compras (Lado Derecho) con animación reactiva
                                    AnimatedContent(
                                        targetState = cartCalculation.totalItemCount > 0,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.88f))
                                                .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.88f))
                                        },
                                        label = "cart_button_animation"
                                    ) { hasItems ->
                                        if (hasItems) {
                                            Surface(
                                                onClick = { showCartScreen = true },
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                shadowElevation = 2.5.dp,
                                                modifier = Modifier
                                                    .height(38.dp)
                                                    .scale(cartScale.value)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 11.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_cart_custom),
                                                        contentDescription = "Mi Carrito",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Text(
                                                        text = "${cartCalculation.totalItemCount} • S/ %.2f".format(cartCalculation.grandTotal),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                onClick = { showCartScreen = true },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                shadowElevation = 1.dp,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .scale(cartScale.value)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_cart_custom),
                                                        contentDescription = "Mi Carrito",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Fila 2: Barra de búsqueda estilo Rappi
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "¿Qué buscas hoy? (ej. café, postre)",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Borrar búsqueda",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (currentTab) {
                    BuyerBottomNavTab.INICIO -> {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = innerPadding.calculateTopPadding() + 4.dp)
                                    .padding(vertical = 6.dp)
                                    .padding(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                    // Advertencias formales emitidas por el Administrador (3 segundos y desaparece)
                    AnimatedVisibility(
                        visible = showWarningBanner && buyerWarnings.isNotEmpty(),
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(400)) + shrinkVertically(tween(400))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            OfficialWarningBanner(
                                warnings = buyerWarnings,
                                isSeller = false,
                                timerProgress = warningTimerProgress.value,
                                onBannerClick = {
                                    hasDismissedWarningBanner = true
                                    showWarningBanner = false
                                    showStrikesBottomSheet = true
                                }
                            )
                        }
                    }



                    // 0. Aviso de notificación dinámica con alarma en movimiento, temporizador de 5 segundos y texto según estado
                    AnimatedVisibility(
                        visible = isNotificationVisible && currentNotificationData != null,
                        enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { -it / 2 }),
                        exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { -it / 2 })
                    ) {
                        if (currentNotificationData != null) {
                            val (sig, text, isReady) = currentNotificationData
                            Card(
                                onClick = {
                                    readNotificationIds = readNotificationIds + currentNotificationIds
                                    isNotificationVisible = false
                                    showNotificationsDialog = true
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isReady) MaterialTheme.extendedColors.successContainer else MaterialTheme.colorScheme.primaryContainer
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isReady) MaterialTheme.extendedColors.success.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Icono animado de notificación como alarma que se mueve
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isReady) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .graphicsLayer {
                                                        rotationZ = bellRotation
                                                        scaleX = bellScale
                                                        scaleY = bellScale
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = "Ver detalles",
                                                        tint = if (isReady) MaterialTheme.extendedColors.onSuccess else MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Texto de notificación según lo solicitado
                                            Text(
                                                text = text,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReady) MaterialTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                isNotificationVisible = false
                                                dismissedAlertSignatures = dismissedAlertSignatures + sig
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Cerrar",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Línea de temporización de 5 segundos con bordes redondeados
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp)
                                            .padding(bottom = 10.dp)
                                            .height(3.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = timerProgress.value)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isReady) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. Carrusel de Fotos para Flyers, Anuncios o Publicidad del Campus
                    CampusFlyerCarousel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onFlyerClick = { flyer ->
                            if (flyer.id == "promo_comunidad" || flyer.id == "flyer_vendedor") {
                                currentTab = BuyerBottomNavTab.FAVORITOS
                            }
                        }
                    )

                    // 2. Carrusel de Categorías con Logos al Estilo Rappi y Fondos Suaves
                    data class RappiCategoryLogo(
                        val key: String,
                        val label: String,
                        val imageRes: Int?,
                        val iconRes: Int? = null,
                        val softBgColor: Color,
                        val softBorderColor: Color,
                        val activeBorderColor: Color
                    )

                    val rappiCategoryLogos = listOf(
                        RappiCategoryLogo(
                            key = "TODOS",
                            label = "Todos",
                            imageRes = null,
                            iconRes = R.drawable.ic_cat_all,
                            softBgColor = Color(0xFFE8F7F3),
                            softBorderColor = Color(0xFFBBECE2),
                            activeBorderColor = Color(0xFF00A884)
                        ),
                        RappiCategoryLogo(
                            key = "COMIDAS",
                            label = "Comidas",
                            imageRes = R.drawable.cat_comidas,
                            softBgColor = Color(0xFFFFF2E8),
                            softBorderColor = Color(0xFFFFD8BF),
                            activeBorderColor = Color(0xFFEA580C)
                        ),
                        RappiCategoryLogo(
                            key = "HAMBURGUESAS",
                            label = "Hamburguesas",
                            imageRes = R.drawable.cat_hamburguesas,
                            softBgColor = Color(0xFFFEF6D8),
                            softBorderColor = Color(0xFFFDE68A),
                            activeBorderColor = Color(0xFFD97706)
                        ),
                        RappiCategoryLogo(
                            key = "BEBIDAS",
                            label = "Bebidas",
                            imageRes = R.drawable.cat_bebidas,
                            softBgColor = Color(0xFFEBF5FF),
                            softBorderColor = Color(0xFFBFDBFE),
                            activeBorderColor = Color(0xFF2563EB)
                        ),
                        RappiCategoryLogo(
                            key = "POSTRES",
                            label = "Postres",
                            imageRes = R.drawable.cat_postres,
                            softBgColor = Color(0xFFFDF0F6),
                            softBorderColor = Color(0xFFFBCFE8),
                            activeBorderColor = Color(0xFFDB2777)
                        ),
                        RappiCategoryLogo(
                            key = "ACCESORIOS",
                            label = "Accesorios",
                            imageRes = R.drawable.cat_accesorios,
                            softBgColor = Color(0xFFF3EFFF),
                            softBorderColor = Color(0xFFDDD6FE),
                            activeBorderColor = Color(0xFF7C3AED)
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        rappiCategoryLogos.forEach { item ->
                            val isSelected = selectedCategoryFilter == item.key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        selectedCategoryFilter = if (isSelected && item.key != "TODOS") "TODOS" else item.key
                                    }
                            ) {
                                Surface(
                                    modifier = Modifier.size(72.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) item.softBgColor else item.softBgColor.copy(alpha = 0.85f),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) item.activeBorderColor else item.softBorderColor
                                    ),
                                    shadowElevation = if (isSelected) 3.5.dp else 1.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.imageRes != null) {
                                            Image(
                                                painter = painterResource(id = item.imageRes),
                                                contentDescription = item.label,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .scale(1.52f)
                                             )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        if (isSelected) Color(0xFF00A884) else item.softBgColor,
                                                        RoundedCornerShape(20.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = item.iconRes ?: R.drawable.ic_cat_all),
                                                    contentDescription = "Todos",
                                                    tint = if (isSelected) Color.White else Color(0xFF00A884),
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) item.activeBorderColor else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 3.dp)
                                            .width(20.dp)
                                            .height(3.dp)
                                            .background(item.activeBorderColor, RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // 3. Tarjeta informativa de puesto seleccionado (si hay filtro activo)
                    if (selectedStoreId != null) {
                        val currentSelectedStore = realStoresWithProducts.find { it.sellerId == selectedStoreId }
                        if (currentSelectedStore != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { selectedStoreForProfile = currentSelectedStore }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CampusGoBusinessAvatar(
                                                avatarUrl = currentSelectedStore.avatarUrl,
                                                storeName = currentSelectedStore.sellerName,
                                                size = 42.dp
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = currentSelectedStore.sellerName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    StoreStatusBadge(
                                                        status = currentSelectedStore.businessStatus,
                                                        acceptingOrders = currentSelectedStore.acceptingOrders
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_location_custom),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = currentSelectedStore.location ?: "Campus",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!currentSelectedStore.openTime.isNullOrBlank()) {
                                                        Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                        Text(
                                                            text = "${currentSelectedStore.openTime} - ${currentSelectedStore.closeTime}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = { selectedStoreId = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Quitar filtro",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedStoreForProfile = currentSelectedStore }
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_store_custom),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Ver perfil completo, banner y puntos de entrega",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Ir al perfil",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Estado de carga o Lista Vacía o Cuadrícula de Productos (Propuesta A)
                    if (isLoadingCatalog) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                                Text(
                                    text = "Cargando delicias universitarias...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (allMatchingProducts.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_store_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val activeCategoryLabel = when (selectedCategoryFilter) {
                                    "COMIDAS" -> "Comidas y Menús"
                                    "HAMBURGUESAS" -> "Hamburguesas"
                                    "BEBIDAS" -> "Bebidas"
                                    "POSTRES" -> "Postres"
                                    "ACCESORIOS" -> "Accesorios"
                                    else -> null
                                }

                                Text(
                                    text = when {
                                        searchQuery.isNotBlank() -> "Sin resultados para \"$searchQuery\""
                                        activeCategoryLabel != null -> "No hay productos en $activeCategoryLabel por ahora"
                                        else -> "No hay productos disponibles por ahora"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = when {
                                        activeCategoryLabel != null -> "Los emprendedores de la sección $activeCategoryLabel publicarán nuevos productos muy pronto."
                                        else -> "Intenta buscando por otro término o selecciona otra categoría o puesto."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategoryFilter = "TODOS"
                                        selectedStoreId = null
                                        onlyOpenStores = false
                                        viewModel.loadCatalog(currentProfile.campus)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(if (activeCategoryLabel != null) "Ver todas las categorías" else "Restablecer Filtros", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // 5. Encabezado de Productos y Cuadrícula Directa de 2 Columnas (Propuesta A)
                        val activeCategoryLabel = when (selectedCategoryFilter) {
                            "COMIDAS" -> "Comidas y Menús"
                            "HAMBURGUESAS" -> "Hamburguesas"
                            "BEBIDAS" -> "Bebidas"
                            "POSTRES" -> "Postres"
                            "ACCESORIOS" -> "Accesorios"
                            else -> null
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when {
                                        selectedStoreId != null -> "PRODUCTOS DEL PUESTO"
                                        activeCategoryLabel != null -> "SECCIÓN DE ${activeCategoryLabel.uppercase()}"
                                        else -> "TODOS LOS PRODUCTOS"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeCategoryLabel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                if (activeCategoryLabel != null) {
                                    Text(
                                        text = "Catálogo exclusivo de $activeCategoryLabel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (activeCategoryLabel != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { selectedCategoryFilter = "TODOS" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Ver todos",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar filtro",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "${allMatchingProducts.size} disponibles",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val productPairs = allMatchingProducts.chunked(2)
                        productPairs.forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val (prod1, store1) = pair[0]
                                val catName1 = categoriesList.find { it.id == prod1.categoryId }?.name
                                BuyerProductGridCard(
                                    product = prod1,
                                    store = store1,
                                    categoryName = catName1,
                                    onClick = { selectedProductForDetail = Pair(prod1, store1) },
                                    onStoreClick = { selectedStoreForProfile = store1 },
                                    onQuickAdd = {
                                        viewModel.addToCart(store1.sellerId, store1.sellerName, prod1, 1)
                                    },
                                    isFavorite = favoriteProductIds.contains(prod1.id),
                                    onToggleFavorite = { onToggleFavoriteAction(prod1.id) },
                                    showStoreTag = (selectedStoreId == null),
                                    modifier = Modifier.weight(1f)
                                )

                                if (pair.size > 1) {
                                    val (prod2, store2) = pair[1]
                                    val catName2 = categoriesList.find { it.id == prod2.categoryId }?.name
                                    BuyerProductGridCard(
                                        product = prod2,
                                        store = store2,
                                        categoryName = catName2,
                                        onClick = { selectedProductForDetail = Pair(prod2, store2) },
                                        onStoreClick = { selectedStoreForProfile = store2 },
                                        onQuickAdd = {
                                            viewModel.addToCart(store2.sellerId, store2.sellerName, prod2, 1)
                                        },
                                        isFavorite = favoriteProductIds.contains(prod2.id),
                                        onToggleFavorite = { onToggleFavoriteAction(prod2.id) },
                                        showStoreTag = (selectedStoreId == null),
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        BuyerBottomNavTab.FAVORITOS -> {
            BuyerFavoritesView(
                favoriteProductIds = favoriteProductIds,
                onToggleFavorite = onToggleFavoriteAction,
                allProducts = allMatchingProducts,
                categoriesList = categoriesList,
                storesList = realStoresWithProducts,
                buyerOrders = buyerOrders,
                onProductClick = { prod, store ->
                    selectedProductForDetail = Pair(prod, store)
                },
                onAddToCart = { prod, store ->
                    viewModel.addToCart(store.sellerId, store.sellerName, prod, 1)
                },
                onStoreClick = { store ->
                    selectedStoreForProfile = store
                },
                onExploreCatalog = {
                    currentTab = BuyerBottomNavTab.INICIO
                }
            )
        }
        BuyerBottomNavTab.PEDIDOS -> {
            key(ordersNavKey) {
                OrderTrackingScreen(
                    buyerProfile = currentProfile,
                    onNavigateBack = null,
                    onNavigateToCart = {
                        showCartScreen = true
                    },
                    onOpenChat = { order, subOrder ->
                        val store = realStoresWithProducts.find { it.sellerId == subOrder.sellerId }
                        val sellerAvatar = store?.avatarUrl
                        val meetingPt = subOrder.meetingPointName ?: order.meetingPointName.ifBlank { "Punto por convenir" }
                        val sellerName = subOrder.sellerName.ifBlank { store?.sellerName ?: "Vendedor Campus Go" }
                        val chatSummary = ActiveChatSummary(
                            subOrderId = subOrder.id,
                            otherUserId = subOrder.sellerId,
                            otherUserName = sellerName,
                            meetingPoint = meetingPt,
                            status = subOrder.status,
                            subtotal = subOrder.subtotalAmount,
                            itemsSummary = subOrder.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                            deliveryCode = subOrder.verificationCode,
                            isBuyerPerspective = true,
                            otherUserAvatarUrl = sellerAvatar
                        )
                        activeChatSummary = chatSummary
                        chatViewModel.initChat(
                            subOrderId = subOrder.id,
                            currentUserId = currentProfile.id,
                            otherUserId = subOrder.sellerId,
                            otherUserName = sellerName,
                            meetingPoint = meetingPt,
                            subOrderStatus = subOrder.status,
                            otherUserAvatarUrl = sellerAvatar,
                            deliveryCode = subOrder.verificationCode
                        )
                    }
                )
            }
        }
        BuyerBottomNavTab.CHATS -> {
            ActiveChatsSheet(
                chats = activeBuyerChats,
                supportTickets = listOfNotNull(buyerActiveTicket),
                onSelectSupportTicket = { activeSupportTicket = it },
                onSelectChat = { selectedChat ->
                    activeChatSummary = selectedChat
                    chatViewModel.initChat(
                        subOrderId = selectedChat.subOrderId,
                        currentUserId = currentProfile.id,
                        otherUserId = selectedChat.otherUserId,
                        otherUserName = selectedChat.otherUserName,
                        meetingPoint = selectedChat.meetingPoint,
                        subOrderStatus = selectedChat.status,
                        otherUserAvatarUrl = selectedChat.otherUserAvatarUrl,
                        deliveryCode = selectedChat.deliveryCode
                    )
                },
                onClose = null,
                userAvatarUrl = currentProfile.avatarUrl
            )
        }
        BuyerBottomNavTab.PERFIL -> {
            BuyerProfileScreen(
                profile = currentProfile,
                warnings = buyerWarnings,
                onNavigateBack = null,
                onSaveProfile = { updated ->
                    coroutineScope.launch {
                        val res = viewModel.updateUserProfile(updated)
                        if (res.isSuccess) {
                            currentProfile = res.getOrNull() ?: updated
                        }
                    }
                },
                onUploadAvatar = { bytes, onUploaded ->
                    coroutineScope.launch {
                        val oldUrl = currentProfile.avatarUrl
                        val path = "avatars/user_${currentProfile.id}.jpg"
                        val res = viewModel.uploadAvatarImage(path, bytes, oldUrl)
                        res.onSuccess { url ->
                            val freshUrl = if (url.contains("?")) url else "$url?v=${System.currentTimeMillis()}"
                            onUploaded(freshUrl)
                            val updated = currentProfile.copy(avatarUrl = freshUrl)
                            viewModel.updateUserProfile(updated)
                            currentProfile = updated
                        }
                    }
                },
                onSignOut = onSignOut
            )
        }
    }
}
}

        // Barra de navegación inferior flotante - Único contorno redondeado visible con fondo exterior transparente
        CampusGoBottomNavBar(
            selectedTab = currentTab,
            onTabSelected = { tab ->
                if (tab == BuyerBottomNavTab.PEDIDOS) {
                    ordersNavKey++
                }
                currentTab = tab
            },
            unreadChatCount = unreadChatCount,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showNotificationsDialog) {
            BuyerNotificationsDialog(
                readyOrders = readyOrdersInfo,
                preparingOrders = preparingOrdersInfo,
                onDismiss = { showNotificationsDialog = false },
                onNavigateToOrders = {
                    showNotificationsDialog = false
                    ordersNavKey++
                    currentTab = BuyerBottomNavTab.PEDIDOS
                }
            )
        }

        if (showStrikesBottomSheet) {
            SellerNotificationsBottomSheet(
                warnings = buyerWarnings,
                onDismiss = { showStrikesBottomSheet = false },
                isSeller = false
            )
        }
}
}
