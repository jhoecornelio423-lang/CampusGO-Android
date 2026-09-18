package com.example.vallego.features.buyer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.vallego.domain.model.Category
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.verificationCode
import com.example.vallego.R
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AdminRepository
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.repository.ProductRepository
import com.example.vallego.features.cart.CartScreen
import com.example.vallego.features.tracking.OrderTrackingScreen
import com.example.vallego.ui.components.EnlargedPhotoViewerDialog
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoProductImage
import com.example.vallego.ui.components.ValleGoUserAvatar
import com.example.vallego.ui.components.compressImageUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    orderRepository: OrderRepository = koinInject()
) {
    var currentProfile by remember { mutableStateOf(profile) }
    var showProfile by remember { mutableStateOf(false) }
    var showCart by remember { mutableStateOf(false) }
    var showTracking by remember { mutableStateOf(false) }
    var selectedStoreForProfile by remember { mutableStateOf<StoreCatalogGroup?>(null) }
    val allMeetingPoints by adminRepository.observeMeetingPoints().collectAsState(initial = emptyList())
    val cartCalculation by cartRepository.cartCalculation.collectAsState()
    val buyerOrders by orderRepository.observeOrdersForBuyer(profile.id).collectAsState(initial = emptyList())
    val readyOrdersInfo = remember(buyerOrders) {
        buyerOrders.flatMap { order ->
            order.subOrders
                .filter { it.status == SubOrderStatus.LISTO || it.status == SubOrderStatus.ESPERANDO_ENTREGA }
                .map { sub -> Triple(order, sub, "${order.id}_${sub.id}") }
        }
    }
    var dismissedReadyAlerts by remember { mutableStateOf(setOf<String>()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("TODOS") }
    var onlyOpenStores by remember { mutableStateOf(false) }
    var selectedStoreId by remember { mutableStateOf<String?>(null) }

    var selectedProductForDetail by remember { mutableStateOf<Pair<Product, StoreCatalogGroup>?>(null) }

    // Manejo nativo del botón / gesto Atrás de Android
    BackHandler(enabled = selectedProductForDetail != null) {
        selectedProductForDetail = null
    }
    BackHandler(enabled = selectedProductForDetail == null && selectedStoreForProfile != null) {
        selectedStoreForProfile = null
    }
    BackHandler(enabled = selectedProductForDetail == null && selectedStoreForProfile == null && showCart) {
        showCart = false
    }
    BackHandler(enabled = selectedProductForDetail == null && selectedStoreForProfile == null && !showCart && showTracking) {
        showTracking = false
    }
    BackHandler(enabled = selectedProductForDetail == null && selectedStoreForProfile == null && !showCart && !showTracking && showProfile) {
        showProfile = false
    }

    var realStoresWithProducts by remember { mutableStateOf<List<StoreCatalogGroup>>(emptyList()) }
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
                        else -> "Emprendimiento Campus Go"
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
        while (isActive) {
            loadCatalog(isSilent = true)
            delay(8000)
        }
    }

    if (showProfile) {
        BuyerProfileScreen(
            profile = currentProfile,
            onNavigateBack = { showProfile = false },
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
                    val path = "avatars/${currentProfile.id}_${System.currentTimeMillis()}.jpg"
                    val res = productRepository.uploadImage("business-assets", path, bytes)
                    res.onSuccess { url ->
                        onUploaded(url)
                    }
                }
            },
            onSignOut = onSignOut,
            modifier = modifier
        )
        return
    }

    if (showTracking) {
        OrderTrackingScreen(
            buyerProfile = currentProfile,
            onNavigateBack = { showTracking = false },
            onNavigateToCart = {
                showTracking = false
                showCart = true
            },
            modifier = modifier
        )
        return
    }

    if (showCart) {
        CartScreen(
            buyerProfile = currentProfile,
            onNavigateBack = { showCart = false },
            onNavigateToTracking = {
                showCart = false
                showTracking = true
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
                showCart = true
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
                val matchesCategory = when (selectedCategoryFilter) {
                    "TODOS" -> true
                    "COMIDAS" -> catName.contains("COMIDA") || catName.contains("ALMUERZO") || prod.name.lowercase().contains("hamburguesa") || prod.name.lowercase().contains("pollo")
                    "POSTRES" -> catName.contains("POSTRE") || catName.contains("DULCE") || prod.name.lowercase().contains("queque") || prod.name.lowercase().contains("torta") || prod.name.lowercase().contains("alfajor")
                    "BEBIDAS" -> catName.contains("BEBIDA") || catName.contains("JUGO") || prod.name.lowercase().contains("chicha") || prod.name.lowercase().contains("café") || prod.name.lowercase().contains("cafe")
                    "SNACKS" -> catName.contains("SNACK") || prod.name.lowercase().contains("papa") || prod.name.lowercase().contains("snack") || prod.name.lowercase().contains("galleta")
                    "PAPELERIA" -> catName.contains("PAPEL") || catName.contains("UTIL") || prod.name.lowercase().contains("cuaderno") || prod.name.lowercase().contains("copia")
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
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            topBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Fila 1: Perfil del Comprador (Foto e Información como UN SOLO BOTÓN a la izquierda) y botones de acción a la derecha
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
                                    .clickable { showProfile = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                ValleGoUserAvatar(
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

                            // Botones de acción a la derecha (Mis Pedidos y Carrito)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Mis Pedidos (Tracking)
                                IconButton(
                                    onClick = { showTracking = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Mis Pedidos",
                                        tint = Color(0xFF16324F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 2. Carrito Destacado
                                if (cartCalculation.totalItemCount > 0) {
                                    Surface(
                                        onClick = { showCart = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF00A884),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Carrito",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
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
                                    IconButton(
                                        onClick = { showCart = true },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "Carrito",
                                            tint = Color(0xFF16324F),
                                            modifier = Modifier.size(22.dp)
                                        )
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
                                    text = "¿Qué buscas hoy? (hamburguesa, café, postre...)",
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
            },
            containerColor = Color(0xFFF8FAFC)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = if (cartCalculation.totalItemCount > 0) 70.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. Banner de aviso en tiempo real de pedidos listos para recoger
                    val visibleReadyOrders = remember(readyOrdersInfo, dismissedReadyAlerts) {
                        readyOrdersInfo.filter { it.third !in dismissedReadyAlerts }
                    }
                    visibleReadyOrders.forEach { (order, subOrder, alertKey) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.5.dp, Color(0xFF00A884)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF00A884),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "¡Tu pedido está listo!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF004D40)
                                            )
                                            Text(
                                                text = "Puesto: ${subOrder.sellerName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00796B)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { dismissedReadyAlerts = dismissedReadyAlerts + alertKey },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Ocultar aviso",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Tu pedido de \"${subOrder.sellerName}\" está listo. Acércate al punto de encuentro \"${order.meetingPointName}\"${if (order.scheduledTime.isNotBlank()) " (Horario: ${order.scheduledTime})" else ""}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF16324F),
                                    fontWeight = FontWeight.Medium
                                )

                                Surface(
                                    color = Color(0xFFE0F2F1),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF80CBC4)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = Color(0xFF00796B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Código de Entrega:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004D40)
                                            )
                                        }
                                        Text(
                                            text = "#${subOrder.verificationCode}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp,
                                            color = Color(0xFF004D40)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showTracking = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ver punto de encuentro y seguimiento",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 1. Carrusel Horizontal de Puestos (Historias del Campus)
                    if (realStoresWithProducts.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PUESTOS DEL CAMPUS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )
                                if (selectedStoreId != null) {
                                    Text(
                                        text = "Ver todos",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00A884),
                                        modifier = Modifier.clickable { selectedStoreId = null }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Burbuja "Todos"
                                val isAllSelected = selectedStoreId == null
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedStoreId = null }
                                        .padding(2.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAllSelected) Color(0xFF00A884) else Color.White,
                                        border = BorderStroke(
                                            width = if (isAllSelected) 2.5.dp else 1.5.dp,
                                            color = if (isAllSelected) Color(0xFF00A884) else Color(0xFFCBD5E1)
                                        ),
                                        shadowElevation = if (isAllSelected) 3.dp else 1.dp,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Store,
                                                contentDescription = "Todos",
                                                tint = if (isAllSelected) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Todos",
                                        fontSize = 11.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) Color(0xFF00A884) else Color(0xFF475569),
                                        maxLines = 1
                                    )
                                }

                                // Cada puesto individual
                                realStoresWithProducts.forEach { store ->
                                    val isSelected = selectedStoreId == store.sellerId
                                    val isOpen = store.acceptingOrders && !store.businessStatus.equals("CERRADO", ignoreCase = true)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(62.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedStoreId = if (selectedStoreId == store.sellerId) null else store.sellerId
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                                    color = if (isSelected) Color(0xFF00A884) else if (isOpen) Color(0xFF34D399) else Color(0xFFCBD5E1)
                                                ),
                                                shadowElevation = if (isSelected) 3.dp else 1.dp,
                                                modifier = Modifier.size(54.dp)
                                            ) {
                                                ValleGoBusinessAvatar(
                                                    avatarUrl = store.avatarUrl,
                                                    storeName = store.sellerName,
                                                    size = 54.dp
                                                )
                                            }

                                            // Punto indicador de disponibilidad
                                            Box(
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .padding(1.5.dp)
                                                    .align(Alignment.BottomEnd)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                !isOpen -> Color(0xFF94A3B8)
                                                                store.businessStatus.equals("SATURADO", ignoreCase = true) -> Color(0xFFF97316)
                                                                else -> Color(0xFF22C55E)
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = store.sellerName.split(" ").take(2).joinToString(" "),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF00A884) else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Carrusel de Categorías con Iconografía Vectorial Moderna (Lucide Vectors)
                    data class CategoryChipData(val key: String, val iconRes: Int, val label: String)
                    val categoryChips = listOf(
                        CategoryChipData("TODOS", com.example.vallego.R.drawable.ic_cat_all, "Todos"),
                        CategoryChipData("COMIDAS", com.example.vallego.R.drawable.ic_cat_food, "Comidas"),
                        CategoryChipData("POSTRES", com.example.vallego.R.drawable.ic_cat_desserts, "Postres"),
                        CategoryChipData("BEBIDAS", com.example.vallego.R.drawable.ic_cat_drinks, "Bebidas"),
                        CategoryChipData("SNACKS", com.example.vallego.R.drawable.ic_cat_snacks, "Snacks"),
                        CategoryChipData("PAPELERIA", com.example.vallego.R.drawable.ic_cat_stationery, "Papelería")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categoryChips.forEach { item ->
                            val isSelected = selectedCategoryFilter == item.key
                            Surface(
                                onClick = { selectedCategoryFilter = item.key },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF00A884) else Color.White,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF00A884) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 1.dp,
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = item.label,
                                        tint = if (isSelected) Color.White else Color(0xFF00A884),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = item.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
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
                                            ValleGoBusinessAvatar(
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
                                                        painter = painterResource(id = R.drawable.ic_meeting_point),
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
                                                imageVector = Icons.Default.Store,
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
                                .padding(40.dp),
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
                            modifier = Modifier.fillMaxWidth(),
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
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\"" else "No hay productos disponibles por ahora",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF16324F)
                                )
                                Text(
                                    text = "Intenta buscando por otro término o selecciona otra categoría o puesto.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
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
                                    Text("Restablecer Filtros", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // 5. Encabezado de Productos y Cuadrícula Directa de 2 Columnas (Propuesta A)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedStoreId != null) "PRODUCTOS DEL PUESTO" else "TODOS LOS PRODUCTOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${allMatchingProducts.size} disponibles",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        val productPairs = allMatchingProducts.chunked(2)
                        productPairs.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 5. Barra Flotante de Carrito Adhesiva Inferior (Rappi Sticky Cart Bar)
            AnimatedVisibility(
                visible = cartCalculation.totalItemCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    onClick = { showCart = true },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF00A884),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${cartCalculation.totalItemCount}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ver Carrito",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "S/ %.2f".format(cartCalculation.grandTotal),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerProfileScreen(
    profile: UserProfile,
    onNavigateBack: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    onUploadAvatar: (ByteArray, (String) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var studentCode by remember(profile) { mutableStateOf(profile.studentCode.orEmpty()) }
    var campus by remember(profile) { mutableStateOf(profile.campus) }
    var avatarUrl by remember(profile) { mutableStateOf(profile.avatarUrl) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = compressImageUri(context, uri)
            if (bytes != null) {
                isUploading = true
                onUploadAvatar(bytes) { newUrl ->
                    isUploading = false
                    avatarUrl = newUrl
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Editar Mi Perfil" else "Mi Perfil",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16324F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            fullName = profile.fullName
                            phone = profile.phone
                            studentCode = profile.studentCode.orEmpty()
                            campus = profile.campus
                            avatarUrl = profile.avatarUrl
                            isEditMode = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF16324F)
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        FilledTonalButton(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE6F7F3),
                                contentColor = Color(0xFF00A884)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Editar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEditMode) {
                        Button(
                            onClick = {
                                val updated = profile.copy(
                                    fullName = fullName.trim().ifBlank { profile.fullName },
                                    phone = phone.trim(),
                                    studentCode = studentCode.trim().takeIf { it.isNotBlank() },
                                    campus = campus.trim().ifBlank { profile.campus },
                                    avatarUrl = avatarUrl
                                )
                                onSaveProfile(updated)
                                isEditMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                fullName = profile.fullName
                                phone = profile.phone
                                studentCode = profile.studentCode.orEmpty()
                                campus = profile.campus
                                avatarUrl = profile.avatarUrl
                                isEditMode = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Cancelar Edición", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                shadowElevation = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = if (!isEditMode) Modifier.clip(CircleShape).clickable { showEnlargedPhoto = true } else Modifier
                        ) {
                            ValleGoUserAvatar(
                                avatarUrl = avatarUrl,
                                name = fullName,
                                size = 96.dp
                            )
                        }
                        if (isEditMode) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00A884),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Cambiar foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isEditMode) {
                        Text(
                            text = "Toca la cámara para cambiar tu foto de perfil",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F)
                        )

                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_school_cap),
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estudiante / Comprador Campus Go",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
                shadowElevation = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "INFORMACIÓN DE LA CUENTA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )

                    if (!isEditMode) {
                        ProfileDetailRow(
                            icon = Icons.Default.Badge,
                            label = "Código Universitario",
                            value = studentCode.ifBlank { "No registrado" }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileDetailRow(
                            icon = Icons.Default.Phone,
                            label = "Teléfono / WhatsApp",
                            value = phone.ifBlank { "No registrado" }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileDetailRow(
                            icon = Icons.Default.LocationOn,
                            label = "Campus Universitario",
                            value = "Campus $campus"
                        )
                    } else {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nombre Completo") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00A884)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = studentCode,
                            onValueChange = { studentCode = it },
                            label = { Text("Código de Estudiante") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF00A884)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono / WhatsApp") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00A884)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = campus,
                            onValueChange = { campus = it },
                            label = { Text("Campus") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00A884)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEnlargedPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = avatarUrl,
            name = fullName,
            roleDescription = "Estudiante / Comprador • Campus $campus",
            onDismiss = { showEnlargedPhoto = false }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00A884),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BuyerProductGridCard(
    product: Product,
    store: StoreCatalogGroup,
    categoryName: String?,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onStoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isStoreAvail = store.acceptingOrders &&
            !store.businessStatus.equals("PAUSADO", ignoreCase = true) &&
            !store.businessStatus.equals("CERRADO", ignoreCase = true)
    val isAvailable = isStoreAvail && product.stock > 0

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEF2F6)),
        shadowElevation = 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                ValleGoProductImage(
                    imageUrl = product.imageUrl,
                    categoryName = categoryName,
                    productName = product.name,
                    emojiSize = 42,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay si no está disponible
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xDD000000),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (product.stock <= 0) "Agotado" else "Puesto Cerrado",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Información del Producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Etiqueta del puesto
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .then(
                            if (onStoreClick != null) Modifier.clickable { onStoreClick() }
                            else Modifier
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = Color(0xFF00A884),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = store.sellerName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A884),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Nombre del producto
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16324F),
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Fila de Precio y Botón rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        onClick = {
                            if (isAvailable) onQuickAdd() else onClick()
                        },
                        shape = CircleShape,
                        color = if (isAvailable) Color(0xFF00A884) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isAvailable) Icons.Default.Add else Icons.Default.Block,
                                contentDescription = "Agregar",
                                tint = if (isAvailable) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

