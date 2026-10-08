package com.example.campusgo.features.cart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.campusgo.R
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.OrderStatus
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.orderCodeDisplay
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.ui.components.IncidentContextType
import com.example.campusgo.ui.components.ReportIncidentDialog
import kotlinx.coroutines.launch
import com.example.campusgo.theme.LocalDarkTheme
import org.koin.compose.koinInject

/**
 * Pantalla completa de Comprobante / Resumen de Pedido (Estilo Boleta Digital).
 * Muestra el desglose detallado de los productos y subpedidos generados tras el checkout.
 */
@Composable
fun OrderSummaryReceiptScreen(
    order: Order,
    onNavigateToTracking: () -> Unit,
    onOpenChat: ((SubOrder) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    orderRepository: OrderRepository = koinInject()
) {
    val isDark = LocalDarkTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showReportDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }
    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (showReportDialog) 20.dp else 0.dp,
        animationSpec = tween(280),
        label = "order_summary_blur"
    )
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (backgroundBlurRadius > 0.dp) Modifier.blur(backgroundBlurRadius) else Modifier),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (isDark) 0.dp else 2.dp,
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Comprobante de Pedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onNavigateToTracking) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "Seguimiento",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (isDark) 0.dp else 8.dp,
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToTracking,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Seguir mi Pedido en Tiempo Real",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    val activeSubOrders = order.subOrders.filter { !it.status.isFinal }
                    if (activeSubOrders.isNotEmpty() && order.status != OrderStatus.COMPLETADA && order.status != OrderStatus.CANCELADA && onOpenChat != null) {
                        OutlinedButton(
                            onClick = { onOpenChat(activeSubOrders.first()) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00A884)),
                            border = BorderStroke(1.dp, Color(0xFF00A884)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat_custom),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coordinar por Chat con el Vendedor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC8102E)),
                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "¿Problema con esta orden? Reportar al Administrador",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }

                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Volver al Catálogo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de Éxito y Código de Boleta
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = "¡Pedido Confirmado!",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Entrega en: ${order.meetingPointName.ifBlank { "Campus Universitario" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Los emprendedores fueron notificados y prepararán tu pedido para la hora indicada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Datos de Entrega
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DATOS DE ENTREGA EN CAMPUS",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location_custom),
                            contentDescription = null,
                            tint = Color(0xFFE59A00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Punto de Encuentro",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = order.meetingPointName.ifBlank { "Campus Universitario" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFF1F5F9))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_alarm_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Horario Acordado",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = order.scheduledTime.ifBlank { "Entrega Inmediata" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Desglose de Productos por Puesto / Emprendimiento
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DESGLOSE POR EMPRENDIMIENTO",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${order.subOrders.size} Puesto(s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    order.subOrders.forEachIndexed { index, subOrder ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_store_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = subOrder.sellerName.ifBlank { "Emprendedor #${index + 1}" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "S/ %.2f".format(subOrder.subtotalAmount),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }

                                HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0))

                                subOrder.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.quantity}x ${item.productName}",
                                            fontSize = 13.sp,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "S/ %.2f".format(item.subtotal),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val ptText = subOrder.meetingPointName?.takeIf { it.isNotBlank() } ?: order.meetingPointName.ifBlank { "Campus" }
                                    val pmText = subOrder.paymentMethod?.name ?: order.paymentMethod?.name ?: "Efectivo"
                                    Text(
                                        text = "📍 $ptText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "💳 $pmText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFF1F5F9))

                    // Resumen Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL A PAGAR",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "S/ %.2f".format(order.totalAmount),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        val targetSeller = order.subOrders.firstOrNull()
        ReportIncidentDialog(
            title = "Reportar Problema con el Pedido",
            subtitle = "Puesto: ${targetSeller?.sellerName ?: "Campus"}",
            contextType = IncidentContextType.ORDER,
            isSubmitting = isSubmittingReport,
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonKey, reasonLabel, details, evidenceBytes ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val result = orderRepository.reportIncident(
                        subOrderId = targetSeller?.id,
                        reporterId = order.buyerId,
                        reportedUserId = targetSeller?.sellerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel },
                        evidenceBytes = evidenceBytes
                    )
                    isSubmittingReport = false
                    showReportDialog = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Reporte enviado con éxito al Administrador del Campus.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error al enviar reporte: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
