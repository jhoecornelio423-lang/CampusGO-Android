package com.example.campusgo.features.tracking

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
import com.example.campusgo.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    buyerProfile: UserProfile,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToCart: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: OrderTrackingViewModel = koinViewModel(),
    orderRepository: OrderRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    var isChatPickerOpen by remember { mutableStateOf(false) }
    var subOrderToReport by remember { mutableStateOf<SubOrder?>(null) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    // Auto prompt de calificación inDrive al completarse la compra
    var promptedSubOrderIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    LaunchedEffect(uiState.orders, uiState.reviewedOrders) {
        if (uiState.subOrderToRate == null && !uiState.isLoading) {
            val unratedCompletedSub = uiState.orders
                .flatMap { it.subOrders }
                .firstOrNull { sub ->
                    (sub.status == SubOrderStatus.COMPLETADO || sub.status == SubOrderStatus.PAGO_CONFIRMADO) &&
                    !promptedSubOrderIds.contains(sub.id) &&
                    !uiState.reviewedOrders.containsKey("${sub.orderId}-${sub.sellerId}") &&
                    !uiState.reviewedOrders.containsKey(sub.id) &&
                    !uiState.reviewedOrders.containsKey(sub.orderId)
                }
            if (unratedCompletedSub != null) {
                promptedSubOrderIds = promptedSubOrderIds + unratedCompletedSub.id
                viewModel.openRateDialog(unratedCompletedSub)
            }
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
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning_custom),
                        contentDescription = null,
                        tint = Color(0xFFC8102E),
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    "¿Cancelar este pedido?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Si cancelas este pedido, se notificará a los puestos y se devolverá el stock reservado inmediatamente a su inventario. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Mantener", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            uiState.orderToCancel?.let { viewModel.confirmCancelOrder(it.id) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E)),
                        enabled = !uiState.isCancelling,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isCancelling) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
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
            onDismiss = { viewModel.dismissRateDialog() },
            onSubmit = { rating, comment ->
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
        BuyerOrderDetailDialog(
            order = detailOrder,
            reviewedOrders = uiState.reviewedOrders,
            onRateSeller = { subOrder -> viewModel.openRateDialog(subOrder) },
            onReportSubOrder = { subOrder ->
                selectedOrderForDetail = null
                subOrderToReport = subOrder
            },
            onOpenChat = { subOrder ->
                selectedOrderForDetail = null
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
            },
            onDismiss = { selectedOrderForDetail = null }
        )
    }

    if (subOrderToReport != null) {
        val subOrder = subOrderToReport!!
        ReportIncidentDialog(
            title = "Reportar Puesto Comercial",
            subtitle = "Puesto: ${subOrder.sellerName.ifBlank { "Vendedor" }}",
            contextType = IncidentContextType.ORDER,
            isSubmitting = isSubmittingReport,
            onDismiss = { subOrderToReport = null },
            onSubmit = { reasonKey, reasonLabel, details ->
                isSubmittingReport = true
                coroutineScope.launch {
                    val result = orderRepository.reportIncident(
                        subOrderId = subOrder.id,
                        reporterId = buyerProfile.id,
                        reportedUserId = subOrder.sellerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel }
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
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Mis Pedidos Campus Go",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003366)
                                )
                            },
                            navigationIcon = {
                                if (onNavigateBack != null) {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Volver"
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                        PrimaryTabRow(
                            selectedTabIndex = if (uiState.selectedTab == TrackingTab.EN_CURSO) 0 else 1,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF003366)
                        ) {
                        Tab(
                            selected = uiState.selectedTab == TrackingTab.EN_CURSO,
                            onClick = { viewModel.setSelectedTab(TrackingTab.EN_CURSO) },
                            text = {
                                Text(
                                    text = "En Curso (${uiState.activeOrders.size})",
                                    fontWeight = if (uiState.selectedTab == TrackingTab.EN_CURSO) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == TrackingTab.HISTORIAL,
                            onClick = { viewModel.setSelectedTab(TrackingTab.HISTORIAL) },
                            text = {
                                Text(
                                    text = "Historial (${uiState.pastOrders.size})",
                                    fontWeight = if (uiState.selectedTab == TrackingTab.HISTORIAL) FontWeight.Bold else FontWeight.Medium
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
                        color = Color(0xFF003366)
                    )
                } else {
                    val currentOrders = if (uiState.selectedTab == TrackingTab.EN_CURSO) {
                        uiState.activeOrders
                    } else {
                        uiState.pastOrders
                    }

                    if (currentOrders.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .padding(bottom = 60.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (uiState.selectedTab == TrackingTab.EN_CURSO) {
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
                                text = if (uiState.selectedTab == TrackingTab.EN_CURSO) {
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
                                text = if (uiState.selectedTab == TrackingTab.EN_CURSO) {
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
                            items(currentOrders, key = { it.id }) { order ->
                                BuyerOrderCard(
                                    order = order,
                                    isHistoryTab = uiState.selectedTab == TrackingTab.HISTORIAL,
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
                                    },
                                    onChatPickerVisibilityChanged = { isChatPickerOpen = it }
                                )
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
                    .background(Color(0xFF0F172A).copy(alpha = 0.35f))
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
            val originalSum = if (order.subOrders.isNotEmpty()) order.subOrders.sumOf { it.subtotalAmount } else order.totalAmount
            val cancelledSum = order.subOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }.sumOf { it.subtotalAmount }
            val hasCancelledSubOrders = cancelledSum > 0.0
            val effectiveToPay = (originalSum - cancelledSum).coerceAtLeast(0.0)

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
                    if (hasCancelledSubOrders) {
                        Text(
                            text = "Total a pagar: S/ %.2f".format(effectiveToPay),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366),
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Total original: S/ %.2f (S/ %.2f cancelado)".format(originalSum, cancelledSum),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE65100)
                        )
                    } else {
                        Text(
                            text = "Total: S/ %.2f".format(originalSum),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366),
                            fontSize = 18.sp
                        )
                    }
                }
                OrderStatusBadge(status = order.status)
            }

            // Alerta si la orden fue Parcialmente Aceptada por rechazo de algún puesto
            if (order.status == OrderStatus.PARCIALMENTE_ACEPTADA) {
                Surface(
                    color = Color(0xFFFFF3E0),
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
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Un puesto no pudo atender su parte. El total se recalculó automáticamente y no pagarás por los ítems cancelados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
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
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Contactar al Vendedor",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Selecciona el puesto con el que deseas chatear:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            activeChatSubOrders.forEach { subOrder ->
                                Surface(
                                    onClick = {
                                        showSellerChatPicker = false
                                        onOpenChat?.invoke(subOrder)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                                color = Color(0xFF003366)
                                            )
                                            Text(
                                                text = "${subOrder.items.size} producto(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_chat_custom),
                                            contentDescription = null,
                                            tint = Color(0xFF00A884),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = null
                )
            }

            // Advertencia si la orden expiró automáticamente
            if (anyPendingExpired) {
                Surface(
                    color = Color(0xFFFFEBEE),
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
                            tint = Color(0xFFC8102E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancelado automáticamente por tiempo de espera agotado (15 min).",
                            color = Color(0xFFC8102E),
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
                                containerColor = Color(0xFFE8F5E9).copy(alpha = 0.45f),
                                contentColor = Color(0xFF00796B)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF00A884).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = "Chat",
                                tint = Color(0xFF00A884),
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
                                containerColor = Color(0xFFEFF6FF).copy(alpha = 0.45f),
                                contentColor = Color(0xFF003366)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF003366).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Repetir",
                                tint = Color(0xFF003366),
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
                                containerColor = if (canCancel) Color(0xFFFFEBEE).copy(alpha = 0.45f) else Color(0xFFF1F5F9).copy(alpha = 0.4f),
                                contentColor = if (canCancel) Color(0xFFC8102E) else Color(0xFF94A3B8),
                                disabledContainerColor = Color(0xFFF1F5F9).copy(alpha = 0.4f),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (canCancel) Color(0xFFC8102E).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = if (canCancel) Color(0xFFC8102E) else Color(0xFF94A3B8),
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
                                containerColor = Color(0xFFF1F5F9).copy(alpha = 0.5f),
                                contentColor = Color(0xFF003366)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF003366).copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Detalle",
                                tint = Color(0xFF003366),
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
    isOrderCompleted: Boolean = false,
    ratingGiven: Int? = null,
    onExpired: (() -> Unit)? = null,
    onRate: (() -> Unit)? = null,
    onOpenChat: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
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
                    val subPm = subOrder.paymentMethod ?: PaymentMethod.EFECTIVO
                    val (subPmBg, subPmColor, subPmName) = when (subPm) {
                        PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                        PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                        PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                        else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), subPm.name)
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
                        color = Color(0xFF003366)
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
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC8102E), modifier = Modifier.size(16.dp))
                        Text(
                            text = "No entregado (Inasistencia reportada)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (subOrder.status == SubOrderStatus.RECHAZADO || subOrder.status == SubOrderStatus.CANCELADO) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC8102E), modifier = Modifier.size(16.dp))
                        Text(
                            text = "No disponible: ${subOrder.rejectionReason ?: "Sin insumos"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
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
                    color = if (isReady) Color(0xFFE0F2F1) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isReady) Color(0xFF80CBC4) else Color(0xFFCBD5E1)),
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
                                tint = if (isReady) Color(0xFF00796B) else Color(0xFF003366),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "CÓDIGO DE ENTREGA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isReady) Color(0xFF004D40) else Color(0xFF003366)
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
                            color = if (isReady) Color(0xFF004D40) else Color(0xFF003366)
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
                        color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
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
                                color = Color(0xFF92400E)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (star in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (star <= ratingGiven) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (onRate != null) {
                    OutlinedButton(
                        onClick = onRate,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agregar Calificación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFD97706)
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
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step2Done) Color(0xFF2E7D32) else Color.LightGray)
        StepCircle(label = "Preparando", isDone = step2Done, isActive = status == SubOrderStatus.ACEPTADO || status == SubOrderStatus.EN_PREPARACION)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step3Done) Color(0xFF2E7D32) else Color.LightGray)
        StepCircle(label = "Listo", isDone = step3Done, isActive = status == SubOrderStatus.LISTO || status == SubOrderStatus.ESPERANDO_ENTREGA)
        HorizontalDivider(modifier = Modifier.weight(1f), color = if (step4Done) Color(0xFF2E7D32) else Color.LightGray)
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
                        isDone -> Color(0xFF2E7D32)
                        isActive -> Color(0xFF003366)
                        else -> Color(0xFFE2E8F0)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive || isDone) Color(0xFF003366) else Color.Gray
        )
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bgColor, textColor, label) = when (status) {
        OrderStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        OrderStatus.EN_PROCESO -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "En Proceso")
        OrderStatus.PARCIALMENTE_ACEPTADA -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Parcialmente Aceptada")
        OrderStatus.COMPLETADA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Completada")
        OrderStatus.CANCELADA -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Cancelada")
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
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrderDetailDialog(
    order: Order,
    reviewedOrders: Map<String, Int> = emptyMap(),
    onRateSeller: ((SubOrder) -> Unit)? = null,
    onReportSubOrder: ((SubOrder) -> Unit)? = null,
    onOpenChat: ((SubOrder) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.5.dp)
                        .background(Color(0xFFCBD5E1), CircleShape)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding()
        ) {
            // Cabecera superior moderna tipo Rappi / iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE6F7F3),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Detalle del Pedido",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color(0xFF16324F)
                        )
                        Text(
                            text = order.meetingPointName.ifBlank { "Entrega en campus" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrderStatusBadge(status = order.status)

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Contenido con scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Tarjeta: Punto de Entrega Acordado
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE6F7F3),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_location_custom),
                                        contentDescription = null,
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "PUNTO DE ENTREGA ACORDADO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = order.meetingPointName.ifBlank { "Campus Universitario" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F),
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_alarm_custom),
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Hora acordada: ${order.scheduledTime.ifBlank { "Lo antes posible" }}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                // 2. Sección: Puestos participantes
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PUESTOS PARTICIPANTES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${order.subOrders.size} puesto${if (order.subOrders.size > 1) "s" else ""}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }

                order.subOrders.forEach { subOrder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Store,
                                                contentDescription = null,
                                                tint = Color(0xFF00A884),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = subOrder.sellerName.ifBlank { "Puesto Comercial" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF16324F),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val subMeetingPoint = subOrder.meetingPointName ?: order.meetingPointName
                                        if (!subMeetingPoint.isNullOrBlank()) {
                                            Text(
                                                text = "📍 $subMeetingPoint",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                SubOrderStatusBadge(status = subOrder.status)
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Lista de productos de este puesto
                            subOrder.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${item.quantity}x",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF16324F)
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.productName,
                                            fontSize = 13.sp,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                    Text(
                                        text = "S/ %.2f".format(item.subtotal),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16324F)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                subOrder.paymentMethod?.let { spm ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        PaymentMethodLogo(method = spm, size = 13.dp)
                                        Text(
                                            text = when (spm) {
                                                PaymentMethod.YAPE -> "YAPE"
                                                PaymentMethod.PLIN -> "PLIN"
                                                PaymentMethod.EFECTIVO -> "EFECTIVO"
                                                else -> spm.name
                                            },
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                                Text(
                                    text = "Subtotal: S/ %.2f".format(subOrder.subtotalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF00A884)
                                )
                            }

                            // PIN de verificación
                            if (subOrder.status == SubOrderStatus.ACEPTADO ||
                                subOrder.status == SubOrderStatus.EN_PREPARACION ||
                                subOrder.status == SubOrderStatus.LISTO ||
                                subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA) {
                                val isReady = subOrder.status == SubOrderStatus.LISTO || subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA
                                Surface(
                                    color = if (isReady) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isReady) Color(0xFF86EFAC) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                tint = if (isReady) Color(0xFF16A34A) else Color(0xFF0284C7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "CÓDIGO PIN DE ENTREGA",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReady) Color(0xFF166534) else Color(0xFF0369A1)
                                                )
                                                Text(
                                                    text = "Muestra al vendedor",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "#${subOrder.verificationCode}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.5.sp,
                                            color = if (isReady) Color(0xFF15803D) else Color(0xFF0284C7)
                                        )
                                    }
                                }
                            }

                            // Calificación
                            val canRateSub = subOrder.status == SubOrderStatus.COMPLETADO ||
                                              subOrder.status == SubOrderStatus.PAGO_CONFIRMADO ||
                                              order.status == OrderStatus.COMPLETADA
                            if (canRateSub) {
                                val rating = reviewedOrders["${subOrder.orderId}-${subOrder.sellerId}"]
                                    ?: reviewedOrders[subOrder.id]
                                    ?: if (order.subOrders.size == 1) reviewedOrders[order.id] else null
                                if (rating != null && rating > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tu calificación:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            for (star in 1..5) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (star <= rating) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (onRateSeller != null) {
                                    OutlinedButton(
                                        onClick = { onRateSeller(subOrder) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .height(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Calificar Puesto",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                            }

                            // Botón de Chat (Solo visible durante la coordinación activa del pedido)
                            val canChatSub = onOpenChat != null && !subOrder.status.isFinal && order.status != OrderStatus.COMPLETADA && order.status != OrderStatus.CANCELADA
                            if (canChatSub) {
                                Button(
                                    onClick = { onOpenChat(subOrder) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE6F7F3),
                                        contentColor = Color(0xFF00A884)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .height(38.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chat_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chat con Vendedor",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }

                            // Botón Reportar
                            if (onReportSubOrder != null) {
                                TextButton(
                                    onClick = { onReportSubOrder(subOrder) },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                                        contentDescription = "Reportar",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reportar problema",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Tarjeta: Total y Resumen de Pago
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val uniquePaymentMethods = order.subOrders.mapNotNull { it.paymentMethod }.distinct()
                        val isSplitPayment = uniquePaymentMethods.size > 1
                        val pm = order.paymentMethod ?: order.subOrders.firstOrNull()?.paymentMethod ?: PaymentMethod.EFECTIVO
                        val (pmBg, pmColor, pmName) = if (isSplitPayment) {
                            Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), "Pagos por puesto (${uniquePaymentMethods.size})")
                        } else {
                            when (pm) {
                                PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                                PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                                PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF16324F), "Efectivo")
                                else -> Triple(Color(0xFFF1F5F9), Color(0xFF16324F), pm.name)
                            }
                        }

                        Surface(
                            color = pmBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (!isSplitPayment) {
                                    PaymentMethodLogo(method = pm, size = 14.dp)
                                }
                                Text(
                                    text = if (isSplitPayment) pmName else "Pago: $pmName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pmColor
                                )
                            }
                        }

                        val originalSum = if (order.subOrders.isNotEmpty()) order.subOrders.sumOf { it.subtotalAmount } else order.totalAmount
                        val cancelledSum = order.subOrders.filter { it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO }.sumOf { it.subtotalAmount }
                        val hasCancelledSubOrders = cancelledSum > 0.0
                        val effectiveToPay = (originalSum - cancelledSum).coerceAtLeast(0.0)

                        if (hasCancelledSubOrders) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Subtotal original (${order.subOrders.size} puestos): S/ %.2f".format(originalSum),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Puesto cancelado: -S/ %.2f".format(cancelledSum),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Total a pagar:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "S/ %.2f".format(effectiveToPay),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF00A884)
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Total:",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "S/ %.2f".format(originalSum),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF00A884)
                                )
                            }
                        }
                    }
                }

                // Botón Cerrar inferior
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16324F)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun SubOrderStatusBadge(status: SubOrderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        SubOrderStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        SubOrderStatus.ACEPTADO -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Aceptado")
        SubOrderStatus.EN_PREPARACION -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "En Preparación")
        SubOrderStatus.LISTO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Listo")
        SubOrderStatus.ESPERANDO_ENTREGA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Esperando")
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), "Completado")
        SubOrderStatus.RECHAZADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Rechazado")
        SubOrderStatus.CANCELADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Cancelado")
        SubOrderStatus.NO_ENTREGADO -> Triple(Color(0xFFECEFF1), Color(0xFF455A64), "No entregado")
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
            color = textColor
        )
    }
}