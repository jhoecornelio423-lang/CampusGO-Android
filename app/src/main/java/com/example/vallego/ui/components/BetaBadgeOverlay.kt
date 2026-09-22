package com.example.vallego.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Marca de agua sutil y completamente no interactiva que muestra la versión beta.
 * No consume eventos táctiles, no obstaculiza botones ni áreas de pulsación y
 * tiene un estilo minimalista translúcido.
 */
@Composable
fun BetaBadgeOverlay(
    versionName: String = "0.5.2",
    modifier: Modifier = Modifier
) {
    // Marca de agua deshabilitada
}
