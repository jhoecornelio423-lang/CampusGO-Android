package com.example.vallego.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val ValleGODarkColorScheme = darkColorScheme(
    primary = Color(0xFF1ABC9C),          // Verde Turquesa brillante
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF70F7D7),
    secondary = Color(0xFFF4B942),        // Amarillo Cálido
    onSecondary = Color(0xFF432C00),
    secondaryContainer = Color(0xFF5F4100),
    onSecondaryContainer = Color(0xFFFFDEA3),
    tertiary = Color(0xFF90CAF9),         // Azul claro para encabezados
    onTertiary = Color(0xFF0D47A1),
    background = Color(0xFF0F172A),       // Slate 900
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),          // Slate 800
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),   // Slate 700
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

@Composable
fun CampusGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ValleGODarkColorScheme else CampusGoLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ValleGOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CampusGoTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
