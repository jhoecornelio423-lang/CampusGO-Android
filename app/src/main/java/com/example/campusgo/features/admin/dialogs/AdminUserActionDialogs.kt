package com.example.campusgo.features.admin.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusgo.domain.model.SellerApplication
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.model.UserRole
import com.example.campusgo.theme.ThemeManager
import com.example.campusgo.ui.components.CampusGoDialogContainerColor
import com.example.campusgo.ui.components.CampusGoDialogShape
import com.example.campusgo.ui.components.CampusGoDialogTonalElevation
import com.example.campusgo.ui.components.campusGoDialogStyle

@Composable
fun RejectApplicationDialog(
    application: SellerApplication,
    onDismiss: () -> Unit,
    onConfirm: (applicationId: String, reason: String) -> Unit
) {
    var reasonSelected by remember { mutableStateOf("Falta permiso de bienestar universitario") }
    val commonReasons = listOf(
        "Falta permiso de bienestar universitario",
        "Ubicación propuesta no autorizada",
        "Giro comercial saturado en este turno",
        "Información del estudiante incompleta",
        "Productos no autorizados para venta en campus"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CampusGoDialogShape,
        containerColor = CampusGoDialogContainerColor,
        tonalElevation = CampusGoDialogTonalElevation,
        modifier = Modifier.campusGoDialogStyle(),
        title = {
            Text("Rechazar Solicitud", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Indica el motivo para informar al estudiante ${application.applicantName}:",
                    style = MaterialTheme.typography.bodySmall
                )
                commonReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reasonSelected = reason }
                    ) {
                        RadioButton(
                            selected = reasonSelected == reason,
                            onClick = { reasonSelected = reason }
                        )
                        Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(application.id, reasonSelected) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirmar Rechazo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SuspendSellerDialog(
    seller: UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (sellerId: String, reason: String) -> Unit
) {
    var suspensionReason by remember { mutableStateOf("Incumplimiento reiterado de entregas / horario") }
    val commonReasons = listOf(
        "Incumplimiento reiterado de entregas / horario",
        "Quejas de calidad o higiene de alimentos",
        "Venta de productos no autorizados por el campus",
        "Reclamos reiterados de cobro o precios indebidos"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CampusGoDialogShape,
        containerColor = CampusGoDialogContainerColor,
        tonalElevation = CampusGoDialogTonalElevation,
        modifier = Modifier.campusGoDialogStyle(),
        title = {
            Text("Suspender Puesto de Venta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Se pausarán las ventas de '${seller.displayStoreName}'. El puesto cambiará a 'SUSPENDIDO' y no recibirá pedidos hasta su reactivación.",
                    style = MaterialTheme.typography.bodySmall
                )
                commonReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { suspensionReason = reason }
                    ) {
                        RadioButton(
                            selected = suspensionReason == reason,
                            onClick = { suspensionReason = reason }
                        )
                        Text(text = reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(seller.id, suspensionReason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirmar Suspensión")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SuspendBuyerDialog(
    buyer: UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (buyerId: String, reason: String) -> Unit
) {
    var suspensionReason by remember { mutableStateOf("Incumplimiento reiterado de recojo de pedidos") }
    val commonBuyerReasons = listOf(
        "Incumplimiento reiterado de recojo de pedidos",
        "Falta de respeto o conducta indebida en el campus",
        "Reclamos reiterados por parte de vendedores",
        "Incumplimiento de pagos o comprobantes inválidos",
        "Uso indebido de la plataforma"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CampusGoDialogShape,
        containerColor = CampusGoDialogContainerColor,
        tonalElevation = CampusGoDialogTonalElevation,
        modifier = Modifier.campusGoDialogStyle(),
        title = {
            Text("Suspender Cuenta de Comprador", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Se suspenderá la cuenta del estudiante '${buyer.fullName}'. El usuario no podrá realizar pedidos hasta que un administrador reactive su acceso.",
                    style = MaterialTheme.typography.bodySmall
                )
                commonBuyerReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { suspensionReason = reason }
                    ) {
                        RadioButton(
                            selected = suspensionReason == reason,
                            onClick = { suspensionReason = reason }
                        )
                        Text(text = reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(buyer.id, suspensionReason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirmar Suspensión")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun IssueWarningDialog(
    targetUser: UserProfile,
    adminId: String,
    onDismiss: () -> Unit,
    onConfirm: (targetUserId: String, reason: String, adminId: String) -> Unit
) {
    val isSeller = targetUser.role == UserRole.EMPRENDEDOR || targetUser.role == UserRole.SUSPENDED
    var reasonSelected by remember {
        mutableStateOf(
            if (isSeller) "Incumplimiento de horario o entrega acordada"
            else "No se presentó a recoger el pedido al punto de encuentro"
        )
    }
    var customReason by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }

    val commonReasons = if (isSeller) {
        listOf(
            "Incumplimiento de horario o entrega acordada",
            "Producto no coincide con la descripción o calidad",
            "Cobro o precio indebido fuera de la plataforma",
            "Falta de respeto o trato inapropiado al comprador"
        )
    } else {
        listOf(
            "No se presentó a recoger el pedido al punto de encuentro",
            "Falta de respeto o trato indebido al vendedor",
            "Cancelaciones reiteradas injustificadas",
            "Incumplimiento de pago o falta de comprobante"
        )
    }

    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val warningColor = if (isDarkMode) Color(0xFFFB923C) else Color(0xFFE65100)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CampusGoDialogShape,
        containerColor = CampusGoDialogContainerColor,
        tonalElevation = CampusGoDialogTonalElevation,
        modifier = Modifier.campusGoDialogStyle(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = warningColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Llamada de Atención",
                    fontWeight = FontWeight.Bold,
                    color = warningColor
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Emitir llamada de atención oficial a: ${if (isSeller) targetUser.displayStoreName else targetUser.fullName}. Quedará registrada permanentemente en su historial disciplinario.",
                    style = MaterialTheme.typography.bodySmall
                )
                commonReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                reasonSelected = reason
                                isCustom = false
                            }
                    ) {
                        RadioButton(
                            selected = !isCustom && reasonSelected == reason,
                            onClick = {
                                reasonSelected = reason
                                isCustom = false
                            }
                        )
                        Text(text = reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true }
                ) {
                    RadioButton(
                        selected = isCustom,
                        onClick = { isCustom = true }
                    )
                    Text(text = "Otro motivo personalizado...", style = MaterialTheme.typography.bodySmall)
                }

                if (isCustom) {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        placeholder = { Text("Escribe el motivo detallado...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            val finalReason = if (isCustom) customReason.trim() else reasonSelected
            Button(
                onClick = { onConfirm(targetUser.id, finalReason, adminId) },
                enabled = finalReason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = warningColor)
            ) {
                Text("Emitir Advertencia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
