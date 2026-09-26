package com.example.vallego.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vallego.domain.model.ProfileWarning

/**
 * Badge compacto para directorios y listados que muestra el conteo de strikes de un usuario.
 */
@Composable
fun StrikeBadge(
    strikes: Int,
    modifier: Modifier = Modifier,
    showAutoSuspensionLabel: Boolean = false
) {
    val bgColor = when {
        strikes >= 5 -> Color(0xFFFFEBEE)
        strikes in 1..4 -> Color(0xFFFFF3E0)
        else -> Color(0xFFE8F5E9)
    }

    val textColor = when {
        strikes >= 5 -> Color(0xFFC8102E)
        strikes in 1..4 -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }

    val icon: ImageVector = when {
        strikes >= 5 -> Icons.Default.Dangerous
        strikes in 1..4 -> Icons.Default.Warning
        else -> Icons.Default.CheckCircle
    }

    val labelText = when {
        strikes >= 5 && showAutoSuspensionLabel -> "$strikes/5 strikes (Suspendido)"
        strikes >= 5 -> "$strikes/5 strikes"
        strikes in 1..4 -> "$strikes/5 strikes"
        else -> "0 strikes"
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Medidor horizontal de 5 pasos para visualizar el progreso hacia la suspensión automática.
 */
@Composable
fun StrikeMeter(
    strikes: Int,
    maxStrikes: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (step in 1..maxStrikes) {
                val isActive = strikes >= step
                val isMaxReached = strikes >= maxStrikes

                val segmentColor = when {
                    !isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    isMaxReached -> Color(0xFFC8102E)
                    else -> Color(0xFFE65100)
                }

                val borderColor = when {
                    !isActive -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    isMaxReached -> Color(0xFFB71C1C)
                    else -> Color(0xFFD84315)
                }

                Surface(
                    color = segmentColor,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isMaxReached) Icons.Default.Dangerous else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$step",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Text(
                                text = "$step",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Subtítulo con rango y estado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1er strike: Advertencia",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "5to: Suspensión",
                style = MaterialTheme.typography.labelSmall,
                color = if (strikes >= 5) Color(0xFFC8102E) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (strikes >= 5) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Tarjeta completa de Gestión y Auditoría de Strikes para perfiles de Vendedor y Comprador.
 */
@Composable
fun StrikeManagementCard(
    strikes: Int,
    warnings: List<ProfileWarning>,
    isSeller: Boolean,
    isSuspended: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (strikes >= 5 || isSuspended) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (strikes >= 5) Color(0xFFFFCDD2)
                            else if (strikes > 0) Color(0xFFFFE082)
                            else Color(0xFFC8E6C9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (strikes >= 5) Icons.Default.Dangerous else if (strikes > 0) Icons.Default.WarningAmber else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (strikes >= 5) Color(0xFFC8102E) else if (strikes > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Sistema Disciplinario de Strikes",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (strikes >= 5) Color(0xFFC8102E) else Color(0xFF003366)
                    )
                    Text(
                        text = if (isSeller) "Control de conducta y cumplimiento de puesto" else "Control de conducta del comprador",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Explicación de la regla de los 5 strikes
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF003366).copy(alpha = 0.15f)),
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
                        tint = Color(0xFF003366),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Regla de Seguridad Campus: Cada llamada de atención oficial equivale a 1 strike. Al recibir 5 strikes, la cuenta es suspendida automáticamente por el sistema de forma inmediata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E293B),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Medidor visual de 5 pasos
            StrikeMeter(strikes = strikes, maxStrikes = 5)

            // Historial detallado de strikes/advertencias emitidas
            if (warnings.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Historial Detallado (${warnings.size} ${if (warnings.size == 1) "strike emitido" else "strikes emitidos"}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF003366),
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    warnings.forEachIndexed { index, warning ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (strikes >= 5) Color(0xFFFFCDD2) else Color(0xFFFFE082)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = if (strikes >= 5) Color(0xFFC8102E) else Color(0xFFE65100),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Strike #${warnings.size - index}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "Advertencia Oficial",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Text(
                                        text = warning.createdAt?.take(10) ?: "Reciente",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = warning.reason,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
