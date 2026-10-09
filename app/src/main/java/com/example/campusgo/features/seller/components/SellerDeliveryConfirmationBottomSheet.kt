package com.example.campusgo.features.seller.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.PaymentMethod
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.orderCodeDisplay
import com.example.campusgo.domain.model.verificationCode
import com.example.campusgo.theme.LocalDarkTheme
import com.example.campusgo.ui.components.CodeSlotStatus
import com.example.campusgo.ui.components.CodeSlots
import com.example.campusgo.ui.components.PaymentMethodLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDeliveryConfirmationBottomSheet(
    subOrder: SubOrder,
    onDismiss: () -> Unit,
    onConfirm: (subOrderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    var inputCode by remember { mutableStateOf("") }
    var bypassCode by remember { mutableStateOf(false) }

    val actualCode = subOrder.verificationCode
    val isCodeValid = inputCode.length == 4 && (actualCode.isNullOrBlank() || inputCode == actualCode)
    val codeStatus = when {
        inputCode.length < 4 -> CodeSlotStatus.IDLE
        isCodeValid -> CodeSlotStatus.SUCCESS
        else -> CodeSlotStatus.ERROR
    }
    val canConfirm = isCodeValid || bypassCode

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Cabecera superior compacta y moderna con botón Atrás y botón Cerrar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE6F7F3),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Confirmar Entrega y Cobro",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Cliente: ${subOrder.buyerName?.ifBlank { "Estudiante" } ?: "Estudiante"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "S/ %.2f".format(subOrder.subtotalAmount),
                                    fontSize = 12.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884),
                                    fontWeight = FontWeight.ExtraBold
                                )
                                subOrder.paymentMethod?.let { pm ->
                                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    PaymentMethodLogo(method = pm, size = 13.dp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Contenido desplazable con scroll nativo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Tarjeta central interactiva de CodeSlots
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Solicita al estudiante su PIN de 4 dígitos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Componente interactivo CodeSlots
                        CodeSlots(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            length = 4,
                            status = codeStatus,
                            autoFocus = false,
                            slotSize = 48.dp,
                            height = 54.dp,
                            gap = 8.dp,
                            radius = 12.dp,
                            accentColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884),
                            dangerColor = Color(0xFFEF4444),
                            successColor = Color(0xFF16A34A)
                        )

                        // Mensajes de retroalimentación de estado
                        AnimatedVisibility(
                            visible = codeStatus == CodeSlotStatus.SUCCESS,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Código PIN verificado correctamente",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = codeStatus == CodeSlotStatus.ERROR,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Código PIN incorrecto",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }

                // Botón de acción principal
                Button(
                    onClick = { onConfirm(subOrder.id) },
                    enabled = canConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (canConfirm) Color.White else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (isCodeValid) "Confirmar y Cobrar S/ %.2f".format(subOrder.subtotalAmount)
                                   else if (bypassCode) "Confirmar Entrega (Sin Código)"
                                   else "Ingresa el PIN de 4 dígitos",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canConfirm) Color.White else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8))
                        )
                    }
                }

                // Opción alternativa compacta de contingencia
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { bypassCode = !bypassCode }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = bypassCode,
                        onCheckedChange = { bypassCode = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¿Comprador sin celular? Confirmar sin código",
                        fontSize = 12.sp,
                        color = if (bypassCode) (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF00A884)) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (bypassCode) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
}
