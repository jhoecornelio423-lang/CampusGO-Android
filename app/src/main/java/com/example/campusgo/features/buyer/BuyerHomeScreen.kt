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
import androidx.compose.animation.togetherWith
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
import com.example.campusgo.domain.repository.AdminRepository
import com.example.campusgo.domain.repository.CartRepository
import com.example.campusgo.domain.repository.ChatRepository
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.domain.repository.ProductRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.material.icons.automirrored.filled.Chat
import com.example.campusgo.features.chat.ActiveChatSummary
import com.example.campusgo.features.chat.ActiveChatsSheet
import com.example.campusgo.features.chat.OrderChatBottomSheet
import com.example.campusgo.features.chat.OrderChatViewModel

data class StoreCatalogGroup(
    val sellerId: String,
    val sellerName: String,
    val location: String?,
    val bannerUrl: String?,
    val avatarUrl: String?,
    val businessStatus: String,
    val openTime: String?,
    val closeTime: String?,
    val description: String?,
    val acceptingOrders: Boolean,
    val products: List<Product>,
    val sellerProfile: UserProfile? = null,
    val businessCategory: String? = null,
    val phone: String = "",
    val ratingAverage: Double = 5.0,
    val supportedMeetingPoints: List<String> = emptyList(),
    val supportedPaymentMethods: List<String> = listOf("EFECTIVO", "YAPE", "PLIN")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    cartRepository: CartRepository = koinInject(),
    productRepository: ProductRepository = koinInject(),
    adminRepository: AdminRepository = koinInject(),
    orderRepository: OrderRepository = koinInject(),
    chatRepository: ChatRepository = koinInject()
) {
    var currentProfile by remember { mutableStateOf(profile) }
    val unreadChatCount by remember(profile.id) {
        chatRepository.observeUnreadCount(profile.id)
    }.collectAsState(initial = 0)
    var currentTab by rememberSaveable { mutableStateOf(BuyerBottomNavTab.INICIO) }
    var favoriteProductIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showStrikesBottomSheet by remember { mutableStateOf(false) }
    var selectedStoreForProfile by remember { mutableStateOf<StoreCatalogGroup?>(null) }
    val allMeetingPoints by adminRepository.observeMeetingPoints().collectAsState(initial = emptyList())
    val cartCalculation by cartRepository.cartCalculation.collectAsState()
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
    val buyerOrders by orderRepository.observeOrdersForBuyer(profile.id).collectAsState(initial = emptyList())
    val buyerWarnings by orderRepository.observeUserWarnings(profile.id).collectAsState(initial = emptyList())
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

    var realStoresWithProducts by remember { mutableStateOf<List<StoreCatalogGroup>>(emptyList()) }

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

    // Manejo nativo del botón / gesto Atrás de Android
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

    var categoriesList by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun loadCatalog(isSilent: Boolean = false) {
        if (!isSilent && realStoresWithProducts.isEmpty()) {
            isLoadingCatalog = true
        }
        coroutineScope.launch {
            val prodsResult = productRepository.getActiveProducts()
            val sellersResult = productRepository.getSellerProfiles()
            val catsResult = productRepository.getCategories()

            val products = prodsResult.getOrDefault(emptyList())
            val sellers = sellersResult.getOrDefault(emptyList()).associateBy { it.id }
            categoriesList = catsResult.getOrDefault(emptyList())

            if (products.isNotEmpty()) {
                val grouped = products.groupBy { it.sellerId }.mapNotNull { (sellerId, sellerProds) ->
                    val seller = sellers[sellerId]
                    // Validación estricta: Solo mostrar el puesto si existe y su rol es EMPRENDEDOR
                    if (seller == null || seller.role != UserRole.EMPRENDEDOR) {
                        return@mapNotNull null
                    }
                    // Si el vendedor tiene el puesto cerrado físicamente y no acepta pedidos, se oculta
                    if (!seller.acceptingOrders && seller.businessStatus == "CERRADO") {
                        return@mapNotNull null
                    }
                    val bName = seller.businessName?.trim().orEmpty()
                    val fName = seller.fullName.trim()
                    val storeTitle = when {
                        bName.isNotBlank() && fName.isNotBlank() && !bName.equals(fName, ignoreCase = true) -> "$bName - $fName"
                        bName.isNotBlank() -> bName
                        fName.isNotBlank() -> fName
                        else -> "Emprendimiento CampusGO"
                    }
                    val loc = seller.businessLocation?.trim()?.takeIf { it.isNotBlank() } ?: "Campus ${currentProfile.campus}"
                    StoreCatalogGroup(
                        sellerId = sellerId,
                        sellerName = storeTitle,
                        location = loc,
                        bannerUrl = seller.bannerUrl,
                        avatarUrl = seller.avatarUrl,
                        businessStatus = seller.businessStatus,
                        openTime = seller.openTime,
                        closeTime = seller.closeTime,
                        description = seller.businessDescription,
                        acceptingOrders = seller.acceptingOrders,
                        products = sellerProds,
                        sellerProfile = seller,
                        businessCategory = seller.businessCategory,
                        phone = seller.phone,
                        ratingAverage = seller.ratingAverage,
                        supportedMeetingPoints = seller.supportedMeetingPoints,
                        supportedPaymentMethods = seller.effectivePaymentMethods
                    )
                }
                realStoresWithProducts = grouped
            } else {
                realStoresWithProducts = emptyList()
            }
            isLoadingCatalog = false
        }
    }

    LaunchedEffect(Unit) {
        adminRepository.refreshMeetingPoints()
        loadCatalog(isSilent = false)
        while (isActive) {
            delay(30000L)
            loadCatalog(isSilent = true)
        }
    }

    LaunchedEffect(profile.id) {
        val favRes = productRepository.getFavoriteProductIds(profile.id)
        if (favRes.isSuccess) {
            favoriteProductIds = favRes.getOrDefault(emptySet())
        }
    }

    val onToggleFavoriteAction: (String) -> Unit = { prodId ->
        val wasFav = favoriteProductIds.contains(prodId)
        favoriteProductIds = if (wasFav) favoriteProductIds - prodId else favoriteProductIds + prodId
        coroutineScope.launch {
            if (wasFav) {
                productRepository.removeFavorite(profile.id, prodId)
            } else {
                productRepository.addFavorite(profile.id, prodId)
            }
        }
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

    // Pantalla completa de Carrito de Compras (oculta barra inferior y muestra flecha de volver)
    if (showCartScreen) {
        CartScreen(
            buyerProfile = currentProfile,
            onNavigateBack = { showCartScreen = false },
            onNavigateToTracking = {
                showCartScreen = false
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
                cartRepository.setStoreName(selectedStoreForProfile!!.sellerId, selectedStoreForProfile!!.sellerName)
                cartRepository.addToCart(product, 1)
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
                    cartRepository.setStoreName(store.sellerId, store.sellerName)
                    cartRepository.addToCart(product, quantity)
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
                cartRepository.setStoreName(store.sellerId, store.sellerName)
                cartRepository.addToCart(product, quantity)
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
                                spotColor = Color(0x1F16324F),
                                ambientColor = Color(0x2816324F),
                                clip = false
                            ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                            color = Color(0xFF16324F),
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
                                            color = Color(0xFF64748B),
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
                                    // 0. Botón de Advertencia / Strikes (igual que en el vendedor)
                                    Box(
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                showStrikesBottomSheet = true
                                            },
                                            shape = CircleShape,
                                            color = if (buyerWarnings.isNotEmpty()) Color(0xFFFFF7ED) else Color.White,
                                            border = BorderStroke(
                                                1.dp,
                                                if (buyerWarnings.isNotEmpty()) Color(0xFFFFD8BF) else Color(0xFFE2E8F0)
                                            ),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = "Avisos y Moderación",
                                                    tint = if (buyerWarnings.isNotEmpty()) Color(0xFFEA580C) else Color(0xFF64748B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Badge con cantidad de strikes activos
                                        if (buyerWarnings.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 2.dp, y = (-2).dp)
                                            ) {
                                                Badge(
                                                    containerColor = Color(0xFFDC2626),
                                                    contentColor = Color.White
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
                                    // hasPendingNotifications derivado arriba de forma reactiva con IDs leídos
                                    Box(
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                readNotificationIds = readNotificationIds + currentNotificationIds
                                                showNotificationsDialog = true
                                            },
                                            shape = CircleShape,
                                            color = if (hasPendingNotifications) Color(0xFFE8F7F2) else Color.White,
                                            border = BorderStroke(1.dp, if (hasPendingNotifications) Color(0xFFCCFBF1) else Color(0xFFE2E8F0)),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Notifications,
                                                    contentDescription = "Notificaciones",
                                                    tint = if (hasPendingNotifications) Color(0xFF00A884) else Color(0xFF16324F),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Punto rojo indicador en la esquina superior derecha con borde blanco de corte
                                        if (hasPendingNotifications) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = (-2).dp, y = 2.dp)
                                                    .background(Color(0xFFEF4444), CircleShape)
                                                    .border(1.5.dp, Color.White, CircleShape)
                                            )
                                        }
                                    }

                                    // 2. Carrito de Compras (Lado Derecho) con animación que se pone verde y más grande
                                    AnimatedContent(
                                        targetState = cartCalculation.totalItemCount > 0,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.88f))
                                                .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.88f))
                                        },
                                        label = "cart_button_animation"
                                    ) { hasItems ->
                                        if (hasItems) {
                                            // Carrito con productos: se pone verde institucional y más grande con el resumen
                                            Surface(
                                                onClick = { showCartScreen = true },
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color(0xFF00A884),
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
                                                        tint = Color.White,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Text(
                                                        text = "${cartCalculation.totalItemCount} • S/ %.2f".format(cartCalculation.grandTotal),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            // Carrito vacío: botón circular blanco minimalista
                                            Surface(
                                                onClick = { showCartScreen = true },
                                                shape = CircleShape,
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                shadowElevation = 1.dp,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .scale(cartScale.value)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_cart_custom),
                                                        contentDescription = "Mi Carrito",
                                                        tint = Color(0xFF16324F),
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
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Borrar búsqueda",
                                                tint = Color(0xFF64748B),
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
                                    focusedContainerColor = Color(0xFFF1F5F9),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF00A884),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF16324F),
                                    unfocusedTextColor = Color(0xFF16324F)
                                )
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFFF8FAFC)
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
                    // Advertencias formales emitidas por el Administrador
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        OfficialWarningBanner(
                            warnings = buyerWarnings,
                            isSeller = false,
                            onBannerClick = { showStrikesBottomSheet = true }
                        )
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
                                    containerColor = if (isReady) Color(0xFFF0FDF4) else Color(0xFFF0F9FF)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isReady) Color(0xFF86EFAC) else Color(0xFFBAE6FD)
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
                                                color = if (isReady) Color(0xFF00A884) else Color(0xFF0284C7),
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
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Texto de notificación según lo solicitado
                                            Text(
                                                text = text,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReady) Color(0xFF14532D) else Color(0xFF0C4A6E)
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
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Línea de temporización de 5 segundos con bordes redondeados y margen estético que no corta las esquinas de la tarjeta
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp)
                                            .padding(bottom = 10.dp)
                                            .height(3.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = timerProgress.value)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isReady) Color(0xFF00A884) else Color(0xFF0284C7))
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
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFB2E7DC)),
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
                                                        color = Color(0xFF16324F),
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
                                                        tint = Color(0xFF00A884),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = currentSelectedStore.location ?: "Campus",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF00A884),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!currentSelectedStore.openTime.isNullOrBlank()) {
                                                        Text(text = "•", fontSize = 11.sp, color = Color(0xFF00A884))
                                                        Text(
                                                            text = "${currentSelectedStore.openTime} - ${currentSelectedStore.closeTime}",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B),
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
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE6F7F3))
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
                                                tint = Color(0xFF00A884),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Ver perfil completo, banner y puntos de entrega",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00A884)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Ir al perfil",
                                            tint = Color(0xFF00A884),
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
                                CircularProgressIndicator(color = Color(0xFF00A884), strokeWidth = 3.dp)
                                Text(
                                    text = "Cargando delicias universitarias...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
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
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_store_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = Color(0xFF94A3B8)
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
                                    color = Color(0xFF16324F),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = when {
                                        activeCategoryLabel != null -> "Los emprendedores de la sección $activeCategoryLabel publicarán nuevos productos muy pronto."
                                        else -> "Intenta buscando por otro término o selecciona otra categoría o puesto."
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategoryFilter = "TODOS"
                                        selectedStoreId = null
                                        onlyOpenStores = false
                                        loadCatalog()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
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
                                    color = if (activeCategoryLabel != null) Color(0xFF00A884) else Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )
                                if (activeCategoryLabel != null) {
                                    Text(
                                        text = "Catálogo exclusivo de $activeCategoryLabel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (activeCategoryLabel != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFE6F7F3),
                                    border = BorderStroke(1.dp, Color(0xFF00A884).copy(alpha = 0.35f)),
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
                                            color = Color(0xFF00A884)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar filtro",
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "${allMatchingProducts.size} disponibles",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
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
                                        cartRepository.setStoreName(store1.sellerId, store1.sellerName)
                                        cartRepository.addToCart(prod1, 1)
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
                                            cartRepository.setStoreName(store2.sellerId, store2.sellerName)
                                            cartRepository.addToCart(prod2, 1)
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
                    cartRepository.setStoreName(store.sellerId, store.sellerName)
                    cartRepository.addToCart(prod, 1)
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
            OrderTrackingScreen(
                buyerProfile = currentProfile,
                onNavigateBack = null,
                onNavigateToCart = {
                    showCartScreen = true
                }
            )
        }
        BuyerBottomNavTab.CHATS -> {
            ActiveChatsSheet(
                chats = activeBuyerChats,
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
                        val res = productRepository.updateUserProfile(updated)
                        if (res.isSuccess) {
                            currentProfile = res.getOrNull() ?: updated
                        }
                    }
                },
                onUploadAvatar = { bytes, onUploaded ->
                    coroutineScope.launch {
                        val oldUrl = currentProfile.avatarUrl
                        val path = "avatars/user_${currentProfile.id}.jpg"
                        val res = productRepository.uploadImage("business-assets", path, bytes)
                        res.onSuccess { url ->
                            if (!oldUrl.isNullOrBlank() && !oldUrl.contains("avatars/user_${currentProfile.id}.jpg")) {
                                productRepository.deleteImage("business-assets", oldUrl)
                            }
                            val freshUrl = if (url.contains("?")) url else "$url?v=${System.currentTimeMillis()}"
                            onUploaded(freshUrl)
                            val updated = currentProfile.copy(avatarUrl = freshUrl)
                            productRepository.updateUserProfile(updated)
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
            onTabSelected = { currentTab = it },
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
