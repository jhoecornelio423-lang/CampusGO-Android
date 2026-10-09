package com.example.campusgo.ui.components.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val DefaultCardShape = RoundedCornerShape(16.dp)

/**
 * Tarjeta base del Design System de CampusGO.
 * Se adapta automáticamente al modo claro y modo oscuro (Obsidian)
 * utilizando los tokens de MaterialTheme y bordes sutiles.
 */
@Composable
fun CampusCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = DefaultCardShape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor
    )
    val cardElevation = if (elevation > 0.dp) CardDefaults.cardElevation(defaultElevation = elevation) else CardDefaults.cardElevation(0.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            border = border,
            elevation = cardElevation
        ) {
            Box { content() }
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            border = border,
            elevation = cardElevation
        ) {
            Box { content() }
        }
    }
}

/**
 * Tarjeta elevada con sombra sutil para elementos flotantes o destacados.
 */
@Composable
fun CampusElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = DefaultCardShape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 3.dp,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable BoxScope.() -> Unit
) {
    CampusCard(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        containerColor = containerColor,
        border = border,
        elevation = elevation,
        content = content
    )
}
