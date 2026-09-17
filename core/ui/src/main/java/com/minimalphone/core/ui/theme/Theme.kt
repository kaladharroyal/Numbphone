package com.minimalphone.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.minimalphone.core.model.AppTheme

val PureBlack = Color(0xFF000000)
val DarkBackground = Color(0xFF0F0F11)
val DarkSurface = Color(0xFF18181B)
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val MinimalAccent = Color(0xFFE2E8F0)
val MutedText = Color(0xFF8E8E93)
val DangerRed = Color(0xFFFF453A)
val SuccessGreen = Color(0xFF32D74B)

// 1. Pure OLED Black
val PureBlackColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    background = PureBlack,
    onBackground = Color(0xFFFFFFFF),
    surface = DarkSurface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF2C2C2E)
)

// 2. E-Ink Paper Monochrome
val EInkPaperColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFF7F7F2),
    background = Color(0xFFF5F5F0),
    onBackground = Color(0xFF181818),
    surface = Color(0xFFEBEBE4),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE0E0D8),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFFD4D4CC)
)

// 3. Warm Amber Minimal
val WarmAmberColorScheme = darkColorScheme(
    primary = Color(0xFFE6A15C),
    onPrimary = Color(0xFF141210),
    background = Color(0xFF12100E),
    onBackground = Color(0xFFF2D3B3),
    surface = Color(0xFF1F1B17),
    onSurface = Color(0xFFE6CCA8),
    surfaceVariant = Color(0xFF2C251F),
    onSurfaceVariant = Color(0xFFB09880),
    outline = Color(0xFF3D332B)
)

val MinimalTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun MinimalTheme(
    appTheme: AppTheme = AppTheme.PURE_BLACK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.PURE_BLACK -> PureBlackColorScheme
        AppTheme.E_INK_PAPER -> EInkPaperColorScheme
        AppTheme.WARM_AMBER -> WarmAmberColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MinimalTypography,
        content = content
    )
}
