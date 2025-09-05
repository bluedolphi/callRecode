package com.example.callrecode.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material Design 3 Dark Color Scheme for Call Recording App
 * Optimized for dark environments with high contrast for recording states
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors - main brand color for recording actions
    primary = RecordingPrimary80,
    onPrimary = Neutral10,
    primaryContainer = RecordingPrimary20,
    onPrimaryContainer = RecordingPrimary80,
    
    // Secondary colors - supporting actions and navigation
    secondary = CallSecondary80,
    onSecondary = Neutral10,
    secondaryContainer = CallSecondary20,
    onSecondaryContainer = CallSecondary80,
    
    // Tertiary colors - accent and audio-related features
    tertiary = AudioTertiary80,
    onTertiary = Neutral10,
    tertiaryContainer = AudioTertiary20,
    onTertiaryContainer = AudioTertiary80,
    
    // Background and surface colors
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    
    // Surface variants for cards and containers
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = Neutral80,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    
    // Outline colors for borders and dividers
    outline = Neutral60,
    outlineVariant = Neutral30,
    
    // Error colors
    error = ErrorRed,
    onError = Neutral99,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/**
 * Material Design 3 Light Color Scheme for Call Recording App
 * Optimized for light environments with clear visual hierarchy
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors - main brand color for recording actions
    primary = RecordingPrimary40,
    onPrimary = Neutral99,
    primaryContainer = RecordingPrimary80,
    onPrimaryContainer = RecordingPrimary20,
    
    // Secondary colors - supporting actions and navigation
    secondary = CallSecondary40,
    onSecondary = Neutral99,
    secondaryContainer = CallSecondary80,
    onSecondaryContainer = CallSecondary20,
    
    // Tertiary colors - accent and audio-related features
    tertiary = AudioTertiary40,
    onTertiary = Neutral99,
    tertiaryContainer = AudioTertiary80,
    onTertiaryContainer = AudioTertiary20,
    
    // Background and surface colors
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    
    // Surface variants for cards and containers
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = Neutral40,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
    
    // Outline colors for borders and dividers
    outline = Neutral50,
    outlineVariant = Neutral80,
    
    // Error colors
    error = ErrorRed,
    onError = Neutral99,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A)
)

/**
 * Main theme composable for the Call Recording App
 * Supports Material Design 3, dynamic colors (Android 12+), and responsive design
 * 
 * @param darkTheme Whether to use dark theme. Defaults to system preference
 * @param dynamicColor Whether to use dynamic colors from system (Android 12+)
 * @param content The content to wrap with the theme
 */
@Composable
fun CallRecodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ but disabled by default for brand consistency
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}