package com.example.campusgo.features.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.BuyerOrderStats
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.StrikeBadge
import com.example.campusgo.ui.components.StrikeManagementCard
import com.example.campusgo.ui.components.CampusGoBusinessAvatar
import com.example.campusgo.theme.ThemeManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pantalla completa dedicada al seguimiento, auditoría y moderación del Comprador.
 * Permite ver información de contacto, métricas de compras, historial de advertencias,
 * suspender/reactivar cuenta y emitir llamadas de atención oficiales.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminBuyerDetailScreen(
    buyer: UserProfile,
    buyerStats: BuyerOrderStats?,
    isLoadingStats: Boolean,
    warnings: List<ProfileWarning>,
    isLoadingWarnings: Boolean,
    onBack: () -> Unit,
    onSuspend: (UserProfile) -> Unit,
    onReactivate: (UserProfile) -> Unit,
    onIssueWarning: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isSuspended = buyer.role == UserRole.SUSPENDED_BUYER || buyer.role == UserRole.SUSPENDED

    var showEnlargedPhoto by remember { mutableStateOf(false) }

    BackHandler(enabled = showEnlargedPhoto) {
        showEnlargedPhoto = false
    }

    BackHandler(enabled = !showEnlargedPhoto) {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Seguimiento de Comprador",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = buyer.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al listado",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        StrikeBadge(strikes = warnings.size, showAutoSuspensionLabel = true)
                        Surface(
                            color = if (isSuspended) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                if (isDarkMode) Color(0xFF1B4D24) else Color(0xFFE8F5E9)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSuspended) "SUSPENDIDO" else "ACTIVO",
                                color = if (isSuspended) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO CARD DEL COMPRADOR (AVATAR CON TAP TO ENLARGE)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuspended) {
                            if (isDarkMode) Color(0xFF381418) else Color(0xFFFFEBEE)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSuspended) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar con borde y animación de clic
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    showEnlargedPhoto = true
                                }
                        ) {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(3.dp, if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                shadowElevation = 4.dp
                            ) {
                                CampusGoBusinessAvatar(
                                    avatarUrl = buyer.avatarUrl,
                                    storeName = buyer.fullName,
                                    size = 88.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = buyer.fullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Estudiante Comprador",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFF00897B).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Campus ${buyer.campus}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF00897B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. MOTIVO DE SUSPENSIÓN (SI ESTÁ SUSPENDIDO)
            if (isSuspended && !buyer.suspensionReason.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF381418) else Color(0xFFFFEBEE)
                        ),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF7F1D1D) else Color(0xFFEF9A9A))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cuenta Suspendida",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Motivo: ${buyer.suspensionReason}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. ACTIVIDAD Y ESTADÍSTICAS DEL COMPRADOR
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Actividad y Estadísticas de Compras",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLoadingStats) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val stats = buyerStats ?: BuyerOrderStats()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BuyerMetricMiniCard(
                                title = "Total Pedidos",
                                value = "${stats.totalOrders}",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            BuyerMetricMiniCard(
                                title = "Completados",
                                value = "${stats.completedOrders}",
                                color = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BuyerMetricMiniCard(
                                title = "Cancelados",
                                value = "${stats.cancelledOrders}",
                                color = if (stats.cancelledOrders > 0) MaterialTheme.colorScheme.error else Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            BuyerMetricMiniCard(
                                title = "Total Invertido",
                                value = "S/ %.2f".format(stats.totalSpent),
                                color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF00A884),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. DATOS DE IDENTIFICACIÓN Y CONTACTO RÁPIDO
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Información Personal y Contacto",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Teléfono con botones de acción directa
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Teléfono de Contacto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = buyer.phone.takeIf { it.isNotBlank() } ?: "No registrado",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (buyer.phone.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${buyer.phone.trim()}"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Llamar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            try {
                                                val clean = buyer.phone.replace("+", "").replace(" ", "").trim()
                                                val phoneWithCountry = if (clean.startsWith("51")) clean else "51$clean"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phoneWithCountry"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_chat_custom),
                                            contentDescription = "WhatsApp",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Código UCV
                        if (!buyer.studentCode.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Código de Estudiante", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = buyer.studentCode, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }

                        // Campus
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location_custom),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Campus Universitario", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = buyer.campus, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 5. HISTORIAL DE STRIKES Y MEDIDAS DISCIPLINARIAS
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (isLoadingWarnings) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        StrikeManagementCard(
                            strikes = warnings.size,
                            warnings = warnings,
                            isSeller = false,
                            isSuspended = isSuspended
                        )
                    }
                }
            }

            // 6. ACCIONES DISCIPLINARIAS Y DE MODERACIÓN (LLAMAR LA ATENCIÓN & SUSPENDER/REACTIVAR)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón para Llamar la Atenci\u00F3n (Solo si no ha alcanzado el tope de 5 strikes)
                    if (warnings.size < 5) {
                        Button(
                            onClick = { onIssueWarning(buyer) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Llamar la Atenci\u00F3n (+1 Strike)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Botón para Suspender o Reactivar
                    if (isSuspended) {
                        Button(
                            onClick = { onReactivate(buyer) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reactivar Cuenta de Comprador", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSuspend(buyer) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                            border = BorderStroke(1.2.dp, Color(0xFFC8102E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Suspender Cuenta de Comprador", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (showEnlargedPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = buyer.avatarUrl,
            name = buyer.fullName,
            roleDescription = "Comprador Universitario • Campus ${buyer.campus}",
            isBanner = false,
            onDismiss = { showEnlargedPhoto = false }
        )
    }
}

@Composable
private fun BuyerMetricMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
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

private fun formatIsoDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "Reciente"
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoDate)
            if (date != null) {
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE")).apply {
                    timeZone = TimeZone.getTimeZone("America/Lima")
                }
                return outputFormat.format(date)
            }
        } catch (_: Exception) {}
    }
    return isoDate
}
