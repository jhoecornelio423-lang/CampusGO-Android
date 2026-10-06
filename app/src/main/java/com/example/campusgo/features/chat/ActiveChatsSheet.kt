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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
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
                            spotColor = Color(0x1F16324F),
                            ambientColor = Color(0x2816324F),
                            clip = false
                        ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                    tint = Color(0xFF1E293B)
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
                            color = Color(0xFF1E293B)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val totalActive = chats.size + visibleSupportTickets.size
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "$totalActive activo${if (totalActive != 1) "s" else ""}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
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
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No tienes chats activos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Los chats de coordinación se habilitan automáticamente cuando tienes un pedido en curso o un caso de soporte con la administración.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isResolved) Color(0xFFE2E8F0) else Color(0xFFBFDBFE)),
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
                color = Color(0xFF003366),
                border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
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
                            color = Color(0xFF003366),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF003366)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Estado del ticket
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isResolved -> Color(0xFFDCFCE7)
                            else -> Color(0xFFDBEAFE)
                        }
                    ) {
                        val statusLabel = if (isResolved) "RESUELTO" else "EN ATENCIÓN"
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isResolved) Color(0xFF166534) else Color(0xFF1E40AF),
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
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedSubject,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF003366),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
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
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.unreadCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = if (chat.unreadCount > 9) "+9" else "${chat.unreadCount}",
                                    color = Color.White,
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
                        color = Color(0xFF003366)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mención breve y distintiva del pedido (código de entrega y productos)
                val codeDisplay = chat.deliveryCode.ifBlank {
                    (kotlin.math.abs(chat.subOrderId.hashCode()) % 9000 + 1000).toString()
                }
                val deliveryTag = "Código #$codeDisplay"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
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
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (chat.itemsSummary.isNotBlank()) {
                                "$deliveryTag • ${chat.itemsSummary}"
                            } else {
                                deliveryTag
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
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
                        tint = Color(0xFFE59A00),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = chat.meetingPoint.ifBlank { "Punto por convenir" },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (chat.status) {
                            SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> Color(0xFFE8F5E9)
                            SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION -> Color(0xFFE0F2FE)
                            else -> Color(0xFFF1F5F9)
                        }
                    ) {
                        Text(
                            text = when (chat.status) {
                                SubOrderStatus.PENDIENTE -> "Pendiente"
                                SubOrderStatus.ACEPTADO -> "Aceptado"
                                SubOrderStatus.EN_PREPARACION -> "En preparación"
                                SubOrderStatus.LISTO -> "Listo para entrega"
                                SubOrderStatus.ESPERANDO_ENTREGA -> "En punto de entrega"
                                else -> "Activo"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (chat.status) {
                                SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> Color(0xFF2E7D32)
                                SubOrderStatus.ACEPTADO, SubOrderStatus.EN_PREPARACION -> Color(0xFF0284C7)
                                else -> Color(0xFF475569)
                            },
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
                            color = Color(0xFF00A884)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_custom),
                            contentDescription = null,
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
