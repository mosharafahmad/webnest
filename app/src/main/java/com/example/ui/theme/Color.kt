package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Google Chrome-inspired core palette
val ChromeBlue = Color(0xFF4285F4)
val ChromeBlueDark = Color(0xFF1A73E8)
val ChromeBlueLight = Color(0xFFE8F0FE)
val ChromeBlueDarkMode = Color(0xFF8AB4F8)

val ChromeGreen = Color(0xFF34A853)
val ChromeGreenLight = Color(0xFFE6F4EA)
val ChromeGreenDarkMode = Color(0xFF81C995)

val ChromeYellow = Color(0xFFFBBC05)
val ChromeYellowDarkText = Color(0xFFB06000)
val ChromeYellowLight = Color(0xFFFEF7E0)
val ChromeYellowDarkMode = Color(0xFFFDD663)

val ChromeRed = Color(0xFFEA4335)
val ChromeRedLight = Color(0xFFFCE8E6)
val ChromeRedDarkMode = Color(0xFFF28B82)

// Light Theme Surfaces & Text
val WebNestBgLight = Color(0xFFF8FAFD)
val WebNestSurfaceLight = Color(0xFFFFFFFF)
val WebNestSurfaceVariantLight = Color(0xFFF1F4F9)
val WebNestTextPrimaryLight = Color(0xFF202124)
val WebNestTextSecondaryLight = Color(0xFF5F6368)
val WebNestBorderLight = Color(0xFFDADCE0)

// Dark Theme Surfaces & Text
val WebNestBgDark = Color(0xFF0F172A)
val WebNestSurfaceDark = Color(0xFF1E293B)
val WebNestSurfaceVariantDark = Color(0xFF334155)
val WebNestTextPrimaryDark = Color(0xFFF8FAFC)
val WebNestTextSecondaryDark = Color(0xFF94A3B8)
val WebNestBorderDark = Color(0xFF334155)

data class AccentPalette(
    val name: String,
    val primary: Color,
    val containerLight: Color,
    val containerDark: Color
)

val BookmarkAccentPalettes = listOf(
    AccentPalette(
        name = "Blue",
        primary = ChromeBlue,
        containerLight = ChromeBlueLight,
        containerDark = Color(0xFF1E3A5F)
    ),
    AccentPalette(
        name = "Green",
        primary = ChromeGreen,
        containerLight = ChromeGreenLight,
        containerDark = Color(0xFF143D29)
    ),
    AccentPalette(
        name = "Yellow",
        primary = ChromeYellow,
        containerLight = ChromeYellowLight,
        containerDark = Color(0xFF42320B)
    ),
    AccentPalette(
        name = "Red",
        primary = ChromeRed,
        containerLight = ChromeRedLight,
        containerDark = Color(0xFF4C1D1B)
    )
)
