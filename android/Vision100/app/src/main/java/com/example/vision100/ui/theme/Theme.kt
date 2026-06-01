package com.example.vision100.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BgGreen,
    secondary = BgRed,
    tertiary = BgWhite,
    background = VisionDarkBackground,
    surface = VisionDarkSurface,
    surfaceVariant = VisionDarkSurfaceVariant,
    primaryContainer = Color(0xFF0B5E48),
    secondaryContainer = Color(0xFF7D1E13),
    tertiaryContainer = Color(0xFF2D3B35),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = VisionInk,
    onPrimaryContainer = Color.White,
    onSecondaryContainer = Color.White,
    onTertiaryContainer = VisionDarkInk,
    onBackground = VisionDarkInk,
    onSurface = VisionDarkInk,
    onSurfaceVariant = Color(0xFFC1D0C9),
    outline = Color(0xFF7D9088),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = BgGreen,
    secondary = BgRed,
    tertiary = BgWhite,
    background = BgWhite,
    surface = VisionSurface,
    surfaceVariant = VisionSurfaceVariant,
    primaryContainer = BgGreenLight,
    secondaryContainer = BgRedLight,
    tertiaryContainer = Color(0xFFF1F4F2),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = VisionInk,
    onPrimaryContainer = Color(0xFF003C2D),
    onSecondaryContainer = Color(0xFF5F130B),
    onTertiaryContainer = VisionInk,
    onBackground = VisionInk,
    onSurface = VisionInk,
    onSurfaceVariant = VisionInkMuted,
    outline = Color(0xFF8CA098),
    error = Color(0xFFBA1A1A)
)

private val VisionShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun Vision100Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VisionShapes,
        content = content
    )
}
