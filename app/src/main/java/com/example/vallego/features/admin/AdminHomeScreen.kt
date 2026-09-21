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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.vallego.ui.components.ValleGoDialogTonalElevation
import com.example.vallego.ui.components.valleGoDialogStyle
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.vallego.R
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
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
import com.example.vallego.domain.model.ApplicationStatus
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.OrderIncident
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
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
            uiState.selectedSellerForSuspension != null

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
                        "Este punto se guardará en Supabase y estará disponible para los alumnos en el campus.",
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

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = if (backgroundBlurRadius > 0.dp) Modifier.fillMaxSize().blur(backgroundBlurRadius) else Modifier.fillMaxSize(),
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
                            onCreateClick = { viewModel.openCreateMeetingPointDialog() }
                        )
                    }
                    AdminTab.SELLER_APPLICATIONS -> {
                        SellerApplicationsTabContent(
                            applications = uiState.sellerApplications,
                            onApprove = { app -> viewModel.approveApplication(app.id, profile.id) },
                            onReject = { app -> viewModel.openRejectionDialog(app) }
                        )
                    }
                    AdminTab.SELLERS_DIRECTORY -> {
                        SellersDirectoryTabContent(
                            sellers = uiState.sellers,
                            onSuspend = { seller -> viewModel.openSuspensionDialog(seller) },
                            onReactivate = { seller -> viewModel.reactivateSeller(seller.id) }
                        )
                    }
                    AdminTab.CAMPUS_METRICS -> {
                        CampusMetricsTabContent(
                            metrics = uiState.metrics,
                            incidents = uiState.incidents
                        )
                    }
                }
            }

            // Barra de Navegación Dock Liquid Glass flotante en la parte inferior
            AdminLiquidGlassDock(
                selectedTab = uiState.selectedTab,
                onSelectTab = { viewModel.setTab(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
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
                Text("Sincronizados en Supabase para el checkout de los alumnos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    CampusMeetingPointItemCard(point = point, onToggle = { onToggle(point) })
                }
            }
        }
    }
}

@Composable
fun CampusMeetingPointItemCard(
    point: CampusMeetingPoint,
    onToggle: () -> Unit
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
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = point.isActive,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun SellerApplicationsTabContent(
    applications: List<SellerApplication>,
    onApprove: (SellerApplication) -> Unit,
    onReject: (SellerApplication) -> Unit
) {
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
                    text = "No hay solicitudes de nuevos vendedores pendientes",
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
            items(applications, key = { it.id }) { app ->
                SellerApplicationCard(
                    application = app,
                    onApprove = { onApprove(app) },
                    onReject = { onReject(app) }
                )
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
    onSuspend: (UserProfile) -> Unit,
    onReactivate: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Puestos del Campus (${sellers.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Gestión y auditoría de emprendedores registrados en la universidad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

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
                        text = "No hay emprendedores registrados en este campus",
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
                items(sellers, key = { it.id }) { seller ->
                    SellerDirectoryCard(
                        seller = seller,
                        onSuspend = { onSuspend(seller) },
                        onReactivate = { onReactivate(seller) }
                    )
                }
            }
        }
    }
}

@Composable
fun SellerDirectoryCard(
    seller: UserProfile,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit
) {
    val isSuspended = seller.role == UserRole.SUSPENDED

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuspended) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
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
                ValleGoBusinessAvatar(
                    avatarUrl = seller.avatarUrl,
                    storeName = seller.displayStoreName,
                    size = 46.dp
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
                        text = "Dueño: ${seller.fullName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (seller.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = seller.phone,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ubicación: ${seller.businessLocation ?: seller.campus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSuspended) {
                    Button(
                        onClick = onReactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Reactivar", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSuspend,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suspender", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CampusMetricsTabContent(
    metrics: CampusMetrics,
    incidents: List<OrderIncident>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF003366)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Telemetría en Vivo del Campus",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Monitoreo en tiempo real de transacciones contra entrega y actividad de puestos.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricCard(
                    title = "Ventas Hoy",
                    value = "S/ %.2f".format(metrics.totalSalesToday),
                    color = Color(0xFF003366),
                    modifier = Modifier.weight(1.3f)
                )
                AdminMetricCard(
                    title = "Pedidos Hoy",
                    value = "${metrics.totalOrdersToday}",
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
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

            // Fila de ítems del Dock con espaciado elástico y altura cómoda
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    AdminDockItem(
                        item = item,
                        isSelected = selectedTab == item.tab,
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
        targetValue = if (isSelected) 74.dp else 48.dp,
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
