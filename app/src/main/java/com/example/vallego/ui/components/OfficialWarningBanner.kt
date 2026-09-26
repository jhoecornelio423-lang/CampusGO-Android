package com.example.vallego.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.R
import com.example.vallego.domain.model.ProfileWarning

@Composable
fun OfficialWarningBanner(
    warnings: List<ProfileWarning>,
    modifier: Modifier = Modifier,
    isSeller: Boolean = false
) {
    if (warnings.isEmpty()) return

    var showDetailsDialog by remember { mutableStateOf(false) }
    val latestWarning = warnings.firstOrNull() ?: return

    Surface(
        color = Color(0xFFFFF8E1),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDetailsDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFECB3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Llamado de Atención Oficial",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFB7410E)
                    )
                    Surface(
                        color = Color(0xFFE65100),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${warnings.size} ${if (warnings.size == 1) "strike" else "strikes"}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Motivo: ${latestWarning.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para ver el historial y regularizar tu estado →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showDetailsDialog) {
        OfficialWarningDetailDialog(
            warnings = warnings,
            isSeller = isSeller,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
fun OfficialWarningDetailDialog(
    warnings: List<ProfileWarning>,
    isSeller: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ValleGoDialogShape,
        containerColor = ValleGoDialogContainerColor,
        tonalElevation = ValleGoDialogTonalElevation,
        modifier = Modifier.valleGoDialogStyle(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Historial de Advertencias",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003366),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Aclaración reglamentaria
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFC8102E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isSeller) {
                                "El Administrador del Campus emitió estas advertencias por incumplimiento de entrega, calidad o conducta. Al acumular 5 strikes, el sistema suspenderá automáticamente tu puesto comercial."
                            } else {
                                "El Administrador del Campus registró estas llamadas de atención por incidencias reportadas. Al acumular 5 strikes, el sistema suspenderá automáticamente tu cuenta impidiendo realizar pedidos."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF880E4F),
                            fontSize = 12.sp
                        )
                    }
                }

                // Medidor visual de 5 strikes
                StrikeMeter(strikes = warnings.size, maxStrikes = 5)

                Text(
                    text = "Registro de ${warnings.size} ${if (warnings.size == 1) "advertencia emitida" else "advertencias emitidas"}:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF003366)
                )

                warnings.forEachIndexed { index, warning ->
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE082)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Advertencia #${warnings.size - index}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = warning.formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF78909C)
                                )
                            }
                            Text(
                                text = warning.reason,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF263238)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        }
    )
}
