package com.example.campusgo.features.seller.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.ui.components.StrikeMeter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerNotificationsBottomSheet(
    warnings: List<ProfileWarning>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSeller: Boolean = true
) {
    val isDark = LocalDarkTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        .background(
                            if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFCBD5E1),
                            CircleShape
                        )
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header del diálogo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (warnings.isNotEmpty()) {
                            if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
                        } else {
                            if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE6F7F3)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (warnings.isNotEmpty()) Icons.Default.WarningAmber else Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = if (warnings.isNotEmpty()) Color(0xFFDC2626) else (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Avisos y Moderación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (warnings.isEmpty()) (if (isSeller) "Todo en orden con tu puesto" else "Todo en orden con tu cuenta") else "${warnings.size} aviso${if (warnings.size != 1) "s" else ""} registrado${if (warnings.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                // 1. Tarjeta Resumen de Strikes y Moderación del Administrador
                item {
                    if (warnings.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF022C22) else Color(0xFFDCFCE7),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "0 Strikes • Historial Limpio",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF14532D)
                                    )
                                    Text(
                                        text = if (isSeller) "¡Excelente! Tu puesto no registra advertencias formales ni sanciones del administrador del campus." else "¡Excelente! Tu cuenta de comprador no registra advertencias formales ni sanciones del administrador.",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF166534),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${warnings.size} de 5 Strikes Registrados",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = when {
                                                warnings.size >= 5 -> if (isSeller) "Cuenta suspendida automáticamente por acumulación de sanciones." else "Cuenta suspendida automáticamente por acumulación de faltas."
                                                warnings.size == 4 -> if (isSeller) "Riesgo crítico: 1 infracción más causará la suspensión del puesto." else "Riesgo crítico: 1 infracción más suspenderá tu cuenta para hacer pedidos."
                                                warnings.size >= 2 -> "Riesgo moderado (${warnings.size}/5 strikes): evita reincidencias."
                                                else -> "Advertencia formal (1/5 strikes): por favor corrige las conductas reportadas."
                                            },
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFFECACA) else Color(0xFFB91C1C),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                StrikeMeter(strikes = warnings.size, maxStrikes = 5)

                                HorizontalDivider(color = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2))

                                Text(
                                    text = if (isSeller) "Regla Disciplinaria Oficial: Cada llamada de atención oficial equivale a 1 strike. Al acumular 5 strikes, el sistema suspenderá automáticamente tu puesto comercial." else "Regla Disciplinaria Oficial: Cada llamada de atención oficial equivale a 1 strike. Al acumular 5 strikes, el sistema suspenderá automáticamente tu cuenta impidiendo realizar pedidos.",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 2. Desglose detallado de cada Strike
                if (warnings.isNotEmpty()) {
                    item {
                        Text(
                            text = "HISTORIAL DE INFRACCIONES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    itemsIndexed(warnings) { index, warning ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = "Strike #${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Text(
                                        text = warning.formattedDate,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = warning.reason,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )

                                if (!warning.ticketId.isNullOrBlank()) {
                                    Text(
                                        text = "Ref. Ticket: #${warning.ticketId.take(8)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Entendido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
