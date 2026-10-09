package com.example.campusgo.features.seller.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.theme.extendedColors
import com.example.campusgo.ui.components.StrikeMeter
import com.example.campusgo.ui.components.campusBottomSheetWindowInsets
import com.example.campusgo.ui.components.preventBottomSheetBounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerNotificationsBottomSheet(
    warnings: List<ProfileWarning>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSeller: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header del diálogo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            shape = CircleShape,
                            color = if (warnings.isNotEmpty()) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.extendedColors.successContainer
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (warnings.isNotEmpty()) Icons.Default.WarningAmber else Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = if (warnings.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.extendedColors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

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
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                // 1. Tarjeta Resumen de Strikes y Moderación del Administrador
                item {
                    if (warnings.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.extendedColors.successContainer,
                            border = BorderStroke(1.dp, MaterialTheme.extendedColors.success.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.extendedColors.success.copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.extendedColors.success,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "0 Strikes • Historial Limpio",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.extendedColors.onSuccessContainer
                                    )
                                    Text(
                                        text = if (isSeller) "¡Excelente! Tu puesto no registra advertencias formales ni sanciones del administrador del campus." else "¡Excelente! Tu cuenta de comprador no registra advertencias formales ni sanciones del administrador.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.extendedColors.onSuccessContainer.copy(alpha = 0.9f),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
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
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${warnings.size} de 5 Strikes Registrados",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = when {
                                                warnings.size >= 5 -> if (isSeller) "Cuenta suspendida automáticamente por acumulación de sanciones." else "Cuenta suspendida automáticamente por acumulación de faltas."
                                                warnings.size == 4 -> if (isSeller) "Riesgo crítico: 1 infracción más causará la suspensión del puesto." else "Riesgo crítico: 1 infracción más suspenderá tu cuenta para hacer pedidos."
                                                warnings.size >= 2 -> "Riesgo moderado (${warnings.size}/5 strikes): evita reincidencias."
                                                else -> "Advertencia formal (1/5 strikes): por favor corrige las conductas reportadas."
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                StrikeMeter(strikes = warnings.size, maxStrikes = 5)

                                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))

                                Text(
                                    text = if (isSeller) "Regla Disciplinaria Oficial: Cada llamada de atención oficial equivale a 1 strike. Al acumular 5 strikes, el sistema suspenderá automáticamente tu puesto comercial." else "Regla Disciplinaria Oficial: Cada llamada de atención oficial equivale a 1 strike. Al acumular 5 strikes, el sistema suspenderá automáticamente tu cuenta impidiendo realizar pedidos.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
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
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = "Strike #${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
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
}
