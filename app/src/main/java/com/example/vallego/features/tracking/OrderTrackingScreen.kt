package com.example.vallego.features.tracking

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
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
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.ui.components.IncidentContextType
import com.example.vallego.ui.components.ReportIncidentDialog
import com.example.vallego.features.chat.OrderChatBottomSheet
import com.example.vallego.features.chat.OrderChatViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.Order
import com.example.vallego.domain.model.OrderStatus
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.verificationCode
import com.example.vallego.domain.repository.AuthRepository
import com.example.vallego.ui.components.PaymentMethodLogo
import com.example.vallego.ui.components.PeekRating
import com.example.vallego.ui.components.SubOrderCountdownTimerBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoDialogContainerColor
import com.example.vallego.ui.components.ValleGoDialogShape
import com.example.vallego.ui.components.ValleGoDialogTonalElevation
import com.example.vallego.ui.components.isSubOrderExpired
import com.example.vallego.ui.components.valleGoDialogStyle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    buyerProfile: UserProfile,
    onNavigateBack: () -> Unit,
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
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
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

    // Modal de Calificación al Vendedor
    if (uiState.subOrderToRate != null) {
        val subOrder = uiState.subOrderToRate!!
        RateSellerDialog(
            sellerName = subOrder.sellerName,
            sellerId = subOrder.sellerId,
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
                    subOrderStatus = subOrder.status
                )
            },
            onDismiss = { selectedOrderForDetail = null }
        )
    }

    if (subOrderToReport != null) {
        val subOrder = subOrderToReport!!
        ReportIncidentDialog(
            title = "Reportar Puesto Comercial",
            subtitle = "Puesto: ${subOrder.sellerName.ifBlank { "Vendedor" }} • Subpedido #${subOrder.id.take(8)}",
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
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Mis Pedidos Campus-Go",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                        }
                    )
                    PrimaryTabRow(
                        selectedTabIndex = if (uiState.selectedTab == TrackingTab.EN_CURSO) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surface,
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
                                .padding(32.dp),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
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
                                            subOrderStatus = subOrder.status
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
            // Cabecera: ID y Estado General
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (order.status == OrderStatus.COMPLETADA) "Pedido Entregado" else "Pedido #${order.id.takeLast(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Total: S/ %.2f".format(order.totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF003366),
                        fontSize = 18.sp
                    )
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

            // Modal para elegir con qué vendedor chatear si el pedido incluye más de un puesto
            if (showSellerChatPicker) {
                AlertDialog(
                    onDismissRequest = { showSellerChatPicker = false },
                    shape = ValleGoDialogShape,
                    containerColor = ValleGoDialogContainerColor,
                    tonalElevation = ValleGoDialogTonalElevation,
                    modifier = Modifier.valleGoDialogStyle(),
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
                            order.subOrders.forEach { subOrder ->
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
            val canChat = onOpenChat != null && order.subOrders.isNotEmpty()
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
                                if (order.subOrders.size == 1) {
                                    onOpenChat(order.subOrders.first())
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
                    text = subOrder.sellerName.ifEmpty { "Emprendimiento Valle-Go" },
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
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ValleGoDialogShape,
        containerColor = ValleGoDialogContainerColor,
        tonalElevation = ValleGoDialogTonalElevation,
        modifier = Modifier.valleGoDialogStyle(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalle del Pedido",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366),
                    style = MaterialTheme.typography.titleLarge
                )
                OrderStatusBadge(status = order.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tarjeta Destacada de Punto de Entrega (Campus-Go)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location_custom),
                                contentDescription = null,
                                tint = Color(0xFFC8102E),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Punto de Entrega Acordado",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF003366)
                            )
                        }
                        Text(
                            text = order.meetingPointName.ifBlank { "Campus Universitario" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alarm_custom),
                                contentDescription = null,
                                tint = Color(0xFF003366),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Hora acordada: ${order.scheduledTime.ifBlank { "Lo antes posible" }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Desglose de Puestos y Productos
                Text(
                    text = "Puestos participantes (${order.subOrders.size}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF003366)
                )

                order.subOrders.forEach { subOrder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subOrder.sellerName.ifBlank { "Puesto Comercial" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF003366)
                                )
                                SubOrderStatusBadge(status = subOrder.status)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            subOrder.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity}x ${item.productName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "S/ %.2f".format(item.subtotal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Subtotal: S/ %.2f".format(subOrder.subtotalAmount),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF003366)
                                )
                            }

                            if (subOrder.status == SubOrderStatus.ACEPTADO ||
                                subOrder.status == SubOrderStatus.EN_PREPARACION ||
                                subOrder.status == SubOrderStatus.LISTO ||
                                subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA) {
                                val isReady = subOrder.status == SubOrderStatus.LISTO || subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA
                                Surface(
                                    color = if (isReady) Color(0xFFE0F2F1) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (isReady) Color(0xFF80CBC4) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                                tint = if (isReady) Color(0xFF00796B) else Color(0xFF003366),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Código PIN de Entrega:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReady) Color(0xFF004D40) else Color(0xFF003366)
                                            )
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
                                            style = MaterialTheme.typography.bodySmall,
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
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
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

                            if (onOpenChat != null) {
                                val isFinalSub = subOrder.status.isFinal || order.status == OrderStatus.COMPLETADA
                                OutlinedButton(
                                    onClick = { onOpenChat(subOrder) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isFinalSub) Color(0xFF64748B) else Color(0xFF00A884)
                                    ),
                                    border = BorderStroke(1.dp, if (isFinalSub) Color(0xFFCBD5E1) else Color(0xFF00A884)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chat_custom),
                                        contentDescription = null,
                                        tint = if (isFinalSub) Color(0xFF64748B) else Color(0xFF00A884),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFinalSub) "Chat con vendedor (Cerrado)" else "Chat con vendedor",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isFinalSub) Color(0xFF64748B) else Color(0xFF00A884)
                                    )
                                }
                            }

                            if (onReportSubOrder != null) {
                                TextButton(
                                    onClick = { onReportSubOrder(subOrder) },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "Reportar",
                                        tint = Color(0xFFC8102E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reportar problema con este puesto",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFC8102E)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Resumen de Pago
                val pm = order.paymentMethod ?: order.subOrders.firstOrNull()?.paymentMethod ?: PaymentMethod.EFECTIVO
                val (pmBg, pmColor, pmName) = when (pm) {
                    PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                    PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                    PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), pm.name)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = pmBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PaymentMethodLogo(method = pm, size = 14.dp)
                            Text(
                                text = "Pago: $pmName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = pmColor
                            )
                        }
                    }
                    Text(
                        text = "Total: S/ %.2f".format(order.totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF003366)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
            ) {
                Text("Cerrar")
            }
        }
    )
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

@Composable
fun RateSellerDialog(
    sellerName: String,
    sellerId: String = "",
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String?) -> Unit
) {
    val authRepository: AuthRepository = koinInject()
    var sellerAvatarUrl by remember(sellerId) { mutableStateOf<String?>(null) }

    LaunchedEffect(sellerId) {
        if (sellerId.isNotBlank()) {
            val result = authRepository.getUserProfile(sellerId)
            if (result.isSuccess) {
                sellerAvatarUrl = result.getOrNull()?.avatarUrl
            }
        }
    }

    var selectedStars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = ValleGoDialogShape,
        containerColor = ValleGoDialogContainerColor,
        tonalElevation = ValleGoDialogTonalElevation,
        modifier = Modifier.valleGoDialogStyle(),
        icon = {
            ValleGoBusinessAvatar(
                avatarUrl = sellerAvatarUrl,
                storeName = sellerName,
                size = 64.dp,
                shape = CircleShape
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Calificar Vendedor",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF003366)
                )
                if (sellerName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sellerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Desliza o toca para calificar tu experiencia:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                // Fila interactiva PeekRating con animación lift, magnify, popScale y tooltip
                PeekRating(
                    value = selectedStars,
                    onChange = { selectedStars = it },
                    size = 38.dp,
                    lift = 8.dp,
                    magnify = 1.22f,
                    popScale = 1.35f,
                    activeColor = Color(0xFFF59E0B),
                    idleColor = Color(0xFFCBD5E1),
                    tipColor = Color(0xFF1E293B),
                    tipTextColor = Color.White,
                    enabled = !isSubmitting
                )

                // Campo opcional de reseña
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario (opcional)") },
                    placeholder = { Text("¿Qué tal estuvo la atención y la entrega?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
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
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF64748B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = { onSubmit(selectedStars, comment.takeIf { it.isNotBlank() }) },
                    enabled = selectedStars in 1..5 && !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF003366),
                        disabledContainerColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Enviar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        dismissButton = null
    )
}