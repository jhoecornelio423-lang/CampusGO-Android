package com.example.campusgo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.R

enum class IncidentContextType {
    SELLER,
    BUYER,
    ORDER
}

data class IncidentReasonOption(
    val key: String,
    val label: String,
    val description: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentDialog(
    title: String,
    subtitle: String? = null,
    contextType: IncidentContextType,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (reasonKey: String, reasonLabel: String, details: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(contextType) {
        when (contextType) {
            IncidentContextType.SELLER -> listOf(
                IncidentReasonOption(
                    "WRONG_DAMAGED_PRODUCT",
                    "Producto vencido, antihigiénico o en mal estado",
                    "El puesto entregó alimentos o productos no aptos para consumo"
                ),
                IncidentReasonOption(
                    "UNAUTHORIZED_CHARGE",
                    "Precios engañosos o cobro mayor al publicado",
                    "Se exigió un precio superior al confirmado en la app"
                ),
                IncidentReasonOption(
                    "NO_SHOW_SELLER",
                    "Vendedor no se presentó al punto acordado",
                    "No acudió a la entrega en el horario pactado"
                ),
                IncidentReasonOption(
                    "INAPPROPRIATE_BEHAVIOR",
                    "Conducta inapropiada o falta de respeto",
                    "Trato inadecuado que vulnera el reglamento del campus"
                ),
                IncidentReasonOption(
                    "STORE_UNAVAILABLE",
                    "Puesto inactivo o no atiende pedidos",
                    "Figura abierto pero no responde ni despacha"
                ),
                IncidentReasonOption(
                    "SCAM_SUSPICION",
                    "Sospecha de fraude o suplantación",
                    "Puesto sospechoso o cuenta no autorizada"
                ),
                IncidentReasonOption(
                    "OTHER",
                    "Otro motivo o irregularidad",
                    "Especifica el motivo en los detalles"
                )
            )
            IncidentContextType.BUYER -> listOf(
                IncidentReasonOption(
                    "NO_SHOW_BUYER",
                    "Comprador no se presentó al punto (No-Show)",
                    "El alumno no recogió su pedido tras esperar en el punto acordado"
                ),
                IncidentReasonOption(
                    "UNAUTHORIZED_CHARGE",
                    "Negativa de pago del monto pactado",
                    "El comprador no pagó el total o intentó pagar menos"
                ),
                IncidentReasonOption(
                    "INAPPROPRIATE_BEHAVIOR",
                    "Conducta irrespetuosa o disturbio",
                    "Trato indebido en el punto de encuentro universitario"
                ),
                IncidentReasonOption(
                    "CANCELADO_VENDEDOR",
                    "Cancelación injustificada en el lugar de entrega",
                    "Canceló cuando el producto ya estaba preparado y listo"
                ),
                IncidentReasonOption(
                    "OTHER",
                    "Otro motivo con el comprador",
                    "Describe la situación en los detalles"
                )
            )
            IncidentContextType.ORDER -> listOf(
                IncidentReasonOption(
                    "NO_SHOW_SELLER",
                    "El vendedor no asistió al punto de encuentro",
                    "Esperé en el horario y lugar indicado sin que se presente"
                ),
                IncidentReasonOption(
                    "WRONG_DAMAGED_PRODUCT",
                    "Producto incompleto, vencido o defectuoso",
                    "El pedido no correspondía con lo comprado o estaba en mal estado"
                ),
                IncidentReasonOption(
                    "UNAUTHORIZED_CHARGE",
                    "Cobro indebido o solicitó más dinero en persona",
                    "Alteración del monto acordado al momento del pago"
                ),
                IncidentReasonOption(
                    "INAPPROPRIATE_BEHAVIOR",
                    "Mala atención durante la entrega",
                    "Falta de respeto o trato descortés en el campus"
                ),
                IncidentReasonOption(
                    "OTHER",
                    "Otro inconveniente con la compra",
                    "Describe lo sucedido detalladamente"
                )
            )
        }
    }

    var selectedKey by remember { mutableStateOf(options.first().key) }
    var detailsText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
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
            // Cabecera estilo Rappi / iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFE2E8F0).copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 10.dp)
            )

            // Contenido desplazable con amplio espacio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Banner de seguridad institucional
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Este reporte llegará directamente al Panel del Administrador del Campus para su investigación y sanción si corresponde.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Título de la sección de motivos
                Text(
                    text = "SELECCIONA EL MOTIVO PRINCIPAL",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                // Tarjetas seleccionables de motivos (estilo Rappi / iOS)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        val isSelected = selectedKey == option.key
                        Surface(
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmitting) {
                                    selectedKey = option.key
                                    validationError = null
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Indicador circular tipo check/radio iOS
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .let {
                                            if (!isSelected) it.background(Color(0xFFF1F5F9), CircleShape)
                                            else it
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFFCBD5E1), CircleShape)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                    )
                                    option.description?.let { desc ->
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Campo de texto de detalles adicionales
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DETALLES DE LO OCURRIDO",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = detailsText,
                        onValueChange = {
                            if (it.length <= 400) {
                                detailsText = it
                                validationError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Describe claramente qué sucedió, lugar exacto, hora o acuerdos no respetados...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (validationError != null) {
                            Text(
                                text = validationError ?: "",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        Text(
                            text = "${detailsText.length}/400",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Barra inferior fija con botones de acción estilo Rappi
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmed = detailsText.trim()
                            if (selectedKey == "OTHER" && trimmed.length < 5) {
                                validationError = "Por favor detalla el motivo del reporte."
                                return@Button
                            }
                            val selectedOption = options.firstOrNull { it.key == selectedKey } ?: options.first()
                            onSubmit(selectedOption.key, selectedOption.label, trimmed)
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviando reporte...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_report_triangle_custom),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviar Reporte al Campus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
