package com.example.campusgo.theme

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
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFE2E8F0)
)

private val CampusGoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B589),          // Verde Esmeralda CampusGO moderno y limpio
    onPrimary = Color(0xFF00281F),
    primaryContainer = Color(0xFF004D3D),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFF59E0B),        // Ámbar cálido
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF38BDF8),         // Sky blue suave
    onTertiary = Color(0xFF082F49),
    background = Color(0xFF0B0F14),       // Obsidian Deep Carbon neutral
    onBackground = Color(0xFFF0F6FC),     // Off-white suave y descansado
    surface = Color(0xFF161B22),          // Superficie oscura neutra y limpia
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = Color(0xFF21262D),   // Tarjetas y elevaciones intermedias
    onSurfaceVariant = Color(0xFF94A3B8), // Texto secundario neutral
    outline = Color(0xFF30363D),          // Bordes sutiles y limpios
    outlineVariant = Color(0xFF21262D)    // Divisores delicados
)

val LocalDarkTheme = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun CampusGOTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CampusGoDarkColorScheme else CampusGoLightColorScheme
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun CampusGoTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = CampusGOTheme(darkTheme, dynamicColor, content)
