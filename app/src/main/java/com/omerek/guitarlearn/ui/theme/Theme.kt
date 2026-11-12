package com.omerek.guitarlearn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Wood = Color(0xFF6D4C41)
val WoodDark = Color(0xFF3E2723)
val Amber = Color(0xFFFFB300)
val InTune = Color(0xFF2E7D32)
val OffTune = Color(0xFFC62828)

private val Light = lightColorScheme(
    primary = Wood,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC7),
    onPrimaryContainer = WoodDark,
    secondary = Color(0xFF8D6E63),
    secondaryContainer = Color(0xFFF5E6DA),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFFFF8F0),
    surface = Color(0xFFFFF8F0),
    surfaceVariant = Color(0xFFF3E5D8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = WoodDark,
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFFFDCC7),
    secondary = Color(0xFFD7CCC8),
    tertiary = Amber,
)

@Composable
fun GuitarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
