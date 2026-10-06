package com.example.campusgo.features.seller

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.ui.components.EnlargedPhotoViewerDialog
import com.example.campusgo.ui.components.IncidentContextType
import com.example.campusgo.ui.components.PaymentMethodLogo
import com.example.campusgo.ui.components.ReportIncidentDialog
import com.example.campusgo.ui.components.CampusGoUserAvatar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrderDetailDialog(
    subOrder: SubOrder,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onStartPrep: (String) -> Unit,
    onMarkReady: (String) -> Unit,
    onOpenDelivery: (SubOrder) -> Unit,
    onOpenRejection: (SubOrder) -> Unit,
    onOpenChat: ((SubOrder) -> Unit)? = null,
    orderRepository: OrderRepository = koinInject()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showReportBuyerDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var showEnlargedBuyerPhoto by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            // Cabecera superior moderna tipo Rappi / iOS (fija)
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
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Detalle de Venta",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color(0xFF16324F)
                        )
                        Text(
                            text = "Cliente: ${subOrder.buyerName?.ifBlank { "Estudiante Universitario" } ?: "Estudiante Universitario"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(status = subOrder.status)

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(17.dp)
                        )
                    }
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
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Tarjeta: Punto de Entrega Acordado
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE6F7F3),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "PUNTO DE ENTREGA OFICIAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = subOrder.meetingPointName ?: "Punto oficial de entrega",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16324F),
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        if (!subOrder.scheduledTime.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Hora de encuentro: ${subOrder.scheduledTime}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Tarjeta: Datos del Cliente (Comprador)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CLIENTE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🎓 Estudiante Campus GO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        if (!subOrder.buyerAvatarUrl.isNullOrBlank()) {
                                            showEnlargedBuyerPhoto = true
                                        }
                                    }
                            ) {
                                CampusGoUserAvatar(
                                    avatarUrl = subOrder.buyerAvatarUrl,
                                    name = subOrder.buyerName,
                                    size = 46.dp
                                )
                                if (!subOrder.buyerAvatarUrl.isNullOrBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF00A884),
                                        border = BorderStroke(1.5.dp, Color.White),
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Ampliar foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subOrder.buyerName?.ifBlank { "Estudiante Universitario" } ?: "Estudiante Universitario",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Comprador verificado",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        if (!subOrder.notes.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "💬",
                                        fontSize = 13.sp
                                    )
                                    Column {
                                        Text(
                                            text = "Nota del cliente:",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = subOrder.notes,
                                            fontSize = 12.sp,
                                            color = Color(0xFF78350F)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenChat != null && !subOrder.status.isFinal) {
                                Button(
                                    onClick = { onOpenChat(subOrder) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE6F7F3),
                                        contentColor = Color(0xFF00A884)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chat_custom),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chat con Cliente",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            TextButton(
                                onClick = { showReportBuyerDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reportar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Tarjeta: Productos Solicitados y Desglose
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRODUCTOS SOLICITADOS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            val totalItems = subOrder.items.sumOf { it.quantity }
                            Text(
                                text = "$totalItems unidad${if (totalItems > 1) "es" else ""}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        subOrder.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
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
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${item.quantity}x",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF16324F)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.productName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                Text(
                                    text = "S/ %.2f".format(item.subtotal),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16324F)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Método de Pago y Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (pmBg, pmTint, pmLabel) = when (subOrder.paymentMethod) {
                                PaymentMethod.YAPE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Yape")
                                PaymentMethod.PLIN -> Triple(Color(0xFFE0F2F1), Color(0xFF00796B), "Plin")
                                PaymentMethod.EFECTIVO -> Triple(Color(0xFFF1F5F9), Color(0xFF16324F), "Efectivo")
                                else -> Triple(Color(0xFFF1F5F9), Color(0xFF16324F), subOrder.paymentMethod?.name ?: "Efectivo")
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
                                    PaymentMethodLogo(method = subOrder.paymentMethod, size = 15.dp)
                                    Text(
                                        text = pmLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pmTint
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Total:",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "S/ %.2f".format(subOrder.subtotalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = Color(0xFF00A884)
                                )
                            }
                        }
                    }
                }

                // 4. Botones de Acción Estilo Rappi / iOS
                Spacer(modifier = Modifier.height(4.dp))

                when (subOrder.status) {
                    SubOrderStatus.PENDIENTE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenRejection(subOrder) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(48.dp)
                            ) {
                                Text("Rechazar", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }

                            Button(
                                onClick = { onAccept(subOrder.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(48.dp)
                            ) {
                                Text("Aceptar Pedido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    SubOrderStatus.ACEPTADO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenRejection(subOrder)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(48.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { onStartPrep(subOrder.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16324F)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.65f)
                                    .height(48.dp)
                            ) {
                                Text("Iniciar Preparación", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    SubOrderStatus.EN_PREPARACION -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenRejection(subOrder)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(48.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { onMarkReady(subOrder.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.65f)
                                    .height(48.dp)
                            ) {
                                Text("Marcar Listo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    SubOrderStatus.LISTO, SubOrderStatus.ESPERANDO_ENTREGA -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenRejection(subOrder)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(48.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { onOpenDelivery(subOrder) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.65f)
                                    .height(48.dp)
                            ) {
                                Text("Confirmar Entrega", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    SubOrderStatus.COMPLETADO -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Venta Completada",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = "Pedido entregado y pago confirmado",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16324F)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16324F)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showReportBuyerDialog) {
        ReportIncidentDialog(
            title = "Reportar Comprador",
            subtitle = "Cliente: ${subOrder.buyerName ?: "Estudiante Universitario"}",
            contextType = IncidentContextType.BUYER,
            isSubmitting = isSubmittingReport,
            onDismiss = { showReportBuyerDialog = false },
            onSubmit = { reasonKey, reasonLabel, details, evidenceBytes ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val result = orderRepository.reportIncident(
                        subOrderId = subOrder.id,
                        reporterId = subOrder.sellerId,
                        reportedUserId = subOrder.buyerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel },
                        evidenceBytes = evidenceBytes
                    )
                    isSubmittingReport = false
                    showReportBuyerDialog = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Reporte enviado al Administrador del Campus.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error al enviar reporte: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showEnlargedBuyerPhoto) {
        EnlargedPhotoViewerDialog(
            photoUrl = subOrder.buyerAvatarUrl,
            name = subOrder.buyerName ?: "Comprador",
            roleDescription = "Estudiante CampusGO",
            isBanner = false,
            onDismiss = { showEnlargedBuyerPhoto = false }
        )
    }
}
