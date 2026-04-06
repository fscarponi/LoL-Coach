package com.lolcoach.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// League of Legends Inspired Palette (Hextech / Deep Sea / Gold)
object LolColors {
    val Gold = Color(0xFFC89B3C)
    val GoldLight = Color(0xFFF0E6D2)
    val BlueDeep = Color(0xFF010A13)
    val BlueMedium = Color(0xFF091428)
    val BlueLight = Color(0xFF0AC8B9)
    val BlueDark = Color(0xFF05111B)
    val Accent = Color(0xFFCDA243)
    val Danger = Color(0xFFEE2B2B)
    val Success = Color(0xFF33D7FF)
    val Surface = Color(0xFF1E2328)
    val OnSurface = Color(0xFFF0E6D2)
    val Border = Color(0xFF3C3C41)
}

private val LolDarkColorScheme = darkColorScheme(
    primary = LolColors.Gold,
    onPrimary = LolColors.BlueDeep,
    secondary = LolColors.BlueLight,
    onSecondary = LolColors.BlueDeep,
    tertiary = LolColors.Accent,
    background = LolColors.BlueDeep,
    onBackground = LolColors.OnSurface,
    surface = LolColors.Surface,
    onSurface = LolColors.OnSurface,
    outline = LolColors.Border,
    error = LolColors.Danger
)

val LolTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.5.sp,
        color = LolColors.GoldLight
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp,
        color = LolColors.Gold
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = LolColors.OnSurface
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = LolColors.Gold.copy(alpha = 0.7f)
    )
)

@Composable
fun LolCoachTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Forcing Dark Theme for that LoL Client feel
    MaterialTheme(
        colorScheme = LolDarkColorScheme,
        typography = LolTypography,
        content = content
    )
}
