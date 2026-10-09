package com.example.campusgo.features.seller.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.campusgo.R
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.repository.AuthRepository
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import com.example.campusgo.ui.components.CampusGoUserAvatar
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.StrikeBadge
import com.example.campusgo.ui.components.StrikeMeter
import com.example.campusgo.ui.components.formatAccountCreationDate
import org.koin.compose.koinInject

@Composable
fun BuyerProfileDetailDialog(
    subOrder: SubOrder,
    strikes: Int,
    onDismiss: () -> Unit,
    onOpenChat: (() -> Unit)? = null
) {
    val authRepository: AuthRepository = koinInject()
    val orderRepository: OrderRepository = koinInject()
    val isDark = LocalDarkTheme.current
    var buyerProfile by remember { mutableStateOf<UserProfile?>(null) }
    var currentStrikes by remember(strikes) { mutableIntStateOf(strikes) }
    var warningList by remember { mutableStateOf<List<ProfileWarning>>(emptyList()) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(subOrder.buyerId, subOrder.id) {
        val effectiveBuyerId = if (!subOrder.buyerId.isNullOrBlank()) {
            subOrder.buyerId
        } else {
            orderRepository.getSubOrderById(subOrder.id).getOrNull()?.buyerId
        }

        if (!effectiveBuyerId.isNullOrBlank()) {
            authRepository.getUserProfile(effectiveBuyerId).onSuccess { prof ->
                buyerProfile = prof
            }
            orderRepository.getUserWarnings(effectiveBuyerId).onSuccess { warns ->
                currentStrikes = warns.size
                warningList = warns
            }
        }
        isLoadingProfile = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top bar de navegación
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Perfil del Comprador",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Contenido
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tarjeta Principal de Identidad
                    val avatarUrl = buyerProfile?.avatarUrl ?: subOrder.buyerAvatarUrl
                    val buyerName = buyerProfile?.fullName?.ifBlank { subOrder.buyerName } ?: subOrder.buyerName?.ifBlank { "Estudiante Universitario" } ?: "Estudiante Universitario"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (!avatarUrl.isNullOrBlank()) {
                                            showEnlargedPhoto = true
                                        }
                                    }
                            ) {
                                CampusGoUserAvatar(
                                    avatarUrl = avatarUrl,
                                    name = buyerName,
                                    size = 88.dp
                                )
                                if (!avatarUrl.isNullOrBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Ampliar foto",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = buyerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = "🎓 Estudiante / Comprador",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (currentStrikes > 0) {
                                    StrikeBadge(strikes = currentStrikes)
                                } else {
                                    Surface(
                                        color = MaterialTheme.extendedColors.successContainer,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = "0 strikes",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.extendedColors.onSuccessContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta de Seguridad y Medidor de Strikes
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentStrikes >= 5) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            } else if (currentStrikes > 0) {
                                MaterialTheme.extendedColors.warningContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.extendedColors.successContainer.copy(alpha = 0.35f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (currentStrikes >= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            else if (currentStrikes > 0) MaterialTheme.extendedColors.warning.copy(alpha = 0.5f)
                            else MaterialTheme.extendedColors.success.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val iconTint = when {
                                    currentStrikes >= 5 -> MaterialTheme.colorScheme.error
                                    currentStrikes > 0 -> MaterialTheme.extendedColors.warning
                                    else -> MaterialTheme.extendedColors.success
                                }
                                val iconVector = when {
                                    currentStrikes >= 5 -> Icons.Default.Dangerous
                                    currentStrikes > 0 -> Icons.Default.Warning
                                    else -> Icons.Default.VerifiedUser
                                }
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = if (currentStrikes == 0) "Historial Disciplinario: Limpio" else "Historial de Advertencias ($currentStrikes/5 strikes)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentStrikes == 0) "0 advertencias registradas" else "Llamadas de atención emitidas por el campus",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Medidor visual de 5 strikes
                            StrikeMeter(strikes = currentStrikes, maxStrikes = 5)

                            Text(
                                text = if (currentStrikes == 0) {
                                    "Este comprador no presenta reportes por inasistencia (No-Show) ni sanciones disciplinarias activas."
                                } else if (currentStrikes >= 5) {
                                    "Cuenta con suspensión automática por acumular 5 faltas o llamadas de atención en el campus."
                                } else {
                                    "Este estudiante cuenta con $currentStrikes advertencia(s) oficial(es) por inasistencia a retiro de pedidos o faltas al reglamento."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            if (warningList.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Detalle de incidencias registradas:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    warningList.forEachIndexed { index, warn ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${warn.reason}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = warn.formattedDate,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta de Datos del Usuario
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Información del Usuario",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_location_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Campus Universitario", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(buyerProfile?.campus ?: subOrder.meetingPointName ?: "UCV - Lima Norte", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_account_created_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Miembro desde", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatAccountCreationDate(buyerProfile?.createdAt), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // Contexto del Pedido Actual
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Contexto del Pedido",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_orders_bag),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Productos solicitados", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val itemsText = subOrder.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                                    Text(itemsText.ifBlank { "Productos del pedido" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_location_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Punto acordado", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(subOrder.meetingPointName ?: "Campus Universitario", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Botón inferior: Volver
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onOpenChat != null && !subOrder.status.isFinal) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenChat()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chat_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Volver", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (showEnlargedPhoto) {
        val avatarUrl = buyerProfile?.avatarUrl ?: subOrder.buyerAvatarUrl
        val buyerName = buyerProfile?.fullName?.ifBlank { subOrder.buyerName } ?: subOrder.buyerName?.ifBlank { "Comprador" } ?: "Comprador"
        EnlargedPhotoViewerDialog(
            photoUrl = avatarUrl,
            name = buyerName,
            roleDescription = "Estudiante CampusGO",
            isBanner = false,
            onDismiss = { showEnlargedPhoto = false }
        )
    }
}
