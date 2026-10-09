package com.example.campusgo.ui.components.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusgo.theme.extendedColors

enum class CampusBadgeVariant {
    Success,
    Warning,
    Error,
    Info,
    Neutral
}

/**
 * Insignia / Badge atómica y estandarizada para toda la aplicación.
 * Reemplaza los cientos de badges con colores quemados para estados de pedidos,
 * estados de tienda ("Abierto", "Cerrado"), advertencias y categorías.
 */
@Composable
fun CampusBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: CampusBadgeVariant = CampusBadgeVariant.Neutral,
    leadingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(100.dp)
) {
    val extended = MaterialTheme.extendedColors
    val (containerColor, contentColor, borderColor) = when (variant) {
        CampusBadgeVariant.Success -> Triple(
            extended.successContainer,
            extended.onSuccessContainer,
            extended.success.copy(alpha = 0.25f)
        )
        CampusBadgeVariant.Warning -> Triple(
            extended.warningContainer,
            extended.onWarningContainer,
            extended.warning.copy(alpha = 0.25f)
        )
        CampusBadgeVariant.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
        )
        CampusBadgeVariant.Info -> Triple(
            extended.infoContainer,
            extended.onInfoContainer,
            extended.info.copy(alpha = 0.25f)
        )
        CampusBadgeVariant.Neutral -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outlineVariant
        )
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
