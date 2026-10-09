package com.example.campusgo.features.chat

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.campusgo.core.notification.CampusGoNotificationHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import com.example.campusgo.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Shield
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.model.formatIncidentType
import com.example.campusgo.ui.components.CampusGoUserAvatar

data class ActiveChatSummary(
    val subOrderId: String,
    val otherUserId: String,
    val otherUserName: String,
    val meetingPoint: String,
    val status: SubOrderStatus,
    val subtotal: Double,
    val itemsSummary: String,
    val deliveryCode: String = "",
    val isBuyerPerspective: Boolean = true,
    val otherUserAvatarUrl: String? = null,
    val unreadCount: Int = 0
)

/**
 * Pantalla / Bandeja de Chats de Pedidos Activos y Mesa de Diálogo de Soporte.
 * Permite que tanto el comprador como el vendedor vean directamente todos sus chats
 * en curso y sus tickets de mediación institucional sin tener que buscar cada pedido individualmente.
 */
@Composable
fun ActiveChatsSheet(
    chats: List<ActiveChatSummary>,
    onSelectChat: (ActiveChatSummary) -> Unit,
    supportTickets: List<SupportTicket> = emptyList(),
    onSelectSupportTicket: (SupportTicket) -> Unit = {},
    onClose: (() -> Unit)? = null,
    userAvatarUrl: String? = null,
    showHeader: Boolean = true
) {
    if (onClose != null) {
        BackHandler(onBack = onClose)
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        CampusGoNotificationHelper.cancelChatNotifications(context)
    }

    val visibleSupportTickets = remember(supportTickets) {
        supportTickets.filter { ticket ->
            ticket.isOpen &&
            !ticket.status.equals("RESUELTO", true) &&
            !ticket.status.equals("SANCIONADO", true) &&
            !ticket.status.equals("CERRADO", true) &&
            !ticket.status.equals("DESCARTADO", true)
        }
    }

    val isDark = LocalDarkTheme.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showHeader) Modifier.statusBarsPadding() else Modifier)
        ) {
            // Header superior
            if (showHeader) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                            spotColor = if (isDark) Color.Black else Color(0x1F16324F),
                            ambientColor = if (isDark) Color.Black else Color(0x2816324F),
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
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onClose != null) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = "Chats de Pedidos Activos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val totalActive = chats.size + visibleSupportTickets.size
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$totalActive activo${if (totalActive != 1) "s" else ""}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (!userAvatarUrl.isNullOrBlank()) {
                            CampusGoUserAvatar(
                                avatarUrl = userAvatarUrl,
                                name = null,
                                size = 32.dp
                            )
                        }
                    }
                }
            }
            }

            if (chats.isEmpty() && visibleSupportTickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp)
                        .padding(bottom = if (onClose == null) (70.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No tienes chats activos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Los chats de coordinación se habilitan automáticamente cuando tienes un pedido en curso o un caso de soporte con la administración.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = if (onClose == null) (76.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Tickets institucionales de soporte y mediación con el Admin (solo casos activos)
                    items(visibleSupportTickets, key = { "support_${it.id}" }) { ticket ->
                        SupportTicketChatItemCard(
                            ticket = ticket,
                            onClick = { onSelectSupportTicket(ticket) }
                        )
                    }

                    // 2. Chats habituales de pedidos entre comprador y vendedor
                    items(chats, key = { it.subOrderId }) { chat ->
                        ActiveChatItemCard(
                            chat = chat,
                            onClick = { onSelectChat(chat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportTicketChatItemCard(
    ticket: SupportTicket,
    onClick: () -> Unit
) {
    val isResolved = !ticket.isOpen || ticket.status.equals("RESUELTO", true) || ticket.status.equals("SANCIONADO", true)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Administración CampusGO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Estado del ticket
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isResolved) MaterialTheme.extendedColors.successContainer else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val statusLabel = if (isResolved) "RESUELTO" else "EN ATENCIÓN"
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isResolved) MaterialTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val formattedSubject = formatIncidentType(ticket.subject)
                Text(
                    text = "Mesa de Mediación Institucional",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedSubject,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActiveChatItemCard(
    chat: ActiveChatSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CampusGoUserAvatar(
                avatarUrl = chat.otherUserAvatarUrl,
                name = chat.otherUserName,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = chat.otherUserName.ifBlank { "Contacto de Pedido" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.unreadCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = if (chat.unreadCount > 9) "+9" else "${chat.unreadCount}",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "S/ %.2f".format(chat.subtotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mención breve y distintiva del pedido (código de entrega solo para comprador, productos para ambos)
                val orderSummaryText = if (chat.isBuyerPerspective && chat.deliveryCode.isNotBlank()) {
                    if (chat.itemsSummary.isNotBlank()) {
                        "Código #${chat.deliveryCode} • ${chat.itemsSummary}"
                    } else {
                        "Código #${chat.deliveryCode}"
                    }
                } else {
                    if (chat.itemsSummary.isNotBlank()) {
                        chat.itemsSummary
                    } else {
                        "Pedido en curso"
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_orders_bag),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = orderSummaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_custom),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = chat.meetingPoint.ifBlank { "Punto por convenir" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (statusBg, statusFg, statusText) = when (chat.status) {
                        SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> Triple(
                            MaterialTheme.extendedColors.successContainer,
                            MaterialTheme.extendedColors.onSuccessContainer,
                            if (chat.status == SubOrderStatus.LISTO) "Listo para entrega" else "En punto de entrega"
                        )
                        SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION -> Triple(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            if (chat.status == SubOrderStatus.ACEPTADO) "Aceptado" else "En preparación"
                        )
                        SubOrderStatus.PENDIENTE -> Triple(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            "Pendiente"
                        )
                        else -> Triple(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            "Activo"
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Abrir Chat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.extendedColors.success
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_custom),
                            contentDescription = null,
                            tint = MaterialTheme.extendedColors.success,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
