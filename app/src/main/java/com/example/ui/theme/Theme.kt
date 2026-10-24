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
    primary = ChromeBlue,
    onPrimary = Color.White,
    primaryContainer = ChromeBlueLight,
    onPrimaryContainer = ChromeBlueDark,
    secondary = ChromeGreen,
    onSecondary = Color.White,
    secondaryContainer = ChromeGreenLight,
    onSecondaryContainer = Color(0xFF0D652D),
    tertiary = ChromeYellow,
    onTertiary = WebNestTextPrimaryLight,
    tertiaryContainer = ChromeYellowLight,
    onTertiaryContainer = ChromeYellowDarkText,
    error = ChromeRed,
    onError = Color.White,
    errorContainer = ChromeRedLight,
    onErrorContainer = Color(0xFFA50E0E),
    background = WebNestBgLight,
    onBackground = WebNestTextPrimaryLight,
    surface = WebNestSurfaceLight,
    onSurface = WebNestTextPrimaryLight,
    surfaceVariant = WebNestSurfaceVariantLight,
    onSurfaceVariant = WebNestTextSecondaryLight,
    outline = WebNestBorderLight,
    outlineVariant = Color(0xFFE8EAED)
)

private val DarkColorScheme = darkColorScheme(
    primary = ChromeBlueDarkMode,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = ChromeGreenDarkMode,
    onSecondary = Color(0xFF0A3818),
    secondaryContainer = Color(0xFF143D29),
    onSecondaryContainer = Color(0xFFCEEAD6),
    tertiary = ChromeYellowDarkMode,
    onTertiary = Color(0xFF3E2700),
    tertiaryContainer = Color(0xFF42320B),
    onTertiaryContainer = Color(0xFFFEF7E0),
    error = ChromeRedDarkMode,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF4C1D1B),
    onErrorContainer = Color(0xFFF9DEDC),
    background = WebNestBgDark,
    onBackground = WebNestTextPrimaryDark,
    surface = WebNestSurfaceDark,
    onSurface = WebNestTextPrimaryDark,
    surfaceVariant = WebNestSurfaceVariantDark,
    onSurfaceVariant = WebNestTextSecondaryDark,
    outline = WebNestBorderDark,
    outlineVariant = Color(0xFF1E293B)
)

val WebNestShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = WebNestShapes,
        content = content
    )
}
