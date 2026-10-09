package com.example.campusgo.features.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.campusgo.R
import com.example.campusgo.ui.components.CampusGoDialogContainerColor
import com.example.campusgo.ui.components.CampusGoDialogShape
import com.example.campusgo.ui.components.CampusGoDialogTonalElevation
import com.example.campusgo.ui.components.campusGoDialogStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.campusgo.theme.ThemeManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.ui.components.CampusGoBusinessAvatar
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.repository.SupportRepository
import com.example.campusgo.features.admin.components.AdminLiquidGlassDock
import com.example.campusgo.features.admin.dialogs.*
import com.example.campusgo.features.admin.tabs.BuyersDirectoryTabContent
import com.example.campusgo.features.admin.tabs.CampusMetricsTabContent
import com.example.campusgo.features.admin.tabs.MeetingPointsTabContent
import com.example.campusgo.features.admin.tabs.SellerApplicationsTabContent
import com.example.campusgo.features.admin.tabs.SellersDirectoryTabContent
import com.example.campusgo.features.chat.SupportChatBottomSheet
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    pendingRoute: com.example.campusgo.core.notification.AppNotificationPayload? = null,
    onClearPendingRoute: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val supportRepository: SupportRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var activeSupportTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var showAdminProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(pendingRoute) {
        when (val route = pendingRoute) {
            is com.example.campusgo.core.notification.AppNotificationPayload.SupportChat -> {
                coroutineScope.launch {
                    val ticketRes = supportRepository.getTicketById(route.ticketId)
                    val ticket = ticketRes.getOrNull()
                    if (ticket != null) {
                        activeSupportTicket = ticket
                        onClearPendingRoute()
                    }
                }
            }
            is com.example.campusgo.core.notification.AppNotificationPayload.AdminIncident -> {
                viewModel.setTab(AdminTab.SELLER_APPLICATIONS)
                onClearPendingRoute()
            }
            else -> {}
        }
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

    val isAnyModalOpen = showAdminProfileDialog ||
            uiState.showCreateMeetingPointDialog ||
            uiState.selectedApplicationForApproval != null ||
            uiState.selectedApplicationForRejection != null ||
            uiState.showManageCategoriesDialog ||
            uiState.selectedSellerForSuspension != null ||
            uiState.selectedBuyerForSuspension != null ||
            uiState.selectedUserForWarning != null ||
            uiState.pointToDelete != null ||
            enlargedPhotoUrl != null

    activeSupportTicket?.let { ticket ->
        SupportChatBottomSheet(
            ticket = ticket,
            currentUserId = profile.id,
            isAdmin = true,
            onDismiss = { activeSupportTicket = null },
            onResolveTicket = {
                coroutineScope.launch {
                    supportRepository.resolveTicket(ticket.id, "Resuelto a través de mediación en chat.")
                    ticket.incidentId?.let { incId ->
                        viewModel.resolveIncident(
                            incidentId = incId,
                            status = "RESUELTO",
                            action = "CHAT_MEDIATION",
                            adminNotes = "Resuelto por el administrador del campus mediante chat de soporte."
                        )
                    }
                    activeSupportTicket = null
                }
            }
        )
        return
    }

    enlargedPhotoUrl?.let { url ->
        EnlargedPhotoViewerDialog(
            photoUrl = url,
            name = "Evidencia Fotográfica",
            roleDescription = "Reporte de Incidencia",
            title = "Evidencia Fotográfica",
            onDismiss = { enlargedPhotoUrl = null }
        )
    }

    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (isAnyModalOpen) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "admin_dialog_blur"
    )

    // Modales de Administración Delegados a Componentes Modulares
    if (uiState.showCreateMeetingPointDialog) {
        CreateMeetingPointDialog(
            onDismiss = { viewModel.dismissCreateMeetingPointDialog() },
            onConfirm = { name, pavilion, description ->
                viewModel.createMeetingPoint(name, pavilion, description)
            }
        )
    }

    if (uiState.selectedApplicationForApproval != null) {
        val application = uiState.selectedApplicationForApproval!!
        AdminApproveApplicationDialog(
            application = application,
            availableCategories = uiState.categories,
            onDismiss = { viewModel.dismissApproveDialog() },
            onConfirm = { appId, finalCategory, addToGlobal ->
                viewModel.confirmApproval(
                    applicationId = appId,
                    adminId = profile.id,
                    category = finalCategory,
                    addToGlobalCategories = addToGlobal
                )
            }
        )
    }

    if (uiState.showManageCategoriesDialog) {
        AdminManageCategoriesDialog(
            categories = uiState.categories,
            onDismiss = { viewModel.dismissManageCategoriesDialog() },
            onCreateCategory = { name, icon -> viewModel.createCategory(name, icon) },
            onDeleteCategory = { id -> viewModel.deleteCategory(id) },
            isLoading = uiState.isManagingCategories
        )
    }

    if (uiState.selectedApplicationForRejection != null) {
        val application = uiState.selectedApplicationForRejection!!
        RejectApplicationDialog(
            application = application,
            onDismiss = { viewModel.dismissRejectionDialog() },
            onConfirm = { appId, reason ->
                viewModel.confirmRejection(appId, reason)
            }
        )
    }

    if (uiState.selectedSellerForSuspension != null) {
        val seller = uiState.selectedSellerForSuspension!!
        SuspendSellerDialog(
            seller = seller,
            onDismiss = { viewModel.dismissSuspensionDialog() },
            onConfirm = { sellerId, reason ->
                viewModel.confirmSellerSuspension(sellerId, reason)
            }
        )
    }

    if (uiState.pointToDelete != null) {
        val point = uiState.pointToDelete!!
        DeleteMeetingPointDialog(
            point = point,
            onDismiss = { viewModel.dismissDeleteMeetingPointDialog() },
            onConfirm = { viewModel.confirmDeleteMeetingPoint() }
        )
    }

    if (uiState.selectedBuyerForSuspension != null) {
        val buyer = uiState.selectedBuyerForSuspension!!
        SuspendBuyerDialog(
            buyer = buyer,
            onDismiss = { viewModel.dismissBuyerSuspensionDialog() },
            onConfirm = { buyerId, reason ->
                viewModel.confirmBuyerSuspension(buyerId, reason)
            }
        )
    }

    if (uiState.selectedUserForWarning != null) {
        val targetUser = uiState.selectedUserForWarning!!
        IssueWarningDialog(
            targetUser = targetUser,
            adminId = profile.id,
            onDismiss = { viewModel.dismissWarningDialog() },
            onConfirm = { targetUserId, reason, adminId ->
                viewModel.confirmIssueWarning(targetUserId, reason, adminId)
            }
        )
    }

    if (showAdminProfileDialog) {
        AdminProfileDialog(
            profile = profile,
            onDismiss = { showAdminProfileDialog = false },
            onSignOut = {
                showAdminProfileDialog = false
                onSignOut()
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
                    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
                    TopAppBar(
                        title = {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAdminProfileDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Panel de Administración",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Campus ${profile.campus} • ${profile.fullName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { ThemeManager.toggleDarkMode(context) }) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = if (isDarkMode) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                                    tint = if (isDarkMode) Color(0xFFF4B942) else MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.refresh() },
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isDarkMode) Color(0xFF70F7D7) else MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Actualizar datos",
                                        tint = if (isDarkMode) Color(0xFF70F7D7) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { showAdminProfileDialog = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_profile_custom),
                                    contentDescription = "Perfil del Administrador",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
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
                    // Contenido de la sección seleccionada
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
                                    onApprove = { app -> viewModel.openApproveDialog(app) },
                                    onReject = { app -> viewModel.openRejectionDialog(app) },
                                    onOpenManageCategories = { viewModel.openManageCategoriesDialog() },
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
                                    onResolveIncident = { inc -> viewModel.resolveIncident(inc.id, "RESUELTO", "RESOLUCION_DIRECTA", "Resuelto por el administrador del campus.") },
                                    onOpenSupportChat = { incident ->
                                        coroutineScope.launch {
                                            val ticketResult = supportRepository.getTicketForIncident(incident.id)
                                            var ticket = ticketResult.getOrNull()
                                            if (ticket == null) {
                                                val reporterId = incident.reporterId ?: incident.reportedUserId ?: profile.id
                                                val created = supportRepository.getOrCreateTicketForIncident(
                                                    userId = reporterId,
                                                    incidentId = incident.id,
                                                    subject = "Reclamo: ${incident.displayIncidentTitle}"
                                                )
                                                ticket = created.getOrNull()
                                            }
                                            if (ticket != null) {
                                                if (incident.isResolved) {
                                                    ticket = ticket.copy(status = incident.status)
                                                }
                                                activeSupportTicket = ticket
                                            } else {
                                                snackbarHostState.showSnackbar("No se pudo iniciar el chat de soporte")
                                            }
                                        }
                                    },
                                    onOpenPhoto = { url -> enlargedPhotoUrl = url }
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

        // Overlay scrim animado para ventanas emergentes
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
