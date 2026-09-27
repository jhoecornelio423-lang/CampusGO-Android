package com.example.vallego.features.seller.components

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.PaymentMethod
import com.example.vallego.domain.model.SubOrder
import com.example.vallego.domain.model.orderCodeDisplay
import com.example.vallego.domain.model.verificationCode
import com.example.vallego.ui.components.CodeSlotStatus
import com.example.vallego.ui.components.CodeSlots
import com.example.vallego.ui.components.PaymentMethodLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDeliveryConfirmationBottomSheet(
    subOrder: SubOrder,
    onDismiss: () -> Unit,
    onConfirm: (subOrderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var inputCode by remember { mutableStateOf("") }
    var bypassCode by remember { mutableStateOf(false) }

    val expectedCode = remember(subOrder) { subOrder.verificationCode.trim() }

    val isCodeValid = inputCode.trim() == expectedCode
    val codeStatus = remember(inputCode, isCodeValid) {
        when {
            isCodeValid -> CodeSlotStatus.SUCCESS
            inputCode.length == 4 && !isCodeValid -> CodeSlotStatus.ERROR
            else -> CodeSlotStatus.IDLE
        }
    }

    val canConfirm = isCodeValid || bypassCode

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
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Cabecera superior compacta y moderna (fija)
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
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Confirmar Entrega y Cobro",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF16324F)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Orden ${subOrder.orderCodeDisplay}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("•", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "S/ %.2f".format(subOrder.subtotalAmount),
                                fontSize = 12.sp,
                                color = Color(0xFF00A884),
                                fontWeight = FontWeight.ExtraBold
                            )
                            subOrder.paymentMethod?.let { pm ->
                                Text("•", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                PaymentMethodLogo(method = pm, size = 13.dp)
                            }
                        }
                    }
                }

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
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Contenido desplazable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tarjeta central interactiva de CodeSlots
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
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
                            color = Color(0xFF64748B),
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
                            accentColor = Color(0xFF00A884),
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
                                    color = Color(0xFF15803D)
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
                        containerColor = Color(0xFF00A884),
                        disabledContainerColor = Color(0xFFE2E8F0)
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
                            tint = if (canConfirm) Color.White else Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (isCodeValid) "Confirmar y Cobrar S/ %.2f".format(subOrder.subtotalAmount)
                                   else if (bypassCode) "Confirmar Entrega (Sin Código)"
                                   else "Ingresa el PIN de 4 dígitos",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canConfirm) Color.White else Color(0xFF94A3B8)
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
                            checkedColor = Color(0xFF00A884),
                            uncheckedColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¿Comprador sin celular? Confirmar sin código",
                        fontSize = 12.sp,
                        color = if (bypassCode) Color(0xFF00A884) else Color(0xFF64748B),
                        fontWeight = if (bypassCode) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
