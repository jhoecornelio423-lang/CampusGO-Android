package com.example.campusgo.features.seller

import androidx.activity.result.PickVisualMediaRequest
import com.example.campusgo.ui.components.StrikeBadge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.campusgo.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import com.example.campusgo.features.chat.ActiveChatSummary
import com.example.campusgo.features.chat.ActiveChatsSheet
import com.example.campusgo.features.chat.OrderChatBottomSheet
import com.example.campusgo.features.chat.OrderChatViewModel
import com.example.campusgo.features.seller.components.SellerBottomNavBar
import com.example.campusgo.features.seller.components.SellerNotificationsBottomSheet
import com.example.campusgo.features.seller.components.SellerDeliveryConfirmationBottomSheet
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.example.campusgo.ui.components.compressImageUri
import com.example.campusgo.ui.components.isSubOrderExpired
import com.example.campusgo.ui.components.PaymentMethodLogo
import com.example.campusgo.ui.components.PaymentMethodLogoByName
import com.example.campusgo.ui.components.formatOrderTime
import com.example.campusgo.theme.LocalDarkTheme
import java.util.UUID
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.domain.model.Product
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.verificationCode
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.OfficialWarningBanner
import com.example.campusgo.ui.components.StoreStatusBadge
import com.example.campusgo.ui.components.SubOrderCountdownTimerBadge
import com.example.campusgo.ui.components.CampusGoBusinessAvatar
import com.example.campusgo.ui.components.CampusGoBusinessBanner
import com.example.campusgo.ui.components.CampusGoProductImage
import com.example.campusgo.ui.components.CampusGoUserAvatar
import com.example.campusgo.domain.repository.ChatRepository
import com.example.campusgo.ui.components.CampusGoDialogContainerColor
import com.example.campusgo.ui.components.CampusGoDialogShape
import com.example.campusgo.ui.components.CampusGoDialogTonalElevation
import com.example.campusgo.ui.components.campusGoDialogStyle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.domain.repository.SupportRepository
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.features.chat.SupportChatBottomSheet
import com.example.campusgo.ui.components.ActiveSupportTicketBanner
import kotlinx.coroutines.launch
import com.example.campusgo.theme.LocalDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    pendingSubOrderId: String? = null,
    onClearPendingSubOrder: () -> Unit = {},
    pendingRoute: com.example.campusgo.core.notification.AppNotificationPayload? = null,
    onClearPendingRoute: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SellerDashboardViewModel = koinViewModel(),
    orderRepository: OrderRepository = koinInject(),
    supportRepository: SupportRepository = koinInject()
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
    var lastReadWarningCount by remember { mutableStateOf(-1) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val chatRepository: ChatRepository = koinInject()
    val unreadChatCount by remember(curProf.id) {
        chatRepository.observeUnreadCount(curProf.id)
    }.collectAsState(initial = 0)
    val chatViewModel: OrderChatViewModel = koinViewModel()
    var activeChatSubOrder by remember { mutableStateOf<SubOrder?>(null) }
    var sellerActiveTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var activeSupportTicket by remember { mutableStateOf<SupportTicket?>(null) }

    LaunchedEffect(curProf.id) {
        while (true) {
            val res = supportRepository.getActiveTicketForUser(curProf.id)
            sellerActiveTicket = res.getOrNull()
            kotlinx.coroutines.delay(4000)
        }
    }

    LaunchedEffect(pendingRoute, pendingSubOrderId, uiState.subOrders) {
        val effectiveSubOrderId = when (val route = pendingRoute) {
            is com.example.campusgo.core.notification.AppNotificationPayload.OrderChat -> route.subOrderId
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
                    deliveryCode = matching.verificationCode
                )
                onClearPendingSubOrder()
                onClearPendingRoute()
            } else {
                coroutineScope.launch {
                    val fetched = orderRepository.getSubOrderById(subId).getOrNull()
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
                    val ticketRes = supportRepository.getTicketById(route.ticketId)
                    val ticket = ticketRes.getOrNull() ?: supportRepository.getActiveTicketForUser(curProf.id).getOrNull()
                    if (ticket != null) {
                        activeSupportTicket = ticket
                        onClearPendingRoute()
                    }
                }
            }
            is com.example.campusgo.core.notification.AppNotificationPayload.Warning -> {
                showNotificationsSheet = true
                onClearPendingRoute()
            }
            is com.example.campusgo.core.notification.AppNotificationPayload.OrderTracking -> {
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

    // Navegación nativa de retroceso para chat y pestañas del vendedor
    BackHandler(enabled = activeChatSubOrder != null) {
        activeChatSubOrder = null
        chatViewModel.clearChat()
    }

    // Regresar a la pestaña principal de Pedidos antes de salir de la app
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
                coroutineScope.launch {
                    val res = supportRepository.getActiveTicketForUser(curProf.id)
                    sellerActiveTicket = res.getOrNull()
                }
            }
        )
        return
    }

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(280),
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
        val isPending = subOrder.status == SubOrderStatus.PENDIENTE
        val defaultReason = if (isPending) "Sin insumos / agotado" else "Comprador no se presentó al punto de encuentro"
        var selectedReason by remember { mutableStateOf(defaultReason) }
        var customReason by remember { mutableStateOf("") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val commonReasons = if (isPending) {
            listOf(
                "Sin insumos / agotado",
                "Puesto cerrado por clase / horario",
                "Tiempo de espera muy alto",
                "Otro motivo"
            )
        } else {
            listOf(
                "Comprador no se presentó al punto de encuentro",
                "Insumos agotados / problema con el producto",
                "Imprevisto en el punto de encuentro",
                "Puesto cerrado por emergencia",
                "Demora excesiva / tiempo insuficiente",
                "Otro motivo"
            )
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissRejectionDialog() },
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
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Cabecera superior moderna fija
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
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFC8102E),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isPending) "Rechazar Subpedido" else "Cancelar Subpedido",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "Cliente: ${subOrder.buyerName?.ifBlank { "Estudiante" } ?: "Estudiante"}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissRejectionDialog() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta informativa
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isPending) {
                                    "El comprador será notificado de inmediato y su orden se recalculará restando este importe."
                                } else {
                                    "El pedido se cancelará. El comprador será notificado y los productos se reincorporarán automáticamente a tu inventario disponible."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9A3412),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Text(
                        text = "Selecciona el motivo:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF16324F)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            commonReasons.forEachIndexed { index, reason ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedReason = reason }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedReason == reason,
                                        onClick = { selectedReason = reason },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC8102E))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                if (index < commonReasons.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                }
                            }
                        }
                    }

                    if (selectedReason == "Otro motivo") {
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            label = { Text("Escribe el motivo detallado") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val finalReason = if (selectedReason == "Otro motivo") {
                                customReason.ifBlank { if (isPending) "Rechazado por el puesto" else "Cancelado por el vendedor" }
                            } else {
                                selectedReason
                            }
                            viewModel.confirmRejection(subOrder.id, finalReason)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isPending) "Confirmar Rechazo" else "Confirmar Cancelación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissRejectionDialog() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Volver", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Bottom Sheet de Confirmación de Entrega y Cobro con Código de Seguridad (CodeSlots)
    if (uiState.selectedSubOrderForDelivery != null) {
        val subOrder = uiState.selectedSubOrderForDelivery!!
        SellerDeliveryConfirmationBottomSheet(
            subOrder = subOrder,
            onDismiss = { viewModel.dismissDeliveryDialog() },
            onConfirm = { subOrderId ->
                viewModel.confirmDeliveryAndPayment(subOrderId)
            }
        )
    }

    // Modal BottomSheet de Agregar Nuevo Producto al Catálogo
    if (uiState.showAddProductDialog) {
        val context = LocalContext.current
        var prodName by remember { mutableStateOf("") }
        var prodPrice by remember { mutableStateOf("") }
        var prodStock by remember { mutableStateOf("10") }
        var prodDesc by remember { mutableStateOf("") }
        var prodImageUrl by remember { mutableStateOf("") }
        var isUploadingPhoto by remember { mutableStateOf(false) }
        val newProductId = remember { UUID.randomUUID().toString() }
        var selectedCatId by remember(uiState.categories) {
            mutableStateOf(uiState.categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a")
        }
        var validationError by remember { mutableStateOf<String?>(null) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val configuration = LocalConfiguration.current
        val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

        val productPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
                if (bytes != null) {
                    isUploadingPhoto = true
                    val path = "products/prod_${newProductId}.jpg"
                    viewModel.uploadAsset("product-images", path, bytes) { uploadedUrl ->
                        prodImageUrl = uploadedUrl
                        isUploadingPhoto = false
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissAddProductDialog() },
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
                // Cabecera superior fija
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
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Nuevo Producto",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "Añadir a tu catálogo comercial",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissAddProductDialog() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vista previa de imagen con botón para seleccionar foto de galería
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { productPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CampusGoProductImage(
                                imageUrl = prodImageUrl.takeIf { it.isNotBlank() },
                                categoryName = uiState.categories.find { it.id == selectedCatId }?.name,
                                productName = prodName,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            if (isUploadingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { productPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            leadingIcon = {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = if (isUploadingPhoto) "Subiendo foto..." else if (prodImageUrl.isBlank()) "Subir foto" else "Cambiar foto",
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }

                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it; validationError = null },
                        label = { Text("Nombre del Producto *") },
                        placeholder = { Text("Ej. Triple de Pollo con Palta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prodPrice,
                            onValueChange = { prodPrice = it.replace(',', '.'); validationError = null },
                            label = { Text("Precio (S/.) *") },
                            placeholder = { Text("6.50") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = prodStock,
                            onValueChange = { prodStock = it.filter { ch -> ch.isDigit() }; validationError = null },
                            label = { Text("Stock inicial *") },
                            placeholder = { Text("15") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    }

                    if (uiState.categories.isNotEmpty()) {
                        var expandedCat by remember { mutableStateOf(false) }
                        val currentCatName = uiState.categories.find { it.id == selectedCatId }?.name ?: "Selecciona Categoría"

                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentCatName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCat,
                                onDismissRequest = { expandedCat = false }
                            ) {
                                uiState.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCatId = cat.id
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("Descripción corta (opcional)") },
                        placeholder = { Text("Detalles para el alumno") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    val errorToDisplay = validationError ?: uiState.errorMessage
                    if (errorToDisplay != null) {
                        Text(
                            text = errorToDisplay,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cleanPrice = prodPrice.trim().replace(',', '.')
                            val cleanStock = prodStock.trim()
                            val p = cleanPrice.toDoubleOrNull()
                            val s = cleanStock.toIntOrNull()
                            if (prodName.isBlank()) {
                                validationError = "Ingresa el nombre del producto."
                            } else if (p == null || p <= 0.0) {
                                validationError = "Ingresa un precio válido mayor a 0 (ej. 5.50)."
                            } else if (s == null || s < 0) {
                                validationError = "Ingresa una cantidad de stock válida (0 o más)."
                            } else {
                                viewModel.createProduct(
                                    name = prodName,
                                    price = p,
                                    stock = s,
                                    categoryId = selectedCatId.ifBlank { uiState.categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a" },
                                    description = prodDesc.ifBlank { prodName },
                                    imageUrl = prodImageUrl.takeIf { it.isNotBlank() },
                                    id = newProductId
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !uiState.isSavingProduct && !isUploadingPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isSavingProduct) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Guardar Producto", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissAddProductDialog() },
                        enabled = !uiState.isSavingProduct,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Modal BottomSheet de Edición Completa de Producto
    if (uiState.selectedProductForEdit != null) {
        val context = LocalContext.current
        val prod = uiState.selectedProductForEdit!!
        var nameInput by remember(prod.id) { mutableStateOf(prod.name) }
        var priceInput by remember(prod.id) { mutableStateOf(prod.price.toString()) }
        var stockInput by remember(prod.id) { mutableStateOf(prod.stock.toString()) }
        var descInput by remember(prod.id) { mutableStateOf(prod.description.orEmpty()) }
        var imageInput by remember(prod.id) { mutableStateOf(prod.imageUrl.orEmpty()) }
        var isUploadingEditPhoto by remember { mutableStateOf(false) }
        var selectedCatId by remember(prod.id) { mutableStateOf(prod.categoryId ?: "") }
        var editError by remember { mutableStateOf<String?>(null) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val configuration = LocalConfiguration.current
        val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

        val editProductPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
                if (bytes != null) {
                    isUploadingEditPhoto = true
                    val path = "products/prod_${prod.id}.jpg"
                    viewModel.uploadAsset("product-images", path, bytes) { uploadedUrl ->
                        imageInput = uploadedUrl
                        isUploadingEditPhoto = false
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEditProductDialog() },
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
                // Cabecera superior fija
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
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Editar Producto",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = prod.name,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissEditProductDialog() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vista previa y selector interactivo de imagen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editProductPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CampusGoProductImage(
                                imageUrl = imageInput.takeIf { it.isNotBlank() },
                                categoryName = uiState.categories.find { it.id == selectedCatId }?.name,
                                productName = nameInput,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            if (isUploadingEditPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { editProductPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            leadingIcon = {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = if (isUploadingEditPhoto) "Subiendo foto..." else if (imageInput.isBlank()) "Subir foto" else "Cambiar foto",
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; editError = null },
                        label = { Text("Nombre del Producto *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it.replace(',', '.'); editError = null },
                            label = { Text("Precio (S/.) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = stockInput,
                            onValueChange = { stockInput = it.filter { ch -> ch.isDigit() }; editError = null },
                            label = { Text("Stock *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    if (uiState.categories.isNotEmpty()) {
                        var expandedCat by remember { mutableStateOf(false) }
                        val currentCatName = uiState.categories.find { it.id == selectedCatId }?.name ?: "Selecciona Categoría"

                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentCatName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCat,
                                onDismissRequest = { expandedCat = false }
                            ) {
                                uiState.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCatId = cat.id
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    if (editError != null) {
                        Text(
                            text = editError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Confirmación inline de eliminación sin popups
                    AnimatedVisibility(visible = showDeleteConfirm) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "¿Eliminar definitivamente \"${prod.name}\"?",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC8102E),
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "El producto se retirará de tu catálogo y ningún comprador podrá ordenarlo.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.deleteProduct(prod.id)
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Text("Sí, Eliminar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { showDeleteConfirm = false },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Text("Cancelar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val p = priceInput.trim().toDoubleOrNull()
                            val s = stockInput.trim().toIntOrNull()
                            if (nameInput.isBlank()) {
                                editError = "El nombre no puede estar vacío."
                            } else if (p == null || p <= 0) {
                                editError = "Precio inválido."
                            } else if (s == null || s < 0) {
                                editError = "Stock inválido."
                            } else {
                                viewModel.updateProduct(
                                    productId = prod.id,
                                    name = nameInput,
                                    price = p,
                                    stock = s,
                                    categoryId = selectedCatId.takeIf { it.isNotBlank() },
                                    description = descInput,
                                    imageUrl = imageInput
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !uiState.isSavingProduct && !isUploadingEditPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isSavingProduct) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Eliminar", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.dismissEditProductDialog() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal BottomSheet de Incidencia: Comprador no se presentó
    if (uiState.selectedSubOrderForNoShow != null) {
        val subOrder = uiState.selectedSubOrderForNoShow!!
        var noShowReason by remember { mutableStateOf("El comprador no asistió al punto en el horario acordado") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissNoShowDialog() },
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
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Cabecera superior fija
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
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFC8102E),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Comprador no se presentó",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "Cliente: ${subOrder.buyerName?.ifBlank { "Estudiante" } ?: "Estudiante"}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissNoShowDialog() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "¿Deseas reportar la inasistencia del comprador? El subpedido cambiará a 'NO ENTREGADO' y las unidades reservadas se restituirán inmediatamente a tu inventario disponible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9A3412),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noShowReason,
                        onValueChange = { noShowReason = it },
                        label = { Text("Detalle de la incidencia") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.confirmBuyerNoShow(subOrder.id, noShowReason)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Confirmar No-Show", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissNoShowDialog() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Modal BottomSheet de Edición de Stock
    if (uiState.selectedProductForStockEdit != null) {
        val prod = uiState.selectedProductForStockEdit!!
        var stockInput by remember(prod.id) { mutableStateOf(prod.stock.toString()) }
        var stockError by remember { mutableStateOf<String?>(null) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEditStockDialog() },
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
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Cabecera superior fija
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
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Actualizar Stock",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = prod.name,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissEditStockDialog() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Cantidad disponible en tiempo real",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF16324F)
                            )
                            Text(
                                text = "Modifica la cantidad disponible para compradores. Si asignas 0 unidades, el producto se pausará automáticamente en tu catálogo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = {
                            stockInput = it.filter { ch -> ch.isDigit() }
                            stockError = null
                        },
                        label = { Text("Stock disponible *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (stockError != null) {
                        Text(
                            text = stockError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val s = stockInput.toIntOrNull()
                            if (s == null || s < 0) {
                                stockError = "Ingresa un número entero válido (0 o más)."
                            } else {
                                viewModel.updateStock(prod.id, s)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Guardar Stock", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissEditStockDialog() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (uiState.selectedSubOrderForDetail != null) {
        val selectedSub = uiState.selectedSubOrderForDetail!!
        val curProf = uiState.sellerProfile ?: profile
        val sellerRating = uiState.sellerReviewedOrders[selectedSub.id]
            ?: uiState.sellerReviewedOrders[selectedSub.orderId]
            ?: (selectedSub.buyerId?.let { uiState.sellerReviewedOrders["${selectedSub.orderId}-$it"] })
        SellerOrderDetailDialog(
            subOrder = selectedSub,
            isProcessing = uiState.processingSubOrderIds.contains(selectedSub.id),
            onDismiss = { viewModel.dismissSubOrderDetail() },
            onAccept = {
                viewModel.acceptSubOrder(it)
                viewModel.dismissSubOrderDetail()
            },
            onStartPrep = {
                viewModel.startPreparation(it)
                viewModel.dismissSubOrderDetail()
            },
            onMarkReady = {
                viewModel.markReady(it)
                viewModel.dismissSubOrderDetail()
            },
            onOpenDelivery = {
                viewModel.dismissSubOrderDetail()
                viewModel.openDeliveryDialog(it)
            },
            onOpenRejection = {
                viewModel.dismissSubOrderDetail()
                viewModel.openRejectionDialog(it)
            },
            onOpenChat = { subOrder ->
                viewModel.dismissSubOrderDetail()
                activeChatSubOrder = subOrder
                chatViewModel.initChat(
                    subOrderId = subOrder.id,
                    currentUserId = curProf.id,
                    otherUserId = subOrder.buyerId ?: "",
                    otherUserName = subOrder.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                    meetingPoint = subOrder.meetingPointName ?: "Punto de entrega",
                    subOrderStatus = subOrder.status,
                    otherUserAvatarUrl = subOrder.buyerAvatarUrl,
                    deliveryCode = subOrder.verificationCode
                )
            }
        )
    }

    // Bottom Sheet de Avisos y Moderación / Strikes del Administrador
    if (showNotificationsSheet) {
        SellerNotificationsBottomSheet(
            warnings = uiState.warnings,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    // Diálogo de Selección de Fecha de Historial (Material 3 DatePickerDialog con diseño Campus GO)
    if (showDatePickerDialog) {
        val initialMillis = remember(uiState.selectedDate) {
            uiState.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val todayUtcEnd = remember {
            LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = remember {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis < todayUtcEnd
                    }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            ),
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp, bottom = 12.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.resetToToday()
                            showDatePickerDialog = false
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ir a Hoy",
                            color = Color(0xFF00A884),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val pickedDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                viewModel.setSelectedDate(pickedDate)
                            }
                            showDatePickerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Ventas", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePickerDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Cerrar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE6F7F3),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Historial de Ventas",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16324F)
                                )
                                Text(
                                    text = "Audita pedidos e ingresos de cualquier fecha",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                },
                headline = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    val formattedHeadline = remember(selectedMillis) {
                        selectedMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            val dayName = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE")).replaceFirstChar { it.uppercase() }
                            val monthName = localDate.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                            "$dayName, ${localDate.dayOfMonth} de $monthName"
                        } ?: "Selecciona un día"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedHeadline,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A884)
                        )
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF16324F),
                    headlineContentColor = Color(0xFF00A884),
                    weekdayContentColor = Color(0xFF64748B),
                    subheadContentColor = Color(0xFF16324F),
                    yearContentColor = Color(0xFF16324F),
                    currentYearContentColor = Color(0xFF00A884),
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = Color(0xFF00A884),
                    dayContentColor = Color(0xFF1E293B),
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = Color(0xFF00A884),
                    todayDateBorderColor = Color(0xFF00A884),
                    todayContentColor = Color(0xFF00A884),
                    dayInSelectionRangeContentColor = Color(0xFF16324F),
                    dayInSelectionRangeContainerColor = Color(0xFFE6F7F3),
                    dividerColor = Color(0xFFF1F5F9),
                    navigationContentColor = Color(0xFF16324F)
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (uiState.selectedTab != SellerTab.PERFIL && uiState.selectedTab != SellerTab.MI_PUESTO) {
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
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(60.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val curProf = uiState.sellerProfile ?: profile

                        // Tienda / Emprendimiento (Al pulsar lleva a la pestaña de Perfil)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSelectedTab(SellerTab.PERFIL) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            CampusGoUserAvatar(
                                avatarUrl = curProf.avatarUrl,
                                name = curProf.businessName ?: curProf.fullName,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                val storeDisplayName = (curProf.businessName?.takeIf { it.isNotBlank() } ?: curProf.fullName).ifBlank { "Mi Puesto" }
                                Text(
                                    text = storeDisplayName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = curProf.businessLocation?.takeIf { it.isNotBlank() } ?: "Campus ${curProf.campus}"
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Controles fijos superiores: Toggle Abrir/Cerrar Puesto + Botón de Notificaciones/Strikes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Toggle interactivo de Abrir/Cerrar Puesto (iOS pill switch style)
                            val isDark = LocalDarkTheme.current
                            val thumbOffset by androidx.compose.animation.core.animateDpAsState(
                                targetValue = if (uiState.isAcceptingOrders) 14.dp else 0.dp,
                                animationSpec = androidx.compose.animation.core.tween(200),
                                label = "stall_toggle"
                            )
                            val toggleBg = if (isDark) {
                                if (uiState.isAcceptingOrders) Color(0xFF004D3D).copy(alpha = 0.45f) else Color(0xFF450A0A).copy(alpha = 0.45f)
                            } else {
                                if (uiState.isAcceptingOrders) Color(0xFFE6F7F3) else Color(0xFFFEE2E2)
                            }
                            val toggleBorder = if (isDark) {
                                if (uiState.isAcceptingOrders) Color(0xFF34D399).copy(alpha = 0.4f) else Color(0xFFF87171).copy(alpha = 0.4f)
                            } else {
                                if (uiState.isAcceptingOrders) Color(0xFF00A884).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
                            }
                            val toggleText = if (isDark) {
                                if (uiState.isAcceptingOrders) Color(0xFF34D399) else Color(0xFFF87171)
                            } else {
                                if (uiState.isAcceptingOrders) Color(0xFF007A60) else Color(0xFFB91C1C)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(toggleBg)
                                    .border(1.dp, toggleBorder, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.toggleAcceptingOrders(!uiState.isAcceptingOrders) }
                                    .padding(start = 9.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(toggleText, CircleShape)
                                )
                                Text(
                                    text = if (uiState.isAcceptingOrders) "Abierto" else "Cerrado",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = toggleText
                                )
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(18.dp)
                                        .background(
                                            if (uiState.isAcceptingOrders) (if (isDark) Color(0xFF00B589) else Color(0xFF00A884)) else (if (isDark) Color(0xFF30363D) else Color(0xFFCBD5E1)),
                                            RoundedCornerShape(9.dp)
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = thumbOffset)
                                            .size(14.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }

                            val unreadWarningsCount = (uiState.warnings.size - lastReadWarningCount).coerceAtLeast(0)

                            // Botón de Notificaciones con Badge para Strikes
                            // Botón de Avisos y Moderación (solo se muestra cuando tiene como mínimo 1 aviso)
                            if (uiState.warnings.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val currentWarningIds = uiState.warnings.map { it.id }.toSet()
                                        val seenWarningIds = prefs.getStringSet("seen_warning_ids_${curProf.id}", emptySet()) ?: emptySet()
                                        prefs.edit().putStringSet("seen_warning_ids_${curProf.id}", seenWarningIds + currentWarningIds).apply()
                                        lastReadWarningCount = uiState.warnings.size
                                        showWarningBanner = false
                                        showNotificationsSheet = true
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadWarningsCount > 0) {
                                                Badge(
                                                    containerColor = Color(0xFFDC2626),
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        text = "$unreadWarningsCount",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = "Avisos y Moderación",
                                            tint = if (unreadWarningsCount > 0) Color(0xFFDC2626) else Color(0xFFE65100),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = if (backgroundBlurRadius > 0.dp) modifier.blur(backgroundBlurRadius) else modifier
        ) { innerPadding ->
            val isDarkMode = LocalDarkTheme.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                when (uiState.selectedTab) {
                    SellerTab.PEDIDOS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AnimatedVisibility(
                                visible = showWarningBanner && uiState.warnings.isNotEmpty(),
                                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(400)) + shrinkVertically(tween(400))
                            ) {
                                OfficialWarningBanner(
                                    warnings = uiState.warnings,
                                    isSeller = true,
                                    timerProgress = timerProgress.value,
                                    onBannerClick = {
                                        lastReadWarningCount = uiState.warnings.size
                                        hasShownWarningBannerOnEntry = true
                                        showWarningBanner = false
                                        showNotificationsSheet = true
                                    }
                                )
                            }


                    val formattedSelectedDate = remember(uiState.selectedDate) {
                        val dayName = uiState.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                            .replaceFirstChar { it.uppercase() }
                        val monthName = uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                        "$dayName, ${uiState.selectedDate.dayOfMonth} de $monthName"
                    }

                    // Indicador de Jornada de Hoy o Historial por Fecha con Botón de Calendario
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alarm_custom),
                                contentDescription = null,
                                tint = if (uiState.isViewingToday) MaterialTheme.colorScheme.onBackground else Color(0xFF00A884),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isViewingToday) "Jornada de Hoy • $formattedSelectedDate" else "Historial • $formattedSelectedDate",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isViewingToday) MaterialTheme.colorScheme.onBackground else Color(0xFF00A884),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!uiState.isViewingToday) {
                                Surface(
                                    onClick = { viewModel.resetToToday() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE6F7F3),
                                    border = BorderStroke(1.dp, Color(0xFF00A884).copy(alpha = 0.35f)),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Today,
                                            contentDescription = "Volver a hoy",
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Hoy",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00A884)
                                        )
                                    }
                                }
                            }

                            // Botón de Calendario sin esquinas cortadas
                            Surface(
                                onClick = { showDatePickerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!uiState.isViewingToday) Color(0xFF00A884) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (!uiState.isViewingToday) Color(0xFF00A884) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shadowElevation = if (!uiState.isViewingToday) 2.dp else 1.dp,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Seleccionar fecha de historial",
                                        tint = if (!uiState.isViewingToday) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Resumen de Métricas / KPIs del Día
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricSummaryCard(
                            title = if (uiState.isViewingToday) "Ganancias Hoy" else "Ganancias",
                            value = "S/ %.2f".format(uiState.displayEarnings),
                            color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF003366),
                            modifier = Modifier.weight(1.3f)
                        )
                        MetricSummaryCard(
                            title = "Pendientes",
                            value = "${uiState.displayPendingCount}",
                            color = if (isDarkMode) Color(0xFFFB923C) else Color(0xFFF57C00),
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "En Preparación",
                            value = "${uiState.displayInPrepCount}",
                            color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF1976D2),
                            modifier = Modifier.weight(1.25f)
                        )
                        MetricSummaryCard(
                            title = "Listos",
                            value = "${uiState.displayReadyCount}",
                            color = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Filtros de Estado en Chips Horizontales
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SellerOrderFilter.values().forEach { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            val label = when (filter) {
                                SellerOrderFilter.TODOS -> if (uiState.isViewingToday) "Hoy (${uiState.displayTotalOrders})" else "Todos (${uiState.displayTotalOrders})"
                                SellerOrderFilter.PENDIENTES -> "Pendientes (${uiState.displayPendingCount})"
                                SellerOrderFilter.EN_PREPARACION -> "En Preparación (${uiState.displayInPrepCount})"
                                SellerOrderFilter.LISTOS -> "Listos (${uiState.displayReadyCount})"
                                SellerOrderFilter.COMPLETADOS -> if (uiState.isViewingToday) "Entregados Hoy (${uiState.displayCompletedCount})" else "Entregados (${uiState.displayCompletedCount})"
                                SellerOrderFilter.RECHAZADOS -> "Rechazados (${uiState.displayRejectedCount})"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // Lista de Subpedidos de la Fecha Seleccionada
                    val displayOrders = remember(uiState.filteredDisplayOrders) {
                        uiState.filteredDisplayOrders
                    }
                    var sellerHistoryLimit by remember(uiState.isViewingToday, uiState.selectedDate) { mutableIntStateOf(20) }
                    val visibleOrders = remember(displayOrders, uiState.isViewingToday, sellerHistoryLimit) {
                        if (!uiState.isViewingToday) {
                            displayOrders.take(sellerHistoryLimit)
                        } else {
                            displayOrders
                        }
                    }

                    if (displayOrders.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_store_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val emptyMsg = if (!uiState.isViewingToday) {
                                    "No se registraron pedidos en esta fecha ($formattedSelectedDate)"
                                } else {
                                    when (uiState.selectedFilter) {
                                        SellerOrderFilter.TODOS -> "No hay subpedidos registrados aún"
                                        SellerOrderFilter.PENDIENTES -> "No hay pedidos pendientes por responder"
                                        SellerOrderFilter.EN_PREPARACION -> "No tienes pedidos en preparación actualmente"
                                        SellerOrderFilter.LISTOS -> "No hay pedidos esperando entrega en este momento"
                                        SellerOrderFilter.COMPLETADOS -> "No hay pedidos entregados registrados"
                                        SellerOrderFilter.RECHAZADOS -> "No hay pedidos rechazados o cancelados"
                                    }
                                }
                                Text(
                                    text = emptyMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (!uiState.isViewingToday) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FilledTonalButton(
                                        onClick = { viewModel.resetToToday() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Today,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ver Pedidos de Hoy")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 76.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                        ) {
                            // 1. Bloque: Pedidos de la fecha seleccionada
                            if (displayOrders.isNotEmpty()) {
                                item(key = "header_orders") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(if (uiState.isViewingToday) Color(0xFF2E7D32) else Color(0xFF0284C7), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState.isViewingToday) "Pedidos de Hoy" else "Historial del Día",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF003366)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${displayOrders.size} pedido${if (displayOrders.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(visibleOrders, key = { it.id }) { subOrder ->
                                    SellerSubOrderCard(
                                        subOrder = subOrder,
                                        isProcessing = uiState.processingSubOrderIds.contains(subOrder.id),
                                        onAccept = { viewModel.acceptSubOrder(subOrder.id) },
                                        onStartPrep = { viewModel.startPreparation(subOrder.id) },
                                        onMarkReady = { viewModel.markReady(subOrder.id) },
                                        onOpenDelivery = { viewModel.openDeliveryDialog(subOrder) },
                                        onOpenRejection = { viewModel.openRejectionDialog(subOrder) },
                                        onOpenNoShow = { viewModel.openNoShowDialog(subOrder) },
                                        onExpired = { viewModel.onSubOrderExpired(subOrder.id) },
                                        onOpenDetail = { viewModel.openSubOrderDetail(subOrder) },
                                        buyerStrikes = subOrder.buyerId?.let { uiState.buyerStrikes[it] } ?: 0,
                                        onOpenChat = {
                                            activeChatSubOrder = subOrder
                                            chatViewModel.initChat(
                                                subOrderId = subOrder.id,
                                                currentUserId = curProf.id,
                                                otherUserId = subOrder.buyerId ?: "",
                                                otherUserName = subOrder.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                                                meetingPoint = subOrder.meetingPointName ?: "Punto de entrega",
                                                subOrderStatus = subOrder.status,
                                                otherUserAvatarUrl = subOrder.buyerAvatarUrl,
                                                deliveryCode = subOrder.verificationCode
                                            )
                                        }
                                    )
                                }

                                if (!uiState.isViewingToday) {
                                    if (displayOrders.size > sellerHistoryLimit) {
                                        item(key = "load_more_seller_history") {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                                        text = "Mostrando ${visibleOrders.size} de ${displayOrders.size} pedidos",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Medium
                                                    )

                                                    Button(
                                                        onClick = { sellerHistoryLimit += 20 },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(46.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF003366),
                                                            contentColor = Color.White
                                                        )
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ExpandMore,
                                                                contentDescription = null,
                                                                tint = Color.White,
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
                                    } else if (displayOrders.size > 20) {
                                        item(key = "all_seller_history_loaded") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Mostrando todos los pedidos del historial (${displayOrders.size})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                item(key = "empty_orders") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_store_custom),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (uiState.isViewingToday) "Sin pedidos para hoy en este filtro" else "Sin pedidos registrados para esta fecha en este filtro",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Ganancias: S/ %.2f".format(uiState.displayEarnings),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            SellerTab.PRODUCTOS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Pestaña Mis Productos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Productos en Venta (${uiState.products.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { viewModel.openAddProductDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nuevo Producto")
                            }
                        }

                        if (uiState.products.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aún no tienes productos registrados en tu puesto",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.openAddProductDialog() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Publicar Primer Producto")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 76.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                            ) {
                                items(uiState.products, key = { it.id }) { product ->
                                    val catName = uiState.categories.find { it.id == product.categoryId }?.name
                                    ProductCard(
                                        product = product,
                                        categoryName = catName,
                                        onToggleActive = { isActive ->
                                            viewModel.toggleProductActive(product.id, isActive)
                                        },
                                        onEditStock = {
                                            viewModel.openEditStockDialog(product)
                                        },
                                        onEditProduct = {
                                            viewModel.openEditProductDialog(product)
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                    val curProf = uiState.sellerProfile ?: profile
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
                                    deliveryCode = sub.verificationCode,
                                    isBuyerPerspective = false,
                                    otherUserAvatarUrl = sub.buyerAvatarUrl
                                )
                            }
                    }

                    ActiveChatsSheet(
                        chats = activeSellerChatSummaries,
                        supportTickets = listOfNotNull(sellerActiveTicket),
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
                                    deliveryCode = summary.deliveryCode
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
            unreadChatCount = unreadChatCount,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
}

@Composable
fun SellerPastDayCard(
    group: DailyOrderGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAccept: (String) -> Unit,
    onStartPrep: (String) -> Unit,
    onMarkReady: (String) -> Unit,
    onOpenDelivery: (SubOrder) -> Unit,
    onOpenRejection: (SubOrder) -> Unit,
    onOpenNoShow: (SubOrder) -> Unit,
    onExpired: (String) -> Unit,
    onOpenDetail: (SubOrder) -> Unit = {},
    onOpenChat: ((SubOrder) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )
                    Text(
                        text = "${group.completedCount} entregados • ${group.orders.size} pedidos totales" +
                                if (group.cancelledCount > 0) " • ${group.cancelledCount} cancelados" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF003366).copy(alpha = 0.08f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "S/ %.2f".format(group.totalEarnings),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF003366),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                        tint = Color(0xFF003366)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    group.orders.forEach { subOrder ->
                        SellerSubOrderCard(
                            subOrder = subOrder,
                            isProcessing = false,
                            onAccept = { onAccept(subOrder.id) },
                            onStartPrep = { onStartPrep(subOrder.id) },
                            onMarkReady = { onMarkReady(subOrder.id) },
                            onOpenDelivery = { onOpenDelivery(subOrder) },
                            onOpenRejection = { onOpenRejection(subOrder) },
                            onOpenNoShow = { onOpenNoShow(subOrder) },
                            onExpired = { onExpired(subOrder.id) },
                            onOpenDetail = { onOpenDetail(subOrder) },
                            onOpenChat = if (onOpenChat != null && !subOrder.status.isFinal) { { onOpenChat(subOrder) } } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SellerSubOrderCard(
    subOrder: SubOrder,
    onAccept: () -> Unit,
    onStartPrep: () -> Unit,
    onMarkReady: () -> Unit,
    onOpenDelivery: () -> Unit,
    onOpenRejection: () -> Unit,
    onOpenNoShow: () -> Unit,
    onExpired: () -> Unit,
    onOpenDetail: (() -> Unit)? = null,
    onOpenChat: (() -> Unit)? = null,
    buyerStrikes: Int = 0,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpiredState by remember(subOrder.id, subOrder.createdAt) {
        mutableStateOf(isSubOrderExpired(subOrder.createdAt))
    }

    LaunchedEffect(isExpiredState) {
        if (isExpiredState && subOrder.status == SubOrderStatus.PENDIENTE) {
            onExpired()
        }
    }

    val isDark = LocalDarkTheme.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Cabecera limpia: Código amigable del pedido, Horario de ingreso, Temporizador y Badge de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Pedido #${subOrder.orderId.takeLast(4).uppercase()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    val orderTime = formatOrderTime(subOrder.createdAt)
                    if (orderTime.isNotBlank()) {
                        Text(
                            text = "• $orderTime",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (subOrder.status == SubOrderStatus.PENDIENTE && !isExpiredState) {
                        SubOrderCountdownTimerBadge(
                            createdAtIso = subOrder.createdAt,
                            status = subOrder.status,
                            onExpired = {
                                isExpiredState = true
                                onExpired()
                            }
                        )
                    }
                    StatusBadge(status = if (isExpiredState && subOrder.status == SubOrderStatus.PENDIENTE) SubOrderStatus.RECHAZADO else subOrder.status)
                }
            }

            // 2. Información del Cliente y Punto de Encuentro Integrado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CampusGoUserAvatar(
                        avatarUrl = subOrder.buyerAvatarUrl,
                        name = subOrder.buyerName,
                        size = 40.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = subOrder.buyerName?.ifBlank { "Estudiante Universitario" } ?: "Estudiante Universitario",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (buyerStrikes > 0) {
                                StrikeBadge(strikes = buyerStrikes)
                            }
                        }

                        // Punto de encuentro y horario acordado
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location_custom),
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = subOrder.meetingPointName?.ifBlank { "Campus Universitario" } ?: "Campus Universitario",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subOrder.scheduledTime.isNullOrBlank()) {
                                Text(
                                    text = "• ⏰ ${subOrder.scheduledTime}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }

                // PIN de entrega destacado si está listo o completado
                if (subOrder.status == SubOrderStatus.LISTO ||
                    subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA ||
                    subOrder.status == SubOrderStatus.COMPLETADO) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "PIN ENTREGA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "#${subOrder.verificationCode}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }
            }

            // Nota especial del comprador (si existe)
            if (!subOrder.notes.isNullOrBlank()) {
                Surface(
                    color = if (isDark) Color(0xFF451A03).copy(alpha = 0.5f) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💬", fontSize = 13.sp)
                        Text(
                            text = "Nota: ${subOrder.notes}",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Lista de Ítems del pedido (Con formato limpio y cantidades destacadas)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (subOrder.items.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1x Subpedido Campus",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "S/ %.2f".format(subOrder.subtotalAmount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    subOrder.items.forEach { item ->
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
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${item.quantity}x",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.productName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "S/ %.2f".format(item.subtotal),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Medio de pago y Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (pmBg, pmTint, pmLabel) = when (subOrder.paymentMethod) {
                    PaymentMethod.YAPE -> Triple(if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.25f) else Color(0xFFF3E5F5), if (isDark) Color(0xFFCE93D8) else Color(0xFF6A1B9A), "Yape")
                    PaymentMethod.PLIN -> Triple(if (isDark) Color(0xFF00796B).copy(alpha = 0.25f) else Color(0xFFE0F2F1), if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B), "Plin")
                    PaymentMethod.EFECTIVO -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Efectivo")
                    else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, subOrder.paymentMethod?.name ?: "Efectivo")
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
                        PaymentMethodLogo(method = subOrder.paymentMethod, size = 14.dp)
                        Text(
                            text = pmLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = pmTint
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "S/ %.2f".format(subOrder.subtotalAmount),
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF003366),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.5.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // 5. Botones de Acción (Con animación de carga inmediata para feedback sin demoras)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenChat != null && !subOrder.status.isFinal) {
                    FilledTonalIconButton(
                        onClick = onOpenChat,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFE6F4EA),
                            contentColor = Color(0xFF00A884)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_custom),
                            contentDescription = "Chat con comprador",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                when (subOrder.status) {
                    SubOrderStatus.PENDIENTE -> {
                        if (isExpiredState) {
                            Text(
                                text = "Expirado (15 min)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC8102E)
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onOpenRejection,
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("Rechazar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onAccept,
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Aceptar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    SubOrderStatus.ACEPTADO -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenRejection,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onStartPrep,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Iniciar Preparación", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    SubOrderStatus.EN_PREPARACION -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenRejection,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onMarkReady,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Marcar Listo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenRejection,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onOpenDelivery,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Verificar Entrega (PIN)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    SubOrderStatus.COMPLETADO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Entregado y Cobrado",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                    SubOrderStatus.RECHAZADO -> {
                        Text(
                            text = "Rechazado: ${subOrder.rejectionReason ?: "Sin motivo"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SubOrderStatus.NO_ENTREGADO -> {
                        Text(
                            text = "No entregado (Inasistencia)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8102E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Text(text = subOrder.status.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (onOpenDetail != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetail() }
                        .padding(top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ver detalle del pedido ›",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF003366)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: SubOrderStatus) {
    val isDark = LocalDarkTheme.current
    val (backgroundColor, textColor, label) = when (status) {
        SubOrderStatus.PENDIENTE ->
            if (isDark) Triple(Color(0xFF78350F).copy(alpha = 0.35f), Color(0xFFFBBF24), "Pendiente")
            else Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        SubOrderStatus.ACEPTADO ->
            if (isDark) Triple(Color(0xFF1E3A8A).copy(alpha = 0.35f), Color(0xFF60A5FA), "Aceptado")
            else Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Aceptado")
        SubOrderStatus.EN_PREPARACION ->
            if (isDark) Triple(Color(0xFF581C87).copy(alpha = 0.35f), Color(0xFFC084FC), "En Preparación")
            else Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "En Preparación")
        SubOrderStatus.LISTO ->
            if (isDark) Triple(Color(0xFF064E3B).copy(alpha = 0.45f), Color(0xFF34D399), "Listo para Entrega")
            else Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Listo para Entrega")
        SubOrderStatus.ESPERANDO_ENTREGA ->
            if (isDark) Triple(Color(0xFF064E3B).copy(alpha = 0.45f), Color(0xFF34D399), "Esperando Entrega")
            else Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Esperando Entrega")
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO ->
            if (isDark) Triple(Color(0xFF004D3D).copy(alpha = 0.45f), Color(0xFF10B981), "Completado")
            else Triple(Color(0xFFE0F2F1), Color(0xFF00695C), "Completado")
        SubOrderStatus.RECHAZADO ->
            if (isDark) Triple(Color(0xFF450A0A).copy(alpha = 0.5f), Color(0xFFF87171), "Rechazado")
            else Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Rechazado")
        SubOrderStatus.CANCELADO ->
            if (isDark) Triple(Color(0xFF450A0A).copy(alpha = 0.5f), Color(0xFFF87171), "Cancelado")
            else Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Cancelado")
        SubOrderStatus.NO_ENTREGADO ->
            if (isDark) Triple(Color(0xFF27272A), Color(0xFF94A3B8), "No entregado")
            else Triple(Color(0xFFECEFF1), Color(0xFF455A64), "No entregado")
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        border = if (isDark) BorderStroke(0.75.dp, textColor.copy(alpha = 0.35f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    categoryName: String?,
    onToggleActive: (Boolean) -> Unit,
    onEditStock: () -> Unit,
    onEditProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isDark = LocalDarkTheme.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen del producto con fallback elegante de categoría (pastel + emoji)
            CampusGoProductImage(
                imageUrl = product.imageUrl,
                categoryName = categoryName,
                productName = product.name,
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = onEditProduct)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditProduct)
            ) {
                if (!categoryName.isNullOrBlank()) {
                    val catTheme = com.example.campusgo.ui.components.resolveCategoryVisualTheme(categoryName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = catTheme.iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ %.2f".format(product.price),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF003366),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        onClick = onEditStock,
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOutOfStock) (if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFFEBEE)) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isOutOfStock) Color(0xFFEF4444) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Stock: ${product.stock}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOutOfStock) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit_product_custom),
                                contentDescription = "Editar Stock",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditProduct,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit_product_custom),
                        contentDescription = "Editar producto completo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val statusText = when {
                    isOutOfStock -> "Sin stock"
                    product.isActive -> "Activo"
                    else -> "Pausado"
                }
                val statusColor = when {
                    isOutOfStock -> Color(0xFFEF4444)
                    product.isActive -> if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                    else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = product.isActive && !isOutOfStock,
                    onCheckedChange = { desired ->
                        if (isOutOfStock) {
                            onToggleActive(false)
                        } else {
                            onToggleActive(desired)
                        }
                    },
                    enabled = !isOutOfStock
                )
            }
        }
    }
}
