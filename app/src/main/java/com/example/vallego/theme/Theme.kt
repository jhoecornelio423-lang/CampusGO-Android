package com.example.vallego.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusGoLightColorScheme = lightColorScheme(
    primary = Color(0xFF16A085),          // Verde Turquesa (Principal)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F6F3),
    onPrimaryContainer = Color(0xFF0D5C4C),
    secondary = Color(0xFFF4B942),        // Amarillo Cálido (Acentos & Promos)
    onSecondary = Color(0xFF16324F),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF16324F),         // Azul Oscuro (Contraste & Encabezados)
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),       // Blanco puro (Fondo principal)
    onBackground = Color(0xFF16324F),
    surface = Color(0xFFFFFFFF),          // Superficie blanca
    onSurface = Color(0xFF16324F),
    surfaceVariant = Color(0xFFF4F6F8),   // Gris claro secundario
    onSurfaceVariant = Color(0xFF4B5563), // Texto gris
    outline = Color(0xFFE5E7EB)
)

@Composable
fun CampusGoTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CampusGoLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ValleGOTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CampusGoTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
