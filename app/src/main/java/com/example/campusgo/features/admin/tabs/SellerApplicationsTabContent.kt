package com.example.campusgo.features.admin.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.campusgo.theme.ThemeManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.campusgo.R
import com.example.campusgo.domain.model.ApplicationStatus
import com.example.campusgo.domain.model.OrderIncident
import com.example.campusgo.domain.model.SellerApplication
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import kotlinx.coroutines.launch

@Composable
fun SellerApplicationsTabContent(
    applications: List<SellerApplication>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onApprove: (SellerApplication) -> Unit,
    onReject: (SellerApplication) -> Unit,
    onOpenManageCategories: () -> Unit = {},
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
    onResolveIncident: (OrderIncident) -> Unit = {},
    onOpenSupportChat: (OrderIncident) -> Unit = {},
    onOpenPhoto: (String) -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    val pendingIncidentsCount = remember(allIncidents) { allIncidents.count { it.isPending } }

    val applicationsListState = rememberLazyListState()
    val incidentsListState = rememberLazyListState()

    LaunchedEffect(selectedFilter) {
        applicationsListState.scrollToItem(0)
    }

    LaunchedEffect(incidentFilter) {
        incidentsListState.scrollToItem(0)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector superior swipeable entre Solicitudes de Puestos y Reportes e Incidencias
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = {
                    Text(
                        text = "Solicitudes (${applications.size})",
                        fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp
                    )
                }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Reportes (${allIncidents.size})",
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                        if (pendingIncidentsCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                // Sección 0: Solicitudes de Vendedor
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Solicitudes de Vendedor",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Revisión y autorización de nuevos emprendedores en el campus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenManageCategories,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_category_custom),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Categorías",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                            state = applicationsListState,
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
                }
            } else {
                // Sección 1: Moderación de Reportes e Incidencias
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            "Centro de Reportes y Moderación",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
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
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                            state = incidentsListState,
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
                                    onResolve = { onResolveIncident(incident) },
                                    onOpenSupportChat = { onOpenSupportChat(incident) },
                                    onOpenPhoto = onOpenPhoto
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
fun AdminIncidentCard(
    incident: OrderIncident,
    reportedUser: UserProfile?,
    reporterUser: UserProfile?,
    onSelectSeller: (UserProfile) -> Unit,
    onSelectBuyer: (UserProfile) -> Unit,
    onIssueWarning: (UserProfile) -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onResolve: () -> Unit,
    onOpenSupportChat: () -> Unit = {},
    onOpenPhoto: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
                val isDarkMode = ThemeManager.isDarkMode.collectAsState().value
                val (statusBg, statusFg, statusLabel) = when (incident.status.uppercase()) {
                    "PENDIENTE" -> Triple(
                        if (isDarkMode) Color(0xFF422006) else Color(0xFFFEF3C7),
                        if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309),
                        "PENDIENTE DE REVISIÓN"
                    )
                    "SANCIONADO" -> Triple(
                        if (isDarkMode) Color(0xFF4C0519) else Color(0xFFFFE4E6),
                        if (isDarkMode) Color(0xFFFB7185) else Color(0xFFBE123C),
                        "SANCIONADO"
                    )
                    "RESUELTO" -> Triple(
                        if (isDarkMode) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857),
                        "RESUELTO"
                    )
                    else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, incident.status)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    tint = if (incident.isPending) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = incident.displayIncidentTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!incident.subOrderId.isNullOrBlank()) {
                Text(
                    text = "Subpedido relacionado: #${incident.subOrderId.take(8).uppercase()}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    val reportedName = reportedUser?.businessName ?: reportedUser?.fullName ?: "ID: ${incident.reportedUserId?.take(8)}"
                    val isSeller = reportedUser?.role == UserRole.EMPRENDEDOR ||
                            reportedUser?.role == UserRole.SUSPENDED
                    Text(
                        text = reportedName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isSeller) "Rol: Vendedor / Puesto Comercial" else "Rol: Estudiante / Comprador",
                        fontSize = 11.sp,
                        color = if (isSeller) Color(0xFF00A884) else MaterialTheme.colorScheme.primary
                    )
                }

                if (reportedUser != null) {
                    val isSeller = reportedUser.role == UserRole.EMPRENDEDOR ||
                            reportedUser.role == UserRole.SUSPENDED
                    OutlinedButton(
                        onClick = {
                            if (isSeller) onSelectSeller(reportedUser) else onSelectBuyer(reportedUser)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Ver Perfil", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Datos del Denunciante
            Column {
                Text(
                    text = "DENUNCIANTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                val reporterName = reporterUser?.businessName ?: reporterUser?.fullName ?: "Usuario #${incident.reporterId?.take(8) ?: "Anónimo"}"
                Text(
                    text = reporterName,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Detalle o testimonio del reclamo
            if (!incident.details.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Detalle del reporte:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = incident.details,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Evidencia fotográfica adjunta
            if (!incident.evidenceUrl.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPhoto(incident.evidenceUrl) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = incident.evidenceUrl,
                            contentDescription = "Evidencia Fotográfica",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Evidencia Fotográfica Adjunta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Toca para ver en tamaño completo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Ver foto",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Botón de Chat de Soporte y Mediación Institucional
            OutlinedButton(
                onClick = onOpenSupportChat,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (incident.isPending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (incident.isPending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat_custom),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (incident.isPending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (incident.isPending) "Abrir Chat con el Usuario" else "Ver Chat de Mediación (Archivado)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Si ya está resuelto o sancionado, mostrar resolución del administrador
            if (!incident.isPending) {
                val isDarkMode = ThemeManager.isDarkMode.collectAsState().value
                Surface(
                    color = if (incident.status == "SANCIONADO") MaterialTheme.colorScheme.errorContainer else if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Resolución del Administrador:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (incident.status == "SANCIONADO") MaterialTheme.colorScheme.onErrorContainer else if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857)
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Botones de Acción para el Administrador (Solo si está PENDIENTE)
            if (incident.isPending) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
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
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Rubro: ${application.category.ifBlank { "General" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF15803D),
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
                        color = MaterialTheme.colorScheme.error,
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rechazar")
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669),
                            contentColor = Color.White
                        )
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
fun ApplicationStatusBadge(status: ApplicationStatus) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val (bgColor, textColor, text) = when (status) {
        ApplicationStatus.PENDIENTE -> Triple(
            if (isDarkMode) Color(0xFF422006) else Color(0xFFFEF3C7),
            if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309),
            "Pendiente"
        )
        ApplicationStatus.APROBADA -> Triple(
            if (isDarkMode) Color(0xFF064E3B) else Color(0xFFD1FAE5),
            if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857),
            "Aprobado"
        )
        ApplicationStatus.RECHAZADA -> Triple(
            if (isDarkMode) Color(0xFF4C0519) else Color(0xFFFFE4E6),
            if (isDarkMode) Color(0xFFFB7185) else Color(0xFFBE123C),
            "Rechazado"
        )
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
