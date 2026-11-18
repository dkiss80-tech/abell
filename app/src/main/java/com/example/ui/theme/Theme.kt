package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AcademyIndigo,
    onPrimary = Color.White,
    primaryContainer = AcademyIndigoContainer,
    onPrimaryContainer = AcademyOnIndigoContainer,
    secondary = StartBellAmber,
    onSecondary = Color.White,
    secondaryContainer = StartBellAmberContainer,
    onSecondaryContainer = StartBellOnContainer,
    tertiary = EndBellTeal,
    onTertiary = Color.White,
    tertiaryContainer = EndBellTealContainer,
    onTertiaryContainer = EndBellOnContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertOnRedContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkStartBellAmber,
    onSecondary = Color(0xFF2B1300),
    secondaryContainer = DarkStartBellContainer,
    onSecondaryContainer = DarkStartBellOnContainer,
    tertiary = DarkEndBellTeal,
    onTertiary = Color(0xFF00201D),
    tertiaryContainer = DarkEndBellContainer,
    onTertiaryContainer = DarkEndBellOnContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkAlertRed,
    onError = Color(0xFF450A0A),
    errorContainer = DarkAlertRedContainer,
    onErrorContainer = DarkAlertOnRedContainer,
)

val AcademyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AcademyBellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AcademyTypography,
        shapes = AcademyShapes,
        content = content,
    )
}
