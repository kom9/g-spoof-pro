package com.gspoof.pro.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val MapBlue = Color(0xFF1A73E8)
private val MapTeal = Color(0xFF12B5CB)
private val MapSurfaceDark = Color(0xFF0E1420)
private val MapSurfaceDark2 = Color(0xFF161D2B)
private val MapGreen = Color(0xFF34A853)
private val MapRed = Color(0xFFEA4335)

private val DarkScheme = darkColorScheme(
    primary = MapBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B3A63),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = MapTeal,
    onSecondary = Color(0xFF00201F),
    tertiary = MapGreen,
    background = Color(0xFF070B12),
    onBackground = Color(0xFFE8EDF5),
    surface = MapSurfaceDark,
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = MapSurfaceDark2,
    onSurfaceVariant = Color(0xFFA9B6C9),
    outline = Color(0xFF2C3849),
    error = MapRed
)

private val LightScheme = lightColorScheme(
    primary = MapBlue,
    secondary = MapTeal,
    tertiary = MapGreen,
    background = Color(0xFFF6F8FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDF1F7),
    outline = Color(0xFFD3DBE6),
    error = MapRed
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun GSpoofTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
