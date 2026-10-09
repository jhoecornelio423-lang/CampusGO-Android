package com.example.campusgo.features.cart

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.OrderStatus
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SubOrderStatus
import com.example.campusgo.domain.model.orderCodeDisplay
import com.example.campusgo.domain.model.verificationCode
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.theme.TurquoiseGreen
import com.example.campusgo.theme.WarmYellow
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import com.example.campusgo.ui.components.DashedDivider
import com.example.campusgo.ui.components.IncidentContextType
import com.example.campusgo.ui.components.PaymentMethodPill
import com.example.campusgo.ui.components.ReportIncidentDialog
import com.example.campusgo.ui.components.formatReceiptOrderDate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla de Comprobante / Boleta Digital Inteligente (E-Receipt Fintech).
 * Con paleta oficial de CampusGo (Verde Turquesa), logo oficial tipográfico,
 * método de pago con logotipo oficial, PIN de seguridad, sin redundancias y botones inferiores diferenciados.
 */
@Composable
fun OrderSummaryReceiptScreen(
    order: Order,
    reviewedOrders: Map<String, Int> = emptyMap(),
    onRateSeller: ((SubOrder) -> Unit)? = null,
    onNavigateToTracking: (() -> Unit)? = null,
    onOpenChat: ((SubOrder) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    orderRepository: OrderRepository = koinInject()
) {
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

    val isCompleted = order.status == OrderStatus.COMPLETADA
    val isCancelled = order.status == OrderStatus.CANCELADA

    val originalSum = if (order.subOrders.isNotEmpty()) order.subOrders.sumOf { it.subtotalAmount } else order.totalAmount
    val cancelledSum = order.subOrders.filter {
        it.status == SubOrderStatus.RECHAZADO || it.status == SubOrderStatus.CANCELADO || it.status == SubOrderStatus.NO_ENTREGADO
    }.sumOf { it.subtotalAmount }
    val hasCancelledSubOrders = cancelledSum > 0.0
    val effectiveTotal = if (isCancelled) 0.0 else (originalSum - cancelledSum).coerceAtLeast(0.0)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (backgroundBlurRadius > 0.dp) Modifier.blur(backgroundBlurRadius) else Modifier),
        topBar = {
            // Barra superior limpia y centrada
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(52.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompleted -> MaterialTheme.colorScheme.onPrimary
                                            isCancelled -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.onPrimary
                                        }
                                    )
                            )
                            Text(
                                text = when {
                                    isCompleted -> "COMPROBANTE FINALIZADO"
                                    isCancelled -> "COMPROBANTE CANCELADO"
                                    else -> "COMPROBANTE EN CURSO"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = if (isCompleted) "Historial de Pedidos" else "Seguimiento en Tiempo Real",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeSubOrders = order.subOrders.filter { !it.status.isFinal }

                    if (onNavigateToTracking != null) {
                        // Botón Principal de Seguimiento (si se llega desde checkout)
                        Button(
                            onClick = onNavigateToTracking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Seguir mi Pedido en Tiempo Real",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Botón de Chat si hay subpedidos activos
                        if (activeSubOrders.isNotEmpty() && !isCompleted && !isCancelled && onOpenChat != null) {
                            OutlinedButton(
                                onClick = { onOpenChat(activeSubOrders.first()) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chat_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Coordinar por Chat con el Vendedor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Botón discreto de retorno
                        TextButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text(
                                text = "Volver al Catálogo",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // Flujo desde Seguimiento / Historial ("Ver Detalle")
                        val hasActiveChat = activeSubOrders.isNotEmpty() && !isCompleted && !isCancelled && onOpenChat != null

                        if (hasActiveChat) {
                            // Acción Principal: Coordinar por Chat
                            Button(
                                onClick = { onOpenChat(activeSubOrders.first()) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chat_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Coordinar por Chat",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            // Acción Secundaria: Cerrar Comprobante
                            OutlinedButton(
                                onClick = onNavigateBack,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Cerrar Comprobante",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            // En pedidos completados o cancelados (sin chat activo), el botón principal es cerrar
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Cerrar Comprobante",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
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
        ) {
            // ==========================================
            // 1. HEADER BRANDING CAMPUSGO
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ícono circular con halo luminoso
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f), CircleShape)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCompleted -> Icons.Default.Check
                                        isCancelled -> Icons.Default.Close
                                        else -> Icons.Default.Check
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isCancelled -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            isCompleted -> "¡ORDEN COMPLETADA!"
                            isCancelled -> "ORDEN CANCELADA"
                            else -> "PEDIDO EN CURSO"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    // Monto Pagado en grande, centrado y limpio
                    Text(
                        text = "S/ %.2f".format(effectiveTotal),
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when {
                            isCompleted -> "Total cancelado • Pedido entregado con éxito"
                            isCancelled -> "Este pedido fue cancelado"
                            else -> "Total a pagar en entrega • Pedido en preparación"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ==========================================
            // 2. BOLETA / TICKET DIGITAL INTELIGENTE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta Principal Tipo Ticket
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Franja Superior del Ticket: Logo Oficial de Letras + Código de Orden
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.letras_logo),
                                    contentDescription = "CampusGo",
                                    modifier = Modifier.height(26.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = formatReceiptOrderDate(order.createdAt),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Chip con botón interactivo de copiar código
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Código de Orden", order.orderCodeDisplay))
                                    Toast.makeText(context, "Código copiado: ${order.orderCodeDisplay}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = order.orderCodeDisplay,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                        // PIN DE RETIRO SEGURO (Estilo Cripto / Tech en gama Verde Turquesa)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PIN DE RETIRO SEGURO",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val pin = order.subOrders.firstOrNull()?.verificationCode ?: order.id.takeLast(4)
                                    pin.forEach { digit ->
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(0xFF022C22), RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = digit.toString(),
                                                color = Color(0xFF34D399),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isCompleted) "Pedido entregado y verificado con código PIN" else "Muestra este PIN al momento de recoger tu pedido",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // LOGÍSTICA DE ENTREGA (Grid Tecnológico)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Punto de Entrega
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location_custom),
                                            contentDescription = null,
                                            tint = WarmYellow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ENTREGA EN",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = order.meetingPointName.ifBlank { "Campus Universitario" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Horario Acordado
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_alarm_custom),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HORARIO",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = order.scheduledTime.ifBlank { "Entrega Inmediata" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // DESGLOSE POR PUESTO / EMPRENDIMIENTO
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_store_custom),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "DETALLE DE PRODUCTOS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${order.subOrders.size} Puesto(s)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            order.subOrders.forEachIndexed { index, subOrder ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header Puesto
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = subOrder.sellerName.ifBlank { "Emprendedor #${index + 1}" },
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            ReceiptSubOrderStatusBadge(status = subOrder.status)
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // Lista de Ítems
                                        subOrder.items.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        modifier = Modifier.padding(vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "${item.quantity}x",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = item.productName,
                                                        fontSize = 12.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "S/ %.2f".format(item.subtotal),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Calificación del puesto si está completado
                                        val canRateSub = subOrder.status == SubOrderStatus.COMPLETADO ||
                                                subOrder.status == SubOrderStatus.PAGO_CONFIRMADO ||
                                                isCompleted
                                        if (canRateSub) {
                                            val rating = reviewedOrders["${subOrder.orderId}-${subOrder.sellerId}"]
                                                ?: reviewedOrders[subOrder.id]
                                                ?: if (order.subOrders.size == 1) reviewedOrders[order.id] else null

                                            if (rating != null && rating > 0) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Tu calificación:",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = WarmYellow
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        for (star in 1..5) {
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = null,
                                                                tint = if (star <= rating) WarmYellow else MaterialTheme.colorScheme.outlineVariant,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (onRateSeller != null) {
                                                OutlinedButton(
                                                    onClick = { onRateSeller(subOrder) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmYellow),
                                                    border = BorderStroke(1.dp, WarmYellow),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp)
                                                        .height(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = WarmYellow,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Calificar Puesto",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = WarmYellow
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // LÍNEA PERFORADA DE BOLETA DIGITAL
                        DashedDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.5.dp,
                            dashLength = 6.dp,
                            gapLength = 4.dp
                        )

                        // RESUMEN FINANCIERO (Invoice Breakdown con Método de Pago Presentable)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subtotal de Productos",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "S/ %.2f".format(originalSum),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (hasCancelledSubOrders) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Puestos cancelados",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "-S/ %.2f".format(cancelledSum),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tarifa de Entrega Campus",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = MaterialTheme.extendedColors.successContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "GRATIS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.extendedColors.onSuccessContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Posición Presentable para el Método de Pago con su Logo Oficial
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Método de Pago",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                PaymentMethodPill(method = order.paymentMethod)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            val totalLabel = when {
                                isCompleted -> "TOTAL CANCELADO"
                                isCancelled -> "TOTAL CANCELADO"
                                else -> "TOTAL A PAGAR"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = totalLabel,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "S/ %.2f".format(effectiveTotal),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // SELLO DE AUTENTICIDAD DIGITAL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transacción Verificada • Red Oficial CampusGo",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ==========================================
                // 3. CENTRO DE AYUDA (DISCRETO Y NO ALARMISTA)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showReportDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "¿Tienes alguna duda sobre tu pedido? Centro de ayuda",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Reporte / Mediación (Solo si el usuario voluntariamente lo solicita desde el pie de página)
    if (showReportDialog) {
        val targetSeller = order.subOrders.firstOrNull()
        ReportIncidentDialog(
            title = "Centro de Ayuda y Mediación",
            subtitle = "Puesto: ${targetSeller?.sellerName ?: "Campus"}",
            contextType = IncidentContextType.ORDER,
            isSubmitting = isSubmittingReport,
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonKey, reasonLabel, details ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val result = orderRepository.reportIncident(
                        subOrderId = targetSeller?.id,
                        reporterId = order.buyerId,
                        reportedUserId = targetSeller?.sellerId,
                        incidentType = reasonKey,
                        details = details.ifBlank { reasonLabel }
                    )
                    isSubmittingReport = false
                    showReportDialog = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Solicitud de asistencia enviada al Administrador.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error al enviar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

/**
 * Insignia visual para el estado del subpedido en el comprobante digital.
 */
@Composable
private fun ReceiptSubOrderStatusBadge(status: SubOrderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        SubOrderStatus.PENDIENTE -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Pendiente"
        )
        SubOrderStatus.ACEPTADO -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Aceptado"
        )
        SubOrderStatus.EN_PREPARACION -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            "En Preparación"
        )
        SubOrderStatus.LISTO -> Triple(
            MaterialTheme.extendedColors.successContainer,
            MaterialTheme.extendedColors.onSuccessContainer,
            "Listo"
        )
        SubOrderStatus.ESPERANDO_ENTREGA -> Triple(
            MaterialTheme.extendedColors.successContainer,
            MaterialTheme.extendedColors.onSuccessContainer,
            "Esperando"
        )
        SubOrderStatus.PAGO_CONFIRMADO, SubOrderStatus.COMPLETADO -> Triple(
            MaterialTheme.extendedColors.successContainer,
            MaterialTheme.extendedColors.success,
            "Completado"
        )
        SubOrderStatus.RECHAZADO -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Rechazado"
        )
        SubOrderStatus.CANCELADO -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Cancelado"
        )
        SubOrderStatus.NO_ENTREGADO -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "No entregado"
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
