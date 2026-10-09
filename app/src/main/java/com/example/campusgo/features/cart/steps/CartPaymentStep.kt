package com.example.campusgo.features.cart.steps

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.features.cart.CartUiState
import com.example.campusgo.features.cart.components.CartModeToggleButton
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.ui.components.PaymentMethodLogo
import com.example.campusgo.ui.components.SlideCommit

@Composable
fun CartPaymentStep(
    uiState: CartUiState,
    onModifyDelivery: () -> Unit,
    onModifyProducts: () -> Unit,
    onSetSplitPaymentMode: (Boolean) -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onSelectSellerPaymentMethod: (sellerId: String, PaymentMethod) -> Unit,
    onConfirmOrder: () -> Unit,
    onClearError: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val context = LocalContext.current
    var isDeliveryExpanded by rememberSaveable { mutableStateOf(false) }
    var disabledPaymentNotice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
    ) {
        // Contenido scrolleable
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen de la Entrega seleccionada (Desplegable iOS / Rappi)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isDeliveryExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 250),
                        label = "chevron_delivery_rot"
                    )
                    val deliveryPointSummary = if (uiState.isSplitDeliveryEffective) {
                        "${uiState.calculation.storeGroups.size} puntos seleccionados"
                    } else {
                        uiState.selectedMeetingPoint?.name ?: "Punto no seleccionado"
                    }

                    // Encabezado interactivo para colapsar/expandir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDeliveryExpanded = !isDeliveryExpanded }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_location_custom),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Datos de Entrega",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$deliveryPointSummary • ${uiState.selectedTimeSlot}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onModifyDelivery,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Cambiar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { isDeliveryExpanded = !isDeliveryExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isDeliveryExpanded) "Colapsar datos de entrega" else "Expandir datos de entrega",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(chevronRotation)
                                )
                            }
                        }
                    }

                    // Contenido detallado desplegable
                    AnimatedVisibility(
                        visible = isDeliveryExpanded,
                        enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            // Punto de encuentro
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Punto:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(64.dp)
                                )
                                if (uiState.isSplitDeliveryEffective) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        uiState.calculation.storeGroups.forEach { group ->
                                            val pt = uiState.selectedMeetingPointsBySeller[group.sellerId]
                                            Text(
                                                text = "${group.sellerName}: ${pt?.name ?: "No seleccionado"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = uiState.selectedMeetingPoint?.let { pt ->
                                            val zone = if (pt.zoneType.equals("EXTERIOR", ignoreCase = true)) " (Exterior)" else " (Interior)"
                                            "${pt.name}$zone"
                                        } ?: "No seleccionado",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Horario
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Horario:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(64.dp)
                                )
                                Text(
                                    text = uiState.selectedTimeSlot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Notas si existen
                            if (uiState.orderNotes.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "Notas:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Text(
                                        text = uiState.orderNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Resumen rápido de productos con acceso a modificarlos
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cart_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${uiState.calculation.totalItemCount} producto(s) • Total: S/ %.2f".format(uiState.calculation.grandTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.calculation.storeGroups.size} puesto(s) seleccionado(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TextButton(
                        onClick = onModifyProducts,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Modificar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Sección: Método de Pago (Contra entrega)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Método de Pago (Contra entrega)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Opciones multi-vendedor para Métodos de Pago
                    if (uiState.isMultiSeller) {
                        if (uiState.hasCommonPaymentMethods) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CartModeToggleButton(
                                    selected = !uiState.isSplitPaymentMode,
                                    onClick = { onSetSplitPaymentMode(false) },
                                    label = "Mismo método para todos",
                                    modifier = Modifier.weight(1f)
                                )
                                CartModeToggleButton(
                                    selected = uiState.isSplitPaymentMode,
                                    onClick = { onSetSplitPaymentMode(true) },
                                    label = "Por cada puesto",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info_custom),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Los puestos seleccionados manejan diferentes métodos de pago. Selecciona la opción de pago para cada puesto:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val standardPaymentMethods = listOf(
                        PaymentMethod.YAPE to ("Yape" to Color(0xFF6A1B9A)),
                        PaymentMethod.PLIN to ("Plin" to Color(0xFF00796B)),
                        PaymentMethod.EFECTIVO to ("Efectivo" to Color(0xFF00A884))
                    )

                    if (uiState.isSplitPaymentEffective) {
                        // Selección de método de pago independiente por puesto
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.calculation.storeGroups.forEach { group ->
                                val allowedMethods = uiState.sellerPaymentMethods[group.sellerId] ?: listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO)
                                val selectedMethod = uiState.selectedPaymentMethodsBySeller[group.sellerId] ?: uiState.selectedPaymentMethod

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF8FAFC)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                                    text = group.sellerName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = "S/ %.2f".format(group.subtotal),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            standardPaymentMethods.forEach { (method, info) ->
                                                val (label, containerColor) = info
                                                val isAvailable = method in allowedMethods
                                                val isSelected = isAvailable && selectedMethod == method

                                                Surface(
                                                    onClick = {
                                                        if (isAvailable) {
                                                            onSelectSellerPaymentMethod(group.sellerId, method)
                                                        } else {
                                                            Toast.makeText(context, "${group.sellerName} no acepta $label actualmente", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = when {
                                                        !isAvailable -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                                                        isSelected -> containerColor
                                                        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                                    },
                                                    border = BorderStroke(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = when {
                                                            !isAvailable -> if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
                                                            isSelected -> containerColor
                                                            else -> if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFCBD5E1)
                                                        }
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 4.dp, vertical = 9.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        PaymentMethodLogo(
                                                            method = method,
                                                            size = 16.dp,
                                                            enabled = isAvailable
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = when {
                                                                !isAvailable -> if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color(0xFF94A3B8)
                                                                isSelected -> Color.White
                                                                else -> if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
                                                            },
                                                            maxLines = 1
                                                        )
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Selector único general
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            standardPaymentMethods.forEach { (method, info) ->
                                val (label, containerColor) = info
                                val isAvailable = method in uiState.availablePaymentMethods
                                val isSelected = isAvailable && uiState.selectedPaymentMethod == method

                                Surface(
                                    onClick = {
                                        if (isAvailable) {
                                            disabledPaymentNotice = null
                                            onSelectPaymentMethod(method)
                                        } else {
                                            val notice = "El método de pago $label no está disponible para este pedido porque el vendedor lo tiene deshabilitado."
                                            disabledPaymentNotice = notice
                                            Toast.makeText(context, "El vendedor no acepta $label actualmente", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        !isAvailable -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                                        isSelected -> containerColor
                                        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = when {
                                            !isAvailable -> if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
                                            isSelected -> containerColor
                                            else -> if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFCBD5E1)
                                        }
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PaymentMethodLogo(
                                            method = method,
                                            size = 16.dp,
                                            enabled = isAvailable
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = when {
                                                !isAvailable -> if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color(0xFF94A3B8)
                                                isSelected -> Color.White
                                                else -> if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
                                            },
                                            maxLines = 1
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notificación cuando se presiona un método deshabilitado
                    AnimatedVisibility(
                        visible = disabledPaymentNotice != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_warning_custom),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = disabledPaymentNotice.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { disabledPaymentNotice = null },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            PaymentMethodLogo(method = uiState.selectedPaymentMethod, size = 15.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (uiState.selectedPaymentMethod) {
                                    PaymentMethod.YAPE -> "Pagas al vendedor mediante código QR o número de celular al momento de la entrega en el campus."
                                    PaymentMethod.PLIN -> "Pagas al vendedor mediante código QR o número de celular al momento de la entrega en el campus."
                                    PaymentMethod.EFECTIVO -> "Pagas en efectivo exacto al vendedor al recibir tus productos."
                                    else -> "Coordinas el pago directamente con el vendedor al recibir tu entrega."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Mensaje de Error si existiera
            if (uiState.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Barra inferior flotante con el Total y Botón Deslizable Confirmar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = if (onNavigateBack == null) (82.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) else (12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Total General (${uiState.calculation.totalItemCount} ${if (uiState.calculation.totalItemCount == 1) "producto" else "productos"}):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Pago:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.isSplitPaymentEffective) {
                                Text(
                                    text = "Por cada puesto (${uiState.calculation.storeGroups.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                PaymentMethodLogo(method = uiState.selectedPaymentMethod, size = 13.dp)
                                Text(
                                    text = uiState.selectedPaymentMethod.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "S/ %.2f".format(uiState.calculation.grandTotal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                SlideCommit(
                    onConfirm = onConfirmOrder,
                    enabled = uiState.canCheckout,
                    isSubmitting = uiState.isSubmitting,
                    hasError = uiState.errorMessage != null,
                    onErrorReset = onClearError,
                    isDone = uiState.placedOrder != null,
                    label = "Desliza para confirmar pedido",
                    doneLabel = "¡Pedido confirmado!",
                    errorLabel = "Error al procesar pedido",
                    trackColor = MaterialTheme.colorScheme.primary,
                    handleColor = MaterialTheme.colorScheme.surface,
                    successColor = MaterialTheme.colorScheme.primary,
                    dangerColor = MaterialTheme.colorScheme.error,
                    height = 54.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
