package com.example.campusgo.features.seller

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.campusgo.core.notification.AppNotificationPayload
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.verificationCode
import com.example.campusgo.features.chat.ActiveChatSummary
import com.example.campusgo.features.chat.ActiveChatsSheet
import com.example.campusgo.features.chat.OrderChatBottomSheet
import com.example.campusgo.features.chat.OrderChatViewModel
import com.example.campusgo.features.chat.SupportChatBottomSheet
import com.example.campusgo.features.seller.components.BuyerProfileDetailDialog
import com.example.campusgo.features.seller.components.SellerAddProductBottomSheet
import com.example.campusgo.features.seller.components.SellerBottomNavBar
import com.example.campusgo.features.seller.components.SellerDashboardHeader
import com.example.campusgo.features.seller.components.SellerDatePickerDialog
import com.example.campusgo.features.seller.components.SellerDeliveryConfirmationBottomSheet
import com.example.campusgo.features.seller.components.SellerEditProductBottomSheet
import com.example.campusgo.features.seller.components.SellerEditStockBottomSheet
import com.example.campusgo.features.seller.components.SellerNoShowBottomSheet
import com.example.campusgo.features.seller.components.SellerNotificationsBottomSheet
import com.example.campusgo.features.seller.components.SellerRejectionBottomSheet
import com.example.campusgo.features.seller.tabs.SellerOrdersTab
import com.example.campusgo.features.seller.tabs.SellerProductsTab
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    pendingSubOrderId: String? = null,
    onClearPendingSubOrder: () -> Unit = {},
    pendingRoute: AppNotificationPayload? = null,
    onClearPendingRoute: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SellerDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val curProf = uiState.sellerProfile ?: profile
    val prefs = remember(context) { context.getSharedPreferences("campusgo_seller_prefs", Context.MODE_PRIVATE) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var hasShownWarningBannerOnEntry by remember { mutableStateOf(false) }
    var showWarningBanner by remember { mutableStateOf(false) }
    val timerProgress = remember { Animatable(1f) }
    var lastReadWarningCount by remember { mutableIntStateOf(-1) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val chatViewModel: OrderChatViewModel = koinViewModel()
    var activeChatSubOrder by remember { mutableStateOf<SubOrder?>(null) }
    var selectedBuyerForProfile by remember { mutableStateOf<SubOrder?>(null) }
    var activeSupportTicket by remember { mutableStateOf<SupportTicket?>(null) }

    LaunchedEffect(pendingRoute, pendingSubOrderId, uiState.subOrders) {
        val effectiveSubOrderId = when (val route = pendingRoute) {
            is AppNotificationPayload.OrderChat -> route.subOrderId
            else -> pendingSubOrderId
        }

        if (!effectiveSubOrderId.isNullOrBlank()) {
            val subId = effectiveSubOrderId
            val matching = uiState.subOrders.find { it.id == subId }
            if (matching != null) {
                activeChatSubOrder = matching
                chatViewModel.initChat(
                    subOrderId = matching.id,
                    currentUserId = profile.id,
                    otherUserId = matching.buyerId ?: "",
                    otherUserName = matching.buyerName?.ifBlank { "Comprador Campus Go" } ?: "Comprador Campus Go",
                    meetingPoint = matching.meetingPointName ?: "Punto por convenir",
                    subOrderStatus = matching.status,
                    otherUserAvatarUrl = matching.buyerAvatarUrl,
                    deliveryCode = ""
                )
                onClearPendingSubOrder()
                onClearPendingRoute()
            } else {
                coroutineScope.launch {
                    val fetched = viewModel.getSubOrderById(subId)
                    if (fetched != null) {
                        activeChatSubOrder = fetched
                        chatViewModel.initChat(
                            subOrderId = fetched.id,
                            currentUserId = profile.id,
                            otherUserId = fetched.buyerId ?: "",
                            otherUserName = fetched.buyerName?.ifBlank { "Comprador Campus Go" } ?: "Comprador Campus Go",
                            meetingPoint = fetched.meetingPointName ?: "Punto por convenir",
                            subOrderStatus = fetched.status,
                            otherUserAvatarUrl = fetched.buyerAvatarUrl,
                            deliveryCode = ""
                        )
                        onClearPendingSubOrder()
                        onClearPendingRoute()
                    }
                }
            }
        }

        when (val route = pendingRoute) {
            is AppNotificationPayload.SupportChat -> {
                coroutineScope.launch {
                    val ticket = viewModel.getTicketById(route.ticketId, curProf.id)
                    if (ticket != null) {
                        activeSupportTicket = ticket
                        onClearPendingRoute()
                    }
                }
            }
            is AppNotificationPayload.Warning -> {
                showNotificationsSheet = true
                onClearPendingRoute()
            }
            is AppNotificationPayload.OrderTracking -> {
                viewModel.setSelectedTab(SellerTab.PEDIDOS)
                onClearPendingRoute()
            }
            else -> {}
        }
    }

    if (activeChatSubOrder != null) {
        OrderChatBottomSheet(
            viewModel = chatViewModel,
            onDismiss = {
                activeChatSubOrder = null
                chatViewModel.clearChat()
            }
        )
        return
    }

    BackHandler(enabled = activeChatSubOrder == null && uiState.selectedTab != SellerTab.PEDIDOS) {
        viewModel.setSelectedTab(SellerTab.PEDIDOS)
    }

    BackHandler(enabled = activeSupportTicket != null) {
        activeSupportTicket = null
    }

    val isAnyModalOpen = uiState.selectedSubOrderForRejection != null ||
            uiState.selectedSubOrderForDelivery != null ||
            uiState.showAddProductDialog ||
            uiState.selectedProductForEdit != null ||
            uiState.selectedSubOrderForNoShow != null ||
            uiState.selectedProductForStockEdit != null ||
            uiState.selectedSubOrderForDetail != null ||
            selectedBuyerForProfile != null ||
            showNotificationsSheet ||
            showDatePickerDialog ||
            activeChatSubOrder != null

    activeSupportTicket?.let { ticket ->
        SupportChatBottomSheet(
            ticket = ticket,
            currentUserId = curProf.id,
            isAdmin = false,
            onDismiss = {
                activeSupportTicket = null
                viewModel.refreshActiveTicket(curProf.id)
            }
        )
        return
    }

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "seller_dialog_blur"
    )

    LaunchedEffect(profile.id) {
        viewModel.initialize(profile.id, profile.acceptingOrders, profile.businessLocation)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.warnings) {
        if (uiState.warnings.isNotEmpty()) {
            val isNewStrike = lastReadWarningCount != -1 && uiState.warnings.size > lastReadWarningCount
            val shouldShow = !hasShownWarningBannerOnEntry || isNewStrike

            lastReadWarningCount = uiState.warnings.size

            if (shouldShow) {
                hasShownWarningBannerOnEntry = true
                showWarningBanner = true
                timerProgress.snapTo(1f)
                timerProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                )
                showWarningBanner = false
            }
        }
    }

    // Modal BottomSheet de Rechazo o Cancelación de Subpedido
    if (uiState.selectedSubOrderForRejection != null) {
        val subOrder = uiState.selectedSubOrderForRejection!!
        SellerRejectionBottomSheet(
            subOrder = subOrder,
            onConfirmRejection = { subId, reason ->
                viewModel.confirmRejection(subId, reason)
            },
            onDismiss = { viewModel.dismissRejectionDialog() }
        )
    }

    // Bottom Sheet de Confirmación de Entrega y Cobro con Código de Seguridad
    if (uiState.selectedSubOrderForDelivery != null) {
        val subOrder = uiState.selectedSubOrderForDelivery!!
        SellerDeliveryConfirmationBottomSheet(
            subOrder = subOrder,
            onConfirm = { subId ->
                viewModel.confirmDeliveryAndPayment(subId)
            },
            onDismiss = { viewModel.dismissDeliveryDialog() }
        )
    }

    // Modal BottomSheet de Agregar Nuevo Producto al Catálogo
    if (uiState.showAddProductDialog) {
        SellerAddProductBottomSheet(
            categories = uiState.categories,
            isSavingProduct = uiState.isSavingProduct,
            errorMessage = uiState.errorMessage,
            onUploadAsset = { bucket, path, bytes, onUploaded ->
                viewModel.uploadAsset(bucket, path, bytes, onUploaded)
            },
            onCreateProduct = { name, price, stock, catId, desc, imgUrl, id ->
                viewModel.createProduct(name, price, stock, catId, desc, imgUrl, id)
            },
            onDismiss = { viewModel.dismissAddProductDialog() }
        )
    }

    // Modal BottomSheet de Edición Completa de Producto
    if (uiState.selectedProductForEdit != null) {
        val prod = uiState.selectedProductForEdit!!
        SellerEditProductBottomSheet(
            product = prod,
            categories = uiState.categories,
            isSavingProduct = uiState.isSavingProduct,
            onUploadAsset = { bucket, path, bytes, onUploaded ->
                viewModel.uploadAsset(bucket, path, bytes, onUploaded)
            },
            onUpdateProduct = { id, name, price, stock, catId, desc, imgUrl ->
                viewModel.updateProduct(id, name, price, stock, catId, desc, imgUrl)
            },
            onDeleteProduct = { id ->
                viewModel.deleteProduct(id)
            },
            onDismiss = { viewModel.dismissEditProductDialog() }
        )
    }

    // Modal BottomSheet de Incidencia: Comprador no se presentó
    if (uiState.selectedSubOrderForNoShow != null) {
        val subOrder = uiState.selectedSubOrderForNoShow!!
        SellerNoShowBottomSheet(
            subOrder = subOrder,
            onConfirmBuyerNoShow = { subId, reason ->
                viewModel.confirmBuyerNoShow(subId, reason)
            },
            onDismiss = { viewModel.dismissNoShowDialog() }
        )
    }

    // Modal BottomSheet de Edición de Stock
    if (uiState.selectedProductForStockEdit != null) {
        val prod = uiState.selectedProductForStockEdit!!
        SellerEditStockBottomSheet(
            product = prod,
            onUpdateStock = { id, newStock ->
                viewModel.updateStock(id, newStock)
            },
            onDismiss = { viewModel.dismissEditStockDialog() }
        )
    }

    // Diálogo de Detalle del Subpedido
    if (uiState.selectedSubOrderForDetail != null) {
        val selectedSub = uiState.selectedSubOrderForDetail!!
        SellerOrderDetailDialog(
            subOrder = selectedSub,
            isProcessing = uiState.processingSubOrderIds.contains(selectedSub.id),
            buyerStrikes = selectedSub.buyerId?.let { uiState.buyerStrikes[it] } ?: 0,
            onDismiss = { viewModel.dismissSubOrderDetail() },
            onAccept = {
                viewModel.acceptSubOrder(selectedSub.id)
                viewModel.dismissSubOrderDetail()
            },
            onStartPrep = {
                viewModel.startPreparation(selectedSub.id)
                viewModel.dismissSubOrderDetail()
            },
            onMarkReady = {
                viewModel.markReady(selectedSub.id)
                viewModel.dismissSubOrderDetail()
            },
            onOpenDelivery = {
                viewModel.openDeliveryDialog(it)
                viewModel.dismissSubOrderDetail()
            },
            onOpenRejection = {
                viewModel.openRejectionDialog(it)
                viewModel.dismissSubOrderDetail()
            },
            onOpenBuyerProfile = { sub ->
                sub.buyerId?.let { viewModel.refreshBuyerStrikes(listOf(it)) }
                selectedBuyerForProfile = sub
            },
            onOpenChat = {
                activeChatSubOrder = selectedSub
                chatViewModel.initChat(
                    subOrderId = selectedSub.id,
                    currentUserId = curProf.id,
                    otherUserId = selectedSub.buyerId ?: "",
                    otherUserName = selectedSub.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                    meetingPoint = selectedSub.meetingPointName ?: "Punto por convenir",
                    subOrderStatus = selectedSub.status,
                    otherUserAvatarUrl = selectedSub.buyerAvatarUrl,
                    deliveryCode = ""
                )
                viewModel.dismissSubOrderDetail()
            }
        )
    }

    // Modal de Perfil Completo del Comprador (Visto por el Vendedor)
    selectedBuyerForProfile?.let { buyerSub ->
        BuyerProfileDetailDialog(
            subOrder = buyerSub,
            strikes = buyerSub.buyerId?.let { uiState.buyerStrikes[it] } ?: 0,
            onDismiss = { selectedBuyerForProfile = null },
            onOpenChat = {
                selectedBuyerForProfile = null
                activeChatSubOrder = buyerSub
                chatViewModel.initChat(
                    subOrderId = buyerSub.id,
                    currentUserId = curProf.id,
                    otherUserId = buyerSub.buyerId ?: "",
                    otherUserName = buyerSub.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                    meetingPoint = buyerSub.meetingPointName ?: "Punto por convenir",
                    subOrderStatus = buyerSub.status,
                    otherUserAvatarUrl = buyerSub.buyerAvatarUrl,
                    deliveryCode = ""
                )
            }
        )
    }

    // Modal BottomSheet de Notificaciones y Avisos de Moderación
    if (showNotificationsSheet) {
        SellerNotificationsBottomSheet(
            warnings = uiState.warnings,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    // Diálogo de Selección de Fecha de Historial
    if (showDatePickerDialog) {
        SellerDatePickerDialog(
            selectedDate = uiState.selectedDate,
            onSelectDate = { pickedDate ->
                viewModel.setSelectedDate(pickedDate)
            },
            onResetToToday = { viewModel.resetToToday() },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (uiState.selectedTab != SellerTab.PERFIL && uiState.selectedTab != SellerTab.MI_PUESTO) {
                    val unreadWarningsCount = (uiState.warnings.size - lastReadWarningCount).coerceAtLeast(0)
                    SellerDashboardHeader(
                        profile = curProf,
                        isAcceptingOrders = uiState.isAcceptingOrders,
                        warnings = uiState.warnings,
                        unreadWarningsCount = unreadWarningsCount,
                        onStoreClick = { viewModel.setSelectedTab(SellerTab.PERFIL) },
                        onToggleAcceptingOrders = { viewModel.toggleAcceptingOrders(it) },
                        onOpenNotifications = {
                            val currentWarningIds = uiState.warnings.map { it.id }.toSet()
                            val seenWarningIds = prefs.getStringSet("seen_warning_ids_${curProf.id}", emptySet()) ?: emptySet()
                            prefs.edit().putStringSet("seen_warning_ids_${curProf.id}", seenWarningIds + currentWarningIds).apply()
                            lastReadWarningCount = uiState.warnings.size
                            showWarningBanner = false
                            showNotificationsSheet = true
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = if (backgroundBlurRadius > 0.dp) modifier.blur(backgroundBlurRadius) else modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                when (uiState.selectedTab) {
                    SellerTab.PEDIDOS -> {
                        SellerOrdersTab(
                            uiState = uiState,
                            showWarningBanner = showWarningBanner,
                            timerProgress = timerProgress.value,
                            onWarningBannerClick = {
                                lastReadWarningCount = uiState.warnings.size
                                hasShownWarningBannerOnEntry = true
                                showWarningBanner = false
                                showNotificationsSheet = true
                            },
                            onResetToToday = { viewModel.resetToToday() },
                            onOpenDatePicker = { showDatePickerDialog = true },
                            onFilterSelected = { filter -> viewModel.setFilter(filter) },
                            onAcceptSubOrder = { id -> viewModel.acceptSubOrder(id) },
                            onStartPrep = { id -> viewModel.startPreparation(id) },
                            onMarkReady = { id -> viewModel.markReady(id) },
                            onOpenDelivery = { sub -> viewModel.openDeliveryDialog(sub) },
                            onOpenRejection = { sub -> viewModel.openRejectionDialog(sub) },
                            onOpenNoShow = { sub -> viewModel.openNoShowDialog(sub) },
                            onSubOrderExpired = { id -> viewModel.onSubOrderExpired(id) },
                            onOpenDetail = { sub ->
                                sub.buyerId?.let { viewModel.refreshBuyerStrikes(listOf(it)) }
                                viewModel.openSubOrderDetail(sub)
                            },
                            onOpenBuyerProfile = { sub ->
                                sub.buyerId?.let { viewModel.refreshBuyerStrikes(listOf(it)) }
                                selectedBuyerForProfile = sub
                            },
                            onOpenChat = { sub ->
                                activeChatSubOrder = sub
                                chatViewModel.initChat(
                                    subOrderId = sub.id,
                                    currentUserId = curProf.id,
                                    otherUserId = sub.buyerId ?: "",
                                    otherUserName = sub.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                                    meetingPoint = sub.meetingPointName ?: "Punto de entrega",
                                    subOrderStatus = sub.status,
                                    otherUserAvatarUrl = sub.buyerAvatarUrl,
                                    deliveryCode = ""
                                )
                            }
                        )
                    }
                    SellerTab.PRODUCTOS -> {
                        SellerProductsTab(
                            products = uiState.products,
                            categories = uiState.categories,
                            onOpenAddProductDialog = { viewModel.openAddProductDialog() },
                            onToggleProductActive = { id, active -> viewModel.toggleProductActive(id, active) },
                            onOpenEditStockDialog = { prod -> viewModel.openEditStockDialog(prod) },
                            onOpenEditProductDialog = { prod -> viewModel.openEditProductDialog(prod) }
                        )
                    }
                    SellerTab.ESTADISTICAS -> {
                        SellerStatisticsScreen(
                            sellerProfile = uiState.sellerProfile ?: profile,
                            subOrders = uiState.subOrders,
                            products = uiState.products,
                            statsData = uiState.statsData,
                            isLoadingStats = uiState.isLoadingStats,
                            onRangeChanged = { range -> viewModel.loadStatistics(range) }
                        )
                    }
                    SellerTab.CHATS -> {
                        val activeSellerChatSummaries = remember(uiState.subOrders) {
                            uiState.subOrders
                                .filter { !it.status.isFinal }
                                .map { sub ->
                                    ActiveChatSummary(
                                        subOrderId = sub.id,
                                        otherUserId = sub.buyerId ?: "",
                                        otherUserName = sub.buyerName?.ifBlank { "Comprador CampusGO" } ?: "Comprador CampusGO",
                                        meetingPoint = sub.meetingPointName ?: "Punto por acordar",
                                        status = sub.status,
                                        subtotal = sub.subtotalAmount,
                                        itemsSummary = sub.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                                        deliveryCode = "",
                                        isBuyerPerspective = false,
                                        otherUserAvatarUrl = sub.buyerAvatarUrl
                                    )
                                }
                        }

                        ActiveChatsSheet(
                            chats = activeSellerChatSummaries,
                            supportTickets = listOfNotNull(uiState.activeSupportTicket),
                            onSelectSupportTicket = { activeSupportTicket = it },
                            onSelectChat = { summary ->
                                val matchingSub = uiState.subOrders.find { it.id == summary.subOrderId }
                                if (matchingSub != null) {
                                    val targetBuyerId = matchingSub.buyerId?.takeIf { it.isNotBlank() }
                                        ?: summary.otherUserId.takeIf { it.isNotBlank() }
                                        ?: ""
                                    val targetBuyerName = matchingSub.buyerName?.ifBlank {
                                        summary.otherUserName.ifBlank { "Comprador" }
                                    } ?: summary.otherUserName.ifBlank { "Comprador" }
                                    activeChatSubOrder = matchingSub
                                    chatViewModel.initChat(
                                        subOrderId = matchingSub.id,
                                        currentUserId = curProf.id,
                                        otherUserId = targetBuyerId,
                                        otherUserName = targetBuyerName,
                                        meetingPoint = matchingSub.meetingPointName ?: summary.meetingPoint.ifBlank { "Punto por convenir" },
                                        subOrderStatus = matchingSub.status,
                                        otherUserAvatarUrl = summary.otherUserAvatarUrl ?: matchingSub.buyerAvatarUrl,
                                        deliveryCode = ""
                                    )
                                }
                            },
                            onClose = null,
                            userAvatarUrl = curProf.avatarUrl,
                            showHeader = false
                        )
                    }
                    SellerTab.PERFIL, SellerTab.MI_PUESTO -> {
                        SellerStoreProfileScreen(
                            profile = profile,
                            sellerProfile = uiState.sellerProfile,
                            warnings = uiState.warnings,
                            availableMeetingPoints = uiState.availableMeetingPoints,
                            availableCategories = uiState.categories,
                            isSaving = uiState.isSavingProfile,
                            isUploading = uiState.isUploadingAsset,
                            onNavigateBack = { viewModel.setSelectedTab(SellerTab.PEDIDOS) },
                            onUploadAsset = { bucket, path, bytes, onUploaded ->
                                viewModel.uploadAsset(bucket, path, bytes, onUploaded)
                            },
                            onSave = { name, status, desc, cat, loc, open, close, banner, avatar, accepting, meetingPoints, paymentMethods ->
                                viewModel.updateBusinessProfile(
                                    businessName = name,
                                    businessStatus = status,
                                    businessDescription = desc,
                                    businessCategory = cat,
                                    businessLocation = loc,
                                    openTime = open,
                                    closeTime = close,
                                    bannerUrl = banner,
                                    avatarUrl = avatar,
                                    acceptingOrders = accepting,
                                    supportedMeetingPoints = meetingPoints,
                                    supportedPaymentMethods = paymentMethods
                                )
                            },
                            onToggleAcceptingOrders = { viewModel.toggleAcceptingOrders(it) },
                            onSignOut = onSignOut,
                            onReportIncident = { key, label, details, evidenceBytes ->
                                viewModel.reportIncident(key, label, details, evidenceBytes = evidenceBytes)
                            },
                            showHeader = false
                        )
                    }
                }
            }
        }

        if (!isAnyModalOpen) {
            SellerBottomNavBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> viewModel.setSelectedTab(tab) },
                pendingOrdersCount = uiState.pendingCount,
                unreadChatCount = uiState.unreadChatCount,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
