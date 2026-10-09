package com.example.campusgo.ui.components

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
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.theme.extendedColors

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
        strikes >= 5 -> MaterialTheme.colorScheme.errorContainer
        strikes in 1..4 -> MaterialTheme.extendedColors.warningContainer
        else -> MaterialTheme.extendedColors.successContainer
    }

    val textColor = when {
        strikes >= 5 -> MaterialTheme.colorScheme.onErrorContainer
        strikes in 1..4 -> MaterialTheme.extendedColors.onWarningContainer
        else -> MaterialTheme.extendedColors.onSuccessContainer
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
                    isMaxReached -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.extendedColors.warning
                }

                val borderColor = when {
                    !isActive -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    isMaxReached -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    else -> MaterialTheme.extendedColors.warning.copy(alpha = 0.8f)
                }

                val contentTint = if (isMaxReached) MaterialTheme.colorScheme.onError else MaterialTheme.extendedColors.onWarning

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
                                    tint = contentTint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$step",
                                    color = contentTint,
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
                color = if (strikes >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = if (strikes >= 5 || isSuspended) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
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
                val iconBoxBg = if (strikes >= 5) MaterialTheme.colorScheme.errorContainer
                    else if (strikes > 0) MaterialTheme.extendedColors.warningContainer
                    else MaterialTheme.extendedColors.successContainer

                val iconTint = if (strikes >= 5) MaterialTheme.colorScheme.error
                    else if (strikes > 0) MaterialTheme.extendedColors.warning
                    else MaterialTheme.extendedColors.success

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (strikes >= 5) Icons.Default.Dangerous else if (strikes > 0) Icons.Default.WarningAmber else Icons.Default.Shield,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Sistema Disciplinario de Strikes",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (strikes >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Regla de Seguridad Campus: Cada llamada de atención oficial equivale a 1 strike. Al recibir 5 strikes, la cuenta es suspendida automáticamente por el sistema de forma inmediata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    warnings.forEachIndexed { index, warning ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (strikes >= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.extendedColors.warning.copy(alpha = 0.5f)),
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
                                        val badgeBg = if (strikes >= 5) MaterialTheme.colorScheme.error else MaterialTheme.extendedColors.warning
                                        val badgeText = if (strikes >= 5) MaterialTheme.colorScheme.onError else MaterialTheme.extendedColors.onWarning
                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Strike #${warnings.size - index}",
                                                color = badgeText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "Advertencia Oficial",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
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
