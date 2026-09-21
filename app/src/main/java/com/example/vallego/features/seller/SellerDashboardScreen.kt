package com.example.vallego.features.seller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.vallego.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import com.example.vallego.features.chat.ActiveChatSummary
import com.example.vallego.features.chat.ActiveChatsSheet
import com.example.vallego.features.chat.OrderChatBottomSheet
import com.example.vallego.features.chat.OrderChatViewModel
import androidx.compose.material.icons.filled.Close
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import com.example.vallego.ui.components.compressImageUri
import com.example.vallego.ui.components.isSubOrderExpired
import com.example.vallego.ui.components.PaymentMethodLogo
import com.example.vallego.ui.components.PaymentMethodLogoByName
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
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.Product
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.verificationCode
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.vallego.ui.components.EnlargedPhotoViewerDialog
import com.example.vallego.ui.components.StoreStatusBadge
import com.example.vallego.ui.components.SubOrderCountdownTimerBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import com.example.vallego.ui.components.ValleGoBusinessBanner
import com.example.vallego.ui.components.ValleGoProductImage
import com.example.vallego.ui.components.ValleGoUserAvatar
import com.example.vallego.domain.repository.ChatRepository
import com.example.vallego.ui.components.ValleGoDialogContainerColor
import com.example.vallego.ui.components.ValleGoDialogShape
import com.example.vallego.ui.components.ValleGoDialogTonalElevation
import com.example.vallego.ui.components.valleGoDialogStyle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SellerDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSellerProfile by remember { mutableStateOf(false) }
    val chatRepository: ChatRepository = koinInject()
    val curProf = uiState.sellerProfile ?: profile
    val unreadChatCount by remember(curProf.id) {
        chatRepository.observeUnreadCount(curProf.id)
    }.collectAsState(initial = 0)
    val chatViewModel: OrderChatViewModel = koinViewModel()
    var activeChatSubOrder by remember { mutableStateOf<SubOrder?>(null) }
    var showActiveChatsSheet by remember { mutableStateOf(false) }

    if (activeChatSubOrder != null) {
        OrderChatBottomSheet(
            viewModel = chatViewModel,
            onDismiss = {
                activeChatSubOrder = null
                chatViewModel.clearChat()
                showActiveChatsSheet = true
            }
        )
        return
    }

    if (showActiveChatsSheet) {
        val curProf = uiState.sellerProfile ?: profile
        val activeSellerChatSummaries = remember(uiState.subOrders) {
            uiState.subOrders
                .filter { !it.status.isFinal }
                .map { sub ->
                    ActiveChatSummary(
                        subOrderId = sub.id,
                        otherUserId = sub.buyerId ?: "",
                        otherUserName = sub.buyerName?.ifBlank { "Comprador Campus-Go" } ?: "Comprador Campus-Go",
                        meetingPoint = sub.meetingPointName ?: "Punto por acordar",
                        status = sub.status,
                        subtotal = sub.subtotalAmount,
                        itemsSummary = sub.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                        isBuyerPerspective = false
                    )
                }
        }

        ActiveChatsSheet(
            chats = activeSellerChatSummaries,
            onSelectChat = { summary ->
                showActiveChatsSheet = false
                val matchingSub = uiState.subOrders.find { it.id == summary.subOrderId }
                if (matchingSub != null) {
                    activeChatSubOrder = matchingSub
                    chatViewModel.initChat(
                        subOrderId = matchingSub.id,
                        currentUserId = curProf.id,
                        otherUserId = matchingSub.buyerId ?: "",
                        otherUserName = matchingSub.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                        meetingPoint = matchingSub.meetingPointName ?: "Punto por convenir",
                        subOrderStatus = matchingSub.status
                    )
                }
            },
            onClose = { showActiveChatsSheet = false }
        )
        return
    }

    if (showSellerProfile) {
        SellerStoreProfileScreen(
            profile = profile,
            sellerProfile = uiState.sellerProfile,
            availableMeetingPoints = uiState.availableMeetingPoints,
            isSaving = uiState.isSavingProfile,
            isUploading = uiState.isUploadingAsset,
            onNavigateBack = { showSellerProfile = false },
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
            onSignOut = onSignOut,
            modifier = modifier
        )
        return
    }

    // Navegación nativa de retroceso para chat y lista de chats activos del vendedor
    BackHandler(enabled = activeChatSubOrder != null) {
        activeChatSubOrder = null
        chatViewModel.clearChat()
        showActiveChatsSheet = true
    }
    BackHandler(enabled = activeChatSubOrder == null && showActiveChatsSheet) {
        showActiveChatsSheet = false
    }

    // Regresar a la pestaña principal de Pedidos antes de salir de la app
    BackHandler(enabled = activeChatSubOrder == null && !showActiveChatsSheet && uiState.selectedTab != SellerTab.PEDIDOS) {
        viewModel.setSelectedTab(SellerTab.PEDIDOS)
    }

    val isAnyModalOpen = uiState.selectedSubOrderForRejection != null ||
            uiState.selectedSubOrderForDelivery != null ||
            uiState.showAddProductDialog ||
            uiState.selectedProductForEdit != null ||
            uiState.selectedSubOrderForNoShow != null ||
            uiState.selectedProductForStockEdit != null ||
            uiState.selectedSubOrderForDetail != null

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

    // Modal de Rechazo o Cancelación de Subpedido
    if (uiState.selectedSubOrderForRejection != null) {
        val subOrder = uiState.selectedSubOrderForRejection!!
        val isPending = subOrder.status == SubOrderStatus.PENDIENTE
        val defaultReason = if (isPending) "Sin insumos / agotado" else "Comprador no se presentó al punto de encuentro"
        var selectedReason by remember { mutableStateOf(defaultReason) }
        var customReason by remember { mutableStateOf("") }

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

        AlertDialog(
            onDismissRequest = { viewModel.dismissRejectionDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text(
                    text = if (isPending) "Rechazar Subpedido" else "Cancelar Subpedido en Curso",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPending) {
                            "El comprador será notificado y su orden se recalculará automáticamente restando este importe."
                        } else {
                            "El pedido se cancelará. El comprador será notificado y los productos se reincorporarán automáticamente a tu inventario disponible."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    commonReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (selectedReason == "Otro motivo") {
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            label = { Text("Escribe el motivo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = if (selectedReason == "Otro motivo") {
                            customReason.ifBlank { if (isPending) "Rechazado por el puesto" else "Cancelado por el vendedor" }
                        } else {
                            selectedReason
                        }
                        viewModel.confirmRejection(subOrder.id, finalReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text(if (isPending) "Confirmar Rechazo" else "Confirmar Cancelación")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRejectionDialog() }) {
                    Text("Volver")
                }
            }
        )
    }

    // Modal de Confirmación de Entrega y Cobro con Código de Seguridad
    if (uiState.selectedSubOrderForDelivery != null) {
        val subOrder = uiState.selectedSubOrderForDelivery!!
        var inputCode by remember { mutableStateOf("") }
        var bypassCode by remember { mutableStateOf(false) }
        val isCodeValid = inputCode.trim() == subOrder.verificationCode
        val canConfirm = isCodeValid || bypassCode

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeliveryDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF003366)
                    )
                    Text("Confirmar Entrega y Cobro", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "¿Confirmas que entregaste el pedido al comprador y recibiste el pago pactado?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Subpedido #${subOrder.id.takeLast(6).uppercase()}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Monto a cobrar: S/ %.2f".format(subOrder.subtotalAmount),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF003366),
                                fontSize = 18.sp
                            )
                            val pmName = when (subOrder.paymentMethod) {
                                PaymentMethod.YAPE -> "Yape"
                                PaymentMethod.PLIN -> "Plin"
                                PaymentMethod.EFECTIVO -> "Efectivo"
                                else -> subOrder.paymentMethod?.name ?: "Efectivo"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Medio acordado:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                PaymentMethodLogo(method = subOrder.paymentMethod, size = 13.dp)
                                Text(
                                    text = pmName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Código de Seguridad PIN de 4 dígitos
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCodeValid) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isCodeValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant),
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
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isCodeValid) Color(0xFF2E7D32) else Color(0xFF003366),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Código de Seguridad (4 dígitos)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCodeValid) Color(0xFF2E7D32) else Color(0xFF003366)
                                )
                            }
                            Text(
                                text = "Pídele al comprador el código PIN de 4 dígitos que ve en su pantalla de seguimiento para verificar su identidad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                        inputCode = newValue
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "####",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 8.sp,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = inputCode.length == 4 && !isCodeValid
                            )
                            if (isCodeValid) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Código verificado correctamente.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (inputCode.length == 4) {
                                Text(
                                    text = "Código incorrecto. Verifica con el comprador.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC8102E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Opción de contingencia sin código
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = bypassCode,
                            onCheckedChange = { bypassCode = it }
                        )
                        Text(
                            text = "¿El comprador no tiene celular a mano? Confirmar sin código.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeliveryAndPayment(subOrder.id) },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirmar Cobro y Entrega")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeliveryDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Agregar Nuevo Producto al Catálogo
    if (uiState.showAddProductDialog) {
        val context = LocalContext.current
        var prodName by remember { mutableStateOf("") }
        var prodPrice by remember { mutableStateOf("") }
        var prodStock by remember { mutableStateOf("10") }
        var prodDesc by remember { mutableStateOf("") }
        var prodImageUrl by remember { mutableStateOf("") }
        var isUploadingPhoto by remember { mutableStateOf(false) }
        var selectedCatId by remember(uiState.categories) {
            mutableStateOf(uiState.categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a")
        }
        var validationError by remember { mutableStateOf<String?>(null) }

        val productPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
                if (bytes != null) {
                    isUploadingPhoto = true
                    val path = "products/prod_${UUID.randomUUID()}_${System.currentTimeMillis()}.jpg"
                    viewModel.uploadAsset("product-images", path, bytes) { uploadedUrl ->
                        prodImageUrl = uploadedUrl
                        isUploadingPhoto = false
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissAddProductDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Nuevo Producto al Catálogo", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Vista previa de imagen con botón para seleccionar foto de galería
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { productPhotoPicker.launch("image/*") },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ValleGoProductImage(
                                imageUrl = prodImageUrl.takeIf { it.isNotBlank() },
                                categoryName = uiState.categories.find { it.id == selectedCatId }?.name,
                                productName = prodName,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (isUploadingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                            onClick = { productPhotoPicker.launch("image/*") },
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = prodStock,
                            onValueChange = { prodStock = it.filter { ch -> ch.isDigit() }; validationError = null },
                            label = { Text("Stock inicial *") },
                            placeholder = { Text("15") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
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
                }
            },
            confirmButton = {
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
                                imageUrl = prodImageUrl.takeIf { it.isNotBlank() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                    enabled = !uiState.isSavingProduct && !isUploadingPhoto
                ) {
                    if (uiState.isSavingProduct) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Guardar Producto")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissAddProductDialog() },
                    enabled = !uiState.isSavingProduct
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Edición Completa de Producto
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

        val editProductPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
                if (bytes != null) {
                    isUploadingEditPhoto = true
                    val path = "products/prod_${prod.id}_${System.currentTimeMillis()}.jpg"
                    viewModel.uploadAsset("product-images", path, bytes) { uploadedUrl ->
                        imageInput = uploadedUrl
                        isUploadingEditPhoto = false
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
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
                            painter = painterResource(id = R.drawable.ic_delete_custom),
                            contentDescription = null,
                            tint = Color(0xFFC8102E),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = { Text("¿Eliminar producto?", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas eliminar \"${prod.name}\"?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProduct(prod.id)
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                    ) {
                        Text("Sí, Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissEditProductDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Editar Producto", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Vista previa y selector interactivo de imagen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editProductPhotoPicker.launch("image/*") },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ValleGoProductImage(
                                imageUrl = imageInput.takeIf { it.isNotBlank() },
                                categoryName = uiState.categories.find { it.id == selectedCatId }?.name,
                                productName = nameInput,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (isUploadingEditPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                            onClick = { editProductPhotoPicker.launch("image/*") },
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
                        singleLine = true
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = stockInput,
                            onValueChange = { stockInput = it.filter { ch -> ch.isDigit() }; editError = null },
                            label = { Text("Stock *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
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
                        maxLines = 3
                    )

                    if (editError != null) {
                        Text(
                            text = editError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                    enabled = !uiState.isSavingProduct && !isUploadingEditPhoto
                ) {
                    if (uiState.isSavingProduct) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Guardar Cambios")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC8102E))
                    ) {
                        Text("Eliminar")
                    }
                    TextButton(onClick = { viewModel.dismissEditProductDialog() }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Modal de Incidencia: Comprador no se presentó
    if (uiState.selectedSubOrderForNoShow != null) {
        val subOrder = uiState.selectedSubOrderForNoShow!!
        var noShowReason by remember { mutableStateOf("El comprador no asistió al punto en el horario acordado") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissNoShowDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Comprador no se presentó", fontWeight = FontWeight.Bold, color = Color(0xFFC8102E))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "¿Deseas reportar la inasistencia del comprador? El subpedido cambiará a 'NO ENTREGADO' y las unidades reservadas se restituirán inmediatamente a tu stock.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = noShowReason,
                        onValueChange = { noShowReason = it },
                        label = { Text("Detalle de la incidencia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmBuyerNoShow(subOrder.id, noShowReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Confirmar No-Show")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoShowDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Edición de Stock
    if (uiState.selectedProductForStockEdit != null) {
        val prod = uiState.selectedProductForStockEdit!!
        var stockInput by remember(prod.id) { mutableStateOf(prod.stock.toString()) }
        var stockError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissEditStockDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Actualizar Stock", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Producto: ${prod.name}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Modifica la cantidad disponible. Si asignas 0, el producto se pausará automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = {
                            stockInput = it.filter { ch -> ch.isDigit() }
                            stockError = null
                        },
                        label = { Text("Stock disponible *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (stockError != null) {
                        Text(
                            text = stockError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = stockInput.toIntOrNull()
                        if (s == null || s < 0) {
                            stockError = "Ingresa un número entero válido (0 o más)."
                        } else {
                            viewModel.updateStock(prod.id, s)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                ) {
                    Text("Guardar Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEditStockDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState.selectedSubOrderForDetail != null) {
        val selectedSub = uiState.selectedSubOrderForDetail!!
        val curProf = uiState.sellerProfile ?: profile
        SellerOrderDetailDialog(
            subOrder = selectedSub,
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
                    subOrderStatus = subOrder.status
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedTab == SellerTab.PRODUCTOS) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddProductDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                    text = { Text("Nuevo Producto") },
                    containerColor = Color(0xFF003366),
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    val curProf = uiState.sellerProfile ?: profile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSellerProfile = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        ValleGoUserAvatar(
                            avatarUrl = curProf.avatarUrl,
                            name = curProf.businessName ?: curProf.fullName,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val storeDisplayName = (curProf.businessName?.takeIf { it.isNotBlank() } ?: curProf.fullName).ifBlank { "Mi Emprendimiento" }
                            Text(
                                text = storeDisplayName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF003366),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = listOfNotNull(
                                curProf.fullName.takeIf { it.isNotBlank() && it != curProf.businessName },
                                curProf.businessLocation?.takeIf { it.isNotBlank() } ?: "Campus ${curProf.campus}"
                            ).joinToString(" • ").ifBlank { "Emprendedor Universitario" }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    val activeSubOrders = remember(uiState.subOrders) {
                        uiState.subOrders.filter { !it.status.isFinal }
                    }
                    IconButton(onClick = { showActiveChatsSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (unreadChatCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444), // Rojo para indicar mensajes no leídos
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadChatCount > 9) "+9" else "$unreadChatCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = "Chats Activos de Pedidos",
                                tint = if (unreadChatCount > 0) Color(0xFFEF4444) else Color(0xFF003366)
                            )
                        }
                    }
                }
            )
        },
        modifier = if (backgroundBlurRadius > 0.dp) modifier.blur(backgroundBlurRadius) else modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Estado del Puesto (Abierto/Cerrado)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isAcceptingOrders) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (uiState.isAcceptingOrders) "Puesto Abierto" else "Puesto Cerrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isAcceptingOrders) Color(0xFF2E7D32) else Color(0xFFC8102E)
                        )
                        Text(
                            text = if (uiState.isAcceptingOrders) "Aceptando subpedidos en campus" else "No visible en catálogo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isAcceptingOrders,
                        onCheckedChange = { viewModel.toggleAcceptingOrders(it) }
                    )
                }
            }

            // Selector de Pestañas: Pedidos, Menú y Estadísticas
            val currentTabIndex = when (uiState.selectedTab) {
                SellerTab.PEDIDOS -> 0
                SellerTab.PRODUCTOS -> 1
                SellerTab.ESTADISTICAS -> 2
                else -> 0
            }
            PrimaryTabRow(
                selectedTabIndex = currentTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF003366)
            ) {
                Tab(
                    selected = uiState.selectedTab == SellerTab.PEDIDOS,
                    onClick = { viewModel.setSelectedTab(SellerTab.PEDIDOS) },
                    text = { Text("Pedidos (${uiState.totalSubOrdersToday})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == SellerTab.PRODUCTOS,
                    onClick = { viewModel.setSelectedTab(SellerTab.PRODUCTOS) },
                    text = { Text("Menú (${uiState.products.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == SellerTab.ESTADISTICAS,
                    onClick = { viewModel.setSelectedTab(SellerTab.ESTADISTICAS) },
                    text = { Text("Estadísticas", fontWeight = FontWeight.Bold) }
                )
            }

            when (uiState.selectedTab) {
                SellerTab.PEDIDOS -> {
                    val todayFormattedDate = remember {
                        val today = LocalDate.now(ZoneId.of("America/Lima"))
                        val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                            .replaceFirstChar { it.uppercase() }
                        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
                        "$dayName, ${today.dayOfMonth} de $monthName"
                    }

                    // Indicador de Jornada de Hoy
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_alarm_custom),
                            contentDescription = null,
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Jornada de Hoy • $todayFormattedDate",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF003366)
                        )
                    }

                    // Resumen de Métricas / KPIs del Día
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricSummaryCard(
                            title = "Ganancias Hoy",
                            value = "S/ %.2f".format(uiState.earningsToday),
                            color = Color(0xFF003366),
                            modifier = Modifier.weight(1.3f)
                        )
                        MetricSummaryCard(
                            title = "Pendientes",
                            value = "${uiState.pendingCount}",
                            color = Color(0xFFF57C00),
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "En prep.",
                            value = "${uiState.inPreparationCount}",
                            color = Color(0xFF1976D2),
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Listos",
                            value = "${uiState.readyCount}",
                            color = Color(0xFF2E7D32),
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
                                SellerOrderFilter.TODOS -> "Hoy (${uiState.totalSubOrdersToday})"
                                SellerOrderFilter.PENDIENTES -> "Pendientes (${uiState.pendingCount})"
                                SellerOrderFilter.EN_PREPARACION -> "En Prep. (${uiState.inPreparationCount})"
                                SellerOrderFilter.LISTOS -> "Listos (${uiState.readyCount})"
                                SellerOrderFilter.COMPLETADOS -> "Entregados Hoy (${uiState.completedCount})"
                                SellerOrderFilter.RECHAZADOS -> "Rechazados"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF003366),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Lista de Subpedidos de Hoy + Historial Acordeón de Días Anteriores
                    val todayOrders = remember(uiState.todayOrders, uiState.selectedFilter) {
                        uiState.filteredTodayOrders
                    }
                    val pastDayGroups = remember(uiState.pastDayGroups, uiState.selectedFilter) {
                        if (uiState.selectedFilter == SellerOrderFilter.TODOS) uiState.pastDayGroups else emptyList()
                    }

                    if (todayOrders.isEmpty() && pastDayGroups.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                val emptyMsg = when (uiState.selectedFilter) {
                                    SellerOrderFilter.TODOS -> "No hay subpedidos registrados aún"
                                    SellerOrderFilter.PENDIENTES -> "No hay pedidos pendientes por responder"
                                    SellerOrderFilter.EN_PREPARACION -> "No tienes pedidos en preparación actualmente"
                                    SellerOrderFilter.LISTOS -> "No hay pedidos esperando entrega en este momento"
                                    SellerOrderFilter.COMPLETADOS -> "No hay pedidos entregados registrados"
                                    SellerOrderFilter.RECHAZADOS -> "No hay pedidos rechazados o cancelados"
                                }
                                Text(
                                    text = emptyMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Bloque: Pedidos de Hoy
                            if (todayOrders.isNotEmpty()) {
                                item(key = "header_today") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF2E7D32), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pedidos de Hoy",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF003366)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${todayOrders.size} pedidos",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(todayOrders, key = { it.id }) { subOrder ->
                                    SellerSubOrderCard(
                                        subOrder = subOrder,
                                        onAccept = { viewModel.acceptSubOrder(subOrder.id) },
                                        onStartPrep = { viewModel.startPreparation(subOrder.id) },
                                        onMarkReady = { viewModel.markReady(subOrder.id) },
                                        onOpenDelivery = { viewModel.openDeliveryDialog(subOrder) },
                                        onOpenRejection = { viewModel.openRejectionDialog(subOrder) },
                                        onOpenNoShow = { viewModel.openNoShowDialog(subOrder) },
                                        onExpired = { viewModel.onSubOrderExpired(subOrder.id) },
                                        onOpenDetail = { viewModel.openSubOrderDetail(subOrder) },
                                        onOpenChat = {
                                            activeChatSubOrder = subOrder
                                            chatViewModel.initChat(
                                                subOrderId = subOrder.id,
                                                currentUserId = profile.id,
                                                otherUserId = subOrder.buyerId ?: "",
                                                otherUserName = subOrder.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                                                meetingPoint = subOrder.meetingPointName ?: "Punto de entrega",
                                                subOrderStatus = subOrder.status
                                            )
                                        }
                                    )
                                }
                            } else {
                                item(key = "empty_today") {
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
                                                    text = "Sin pedidos para hoy en este filtro",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Ganancias de hoy: S/ %.2f".format(uiState.earningsToday),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Bloque: Historial de Días Anteriores (Acordeón)
                            if (pastDayGroups.isNotEmpty()) {
                                item(key = "header_past_days") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp, bottom = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = Color(0xFF003366),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Historial de Días Anteriores",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF003366)
                                        )
                                    }
                                }
                                items(pastDayGroups, key = { it.date }) { group ->
                                    val isExpanded = uiState.expandedPastDates.contains(group.date)
                                    SellerPastDayCard(
                                        group = group,
                                        isExpanded = isExpanded,
                                        onToggleExpand = { viewModel.togglePastDayExpanded(group.date) },
                                        onAccept = { viewModel.acceptSubOrder(it) },
                                        onStartPrep = { viewModel.startPreparation(it) },
                                        onMarkReady = { viewModel.markReady(it) },
                                        onOpenDelivery = { viewModel.openDeliveryDialog(it) },
                                        onOpenRejection = { viewModel.openRejectionDialog(it) },
                                        onOpenNoShow = { viewModel.openNoShowDialog(it) },
                                        onExpired = { viewModel.onSubOrderExpired(it) },
                                        onOpenDetail = { viewModel.openSubOrderDetail(it) },
                                        onOpenChat = { subOrder ->
                                            activeChatSubOrder = subOrder
                                            val curProf = uiState.sellerProfile ?: profile
                                            chatViewModel.initChat(
                                                subOrderId = subOrder.id,
                                                currentUserId = curProf.id,
                                                otherUserId = subOrder.buyerId ?: "",
                                                otherUserName = subOrder.buyerName?.ifBlank { "Comprador" } ?: "Comprador",
                                                meetingPoint = subOrder.meetingPointName ?: "Punto de entrega",
                                                subOrderStatus = subOrder.status
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                SellerTab.PRODUCTOS -> {
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
                            color = Color(0xFF003366)
                        )
                        Button(
                            onClick = { viewModel.openAddProductDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                                ) {
                                    Text("Publicar Primer Producto")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                else -> {}
            }
        }

        // Overlay elegante desenfocado / scrim para los diálogos emergentes
        AnimatedVisibility(
            visible = isAnyModalOpen,
            enter = fadeIn(androidx.compose.animation.core.tween(250)),
            exit = fadeOut(androidx.compose.animation.core.tween(200)),
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
                            onAccept = { onAccept(subOrder.id) },
                            onStartPrep = { onStartPrep(subOrder.id) },
                            onMarkReady = { onMarkReady(subOrder.id) },
                            onOpenDelivery = { onOpenDelivery(subOrder) },
                            onOpenRejection = { onOpenRejection(subOrder) },
                            onOpenNoShow = { onOpenNoShow(subOrder) },
                            onExpired = { onExpired(subOrder.id) },
                            onOpenDetail = { onOpenDetail(subOrder) },
                            onOpenChat = if (onOpenChat != null) { { onOpenChat(subOrder) } } else null
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: ID del subpedido, Temporizador de 15 min y Badge de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subpedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Total: S/ %.2f".format(subOrder.subtotalAmount),
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.ExtraBold
                    )
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

            // Punto de Entrega y Comprador (Banner Campus-Go)
            if (!subOrder.meetingPointName.isNullOrBlank() || !subOrder.buyerName.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onOpenDetail != null) Modifier.clickable { onOpenDetail() }
                            else Modifier
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_location_custom),
                                    contentDescription = null,
                                    tint = Color(0xFFC8102E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = subOrder.meetingPointName ?: "Punto de encuentro",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF003366)
                                )
                            }
                            if (!subOrder.scheduledTime.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_alarm_custom),
                                        contentDescription = null,
                                        tint = Color(0xFF003366),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = subOrder.scheduledTime,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        if (!subOrder.buyerName.isNullOrBlank()) {
                            Text(
                                text = "Comprador: ${subOrder.buyerName}" + if (!subOrder.buyerPhone.isNullOrBlank()) " (${subOrder.buyerPhone})" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Lista de Ítems del subpedido
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (subOrder.items.isEmpty()) {
                    Text(
                        text = "• 1x Subpedido Campus (S/ %.2f)".format(subOrder.subtotalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    subOrder.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "S/ %.2f".format(item.subtotal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Medio de pago y Botones de Acción según el estado actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (pmBg, pmTint, pmLabel) = when (subOrder.paymentMethod) {
                    PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                    PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                    PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), subOrder.paymentMethod?.name ?: "Efectivo")
                }
                Surface(
                    color = pmBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PaymentMethodLogo(method = subOrder.paymentMethod, size = 13.dp)
                        Text(
                            text = pmLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = pmTint
                        )
                    }
                }

                when (subOrder.status) {
                    SubOrderStatus.PENDIENTE -> {
                        if (isExpiredState) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_alarm_custom),
                                    contentDescription = null,
                                    tint = Color(0xFFC8102E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Cancelado automáticamente por tiempo agotado (15 min)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC8102E)
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onOpenChat != null) {
                                    FilledTonalIconButton(
                                        onClick = onOpenChat,
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = Color(0xFFE6F4EA),
                                            contentColor = Color(0xFF00A884)
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Chat con comprador", modifier = Modifier.size(18.dp))
                                    }
                                }
                                OutlinedButton(
                                    onClick = onOpenRejection,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Rechazar")
                                }
                                Button(
                                    onClick = onAccept,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Aceptar")
                                }
                            }
                        }
                    }
                    SubOrderStatus.ACEPTADO -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenChat != null) {
                                FilledTonalIconButton(
                                    onClick = onOpenChat,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFFE6F4EA),
                                        contentColor = Color(0xFF00A884)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Chat con comprador", modifier = Modifier.size(18.dp))
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenRejection,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onStartPrep,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Iniciar Preparación", fontSize = 12.sp)
                            }
                        }
                    }
                    SubOrderStatus.EN_PREPARACION -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenChat != null) {
                                FilledTonalIconButton(
                                    onClick = onOpenChat,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFFE6F4EA),
                                        contentColor = Color(0xFF00A884)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Chat con comprador", modifier = Modifier.size(18.dp))
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenRejection,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onMarkReady,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Marcar Listo", fontSize = 12.sp)
                            }
                        }
                    }
                    SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenChat != null) {
                                FilledTonalIconButton(
                                    onClick = onOpenChat,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFFE6F4EA),
                                        contentColor = Color(0xFF00A884)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Chat con comprador", modifier = Modifier.size(18.dp))
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenRejection,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Cancelar", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onOpenDelivery,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Confirmar Entrega", fontSize = 12.sp)
                            }
                        }
                    }
                    SubOrderStatus.COMPLETADO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Entregado y Cobrado",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            if (onOpenChat != null) {
                                FilledTonalIconButton(
                                    onClick = onOpenChat,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFFF1F5F9),
                                        contentColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Ver chat de pedido", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    SubOrderStatus.RECHAZADO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rechazado: ${subOrder.rejectionReason ?: "Sin motivo"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC8102E),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (onOpenChat != null) {
                                FilledTonalIconButton(
                                    onClick = onOpenChat,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFFF1F5F9),
                                        contentColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Ver chat de pedido", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    SubOrderStatus.NO_ENTREGADO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonOff,
                                    contentDescription = null,
                                    tint = Color(0xFFC8102E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "No entregado (Inasistencia)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC8102E)
                                )
                            }
                            if (onOpenChat != null) {
                                FilledTonalIconButton(
                                    onClick = onOpenChat,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFFF1F5F9),
                                        contentColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_chat_custom), contentDescription = "Ver chat de pedido", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = subOrder.status.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (onOpenDetail != null) {
                OutlinedButton(
                    onClick = onOpenDetail,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF003366)),
                    border = BorderStroke(1.dp, Color(0xFF003366).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Ver Detalle del Pedido", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: SubOrderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        SubOrderStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        SubOrderStatus.ACEPTADO -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Aceptado")
        SubOrderStatus.EN_PREPARACION -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "En Preparación")
        SubOrderStatus.LISTO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Listo para Entrega")
        SubOrderStatus.ESPERANDO_ENTREGA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Esperando Entrega")
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), "Completado")
        SubOrderStatus.RECHAZADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Rechazado")
        SubOrderStatus.CANCELADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Cancelado")
        SubOrderStatus.NO_ENTREGADO -> Triple(Color(0xFFECEFF1), Color(0xFF455A64), "No entregado")
    }

    Surface(
        color = backgroundColor,
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
fun ProductCard(
    product: Product,
    categoryName: String?,
    onToggleActive: (Boolean) -> Unit,
    onEditStock: () -> Unit,
    onEditProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            ValleGoProductImage(
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
                    val catTheme = com.example.vallego.ui.components.resolveCategoryVisualTheme(categoryName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = catTheme.iconResId),
                            contentDescription = null,
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF003366),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
                        color = Color(0xFF003366),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        onClick = onEditStock,
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOutOfStock) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOutOfStock) Color(0xFFC8102E).copy(alpha = 0.5f) else Color.LightGray
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
                                color = if (isOutOfStock) Color(0xFFC8102E) else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit_product_custom),
                                contentDescription = "Editar Stock",
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF003366)
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
                        tint = Color(0xFF003366),
                        modifier = Modifier.size(18.dp)
                    )
                }

                val statusText = when {
                    isOutOfStock -> "Sin stock"
                    product.isActive -> "Activo"
                    else -> "Pausado"
                }
                val statusColor = when {
                    isOutOfStock -> Color(0xFFC8102E)
                    product.isActive -> Color(0xFF2E7D32)
                    else -> Color(0xFFD97706)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerStoreProfileScreen(
    profile: UserProfile,
    sellerProfile: UserProfile?,
    availableMeetingPoints: List<CampusMeetingPoint> = emptyList(),
    isSaving: Boolean,
    isUploading: Boolean = false,
    onNavigateBack: () -> Unit,
    onUploadAsset: ((bucket: String, path: String, bytes: ByteArray, onUploaded: (String) -> Unit) -> Unit)? = null,
    onSave: (
        businessName: String,
        businessStatus: String,
        businessDescription: String?,
        businessCategory: String?,
        businessLocation: String?,
        openTime: String?,
        closeTime: String?,
        bannerUrl: String?,
        avatarUrl: String?,
        acceptingOrders: Boolean,
        supportedMeetingPoints: List<String>,
        supportedPaymentMethods: List<String>
    ) -> Unit,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile = sellerProfile ?: profile
    var isEditMode by remember { mutableStateOf(false) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var enlargedPhotoTitle by remember { mutableStateOf("") }
    var enlargedPhotoRole by remember { mutableStateOf("") }
    var isEnlargedBanner by remember { mutableStateOf(false) }

    // Manejo de retroceso nativo de Android en el perfil del vendedor
    BackHandler(enabled = showEnlargedPhoto) {
        showEnlargedPhoto = false
    }
    BackHandler(enabled = !showEnlargedPhoto && isEditMode) {
        isEditMode = false
    }
    BackHandler(enabled = !showEnlargedPhoto && !isEditMode) {
        onNavigateBack()
    }

    var selectedMeetingPoints by remember(activeProfile.id, activeProfile.supportedMeetingPoints) {
        mutableStateOf(activeProfile.supportedMeetingPoints.toSet())
    }
    var selectedPaymentMethods by remember(activeProfile.id, activeProfile.supportedPaymentMethods) {
        mutableStateOf(activeProfile.effectivePaymentMethods.toSet())
    }

    var businessName by remember(activeProfile.id, activeProfile.businessName) {
        mutableStateOf(activeProfile.businessName ?: activeProfile.fullName)
    }
    var description by remember(activeProfile.id, activeProfile.businessDescription) {
        mutableStateOf(activeProfile.businessDescription.orEmpty())
    }
    var category by remember(activeProfile.id, activeProfile.businessCategory) {
        mutableStateOf(activeProfile.businessCategory.orEmpty())
    }
    var location by remember(activeProfile.id, activeProfile.businessLocation) {
        mutableStateOf(activeProfile.businessLocation ?: activeProfile.campus)
    }
    var openTime by remember(activeProfile.id, activeProfile.openTime) {
        mutableStateOf(activeProfile.openTime ?: "08:00")
    }
    var closeTime by remember(activeProfile.id, activeProfile.closeTime) {
        mutableStateOf(activeProfile.closeTime ?: "18:00")
    }
    var bannerUrl by remember(activeProfile.id, activeProfile.bannerUrl) {
        mutableStateOf(activeProfile.bannerUrl.orEmpty())
    }
    var avatarUrl by remember(activeProfile.id, activeProfile.avatarUrl) {
        mutableStateOf(activeProfile.avatarUrl.orEmpty())
    }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var businessStatus by remember(activeProfile.id, activeProfile.businessStatus) {
        mutableStateOf(activeProfile.businessStatus.ifBlank { "ABIERTO" })
    }
    var acceptingOrders by remember(activeProfile.id, activeProfile.acceptingOrders) {
        mutableStateOf(activeProfile.acceptingOrders)
    }

    fun resetFields() {
        businessName = activeProfile.businessName ?: activeProfile.fullName
        description = activeProfile.businessDescription.orEmpty()
        category = activeProfile.businessCategory.orEmpty()
        location = activeProfile.businessLocation ?: activeProfile.campus
        openTime = activeProfile.openTime ?: "08:00"
        closeTime = activeProfile.closeTime ?: "18:00"
        bannerUrl = activeProfile.bannerUrl.orEmpty()
        avatarUrl = activeProfile.avatarUrl.orEmpty()
        businessStatus = activeProfile.businessStatus.ifBlank { "ABIERTO" }
        acceptingOrders = activeProfile.acceptingOrders
        selectedMeetingPoints = activeProfile.supportedMeetingPoints.toSet()
        selectedPaymentMethods = activeProfile.effectivePaymentMethods.toSet()
        isEditMode = false
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 1200, quality = 82)
            if (bytes != null && onUploadAsset != null) {
                isUploadingBanner = true
                val path = "banners/banner_${activeProfile.id}_${System.currentTimeMillis()}.jpg"
                onUploadAsset("business-assets", path, bytes) { uploadedUrl ->
                    bannerUrl = uploadedUrl
                    isUploadingBanner = false
                }
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 512, quality = 85)
            if (bytes != null && onUploadAsset != null) {
                isUploadingAvatar = true
                val path = "avatars/avatar_${activeProfile.id}_${System.currentTimeMillis()}.jpg"
                onUploadAsset("business-assets", path, bytes) { uploadedUrl ->
                    avatarUrl = uploadedUrl
                    isUploadingAvatar = false
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
                        text = if (isEditMode) "Editar Mi Puesto" else "Mi Puesto Comercial",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            resetFields()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF003366)
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        FilledTonalButton(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE3F2FD),
                                contentColor = Color(0xFF0284C7)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_edit_store_custom), contentDescription = null, modifier = Modifier.size(16.dp))
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
                                onSave(
                                    businessName,
                                    businessStatus,
                                    description,
                                    category,
                                    location,
                                    openTime,
                                    closeTime,
                                    bannerUrl,
                                    avatarUrl,
                                    acceptingOrders,
                                    selectedMeetingPoints.toList(),
                                    selectedPaymentMethods.toList()
                                )
                                isEditMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSaving && !isUploadingBanner && !isUploadingAvatar,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardando...")
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { resetFields() },
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
                                painter = painterResource(id = R.drawable.ic_logout_custom),
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera: Portada y Avatar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .then(
                                if (isEditMode) Modifier.clickable { bannerPickerLauncher.launch("image/*") }
                                else Modifier.clickable {
                                    enlargedPhotoUrl = bannerUrl.takeIf { it.isNotBlank() }
                                    enlargedPhotoTitle = businessName.ifBlank { activeProfile.fullName }
                                    enlargedPhotoRole = "Banner del Puesto"
                                    isEnlargedBanner = true
                                    showEnlargedPhoto = true
                                }
                            )
                    ) {
                        ValleGoBusinessBanner(
                            bannerUrl = bannerUrl.takeIf { it.isNotBlank() },
                            storeName = businessName,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isEditMode) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isUploadingBanner) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("Subiendo...", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Text("Cambiar portada", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Logo / Avatar
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 80.dp)
                            .then(
                                if (isEditMode) Modifier.clickable { avatarPickerLauncher.launch("image/*") }
                                else Modifier.clip(CircleShape).clickable {
                                    enlargedPhotoUrl = avatarUrl.takeIf { it.isNotBlank() }
                                    enlargedPhotoTitle = businessName.ifBlank { activeProfile.fullName }
                                    enlargedPhotoRole = "Emprendedor Universitario • Campus ${activeProfile.campus}"
                                    isEnlargedBanner = false
                                    showEnlargedPhoto = true
                                }
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(3.5.dp, Color.White),
                            shadowElevation = 4.dp
                        ) {
                            ValleGoBusinessAvatar(
                                avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                                storeName = businessName,
                                size = 76.dp
                            )
                        }
                        if (isEditMode) {
                            Surface(
                                color = Color(0xFF003366),
                                shape = CircleShape,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploadingAvatar) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar logo", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = businessName.ifBlank { "Nombre del Puesto" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                            Text(
                                text = "Responsable: ${activeProfile.fullName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Badge Emprendedor Autorizado
                            Surface(
                                color = Color(0xFFE6F6F3),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_authorized_seller_custom),
                                        contentDescription = null,
                                        tint = Color(0xFF0D5C4C),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Emprendedor Autorizado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0D5C4C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Calificación promedio del vendedor
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "%.1f".format(activeProfile.ratingAverage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                                Text(
                                    text = "Calificación de clientes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        StoreStatusBadge(status = businessStatus, acceptingOrders = acceptingOrders)
                    }
                }
            }

            if (!isEditMode) {
                // MODO LECTURA
                if (businessStatus == "SATURADO") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Modo Saturado activo: Tus clientes ven un aviso de alta demanda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else if (businessStatus == "PAUSADO") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Modo Pausado: Las compras están deshabilitadas temporalmente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else if (businessStatus == "CERRADO") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Puesto Cerrado: No visible para pedidos en catálogo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC8102E),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "INFORMACIÓN DEL PUESTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        SellerProfileDetailRow(
                            label = "Descripción",
                            value = description.ifBlank { "Sin descripción detallada registrada." }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        SellerProfileDetailRow(
                            label = "Calificación promedio",
                            value = "⭐ %.1f de 5.0 estrellas".format(activeProfile.ratingAverage)
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        SellerProfileDetailRow(
                            label = "Giro comercial / Categoría",
                            value = category.ifBlank { "Comidas / Varios" }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        SellerProfileDetailRow(
                            label = "Ubicación en campus",
                            value = location.ifBlank { "Campus ${activeProfile.campus}" }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        SellerProfileDetailRow(
                            label = "Horario de atención",
                            value = "$openTime - $closeTime"
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        SellerProfileDetailRow(
                            label = "Recepción de pedidos",
                            value = if (acceptingOrders) "Aceptando pedidos activamente" else "Pedidos desactivados"
                        )
                    }
                }

                // Puntos de Entrega Habilitados (Modo Lectura)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PUNTOS DE ENTREGA HABILITADOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${selectedMeetingPoints.size} seleccionados",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884)
                            )
                        }

                        if (selectedMeetingPoints.isEmpty()) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No has seleccionado puntos de entrega. Tus compradores no podrán programar entregas hasta que habilites al menos un punto.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedMeetingPoints.forEach { pointId ->
                                    val pt = availableMeetingPoints.find { it.id == pointId }
                                    val label = pt?.name ?: pointId
                                    val isExt = pt?.zoneType == "EXTERIOR"
                                    Surface(
                                        color = if (isExt) Color(0xFFE8F5E9) else Color(0xFFEDE7F6),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isExt) Color(0xFF81C784) else Color(0xFFB39DDB))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_location_custom),
                                                contentDescription = null,
                                                tint = if (isExt) Color(0xFF2E7D32) else Color(0xFF512DA8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isExt) Color(0xFF1B5E20) else Color(0xFF311B92)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Métodos de Pago Aceptados (Modo Lectura)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MÉTODOS DE PAGO ACEPTADOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${selectedPaymentMethods.size} activos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val availableMethods = listOf("EFECTIVO", "YAPE", "PLIN")
                            selectedPaymentMethods.sortedBy { availableMethods.indexOf(it) }.forEach { method ->
                                val (bg, tint, label) = when (method) {
                                    "YAPE" -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                                    "PLIN" -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                                    "EFECTIVO" -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), method)
                                }
                                Surface(
                                    color = bg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        PaymentMethodLogoByName(name = method, size = 14.dp)
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = tint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // MODO EDICIÓN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ESTADO OPERATIVO DEL PUESTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val states = listOf(
                                "ABIERTO" to "Abierto",
                                "SATURADO" to "Saturado",
                                "PAUSADO" to "Pausado",
                                "CERRADO" to "Cerrado"
                            )
                            states.forEach { (statusKey, label) ->
                                val isSelected = businessStatus.equals(statusKey, ignoreCase = true)
                                OutlinedButton(
                                    onClick = {
                                        businessStatus = statusKey
                                        if (statusKey == "CERRADO") acceptingOrders = false
                                        if (statusKey == "ABIERTO" || statusKey == "SATURADO") acceptingOrders = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color(0xFF003366) else Color.Transparent,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        if (businessStatus == "SATURADO") {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Modo Saturado: Los compradores verán un aviso de alta demanda indicando que su pedido puede tardar un poco más.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Nombre Comercial del Puesto *") },
                            placeholder = { Text("Ej. El Rincón del Sabor Universitario") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción del Negocio") },
                            placeholder = { Text("Ej. Hamburguesas artesanales, triples y jugos") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Giro / Categoría") },
                                placeholder = { Text("Comidas / Snacks") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Ubicación") },
                                placeholder = { Text("Pabellón A / Cafetería") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = openTime,
                                onValueChange = { openTime = it },
                                label = { Text("Apertura") },
                                placeholder = { Text("08:00 AM") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = closeTime,
                                onValueChange = { closeTime = it },
                                label = { Text("Cierre") },
                                placeholder = { Text("06:00 PM") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Puntos de Entrega (Modo Edición con Selección)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PUNTOS DE ENTREGA AUTORIZADOS *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Marca únicamente los puntos donde realmente puedes entregar pedidos. Solo estos puntos aparecerán al comprador al realizar su pedido:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        if (availableMeetingPoints.isEmpty()) {
                            Text(
                                text = "Cargando puntos de encuentro...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableMeetingPoints.forEach { point ->
                                    val isChecked = selectedMeetingPoints.contains(point.id)
                                    val isExterior = point.zoneType == "EXTERIOR"
                                    Surface(
                                        color = if (isChecked) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChecked) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMeetingPoints = if (isChecked) {
                                                    selectedMeetingPoints - point.id
                                                } else {
                                                    selectedMeetingPoints + point.id
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedMeetingPoints = if (checked) {
                                                        selectedMeetingPoints + point.id
                                                    } else {
                                                        selectedMeetingPoints - point.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF003366))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = point.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Surface(
                                                        color = if (isExterior) Color(0xFFDCFCE7) else Color(0xFFF3E8FF),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isExterior) "Exterior" else "Interior",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isExterior) Color(0xFF15803D) else Color(0xFF7E22CE),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                val ptDesc = point.description
                                                if (!ptDesc.isNullOrBlank()) {
                                                    Text(
                                                        text = ptDesc,
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

                // Métodos de Pago Aceptados (Modo Edición con Selección)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "MÉTODOS DE PAGO ACEPTADOS *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF003366),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Selecciona qué formas de pago aceptas de tus clientes (debes mantener al menos una activa):",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        val paymentOptions = listOf(
                            Triple("EFECTIVO", "Efectivo", "Pago contra entrega al momento de recibir el pedido"),
                            Triple("YAPE", "Yape", "Transferencia móvil directa BCP"),
                            Triple("PLIN", "Plin", "Transferencia interbancaria (BBVA, Interbank, etc.)")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            paymentOptions.forEach { (code, label, desc) ->
                                val isChecked = selectedPaymentMethods.contains(code)
                                val (bgTint, textTint) = when (code) {
                                    "YAPE" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
                                    "PLIN" -> Color(0xFFE0F2F1) to Color(0xFF00796B)
                                    else -> Color(0xFFF1F5F9) to Color(0xFF003366)
                                }

                                Surface(
                                    color = if (isChecked) bgTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isChecked) textTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPaymentMethods = if (isChecked) {
                                                if (selectedPaymentMethods.size > 1) selectedPaymentMethods - code else selectedPaymentMethods
                                            } else {
                                                selectedPaymentMethods + code
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedPaymentMethods = if (checked) {
                                                    selectedPaymentMethods + code
                                                } else {
                                                    if (selectedPaymentMethods.size > 1) selectedPaymentMethods - code else selectedPaymentMethods
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = textTint)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                PaymentMethodLogoByName(name = code, size = 16.dp)
                                                Text(
                                                    text = label,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = textTint
                                                )
                                            }
                                            Text(
                                                text = desc,
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
    }

    if (showEnlargedPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = enlargedPhotoUrl,
            name = enlargedPhotoTitle,
            roleDescription = enlargedPhotoRole,
            isBanner = isEnlargedBanner,
            onDismiss = { showEnlargedPhoto = false }
        )
    }
}

@Composable
private fun SellerProfileDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
fun SellerOrderDetailDialog(
    subOrder: SubOrder,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onStartPrep: (String) -> Unit,
    onMarkReady: (String) -> Unit,
    onOpenDelivery: (SubOrder) -> Unit,
    onOpenRejection: (SubOrder) -> Unit,
    onOpenChat: ((SubOrder) -> Unit)? = null
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
                StatusBadge(status = subOrder.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tarjeta Destacada de Entrega Campus-Go
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
                            text = subOrder.meetingPointName ?: "Punto oficial de entrega",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!subOrder.scheduledTime.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF003366),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Hora de encuentro: ${subOrder.scheduledTime}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Datos del Comprador
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Datos del Cliente",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF003366)
                        )
                        Text(
                            text = "Comprador: ${subOrder.buyerName ?: "Estudiante Universitario"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!subOrder.buyerPhone.isNullOrBlank()) {
                            Text(
                                text = "Teléfono: ${subOrder.buyerPhone}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!subOrder.notes.isNullOrBlank()) {
                            Text(
                                text = "Instrucciones: ${subOrder.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onOpenChat != null) {
                    OutlinedButton(
                        onClick = { onOpenChat(subOrder) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (subOrder.status.isFinal) Color(0xFF64748B) else Color(0xFF00A884)
                        ),
                        border = BorderStroke(1.dp, if (subOrder.status.isFinal) Color(0xFFCBD5E1) else Color(0xFF00A884)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_custom),
                            contentDescription = null,
                            tint = if (subOrder.status.isFinal) Color(0xFF64748B) else Color(0xFF00A884),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (subOrder.status.isFinal) "Chat con comprador (Cerrado)" else "Chat con comprador",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Desglose de Productos
                Text(
                    text = "Productos solicitados:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF003366)
                )

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

                HorizontalDivider()

                // Total y Método de pago
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (pmBg, pmTint, pmLabel) = when (subOrder.paymentMethod) {
                        PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                        PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                        PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), "Efectivo")
                        else -> Triple(Color(0xFFF1F5F9), Color(0xFF003366), subOrder.paymentMethod?.name ?: "Efectivo")
                    }
                    Surface(
                        color = pmBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PaymentMethodLogo(method = subOrder.paymentMethod, size = 15.dp)
                            Text(pmLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = pmTint)
                        }
                    }
                    Text(
                        text = "Total: S/ %.2f".format(subOrder.subtotalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF003366)
                    )
                }
            }
        },
        confirmButton = {
            when (subOrder.status) {
                SubOrderStatus.PENDIENTE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenRejection(subOrder) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E))
                        ) {
                            Text("Rechazar")
                        }
                        Button(
                            onClick = { onAccept(subOrder.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Aceptar Pedido")
                        }
                    }
                }
                SubOrderStatus.ACEPTADO -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenRejection(subOrder)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E))
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { onStartPrep(subOrder.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                        ) {
                            Text("Iniciar Preparación")
                        }
                    }
                }
                SubOrderStatus.EN_PREPARACION -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenRejection(subOrder)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E))
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { onMarkReady(subOrder.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Marcar Listo")
                        }
                    }
                }
                SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenRejection(subOrder)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E))
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { onOpenDelivery(subOrder) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                        ) {
                            Text("Confirmar Entrega")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        },
        dismissButton = {
            if (subOrder.status == SubOrderStatus.PENDIENTE ||
                subOrder.status == SubOrderStatus.ACEPTADO ||
                subOrder.status == SubOrderStatus.EN_PREPARACION ||
                subOrder.status == SubOrderStatus.LISTO ||
                subOrder.status == SubOrderStatus.ESPERANDO_ENTREGA) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    )
}