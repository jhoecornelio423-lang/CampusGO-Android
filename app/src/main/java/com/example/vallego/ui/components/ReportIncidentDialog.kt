package com.example.vallego.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun ReportIncidentDialog(
    title: String,
    subtitle: String? = null,
    contextType: IncidentContextType,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (reasonKey: String, reasonLabel: String, details: String) -> Unit
) {
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

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = Color(0xFFC8102E),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF003366),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mensaje informativo institucional
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Este reporte llegará directamente al Panel del Administrador para su investigación, llamada de atención o sanción.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB7410E),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "Selecciona el motivo principal:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366)
                )

                // Lista de motivos seleccionables
                options.forEach { option ->
                    val isSelected = selectedKey == option.key
                    Surface(
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF003366) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) {
                                selectedKey = option.key
                                validationError = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedKey = option.key
                                    validationError = null
                                },
                                enabled = !isSubmitting,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF003366),
                                    unselectedColor = Color(0xFF94A3B8)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) Color(0xFF003366) else Color(0xFF334155)
                                )
                                option.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Campo de texto para detalles
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Detalles adicionales de lo ocurrido:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF003366)
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
                            .height(110.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF003366),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (validationError != null) {
                            Text(
                                text = validationError ?: "",
                                color = Color(0xFFC8102E),
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
        },
        confirmButton = {
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
                    containerColor = Color(0xFFC8102E),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviando...")
                } else {
                    Text(
                        text = "Enviar Reporte",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Text(
                    text = "Cancelar",
                    color = Color(0xFF64748B)
                )
            }
        }
    )
}
