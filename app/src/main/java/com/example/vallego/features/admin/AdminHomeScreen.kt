package com.example.vallego.features.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.vallego.ui.components.ValleGoDialogContainerColor
import com.example.vallego.ui.components.ValleGoDialogShape
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Group
import com.example.vallego.ui.components.ValleGoDialogTonalElevation
import com.example.vallego.ui.components.valleGoDialogStyle
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
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
import com.example.vallego.domain.model.ApplicationStatus
import com.example.vallego.domain.model.CampusDetailedMetrics
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.MetricsPeriod
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.ui.components.PaymentMethodLogoByName
import com.example.vallego.ui.components.StrikeBadge
import com.example.vallego.ui.components.ValleGoBusinessAvatar
import java.util.UUID
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    val isAnyModalOpen = uiState.showCreateMeetingPointDialog ||
            uiState.selectedApplicationForRejection != null ||
            uiState.selectedSellerForSuspension != null ||
            uiState.selectedBuyerForSuspension != null ||
            uiState.selectedUserForWarning != null ||
            uiState.pointToDelete != null

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "admin_dialog_blur"
    )

    // Modal Crear Punto de Encuentro
    if (uiState.showCreateMeetingPointDialog) {
        var pointName by remember { mutableStateOf("") }
        var pavilion by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateMeetingPointDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Nuevo Punto de Encuentro Oficial", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Este punto de encuentro oficial estará disponible para la entrega de pedidos a los alumnos en el campus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = { Text("Nombre del Punto (ej. Biblioteca)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pavilion,
                        onValueChange = { pavilion = it },
                        label = { Text("Pabellón / Sector (ej. Pabellón C)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Referencia (ej. Frente a torniquetes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createMeetingPoint(pointName, pavilion, description)
                    },
                    enabled = pointName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                ) {
                    Text("Guardar Punto")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateMeetingPointDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Rechazar Solicitud de Vendedor
    if (uiState.selectedApplicationForRejection != null) {
        val application = uiState.selectedApplicationForRejection!!
        var reasonSelected by remember { mutableStateOf("Falta permiso de bienestar universitario") }
        val commonReasons = listOf(
            "Falta permiso de bienestar universitario",
            "Ubicación propuesta no autorizada",
            "Giro comercial saturado en este turno",
            "Información del estudiante incompleta",
            "Productos no autorizados para venta en campus"
        )

        AlertDialog(
            onDismissRequest = { viewModel.dismissRejectionDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Rechazar Solicitud", fontWeight = FontWeight.Bold, color = Color(0xFFC8102E))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Indica el motivo para informar al estudiante ${application.applicantName}:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    commonReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reasonSelected = reason }
                        ) {
                            RadioButton(
                                selected = reasonSelected == reason,
                                onClick = { reasonSelected = reason }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRejection(application.id, reasonSelected) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Confirmar Rechazo")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRejectionDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Suspender Puesto de Venta
    if (uiState.selectedSellerForSuspension != null) {
        val seller = uiState.selectedSellerForSuspension!!
        var suspensionReason by remember { mutableStateOf("Incumplimiento reiterado de entregas / horario") }
        val commonReasons = listOf(
            "Incumplimiento reiterado de entregas / horario",
            "Quejas de calidad o higiene de alimentos",
            "Venta de productos no autorizados por el campus",
            "Reclamos reiterados de cobro o precios indebidos"
        )

        AlertDialog(
            onDismissRequest = { viewModel.dismissSuspensionDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Suspender Puesto de Venta", fontWeight = FontWeight.Bold, color = Color(0xFFC8102E))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se pausarán las ventas de '${seller.displayStoreName}'. El puesto cambiará a 'SUSPENDIDO' y no recibirá pedidos hasta su reactivación.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    commonReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { suspensionReason = reason }
                        ) {
                            RadioButton(
                                selected = suspensionReason == reason,
                                onClick = { suspensionReason = reason }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSellerSuspension(seller.id, suspensionReason) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Confirmar Suspensión")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSuspensionDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Confirmar Eliminación de Punto de Encuentro
    if (uiState.pointToDelete != null) {
        val point = uiState.pointToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteMeetingPointDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Eliminar Punto de Encuentro", fontWeight = FontWeight.Bold, color = Color(0xFFC8102E))
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar permanentemente el punto '${point.name}'? Ya no aparecerá en el mapa de entregas de los estudiantes.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteMeetingPoint() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteMeetingPointDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Suspender Comprador
    if (uiState.selectedBuyerForSuspension != null) {
        val buyer = uiState.selectedBuyerForSuspension!!
        var suspensionReason by remember { mutableStateOf("Incumplimiento reiterado de recojo de pedidos") }
        val commonBuyerReasons = listOf(
            "Incumplimiento reiterado de recojo de pedidos",
            "Falta de respeto o conducta indebida en el campus",
            "Reclamos reiterados por parte de vendedores",
            "Incumplimiento de pagos o comprobantes inválidos",
            "Uso indebido de la plataforma"
        )

        AlertDialog(
            onDismissRequest = { viewModel.dismissBuyerSuspensionDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Text("Suspender Cuenta de Comprador", fontWeight = FontWeight.Bold, color = Color(0xFFC8102E))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se suspenderá la cuenta del estudiante '${buyer.fullName}'. El usuario no podrá realizar pedidos hasta que un administrador reactive su acceso.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    commonBuyerReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { suspensionReason = reason }
                        ) {
                            RadioButton(
                                selected = suspensionReason == reason,
                                onClick = { suspensionReason = reason }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmBuyerSuspension(buyer.id, suspensionReason) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8102E))
                ) {
                    Text("Confirmar Suspensión")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBuyerSuspensionDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Llamar la Atención (Vendedor o Comprador)
    if (uiState.selectedUserForWarning != null) {
        val targetUser = uiState.selectedUserForWarning!!
        val isSeller = targetUser.role == UserRole.EMPRENDEDOR || targetUser.role == UserRole.SUSPENDED
        var reasonSelected by remember {
            mutableStateOf(
                if (isSeller) "Incumplimiento de horario o entrega acordada"
                else "No se presentó a recoger el pedido al punto de encuentro"
            )
        }
        var customReason by remember { mutableStateOf("") }
        var isCustom by remember { mutableStateOf(false) }

        val commonReasons = if (isSeller) {
            listOf(
                "Incumplimiento de horario o entrega acordada",
                "Producto no coincide con la descripción o calidad",
                "Cobro o precio indebido fuera de la plataforma",
                "Falta de respeto o trato inapropiado al comprador"
            )
        } else {
            listOf(
                "No se presentó a recoger el pedido al punto de encuentro",
                "Falta de respeto o trato indebido al vendedor",
                "Cancelaciones reiteradas injustificadas",
                "Incumplimiento de pago o falta de comprobante"
            )
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissWarningDialog() },
            shape = ValleGoDialogShape,
            containerColor = ValleGoDialogContainerColor,
            tonalElevation = ValleGoDialogTonalElevation,
            modifier = Modifier.valleGoDialogStyle(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Llamada de Atención",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Emitir llamada de atención oficial a: ${if (isSeller) targetUser.displayStoreName else targetUser.fullName}. Quedará registrada permanentemente en su historial disciplinario.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    commonReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reasonSelected = reason
                                    isCustom = false
                                }
                        ) {
                            RadioButton(
                                selected = !isCustom && reasonSelected == reason,
                                onClick = {
                                    reasonSelected = reason
                                    isCustom = false
                                }
                            )
                            Text(text = reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustom = true }
                    ) {
                        RadioButton(
                            selected = isCustom,
                            onClick = { isCustom = true }
                        )
                        Text(text = "Otro motivo personalizado...", style = MaterialTheme.typography.bodySmall)
                    }

                    if (isCustom) {
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            placeholder = { Text("Escribe el motivo detallado...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                val finalReason = if (isCustom) customReason.trim() else reasonSelected
                Button(
                    onClick = { viewModel.confirmIssueWarning(targetUser.id, finalReason, profile.id) },
                    enabled = finalReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Emitir Advertencia")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissWarningDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (backgroundBlurRadius > 0.dp) Modifier.blur(backgroundBlurRadius) else Modifier)
    ) {
        if (uiState.selectedSellerDetail != null) {
            AdminSellerDetailScreen(
                seller = uiState.selectedSellerDetail!!,
                products = uiState.sellerProducts,
                isLoadingProducts = uiState.isLoadingSellerProducts,
                sellerStats = uiState.sellerStats,
                warnings = uiState.userWarnings,
                onBack = { viewModel.closeSellerDetail() },
                onSuspend = { seller -> viewModel.openSuspensionDialog(seller) },
                onReactivate = { seller -> viewModel.reactivateSeller(seller.id) },
                onIssueWarning = { seller -> viewModel.openWarningDialog(seller) }
            )
        } else if (uiState.selectedBuyerDetail != null) {
            AdminBuyerDetailScreen(
                buyer = uiState.selectedBuyerDetail!!,
                buyerStats = uiState.buyerStats,
                isLoadingStats = uiState.isLoadingBuyerDetail,
                warnings = uiState.userWarnings,
                isLoadingWarnings = uiState.isLoadingWarnings,
                onBack = { viewModel.closeBuyerDetail() },
                onSuspend = { buyer -> viewModel.openBuyerSuspensionDialog(buyer) },
                onReactivate = { buyer -> viewModel.reactivateBuyer(buyer.id) },
                onIssueWarning = { buyer -> viewModel.openWarningDialog(buyer) }
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Panel de Administración",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003366)
                                )
                                Text(
                                    text = "Campus ${profile.campus} • ${profile.fullName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.refresh() },
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF003366)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Actualizar datos",
                                        tint = Color(0xFF003366)
                                    )
                                }
                            }
                            IconButton(onClick = onSignOut) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_logout_custom),
                                    contentDescription = "Cerrar sesión"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Contenido de la sección seleccionada (con espacio inferior para no solaparse con el Dock flotante)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
                    ) {
                        when (uiState.selectedTab) {
                            AdminTab.MEETING_POINTS -> {
                                MeetingPointsTabContent(
                                    meetingPoints = uiState.meetingPoints,
                                    onToggle = { point -> viewModel.toggleMeetingPoint(point.id, point.isActive) },
                                    onDelete = { point -> viewModel.openDeleteMeetingPointDialog(point) },
                                    onCreateClick = { viewModel.openCreateMeetingPointDialog() }
                                )
                            }
                            AdminTab.SELLER_APPLICATIONS -> {
                                SellerApplicationsTabContent(
                                    applications = uiState.filteredApplications,
                                    selectedFilter = uiState.applicationFilter,
                                    onFilterChange = { viewModel.onApplicationFilterChange(it) },
                                    onApprove = { app -> viewModel.approveApplication(app.id, profile.id) },
                                    onReject = { app -> viewModel.openRejectionDialog(app) },
                                    incidents = uiState.filteredIncidents,
                                    allIncidents = uiState.incidents,
                                    incidentFilter = uiState.incidentFilter,
                                    onIncidentFilterChange = { viewModel.onIncidentFilterChange(it) },
                                    sellers = uiState.sellers,
                                    buyers = uiState.buyers,
                                    onSelectSeller = { seller -> viewModel.onSelectSeller(seller) },
                                    onSelectBuyer = { buyer -> viewModel.onSelectBuyer(buyer) },
                                    onIssueWarningForIncident = { inc, user -> viewModel.openWarningDialogForIncident(inc, user) },
                                    onSuspendForIncident = { inc, user -> viewModel.openSuspensionDialogForIncident(inc, user) },
                                    onResolveIncident = { inc -> viewModel.resolveIncident(inc.id, "RESUELTO", "RESOLUCION_DIRECTA", "Resuelto por el administrador del campus.") }
                                )
                            }
                            AdminTab.SELLERS_DIRECTORY -> {
                                SellersDirectoryTabContent(
                                    sellers = uiState.filteredSellers,
                                    strikesMap = uiState.userStrikesMap,
                                    searchQuery = uiState.sellerSearchQuery,
                                    onSearchQueryChange = { viewModel.onSellerSearchQueryChange(it) },
                                    onSelectSeller = { seller -> viewModel.onSelectSeller(seller) },
                                    onSuspend = { seller -> viewModel.openSuspensionDialog(seller) },
                                    onReactivate = { seller -> viewModel.reactivateSeller(seller.id) },
                                    onIssueWarning = { seller -> viewModel.openWarningDialog(seller) }
                                )
                            }
                            AdminTab.BUYERS_DIRECTORY -> {
                                BuyersDirectoryTabContent(
                                    buyers = uiState.filteredBuyers,
                                    strikesMap = uiState.userStrikesMap,
                                    searchQuery = uiState.buyerSearchQuery,
                                    onSearchQueryChange = { viewModel.onBuyerSearchQueryChange(it) },
                                    onSelectBuyer = { buyer -> viewModel.onSelectBuyer(buyer) },
                                    onSuspend = { buyer -> viewModel.openBuyerSuspensionDialog(buyer) },
                                    onReactivate = { buyer -> viewModel.reactivateBuyer(buyer.id) },
                                    onIssueWarning = { buyer -> viewModel.openWarningDialog(buyer) }
                                )
                            }
                            AdminTab.CAMPUS_METRICS -> {
                                CampusMetricsTabContent(
                                    metrics = uiState.metrics,
                                    detailedMetrics = uiState.detailedMetrics,
                                    selectedPeriod = uiState.selectedMetricsPeriod,
                                    isLoadingMetrics = uiState.isLoadingMetrics,
                                    onPeriodSelect = { viewModel.setMetricsPeriod(it) },
                                    onSelectSeller = { seller -> viewModel.onSelectSeller(seller) },
                                    sellers = uiState.sellers,
                                    incidents = uiState.incidents
                                )
                            }
                        }
                    }

                    // Barra de Navegación Dock Liquid Glass flotante en la parte inferior
                    AdminLiquidGlassDock(
                        selectedTab = uiState.selectedTab,
                        pendingApplicationsCount = uiState.metrics.pendingApplicationsCount + uiState.incidents.count { it.isPending },
                        onSelectTab = { viewModel.setTab(it) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
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
fun MeetingPointsTabContent(
    meetingPoints: List<CampusMeetingPoint>,
    onToggle: (CampusMeetingPoint) -> Unit,
    onDelete: (CampusMeetingPoint) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Puntos Oficiales del Campus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Puntos oficiales habilitados para las entregas a los alumnos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo")
            }
        }

        if (meetingPoints.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_custom),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No hay puntos de encuentro configurados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(meetingPoints, key = { it.id }) { point ->
                    CampusMeetingPointItemCard(
                        point = point,
                        onToggle = { onToggle(point) },
                        onDelete = { onDelete(point) }
                    )
                }
            }
        }
    }
}

@Composable
fun CampusMeetingPointItemCard(
    point: CampusMeetingPoint,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (point.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (point.isActive) Color(0xFF003366).copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_custom),
                        contentDescription = null,
                        tint = if (point.isActive) Color(0xFF003366) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = point.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    point.pavilion?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location_custom),
                                contentDescription = null,
                                tint = Color(0xFF003366),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF003366),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    point.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar punto",
                        tint = Color(0xFFC8102E).copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Switch(
                    checked = point.isActive,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@Composable
fun SellerApplicationsTabContent(
    applications: List<SellerApplication>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onApprove: (SellerApplication) -> Unit,
    onReject: (SellerApplication) -> Unit,
    incidents: List<OrderIncident> = emptyList(),
    allIncidents: List<OrderIncident> = emptyList(),
    incidentFilter: String = "TODAS",
    onIncidentFilterChange: (String) -> Unit = {},
    sellers: List<UserProfile> = emptyList(),
    buyers: List<UserProfile> = emptyList(),
    onSelectSeller: (UserProfile) -> Unit = {},
    onSelectBuyer: (UserProfile) -> Unit = {},
    onIssueWarningForIncident: (OrderIncident, UserProfile) -> Unit = { _, _ -> },
    onSuspendForIncident: (OrderIncident, UserProfile) -> Unit = { _, _ -> },
    onResolveIncident: (OrderIncident) -> Unit = {}
) {
    var activeSubSection by rememberSaveable { mutableStateOf(0) } // 0 = Solicitudes, 1 = Reportes e Incidencias
    val pendingIncidentsCount = remember(allIncidents) { allIncidents.count { it.isPending } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector superior entre Solicitudes de Puestos y Reportes e Incidencias
        PrimaryTabRow(
            selectedTabIndex = activeSubSection,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF003366)
        ) {
            Tab(
                selected = activeSubSection == 0,
                onClick = { activeSubSection = 0 },
                text = {
                    Text(
                        text = "Solicitudes (${applications.size})",
                        fontWeight = if (activeSubSection == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp
                    )
                }
            )
            Tab(
                selected = activeSubSection == 1,
                onClick = { activeSubSection = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Reportes (${allIncidents.size})",
                            fontWeight = if (activeSubSection == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                        if (pendingIncidentsCount > 0) {
                            Surface(
                                color = Color(0xFFC8102E),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$pendingIncidentsCount",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        if (activeSubSection == 0) {
            // Sección 0: Solicitudes de Vendedor
            Column {
                Text(
                    "Solicitudes de Vendedor",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF003366)
                )
                Text(
                    "Revisión y autorización de nuevos emprendedores en el campus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filtros de estado para Solicitudes
            val filterOptions = listOf(
                "TODAS" to "Todas",
                "PENDIENTE" to "Pendientes",
                "APROBADA" to "Aprobadas",
                "RECHAZADA" to "Rechazadas"
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterOptions.forEach { (key, label) ->
                    val isSelected = selectedFilter.equals(key, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF003366),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (applications.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when (selectedFilter.uppercase()) {
                                "PENDIENTE" -> "No hay solicitudes pendientes de aprobación"
                                "APROBADA" -> "No hay solicitudes aprobadas"
                                "RECHAZADA" -> "No hay solicitudes rechazadas"
                                else -> "No hay solicitudes de nuevos vendedores registradas"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(applications, key = { it.id }) { app ->
                        SellerApplicationCard(
                            application = app,
                            onApprove = { onApprove(app) },
                            onReject = { onReject(app) }
                        )
                    }
                }
            }
        } else {
            // Sección 1: Moderación de Reportes e Incidencias
            Column {
                Text(
                    "Centro de Reportes y Moderación",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF003366)
                )
                Text(
                    "Reclamos entre estudiantes, compradores y vendedores del campus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filtros de estado de Incidencias
            val incidentFilterOptions = listOf(
                "TODAS" to "Todas (${allIncidents.size})",
                "PENDIENTES" to "Pendientes (${allIncidents.count { it.isPending }})",
                "SANCIONADO" to "Sancionadas (${allIncidents.count { it.status.equals("SANCIONADO", ignoreCase = true) }})",
                "RESUELTO" to "Resueltas (${allIncidents.count { it.status.equals("RESUELTO", ignoreCase = true) }})"
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                incidentFilterOptions.forEach { (key, label) ->
                    val isSelected = incidentFilter.equals(key, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onIncidentFilterChange(key) },
                        label = { Text(label, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF003366),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (incidents.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF00A884)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when (incidentFilter.uppercase()) {
                                "PENDIENTES", "PENDIENTE" -> "¡Excelente! No hay reportes pendientes de moderación en el campus."
                                "SANCIONADO" -> "No hay sanciones registradas en este momento."
                                "RESUELTO" -> "No hay incidencias resueltas registradas."
                                else -> "No se han emitido reportes de incidencias aún."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(incidents, key = { it.id }) { incident ->
                        val reportedUser = sellers.find { it.id == incident.reportedUserId }
                            ?: buyers.find { it.id == incident.reportedUserId }
                        val reporterUser = sellers.find { it.id == incident.reporterId }
                            ?: buyers.find { it.id == incident.reporterId }

                        AdminIncidentCard(
                            incident = incident,
                            reportedUser = reportedUser,
                            reporterUser = reporterUser,
                            onSelectSeller = onSelectSeller,
                            onSelectBuyer = onSelectBuyer,
                            onIssueWarning = { user -> onIssueWarningForIncident(incident, user) },
                            onSuspend = { user -> onSuspendForIncident(incident, user) },
                            onResolve = { onResolveIncident(incident) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminIncidentCard(
    incident: OrderIncident,
    reportedUser: UserProfile?,
    reporterUser: UserProfile?,
    onSelectSeller: (UserProfile) -> Unit,
    onSelectBuyer: (UserProfile) -> Unit,
    onIssueWarning: (UserProfile) -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Encabezado: Estado y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusBg, statusFg, statusLabel) = when (incident.status.uppercase()) {
                    "PENDIENTE" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "PENDIENTE DE REVISIÓN")
                    "SANCIONADO" -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "SANCIONADO")
                    "RESUELTO" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "RESUELTO")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), incident.status)
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = incident.createdAt?.take(10) ?: "Fecha no disponible",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            // Título de la Infracción / Motivo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = if (incident.isPending) Color(0xFFE65100) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = incident.displayIncidentTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color(0xFF003366)
                )
            }

            if (!incident.subOrderId.isNullOrBlank()) {
                Text(
                    text = "Subpedido relacionado: #${incident.subOrderId.take(8).uppercase()}",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Datos del Usuario Reportado (Infractor / Acusado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "USUARIO REPORTADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    val reportedName = reportedUser?.businessName ?: reportedUser?.fullName ?: "ID: ${incident.reportedUserId?.take(8)}"
                    val isSeller = reportedUser?.role == com.example.vallego.domain.model.UserRole.EMPRENDEDOR ||
                            reportedUser?.role == com.example.vallego.domain.model.UserRole.SUSPENDED
                    Text(
                        text = reportedName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (isSeller) "Rol: Vendedor / Puesto Comercial" else "Rol: Estudiante / Comprador",
                        fontSize = 11.sp,
                        color = if (isSeller) Color(0xFF00A884) else Color(0xFF003366)
                    )
                }

                if (reportedUser != null) {
                    val isSeller = reportedUser.role == com.example.vallego.domain.model.UserRole.EMPRENDEDOR ||
                            reportedUser.role == com.example.vallego.domain.model.UserRole.SUSPENDED
                    OutlinedButton(
                        onClick = {
                            if (isSeller) onSelectSeller(reportedUser) else onSelectBuyer(reportedUser)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(1.dp, Color(0xFF003366))
                    ) {
                        Text("Ver Perfil", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003366))
                    }
                }
            }

            // Datos del Denunciante
            Column {
                Text(
                    text = "DENUNCIANTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                val reporterName = reporterUser?.businessName ?: reporterUser?.fullName ?: "Usuario #${incident.reporterId?.take(8) ?: "Anónimo"}"
                Text(
                    text = reporterName,
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569)
                )
            }

            // Detalle o testimonio del reclamo
            if (!incident.details.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Detalle del reporte:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = incident.details,
                            fontSize = 12.5.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Si ya está resuelto o sancionado, mostrar resolución del administrador
            if (!incident.isPending) {
                Surface(
                    color = if (incident.status == "SANCIONADO") Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Resolución del Administrador:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (incident.status == "SANCIONADO") Color(0xFFC8102E) else Color(0xFF2E7D32)
                        )
                        if (!incident.resolutionAction.isNullOrBlank()) {
                            Text(
                                text = "Acción: ${incident.resolutionAction}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!incident.adminNotes.isNullOrBlank()) {
                            Text(
                                text = "Nota: ${incident.adminNotes}",
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Botones de Acción para el Administrador (Solo si está PENDIENTE)
            if (incident.isPending) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reportedUser != null) {
                        // Botón 1: Llamar la atención
                        Button(
                            onClick = { onIssueWarning(reportedUser) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning_custom),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Llamar atención", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botón 2: Suspender Cuenta
                        OutlinedButton(
                            onClick = { onSuspend(reportedUser) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                            border = BorderStroke(1.dp, Color(0xFFC8102E)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Suspender", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón 3: Resolver sin sanción
                    OutlinedButton(
                        onClick = onResolve,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00A884)),
                        border = BorderStroke(1.dp, Color(0xFF00A884)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = if (reportedUser == null) Modifier.fillMaxWidth().height(38.dp) else Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Resolver", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SellerApplicationCard(
    application: SellerApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.storeName.ifBlank { "Nuevo Emprendimiento" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )
                    Text(
                        text = "Rubro: ${application.category.ifBlank { "General" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                ApplicationStatusBadge(status = application.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Postulante: ${application.applicantName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (application.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Contacto: ${application.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!application.proposedLocation.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ubicación propuesta: ${application.proposedLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (application.description.isNotBlank()) {
                    Text(
                        text = "Descripción: ${application.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (application.status == ApplicationStatus.RECHAZADA && !application.rejectionReason.isNullOrBlank()) {
                    Text(
                        text = "Motivo de rechazo: ${application.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC8102E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (application.status == ApplicationStatus.PENDIENTE) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rechazar")
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aprobar")
                    }
                }
            }
        }
    }
}

@Composable
fun SellersDirectoryTabContent(
    sellers: List<UserProfile>,
    strikesMap: Map<String, Int> = emptyMap(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectSeller: (UserProfile) -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onReactivate: (UserProfile) -> Unit,
    onIssueWarning: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Puestos del Campus (${sellers.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Gestión, auditoría y seguimiento detallado a pantalla completa de puestos universitarios", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Barra de búsqueda con icono y botón para limpiar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por puesto, titular o rubro...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFF003366))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (sellers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_store_custom),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron puestos que coincidan con '$searchQuery'" else "No hay emprendedores registrados en este campus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(sellers, key = { it.id }) { seller ->
                    SellerDirectoryCard(
                        seller = seller,
                        strikes = strikesMap[seller.id] ?: 0,
                        onSelectSeller = { onSelectSeller(seller) },
                        onSuspend = { onSuspend(seller) },
                        onReactivate = { onReactivate(seller) },
                        onIssueWarning = { onIssueWarning(seller) }
                    )
                }
            }
        }
    }
}

@Composable
fun SellerDirectoryCard(
    seller: UserProfile,
    strikes: Int = 0,
    onSelectSeller: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onIssueWarning: () -> Unit
) {
    val isSuspended = seller.role == UserRole.SUSPENDED

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuspended) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectSeller)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ValleGoBusinessAvatar(
                    avatarUrl = seller.avatarUrl,
                    storeName = seller.displayStoreName,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seller.displayStoreName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSuspended) Color(0xFFC8102E) else Color(0xFF003366)
                    )
                    Text(
                        text = "Titular: ${seller.fullName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    seller.businessCategory?.takeIf { it.isNotBlank() }?.let { cat ->
                        Text(
                            text = "Rubro: $cat",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00897B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = if (isSuspended) Color(0xFFC8102E) else if (seller.acceptingOrders) Color(0xFF2E7D32) else Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSuspended) "SUSPENDIDO" else if (seller.acceptingOrders) "ABIERTO" else "CERRADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    StrikeBadge(strikes = strikes, showAutoSuspensionLabel = true)
                }
            }

            if (isSuspended && !seller.suspensionReason.isNullOrBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Motivo de suspensión: ${seller.suspensionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC8102E),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_custom),
                        contentDescription = null,
                        tint = Color(0xFF003366),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = seller.businessLocation ?: seller.campus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (strikes < 5) {
                        IconButton(
                            onClick = onIssueWarning,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Llamar la atención",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onSelectSeller,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Ver Detalle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }

                    if (isSuspended) {
                        Button(
                            onClick = onReactivate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Reactivar", fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSuspend,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Suspender", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuyersDirectoryTabContent(
    buyers: List<UserProfile>,
    strikesMap: Map<String, Int> = emptyMap(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectBuyer: (UserProfile) -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onReactivate: (UserProfile) -> Unit,
    onIssueWarning: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Compradores del Campus (${buyers.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Gestión, auditoría y seguimiento especializado de estudiantes compradores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Barra de búsqueda con icono y botón para limpiar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre, teléfono, código...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFF003366))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (buyers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron compradores que coincidan con '$searchQuery'" else "No hay compradores registrados en este campus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(buyers, key = { it.id }) { buyer ->
                    BuyerDirectoryCard(
                        buyer = buyer,
                        strikes = strikesMap[buyer.id] ?: 0,
                        onSelectBuyer = { onSelectBuyer(buyer) },
                        onSuspend = { onSuspend(buyer) },
                        onReactivate = { onReactivate(buyer) },
                        onIssueWarning = { onIssueWarning(buyer) }
                    )
                }
            }
        }
    }
}

@Composable
fun BuyerDirectoryCard(
    buyer: UserProfile,
    strikes: Int = 0,
    onSelectBuyer: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onIssueWarning: () -> Unit
) {
    val context = LocalContext.current
    val isSuspended = buyer.role == UserRole.SUSPENDED_BUYER || buyer.role == UserRole.SUSPENDED

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuspended) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectBuyer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ValleGoBusinessAvatar(
                    avatarUrl = buyer.avatarUrl,
                    storeName = buyer.fullName,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buyer.fullName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSuspended) Color(0xFFC8102E) else Color(0xFF003366)
                    )
                    Text(
                        text = "Campus: ${buyer.campus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!buyer.studentCode.isNullOrBlank()) {
                        Text(
                            text = "Cód: ${buyer.studentCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00897B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = if (isSuspended) Color(0xFFC8102E) else Color(0xFF2E7D32),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSuspended) "SUSPENDIDO" else "ACTIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    StrikeBadge(strikes = strikes, showAutoSuspensionLabel = true)
                }
            }

            if (isSuspended && !buyer.suspensionReason.isNullOrBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Motivo de suspensión: ${buyer.suspensionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC8102E),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Teléfono con accesos directos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF003366),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buyer.phone.takeIf { it.isNotBlank() } ?: "Sin teléfono",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (buyer.phone.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Llamar",
                            tint = Color(0xFF003366),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${buyer.phone.trim()}"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_custom),
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    try {
                                        val clean = buyer.phone.replace("+", "").replace(" ", "").trim()
                                        val phoneWithCountry = if (clean.startsWith("51")) clean else "51$clean"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phoneWithCountry"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (strikes < 5) {
                        IconButton(
                            onClick = onIssueWarning,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Llamar la atención",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onSelectBuyer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Ver Perfil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }

                    if (isSuspended) {
                        Button(
                            onClick = onReactivate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Reactivar", fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSuspend,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Suspender", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CampusMetricsTabContent(
    metrics: CampusMetrics,
    detailedMetrics: CampusDetailedMetrics,
    selectedPeriod: MetricsPeriod,
    isLoadingMetrics: Boolean,
    onPeriodSelect: (MetricsPeriod) -> Unit,
    onSelectSeller: (UserProfile) -> Unit,
    sellers: List<UserProfile>,
    incidents: List<OrderIncident>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Tarjeta Ejecutiva Principal
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF003366)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Centro de Analítica del Campus",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            color = Color(0xFF00A884).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "EN VIVO",
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Supervisión ejecutiva de ventas, pedidos, ranking de puestos y métodos de pago.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 2. Selector de Período Temporal
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Período de Análisis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricsPeriod.values().forEach { period ->
                        val isSelected = period == selectedPeriod
                        Surface(
                            onClick = { onPeriodSelect(period) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF003366) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                if (isLoadingMetrics) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color(0xFF003366)
                    )
                }
            }
        }

        // 3. Cuatro KPIs Principales (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminMetricCard(
                        title = "Ventas en Período",
                        value = "S/ %.2f".format(detailedMetrics.totalSales),
                        color = Color(0xFF003366),
                        subtitle = "${detailedMetrics.completedOrders} pedidos cobrados",
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Pedidos Totales",
                        value = "${detailedMetrics.totalOrders}",
                        color = Color(0xFF00A884),
                        subtitle = "${detailedMetrics.cancelledOrders} cancelados / no-show",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminMetricCard(
                        title = "Ticket Promedio",
                        value = "S/ %.2f".format(detailedMetrics.averageTicket),
                        color = Color(0xFF1976D2),
                        subtitle = "Gasto promedio por orden",
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Tasa de Éxito",
                        value = "%.1f%%".format(detailedMetrics.fulfillmentRate),
                        color = if (detailedMetrics.fulfillmentRate >= 80.0) Color(0xFF2E7D32) else Color(0xFFE65100),
                        subtitle = "Entregas efectivas",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Ranking de Ventas por Puesto / Vendedor
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ranking de Ventas por Puesto",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366)
                        )
                    }
                    Text(
                        text = "${detailedMetrics.sellerRankings.size} puestos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Toca cualquier puesto para ver su seguimiento detallado a pantalla completa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (detailedMetrics.sellerRankings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay registros de ventas para ${selectedPeriod.label.lowercase()}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(detailedMetrics.sellerRankings, key = { it.sellerId }) { rank ->
                val sellerProfile = sellers.find { it.id == rank.sellerId } ?: UserProfile(
                    id = rank.sellerId,
                    fullName = rank.ownerName,
                    businessName = rank.storeName,
                    avatarUrl = rank.avatarUrl,
                    role = UserRole.EMPRENDEDOR
                )
                val rankIndex = detailedMetrics.sellerRankings.indexOf(rank) + 1

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSeller(sellerProfile) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Medalla o posición
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (rankIndex) {
                                            1 -> Color(0xFFFFD700)
                                            2 -> Color(0xFFCFD8DC)
                                            3 -> Color(0xFFD7CCC8)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rankIndex",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (rankIndex == 1) Color(0xFF78350F) else Color(0xFF334155)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))

                            ValleGoBusinessAvatar(
                                avatarUrl = rank.avatarUrl,
                                storeName = rank.storeName,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rank.storeName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF003366),
                                    maxLines = 1
                                )
                                Text(
                                    text = rank.ownerName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "S/ %.2f".format(rank.totalSales),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF003366),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${rank.completedOrders} pedidos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00A884),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Ver detalle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Barra de progreso de participación de ventas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = { (rank.percentage.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF003366),
                                trackColor = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "%.1f%%".format(rank.percentage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                        }
                    }
                }
            }
        }

        // 5. Métodos de Pago
        if (detailedMetrics.paymentMethods.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Métodos de Pago Utilizados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detailedMetrics.paymentMethods.forEach { method ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PaymentMethodLogoByName(name = method.method, size = 22.dp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = method.method,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF003366)
                                    )
                                    Text(
                                        text = "S/ %.2f".format(method.totalAmount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF00A884)
                                    )
                                    Text(
                                        text = "${method.count} órdenes (%.0f%%)".format(method.percentage),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Puntos de Entrega Más Concurridos
        if (detailedMetrics.topMeetingPoints.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Puntos de Entrega con Mayor Tráfico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003366)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            detailedMetrics.topMeetingPoints.forEach { pt ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location_custom),
                                            contentDescription = null,
                                            tint = Color(0xFF003366),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pt.pointName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "${pt.count} entregas (%.0f%%)".format(pt.percentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF003366)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Resumen Operativo del Campus
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Operaciones en Campus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = "Puestos Activos",
                        value = "${metrics.activeSellersCount}",
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Puntos Activos",
                        value = "${metrics.activeMeetingPointsCount}",
                        color = Color(0xFFF57C00),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Postulaciones",
                        value = "${metrics.pendingApplicationsCount}",
                        color = Color(0xFFC8102E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 8. Registro de Incidencias
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = Color(0xFF003366),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registro de Incidencias (${incidents.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366)
                )
            }
        }

        if (incidents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sin incidencias reportadas en el campus.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(incidents, key = { it.id.ifBlank { UUID.randomUUID().toString() } }) { incident ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning_custom),
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (incident.incidentType) {
                                    "NO_SHOW_BUYER" -> "Comprador no se presentó (No-Show)"
                                    "CANCELADO_VENDEDOR" -> "Cancelado por el puesto"
                                    else -> incident.incidentType
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100)
                            )
                            incident.details?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
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

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    color: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ApplicationStatusBadge(status: ApplicationStatus) {
    val (bgColor, textColor, text) = when (status) {
        ApplicationStatus.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        ApplicationStatus.APROBADA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Aprobado")
        ApplicationStatus.RECHAZADA -> Triple(Color(0xFFFFEBEE), Color(0xFFC8102E), "Rechazado")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Estructura de datos para un ítem del Dock estilo React Bits
 */
private data class AdminDockItemData(
    val tab: AdminTab,
    val label: String,
    val iconResId: Int? = null,
    val iconVector: ImageVector? = null
)

/**
 * Dock Navigation Bar estilo React Bits con efecto Liquid Glass translúcido / blur para el Panel de Administración.
 * - Fondo de cristal líquido translúcido claro con reflejo especular en bordes y brillo satinado
 * - Ítems con animación Spring elástica física (magnificación y elevación)
 * - Sin etiqueta superior y sin números de notificación (diseño ultra limpio y minimalista)
 */
@Composable
fun AdminLiquidGlassDock(
    selectedTab: AdminTab,
    pendingApplicationsCount: Int = 0,
    onSelectTab: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            AdminDockItemData(
                tab = AdminTab.MEETING_POINTS,
                label = "Puntos",
                iconResId = R.drawable.ic_location_custom
            ),
            AdminDockItemData(
                tab = AdminTab.SELLER_APPLICATIONS,
                label = "Solicitudes",
                iconVector = Icons.Default.VerifiedUser
            ),
            AdminDockItemData(
                tab = AdminTab.SELLERS_DIRECTORY,
                label = "Puestos",
                iconResId = R.drawable.ic_store_custom
            ),
            AdminDockItemData(
                tab = AdminTab.BUYERS_DIRECTORY,
                label = "Alumnos",
                iconVector = Icons.Default.Group
            ),
            AdminDockItemData(
                tab = AdminTab.CAMPUS_METRICS,
                label = "Métricas",
                iconVector = Icons.AutoMirrored.Filled.TrendingUp
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Panel del Dock (React Bits .dock-panel) con efecto Liquid Glass translúcido / blur
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x33003366),
                    ambientColor = Color(0x1F000000)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    // Liquid Glass Translucent Frosted (vidrio líquido claro translúcido)
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xCCFFFFFF), // Cristal líquido frosted con alta transparencia
                            Color(0xAAFFFFFF)  // Base translúcida satinada
                        )
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xF0FFFFFF), // Reflejo especular blanco puro en borde superior
                            Color(0x80FFFFFF), // Difusión intermedia del cristal
                            Color(0x30FFFFFF)  // Borde inferior sutil
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            // Capa de brillo satinado del cristal líquido (Gloss Sheen)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x66FFFFFF),
                                Color(0x1AFFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Fila de ítems del Dock con espaciado elástico y altura cómoda para 5 ítems
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    AdminDockItem(
                        item = item,
                        isSelected = selectedTab == item.tab,
                        badgeCount = if (item.tab == AdminTab.SELLER_APPLICATIONS) pendingApplicationsCount else 0,
                        onClick = { onSelectTab(item.tab) }
                    )
                }
            }
        }
    }
}

/**
 * Ítem individual del Dock (React Bits <DockItem>) con animación de resorte (spring),
 * centrado vertical cómodo y feedback de cristal líquido translúcido.
 */
@Composable
private fun AdminDockItem(
    item: AdminDockItemData,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    // Spring physics para magnificación suave y balanceada
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dock_item_scale"
    )

    val itemWidth by animateDpAsState(
        targetValue = if (isSelected) 64.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dock_item_width"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(itemWidth)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x330284C7), // Cristal cian / azul activo
                            Color(0x1A003366)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x14000000), // Vidrio reposado suave
                            Color(0x08000000)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 1.3.dp else 0.8.dp,
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF0284C7), Color(0x66003366))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0x40FFFFFF), Color(0x15FFFFFF))
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
        ) {
            val iconTint = if (isSelected) Color(0xFF003366) else Color(0xFF475569)
            val iconModifier = Modifier.size(20.dp)

            Box(contentAlignment = Alignment.Center) {
                if (item.iconResId != null) {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = iconModifier
                    )
                } else if (item.iconVector != null) {
                    Icon(
                        imageVector = item.iconVector,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = iconModifier
                    )
                }

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFFC8102E), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            // Si está seleccionado, mostrar etiqueta compacta en color corporativo
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = Color(0xFF003366),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
