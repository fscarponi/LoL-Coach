package com.lolcoach.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object LolColors {
    val Gold = Color(0xFFC89B3C)
    val GoldLight = Color(0xFFF0E6D2)
    val GoldDark = Color(0xFF785A28)
    val BlueDeep = Color(0xFF010A13)
    val BlueMedium = Color(0xFF091428)
    val BlueLight = Color(0xFF0AC8B9)
    val BlueDark = Color(0xFF05111B)
    val Accent = Color(0xFFCDA243)
    val Danger = Color(0xFFEE2B2B)
    val DangerMuted = Color(0xFFCF4040)
    val Success = Color(0xFF33D7FF)
    val SuccessGreen = Color(0xFF49B04B)
    val Surface = Color(0xFF1E2328)
    val SurfaceElevated = Color(0xFF252A30)
    val OnSurface = Color(0xFFF0E6D2)
    val Border = Color(0xFF3C3C41)
    val BorderGold = Color(0xFF463714)
    val Muted = Color(0xFF5B5A56)

    val TopBarGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF091428), Color(0xFF0A1929), Color(0xFF091428))
    )
    val CardGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E2328), Color(0xFF1A1F24))
    )
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
        fontSize = 20.sp,
        letterSpacing = 0.5.sp,
        color = LolColors.GoldLight
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.3.sp,
        color = LolColors.Gold
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp,
        color = LolColors.Gold
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp,
        color = LolColors.OnSurface
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = LolColors.OnSurface
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = LolColors.Gold.copy(alpha = 0.7f)
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = LolColors.Gold
    )
)

@Composable
fun LolCoachTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LolDarkColorScheme,
        typography = LolTypography,
        content = content
    )
}
