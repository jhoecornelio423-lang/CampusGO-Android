package com.example.vallego.features.seller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.vallego.R
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.SubOrderStatus
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.ui.components.IncidentContextType
import com.example.vallego.ui.components.PaymentMethodLogo
import com.example.vallego.ui.components.ReportIncidentDialog
import com.example.vallego.ui.components.ValleGoDialogContainerColor
import com.example.vallego.ui.components.ValleGoDialogShape
import com.example.vallego.ui.components.ValleGoDialogTonalElevation
import com.example.vallego.ui.components.valleGoDialogStyle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showReportBuyerDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC8102E)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reportar Comprador",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

    if (showReportBuyerDialog) {
        ReportIncidentDialog(
            title = "Reportar Comprador",
            subtitle = "Cliente: ${subOrder.buyerName ?: "Estudiante Universitario"}",
            contextType = IncidentContextType.BUYER,
            isSubmitting = isSubmittingReport,
            onDismiss = { showReportBuyerDialog = false },
            onSubmit = { reasonKey, reasonLabel, details ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val result = orderRepository.reportIncident(
                        subOrderId = subOrder.id,
                        reporterId = subOrder.sellerId,
                        reportedUserId = subOrder.buyerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel }
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
}
