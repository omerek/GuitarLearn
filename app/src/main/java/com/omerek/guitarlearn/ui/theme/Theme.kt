package com.omerek.guitarlearn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ahşap / gitar paleti
val Wood = Color(0xFF8D5524)
val WoodDark = Color(0xFF4E2A12)
val WoodDeep = Color(0xFF2B1608)
val Amber = Color(0xFFFFB300)
val Cream = Color(0xFFFFF6EA)
val InTune = Color(0xFF2E9E5B)
val NearTune = Color(0xFFF2A516)
val OffTune = Color(0xFFE5483B)

// Akor şeması renkleri (açık/koyu temada aynı kalır)
val Fretboard = Color(0xFF3B2314)
val FretboardLight = Color(0xFF5A3620)
val FretSilver = Color(0xFFC9CED6)
val NutIvory = Color(0xFFF3E9D2)
val StringColor = Color(0xFFE9E4DA)
val FingerColors = listOf(
    Color(0xFFE53935), // 1 işaret
    Color(0xFFFB8C00), // 2 orta
    Color(0xFF43A047), // 3 yüzük
    Color(0xFF1E88E5), // 4 serçe
)

/** Başlık alanlarında kullanılan sıcak ahşap geçişi. */
val HeaderBrush = Brush.linearGradient(listOf(Color(0xFF9C5B2B), WoodDark))
val AccentBrush = Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00)))

private val Light = lightColorScheme(
    primary = Color(0xFF8D4E1F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF765847),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEBD9),
    onSecondaryContainer = Color(0xFF2B1709),
    tertiary = Color(0xFFD35400),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    background = Cream,
    onBackground = Color(0xFF221A15),
    surface = Cream,
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4E4D4),
    onSurfaceVariant = Color(0xFF52443A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFBF6),
    surfaceContainer = Color(0xFFFBEFE3),
    surfaceContainerHigh = Color(0xFFF6E8DA),
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD8C2B4),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2300),
    primaryContainer = Color(0xFF6A3A12),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE5BFA9),
    secondaryContainer = Color(0xFF5C4032),
    onSecondaryContainer = Color(0xFFFFEBD9),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF3E2000),
    tertiaryContainer = Color(0xFF5E3B00),
    background = Color(0xFF1A120D),
    surface = Color(0xFF1A120D),
    surfaceVariant = Color(0xFF3A2C23),
    surfaceContainerLowest = Color(0xFF140D09),
    surfaceContainerLow = Color(0xFF221812),
    surfaceContainer = Color(0xFF271C16),
    surfaceContainerHigh = Color(0xFF32261F),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun GuitarTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
