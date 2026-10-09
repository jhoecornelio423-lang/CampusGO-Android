package com.example.campusgo.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Paleta Material 3: Modo Claro
// ==========================================
val CampusGoLightColorScheme = lightColorScheme(
    primary = Turquoise500,
    onPrimary = Color.White,
    primaryContainer = Turquoise50,
    onPrimaryContainer = Turquoise800,
    secondary = Amber400,
    onSecondary = Amber900,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber800,
    tertiary = InfoLight,
    onTertiary = Color.White,
    tertiaryContainer = InfoLightContainer,
    onTertiaryContainer = OnInfoLightContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorLightContainer,
    onErrorContainer = OnErrorLightContainer
)

// ==========================================
// Paleta Material 3: Modo Oscuro (Obsidian)
// ==========================================
val CampusGoDarkColorScheme = darkColorScheme(
    primary = Turquoise600,
    onPrimary = Color(0xFF00281F),
    primaryContainer = Color(0xFF004D3D),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Amber500,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Amber800,
    onSecondaryContainer = Amber200,
    tertiary = InfoDark,
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = InfoDarkContainer,
    onTertiaryContainer = OnInfoDarkContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    onError = Color(0xFF450A0A),
    errorContainer = ErrorDarkContainer,
    onErrorContainer = OnErrorDarkContainer
)

// ==========================================
// Colores Semánticos Extendidos para CampusGO
// ==========================================
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val brandTurquoise: Color,
    val brandAmber: Color
)

private val LightExtendedColors = ExtendedColors(
    success = SuccessLight,
    onSuccess = Color.White,
    successContainer = SuccessLightContainer,
    onSuccessContainer = OnSuccessLightContainer,
    warning = WarningLight,
    onWarning = Color.White,
    warningContainer = WarningLightContainer,
    onWarningContainer = OnWarningLightContainer,
    info = InfoLight,
    onInfo = Color.White,
    infoContainer = InfoLightContainer,
    onInfoContainer = OnInfoLightContainer,
    cardBackground = LightSurface,
    cardBorder = LightOutlineVariant,
    inputBackground = LightSurfaceVariant,
    inputBorder = LightOutline,
    textPrimary = LightOnBackground,
    textSecondary = LightOnSurfaceVariant,
    textMuted = Color(0xFF94A3B8),
    brandTurquoise = Turquoise500,
    brandAmber = Amber400
)

private val DarkExtendedColors = ExtendedColors(
    success = SuccessDark,
    onSuccess = Color(0xFF022C22),
    successContainer = SuccessDarkContainer,
    onSuccessContainer = OnSuccessDarkContainer,
    warning = WarningDark,
    onWarning = Color(0xFF451A03),
    warningContainer = WarningDarkContainer,
    onWarningContainer = OnWarningDarkContainer,
    info = InfoDark,
    onInfo = Color(0xFF082F49),
    infoContainer = InfoDarkContainer,
    onInfoContainer = OnInfoDarkContainer,
    cardBackground = DarkSurface,
    cardBorder = DarkOutlineVariant,
    inputBackground = DarkSurfaceVariant,
    inputBorder = DarkOutline,
    textPrimary = DarkOnBackground,
    textSecondary = DarkOnSurfaceVariant,
    textMuted = Color(0xFF64748B),
    brandTurquoise = Turquoise600,
    brandAmber = Amber500
)

val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * Acceso directo y centralizado a los colores extendidos de CampusGO.
 * Ejemplo de uso en cualquier Composable:
 * `MaterialTheme.extendedColors.successContainer` o `MaterialTheme.campusColors.cardBorder`
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

val MaterialTheme.campusColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

// ==========================================
// Theme Principal de CampusGO
// ==========================================
@Composable
fun CampusGOTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CampusGoDarkColorScheme else CampusGoLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalExtendedColors provides extendedColors
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
