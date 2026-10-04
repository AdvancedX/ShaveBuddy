package com.shavebuddy.demo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF28665E), onPrimary = Color.White,
    primaryContainer = Color(0xFFDEEBE5), onPrimaryContainer = Color(0xFF214D45),
    secondary = Color(0xFF4C746A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEEBE5), onSecondaryContainer = Color(0xFF214D45),
    background = Color(0xFFF7F6F0), onBackground = Color(0xFF252E2B),
    surface = Color(0xFFFDFCF8), onSurface = Color(0xFF252E2B),
    surfaceVariant = Color(0xFFEBEDE6), onSurfaceVariant = Color(0xFF606C65),
    surfaceContainerLowest = Color(0xFFFFFEFA),
    surfaceContainerLow = Color(0xFFF3F4ED),
    surfaceContainer = Color(0xFFEDF0E9),
    surfaceContainerHigh = Color(0xFFE7ECE4),
    surfaceContainerHighest = Color(0xFFE0E8DF),
    outline = Color(0xFF7B8780), outlineVariant = Color(0xFFC7D2C9),
    tertiaryContainer = Color(0xFFF4E7CA), onTertiaryContainer = Color(0xFF735722),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3D0C0), onPrimary = Color(0xFF143B33),
    primaryContainer = Color(0xFF244E44), onPrimaryContainer = Color(0xFFCEEADE),
    secondary = Color(0xFFAFD1C3), onSecondary = Color(0xFF183B33),
    secondaryContainer = Color(0xFF31584D), onSecondaryContainer = Color(0xFFDBEFE5),
    background = Color(0xFF141C19), onBackground = Color(0xFFE1E8E1),
    surface = Color(0xFF1D2622), onSurface = Color(0xFFE1E8E1),
    surfaceVariant = Color(0xFF2D3932), onSurfaceVariant = Color(0xFFB5C1B9),
    surfaceContainerLowest = Color(0xFF101815),
    surfaceContainerLow = Color(0xFF202B25),
    surfaceContainer = Color(0xFF27332B),
    surfaceContainerHigh = Color(0xFF2F3B32),
    surfaceContainerHighest = Color(0xFF39463B),
    outline = Color(0xFF93A499), outlineVariant = Color(0xFF4D5D52),
    tertiaryContainer = Color(0xFF584421), onTertiaryContainer = Color(0xFFF4E0AE),
)

@Composable
fun ShaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
