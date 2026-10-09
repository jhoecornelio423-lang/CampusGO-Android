package com.example.campusgo.features.tracking

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import com.example.campusgo.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ReportProblem
import kotlinx.coroutines.launch
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.ui.components.IncidentContextType
import com.example.campusgo.ui.components.ReportIncidentDialog
import com.example.campusgo.features.chat.OrderChatBottomSheet
import com.example.campusgo.features.chat.OrderChatViewModel
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.campusgo.features.cart.OrderSummaryReceiptScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.OrderStatus
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.orderCodeDisplay
import com.example.campusgo.domain.model.verificationCode
import com.example.campusgo.domain.repository.AuthRepository
import com.example.campusgo.ui.components.PaymentMethodLogo
import com.example.campusgo.ui.components.PeekRating
import com.example.campusgo.ui.components.RateExperienceBottomSheet
import com.example.campusgo.ui.components.SubOrderCountdownTimerBadge
import com.example.campusgo.ui.components.CampusGoBusinessAvatar
import com.example.campusgo.ui.components.CampusGoDialogContainerColor
import com.example.campusgo.ui.components.CampusGoDialogShape
import com.example.campusgo.ui.components.CampusGoDialogTonalElevation
import com.example.campusgo.ui.components.isSubOrderExpired
import com.example.campusgo.ui.components.campusGoDialogStyle
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    buyerProfile: UserProfile,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToCart: (() -> Unit)? = null,
    onOpenChat: ((Order, SubOrder) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: OrderTrackingViewModel = koinViewModel(),
    orderRepository: OrderRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    // Siempre priorizar y mostrar primero "En curso" al abrir o acceder al apartado de pedidos
    LaunchedEffect(Unit) {
        viewModel.resetToActiveTab()
        pagerState.scrollToPage(0)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetToActiveTab()
        }
    }

    // Sincronizar el swipe del Pager con el ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val targetTab = if (pagerState.currentPage == 0) TrackingTab.EN_CURSO else TrackingTab.HISTORIAL
        if (uiState.selectedTab != targetTab) {
            viewModel.setSelectedTab(targetTab)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    var isChatPickerOpen by remember { mutableStateOf(false) }
    var subOrderToReport by remember { mutableStateOf<SubOrder?>(null) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val ratingPrefs = remember(context, buyerProfile.id) {
        context.getSharedPreferences("campusgo_rating_prefs_${buyerProfile.id}", Context.MODE_PRIVATE)
    }

    // Monitoreo de transición de estados: solo notifica si un subpedido pasa a COMPLETADO en tiempo real
    val previousSubOrderStatuses = remember { mutableMapOf<String, SubOrderStatus>() }
    var isInitialOrdersLoad by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.orders, uiState.reviewedOrders) {
        if (uiState.isLoading) return@LaunchedEffect

        val currentSubs = uiState.orders.flatMap { it.subOrders }

        if (isInitialOrdersLoad) {
            // En la primera carga de la pantalla, guardamos los estados de todos los subpedidos
            // existentes para nunca abrir el popup automáticamente por pedidos antiguos o pasados.
            currentSubs.forEach { sub ->
                previousSubOrderStatuses[sub.id] = sub.status
            }
            if (currentSubs.isNotEmpty()) {
                isInitialOrdersLoad = false
            }
            return@LaunchedEffect
        }

        // Si ya pasó la carga inicial, verificar si algún pedido pasó a COMPLETADO durante esta sesión activa
        if (uiState.subOrderToRate == null) {
            for (sub in currentSubs) {
                val prevStatus = previousSubOrderStatuses[sub.id]
                val isNowCompleted = sub.status == SubOrderStatus.COMPLETADO || sub.status == SubOrderStatus.PAGO_CONFIRMADO
                val wasActive = prevStatus != null &&
                        prevStatus != SubOrderStatus.COMPLETADO &&
                        prevStatus != SubOrderStatus.PAGO_CONFIRMADO &&
                        prevStatus != SubOrderStatus.CANCELADO
                val alreadyPrompted = ratingPrefs.getBoolean("prompted_${sub.id}", false)
                val alreadyReviewed = uiState.reviewedOrders.containsKey("${sub.orderId}-${sub.sellerId}") ||
                        uiState.reviewedOrders.containsKey(sub.id) ||
                        uiState.reviewedOrders.containsKey(sub.orderId)

                if (isNowCompleted && wasActive && !alreadyPrompted && !alreadyReviewed) {
                    ratingPrefs.edit().putBoolean("prompted_${sub.id}", true).apply()
                    viewModel.openRateDialog(sub)
                    break
                }
            }
        }

        // Mantener actualizado el mapa de estados previos
        currentSubs.forEach { sub ->
            previousSubOrderStatuses[sub.id] = sub.status
        }
    }

    val isAnyModalOpen = uiState.orderToCancel != null ||
            uiState.subOrderToRate != null ||
            selectedOrderForDetail != null ||
            isChatPickerOpen ||
            subOrderToReport != null

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "order_tracking_blur"
    )

    LaunchedEffect(buyerProfile.id) {
        viewModel.initialize(buyerProfile.id)
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    // Modal de confirmación para cancelar pedido
    if (uiState.orderToCancel != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelDialog() },
            shape = CampusGoDialogShape,
            containerColor = CampusGoDialogContainerColor,
            tonalElevation = CampusGoDialogTonalElevation,
            modifier = Modifier.campusGoDialogStyle(),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning_custom),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    "¿Cancelar este pedido?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Si cancelas este pedido, se notificará a los puestos y se devolverá el stock reservado inmediatamente a su inventario. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.dismissCancelDialog() },
                        enabled = !uiState.isCancelling,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("Mantener", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            uiState.orderToCancel?.let { viewModel.confirmCancelOrder(it.id) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        enabled = !uiState.isCancelling,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isCancelling) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                        } else {
                            Text("Sí, cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    // Modal de Calificación al Vendedor (Estilo inDrive / Rappi Bottom Sheet)
    if (uiState.subOrderToRate != null) {
        val subOrder = uiState.subOrderToRate!!
        val authRepo: AuthRepository = koinInject()
        var sellerAvatarUrl by remember(subOrder.sellerId) { mutableStateOf<String?>(null) }
        LaunchedEffect(subOrder.sellerId) {
            if (subOrder.sellerId.isNotBlank()) {
                val res = authRepo.getUserProfile(subOrder.sellerId)
                if (res.isSuccess) {
                    sellerAvatarUrl = res.getOrNull()?.avatarUrl
                }
            }
        }

        RateExperienceBottomSheet(
            title = "¡Pedido Entregado! 🎉",
            subtitle = "Cierra tu pedido calificando la atención del puesto",
            targetName = subOrder.sellerName.ifBlank { "Puesto de Campus" },
            targetAvatarUrl = sellerAvatarUrl,
            targetRoleLabel = "Puesto Universitario",
            isStore = true,
            promptText = "¿Cómo estuvo tu experiencia y la entrega del pedido?",
            commentPlaceholder = "¿Qué tal estuvo la atención y la comida? (Opcional)",
            submitButtonText = "Cerrar Pedido y Calificar ⭐",
            isSubmitting = uiState.isSubmittingReview,
            onDismiss = {
                ratingPrefs.edit().putBoolean("prompted_${subOrder.id}", true).apply()
                viewModel.dismissRateDialog()
            },
            onSubmit = { rating, comment ->
                ratingPrefs.edit().putBoolean("prompted_${subOrder.id}", true).apply()
                viewModel.submitReview(
                    buyerId = buyerProfile.id,
                    orderId = subOrder.orderId,
                    sellerId = subOrder.sellerId,
                    rating = rating,
                    comment = comment
                )
            }
        )
    }

    val chatViewModel: OrderChatViewModel = koinViewModel()
    var activeChatSubOrder by remember { mutableStateOf<Pair<Order, SubOrder>?>(null) }

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

    if (selectedOrderForDetail != null) {
        val detailOrder = selectedOrderForDetail!!
        Dialog(
            onDismissRequest = { selectedOrderForDetail = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            OrderSummaryReceiptScreen(
                order = detailOrder,
                reviewedOrders = uiState.reviewedOrders,
                onRateSeller = { subOrder -> viewModel.openRateDialog(subOrder) },
                onNavigateToTracking = null,
                onOpenChat = { subOrder ->
                    selectedOrderForDetail = null
                    if (onOpenChat != null) {
                        onOpenChat(detailOrder, subOrder)
                    } else {
                        activeChatSubOrder = Pair(detailOrder, subOrder)
                        chatViewModel.initChat(
                            subOrderId = subOrder.id,
                            currentUserId = buyerProfile.id,
                            otherUserId = subOrder.sellerId,
                            otherUserName = subOrder.sellerName.ifBlank { "Vendedor" },
                            meetingPoint = subOrder.meetingPointName ?: detailOrder.meetingPointName,
                            subOrderStatus = subOrder.status,
                            deliveryCode = subOrder.verificationCode
                        )
                    }
                },
                onNavigateBack = { selectedOrderForDetail = null }
            )
        }
    }

    if (subOrderToReport != null) {
        val subOrder = subOrderToReport!!
        ReportIncidentDialog(
            title = "Reportar Puesto Comercial",
            subtitle = "Puesto: ${subOrder.sellerName.ifBlank { "Vendedor" }}",
            contextType = IncidentContextType.ORDER,
            isSubmitting = isSubmittingReport,
            onDismiss = { subOrderToReport = null },
            onSubmit = { reasonKey, reasonLabel, details, evidenceBytes ->
                isSubmittingReport = true
                coroutineScope.launch {
                    val result = orderRepository.reportIncident(
                        subOrderId = subOrder.id,
                        reporterId = buyerProfile.id,
                        reportedUserId = subOrder.sellerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel },
                        evidenceBytes = evidenceBytes
                    )
                    isSubmittingReport = false
                    subOrderToReport = null
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("Reporte enviado con éxito al Administrador.")
                    } else {
                        val errMsg = result.exceptionOrNull()?.message ?: "Error al registrar reporte."
                        snackbarHostState.showSnackbar(errMsg)
                    }
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 0.dp
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Mis Pedidos Campus Go",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            navigationIcon = {
                                if (onNavigateBack != null) {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Volver",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                        PrimaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                text = {
                                    Text(
                                        text = "En Curso (${uiState.activeOrders.size})",
                                        fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            Tab(
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                text = {
                                    Text(
                                        text = "Historial (${uiState.pastOrders.size})",
                                        fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            },
            modifier = if (backgroundBlurRadius > 0.dp) Modifier.fillMaxSize().blur(backgroundBlurRadius) else Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val isEnCursoPage = (page == 0)
                        val ordersForPage = if (isEnCursoPage) uiState.activeOrders else uiState.paginatedPastOrders

                        if (ordersForPage.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp)
                                    .padding(bottom = 60.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (isEnCursoPage) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_store_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isEnCursoPage) {
                                        "No tienes pedidos en curso"
                                    } else {
                                        "Sin historial de compras"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isEnCursoPage) {
                                        "Tus pedidos activos aparecerán aquí para que hagas seguimiento a la entrega."
                                    } else {
                                        "Tus pedidos completados o cancelados se guardarán aquí."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(ordersForPage, key = { it.id }) { order ->
                                    BuyerOrderCard(
                                        order = order,
                                        isHistoryTab = !isEnCursoPage,
                                        reviewedOrders = uiState.reviewedOrders,
                                        onCancelOrder = { viewModel.openCancelDialog(order) },
                                        onRepeatOrder = {
                                            viewModel.repeatOrder(order) {
                                                onNavigateToCart?.invoke()
                                            }
                                        },
                                        onExpiredSubOrder = { viewModel.expirePendingOrders() },
                                        onOpenDetail = { selectedOrderForDetail = order },
                                        onRateSeller = { subOrder -> viewModel.openRateDialog(subOrder) },
                                        onOpenChat = { subOrder ->
                                            if (onOpenChat != null) {
                                                onOpenChat(order, subOrder)
                                            } else {
                                                activeChatSubOrder = Pair(order, subOrder)
                                                chatViewModel.initChat(
                                                    subOrderId = subOrder.id,
                                                    currentUserId = buyerProfile.id,
                                                    otherUserId = subOrder.sellerId,
                                                    otherUserName = subOrder.sellerName.ifBlank { "Vendedor" },
                                                    meetingPoint = subOrder.meetingPointName ?: order.meetingPointName,
                                                    subOrderStatus = subOrder.status,
                                                    deliveryCode = subOrder.verificationCode
                                                )
                                            }
                                        },
                                        onChatPickerVisibilityChanged = { isChatPickerOpen = it }
                                    )
                                }

                                if (!isEnCursoPage) {
                                    if (uiState.hasMorePastOrders) {
                                        item(key = "load_more_history_button") {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Mostrando ${uiState.paginatedPastOrders.size} de ${uiState.pastOrders.size} pedidos",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )

                                                    Button(
                                                        onClick = { viewModel.loadMoreHistory() },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(46.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ExpandMore,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Cargar más",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (uiState.pastOrders.size > 20) {
                                        item(key = "all_history_loaded") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Mostrando todos los pedidos del historial (${uiState.pastOrders.size})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay elegante desenfocado / scrim para enfocar la ventana emergente activa
        AnimatedVisibility(
            visible = isAnyModalOpen,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun BuyerOrderCard(
    order: Order,
    isHistoryTab: Boolean = false,
    reviewedOrders: Map<String, Int> = emptyMap(),
    onCancelOrder: (() -> Unit)? = null,
    onRepeatOrder: (() -> Unit)? = null,
    onExpiredSubOrder: (() -> Unit)? = null,
    onOpenDetail: (() -> Unit)? = null,
    onRateSeller: ((SubOrder) -> Unit)? = null,
    onOpenChat: ((SubOrder) -> Unit)? = null,
    onChatPickerVisibilityChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSellerChatPicker by remember { mutableStateOf(false) }

    LaunchedEffect(showSellerChatPicker) {
        onChatPickerVisibilityChanged?.invoke(showSellerChatPicker)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabecera: Título Amigable y Total Coherente
            val isDark = LocalDarkTheme.current
            val originalSum = if (order.subOrders.isNotEmpty()) order.subOrders.sumOf { it.subtotalAmount } else order.totalAmount
            val cancelledSum = order.subOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }.sumOf { it.subtotalAmount }
            val hasCancelledSubOrders = cancelledSum > 0.0
            val effectiveToPay = (originalSum - cancelledSum).coerceAtLeast(0.0)
            val isOrderCancelled = order.status == OrderStatus.CANCELADA || (order.subOrders.isNotEmpty() && order.subOrders.all { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO })

            val orderFriendlyTitle = when (order.status) {
                OrderStatus.COMPLETADA -> "Pedido Entregado"
                OrderStatus.CANCELADA -> "Pedido Cancelado"
                OrderStatus.EN_PROCESO -> "Pedido en Preparación"
                OrderStatus.PARCIALMENTE_ACEPTADA -> "Pedido en Curso"
                OrderStatus.PENDIENTE -> "Pedido Solicitado"
                else -> "Seguimiento de Pedido"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = orderFriendlyTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val effectiveOrderPm = when {
                        order.paymentMethod != null && order.paymentMethod != PaymentMethod.EFECTIVO -> order.paymentMethod
                        order.subOrders.any { it.paymentMethod != null && it.paymentMethod != PaymentMethod.EFECTIVO } ->
                            order.subOrders.firstOrNull { it.paymentMethod != null && it.paymentMethod != PaymentMethod.EFECTIVO }?.paymentMethod
                        else -> order.paymentMethod ?: PaymentMethod.EFECTIVO
                    }
                    val (pmBg, pmColor, pmName) = when (effectiveOrderPm) {
                        PaymentMethod.YAPE -> Triple(if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.25f) else Color(0xFFF3E5F5), if (isDark) Color(0xFFCE93D8) else Color(0xFF6A1B9A), "Yape")
                        PaymentMethod.PLIN -> Triple(if (isDark) Color(0xFF00796B).copy(alpha = 0.25f) else Color(0xFFE0F2F1), if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B), "Plin")
                        PaymentMethod.EFECTIVO -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Efectivo")
                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, effectiveOrderPm?.name ?: "Efectivo")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isOrderCancelled) {
                            Text(
                                text = "Total: S/ %.2f".format(originalSum),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 16.sp
                            )
                        } else if (hasCancelledSubOrders) {
                            Text(
                                text = "Total a pagar: S/ %.2f".format(effectiveToPay),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        } else {
                            Text(
                                text = "Total: S/ %.2f".format(originalSum),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        }
                        Surface(
                            color = pmBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PaymentMethodLogo(method = effectiveOrderPm, size = 13.dp)
                                Text(
                                    text = pmName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = pmColor
                                )
                            }
                        }
                    }
                    if (isOrderCancelled) {
                        Text(
                            text = "Pedido cancelado",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (hasCancelledSubOrders) {
                        Text(
                            text = "Total original: S/ %.2f (S/ %.2f cancelado)".format(originalSum, cancelledSum),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                OrderStatusBadge(status = order.status)
            }

            // Alerta si la orden fue Parcialmente Aceptada por rechazo de algún puesto
            if (order.status == OrderStatus.PARCIALMENTE_ACEPTADA) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Un puesto no pudo atender su parte. El total se recalculó automáticamente y no pagarás por los ítems cancelados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()

            // Desglose de Subpedidos en Vivo
            Text(
                text = "Puestos participantes (${order.subOrders.size}):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            order.subOrders.forEach { subOrder ->
                val rating = reviewedOrders["${subOrder.orderId}-${subOrder.sellerId}"]
                    ?: reviewedOrders[subOrder.id]
                    ?: if (order.subOrders.size == 1) reviewedOrders[order.id] else null
                SubOrderTrackingItem(
                    subOrder = subOrder,
                    orderPaymentMethod = order.paymentMethod,
                    isOrderCompleted = order.status == OrderStatus.COMPLETADA,
                    ratingGiven = rating,
                    onExpired = onExpiredSubOrder,
                    onRate = if (onRateSeller != null) { { onRateSeller(subOrder) } } else null,
                    onOpenChat = if (onOpenChat != null) { { onOpenChat(subOrder) } } else null
                )
            }

            val anyPendingExpired = remember(order.subOrders) {
                val pendings = order.subOrders.filter { it.status == SubOrderStatus.PENDIENTE }
                pendings.isNotEmpty() && pendings.any { isSubOrderExpired(it.createdAt) }
            }

            LaunchedEffect(anyPendingExpired) {
                if (anyPendingExpired) {
                    onExpiredSubOrder?.invoke()
                }
            }

            val activeChatSubOrders = remember(order.subOrders) {
                order.subOrders.filter { !it.status.isFinal }
            }

            // Modal para elegir con qué vendedor chatear si el pedido incluye más de un puesto
            if (showSellerChatPicker) {
                AlertDialog(
                    onDismissRequest = { showSellerChatPicker = false },
                    shape = CampusGoDialogShape,
                    containerColor = CampusGoDialogContainerColor,
                    tonalElevation = CampusGoDialogTonalElevation,
                    modifier = Modifier.campusGoDialogStyle(),
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.extendedColors.successContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = null,
                                tint = MaterialTheme.extendedColors.success,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Contactar al Vendedor",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Selecciona el puesto con el que deseas chatear:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            activeChatSubOrders.forEach { subOrder ->
                                Surface(
                                    onClick = {
                                        showSellerChatPicker = false
                                        onOpenChat?.invoke(subOrder)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = subOrder.sellerName.ifBlank { "Puesto Comercial" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${subOrder.items.size} producto(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_chat_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.extendedColors.success,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSellerChatPicker = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    dismissButton = null
                )
            }

            // Advertencia si la orden expiró automáticamente
            if (anyPendingExpired) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancelado automáticamente por tiempo de espera agotado (15 min).",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── Barra Horizontal Lineal de 3 Botones de Acción (Chat, Cancelar/Repetir, Detalle) ──
            val hasRejectedSubOrder = order.subOrders.any { it.status == SubOrderStatus.RECHAZADO }
            val isOrderPending = order.status == OrderStatus.PENDIENTE
            val canCancel = !isHistoryTab && isOrderPending && !hasRejectedSubOrder
            val canRepeat = order.status == OrderStatus.COMPLETADA && onRepeatOrder != null
            val canChat = onOpenChat != null && !isHistoryTab && order.status != OrderStatus.COMPLETADA && order.status != OrderStatus.CANCELADA && activeChatSubOrders.isNotEmpty()
            val canOpenDetail = onOpenDetail != null

            if (canChat || canCancel || canRepeat || canOpenDetail || !isHistoryTab) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Botón Chat con el Vendedor
                    if (canChat) {
                        OutlinedButton(
                            onClick = {
                                if (activeChatSubOrders.size == 1) {
                                    onOpenChat(activeChatSubOrders.first())
                                } else {
                                    showSellerChatPicker = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.extendedColors.successContainer.copy(alpha = 0.45f),
                                contentColor = MaterialTheme.extendedColors.onSuccessContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.extendedColors.success.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = "Chat",
                                tint = MaterialTheme.extendedColors.success,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Chat",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // 2. Botón Cancelar Pedido (o Repetir Pedido si ya está completado)
                    if (canRepeat) {
                        OutlinedButton(
                            onClick = { onRepeatOrder() },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Repetir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Repetir",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else if (!isHistoryTab && order.status != OrderStatus.COMPLETADA && order.status != OrderStatus.CANCELADA) {
                        OutlinedButton(
                            onClick = {
                                if (canCancel) {
                                    onCancelOrder?.invoke()
                                }
                            },
                            enabled = canCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (canCancel) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = if (canCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.outline
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (canCancel) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = if (canCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cancelar",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Botón Ver Detalle del Pedido
                    if (canOpenDetail) {
                        OutlinedButton(
                            onClick = { onOpenDetail.invoke() },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Detalle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Detalle",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubOrderTrackingItem(
    subOrder: SubOrder,
    orderPaymentMethod: PaymentMethod? = null,
    isOrderCompleted: Boolean = false,
    ratingGiven: Int? = null,
    onExpired: (() -> Unit)? = null,
    onRate: (() -> Unit)? = null,
    onOpenChat: (() -> Unit)? = null
) {
    val isDarkSub = LocalDarkTheme.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subOrder.sellerName.ifEmpty { "Emprendimiento Campus Go" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val subPm = when {
                        subOrder.paymentMethod != null && subOrder.paymentMethod != PaymentMethod.EFECTIVO -> subOrder.paymentMethod
                        orderPaymentMethod != null && orderPaymentMethod != PaymentMethod.EFECTIVO -> orderPaymentMethod
                        subOrder.paymentMethod != null -> subOrder.paymentMethod
                        else -> orderPaymentMethod ?: PaymentMethod.EFECTIVO
                    }
                    val (subPmBg, subPmColor, subPmName) = when (subPm) {
                        PaymentMethod.YAPE -> Triple(if (isDarkSub) Color(0xFF7B1FA2).copy(alpha = 0.25f) else Color(0xFFF3E5F5), if (isDarkSub) Color(0xFFCE93D8) else Color(0xFF6A1B9A), "Yape")
                        PaymentMethod.PLIN -> Triple(if (isDarkSub) Color(0xFF00796B).copy(alpha = 0.25f) else Color(0xFFE0F2F1), if (isDarkSub) Color(0xFF80CBC4) else Color(0xFF00796B), "Plin")
                        PaymentMethod.EFECTIVO -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Efectivo")
                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, subPm.name)
                    }
                    Surface(
                        color = subPmBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PaymentMethodLogo(method = subPm, size = 13.dp)
                            Text(
                                text = subPmName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = subPmColor
                            )
                        }
                    }
                    Text(
                        text = "S/ %.2f".format(subOrder.subtotalAmount),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Countdown Timer si está pendiente de aceptación por el vendedor
            if (subOrder.status == SubOrderStatus.PENDIENTE) {
                SubOrderCountdownTimerBadge(
                    createdAtIso = subOrder.createdAt,
                    status = subOrder.status,
                    onExpired = onExpired
                )
            }

            // Lista compacta de productos
            subOrder.items.forEach { item ->
                Text(
                    text = "• ${item.quantity}x ${item.productName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stepper / Estado en vivo del subpedido
            if (subOrder.status == SubOrderStatus.NO_ENTREGADO) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            text = "No entregado (Inasistencia reportada)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (subOrder.status == SubOrderStatus.RECHAZADO || subOrder.status == SubOrderStatus.CANCELADO) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            text = "No disponible: ${subOrder.rejectionReason ?: "Sin insumos"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                TrackingStepper(status = subOrder.status)
            }

            // Código de Seguridad PIN de Entrega para el Comprador
            if (subOrder.status == SubOrderStatus.ACEPTADO ||
                subOrder.status == SubOrderStatus.EN_PREPARACION ||
                subOrder.status == SubOrderStatus.LISTO ||
                subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA) {
                val isReady = subOrder.status == SubOrderStatus.LISTO || subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA
                Surface(
                    color = if (isReady) MaterialTheme.extendedColors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isReady) MaterialTheme.extendedColors.success.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isReady) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "CÓDIGO DE ENTREGA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isReady) MaterialTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Dile este PIN al vendedor al retirar",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "#${subOrder.verificationCode}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = if (isReady) MaterialTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }


            // Calificación al Vendedor
            val canRate = subOrder.status == SubOrderStatus.COMPLETADO ||
                          subOrder.status == SubOrderStatus.PAGO_CONFIRMADO ||
                          isOrderCompleted
            if (canRate) {
                Spacer(modifier = Modifier.height(2.dp))
                if (ratingGiven != null && ratingGiven > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tu calificación:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (star in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (star <= ratingGiven) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (onRate != null) {
                    OutlinedButton(
                        onClick = onRate,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agregar Calificación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingStepper(status: SubOrderStatus) {
    val step1Done = true // Solicitado
    val step2Done = status in listOf(SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION, SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA, SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO)
    val step3Done = status in listOf(SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA, SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO)
    val step4Done = status in listOf(SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepCircle(label = "Enviado", isDone = step1Done, isActive = status == SubOrderStatus.PENDIENTE)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step2Done) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.outlineVariant)
        StepCircle(label = "Preparando", isDone = step2Done, isActive = status == SubOrderStatus.ACEPTADO || status == SubOrderStatus.EN_PREPARACION)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step3Done) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.outlineVariant)
        StepCircle(label = "Listo", isDone = step3Done, isActive = status == SubOrderStatus.LISTO || status == SubOrderStatus.ESPERANDO_ENTREGA)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step4Done) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.outlineVariant)
        StepCircle(label = "Entregado", isDone = step4Done, isActive = step4Done)
    }
}

@Composable
fun StepCircle(label: String, isDone: Boolean, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> MaterialTheme.extendedColors.success
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive || isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bgColor, textColor, label) = when (status) {
        OrderStatus.PENDIENTE -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Pendiente")
        OrderStatus.EN_PROCESO -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "En Proceso")
        OrderStatus.PARCIALMENTE_ACEPTADA -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Parcialmente Aceptada")
        OrderStatus.COMPLETADA -> Triple(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer, "Completada")
        OrderStatus.CANCELADA -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Cancelada")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}


@Composable
fun SubOrderStatusBadge(status: SubOrderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        SubOrderStatus.PENDIENTE -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Pendiente")
        SubOrderStatus.ACEPTADO -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Aceptado")
        SubOrderStatus.EN_PREPARACION -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "En Preparación")
        SubOrderStatus.LISTO -> Triple(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer, "Listo")
        SubOrderStatus.ESPERANDO_ENTREGA -> Triple(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer, "Esperando")
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> Triple(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer, "Completado")
        SubOrderStatus.RECHAZADO -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Rechazado")
        SubOrderStatus.CANCELADO -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Cancelado")
        SubOrderStatus.NO_ENTREGADO -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "No entregado")
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
