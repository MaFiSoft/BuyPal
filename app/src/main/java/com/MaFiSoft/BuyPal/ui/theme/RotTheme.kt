// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/RotTheme.kt
// Stand: 2025-07-09_12:16:00, Codezeilen: ~70 (Neues Theme für Rot)

package com.MaFiSoft.BuyPal.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RotHellColorScheme = lightColorScheme(
    primary = RotHellPrimary,
    onPrimary = RotHellOnPrimary,
    primaryContainer = RotHellPrimaryContainer,
    onPrimaryContainer = RotHellOnPrimaryContainer,
    secondary = RotHellSecondary,
    onSecondary = RotHellOnSecondary,
    secondaryContainer = RotHellSecondaryContainer,
    onSecondaryContainer = RotHellOnSecondaryContainer,
    tertiary = RotHellTertiary,
    onTertiary = RotHellOnTertiary,
    tertiaryContainer = RotHellTertiaryContainer,
    onTertiaryContainer = RotHellOnTertiaryContainer,
    error = RotHellError,
    onError = RotHellOnError,
    errorContainer = RotHellErrorContainer,
    onErrorContainer = RotHellOnErrorContainer,
    background = RotHellBackground,
    onBackground = RotHellOnBackground,
    surface = RotHellSurface,
    onSurface = RotHellOnSurface,
    surfaceVariant = RotHellSurfaceVariant,
    onSurfaceVariant = RotHellOnSurfaceVariant,
    outline = RotHellOutline,
    inverseOnSurface = RotHellInverseOnSurface,
    inverseSurface = RotHellInverseSurface,
    inversePrimary = RotHellInversePrimary,
    surfaceTint = RotHellSurfaceTint,
    outlineVariant = RotHellOutlineVariant,
    scrim = RotHellScrim
)

private val RotDunkelColorScheme = darkColorScheme(
    primary = RotDunkelPrimary,
    onPrimary = RotDunkelOnPrimary,
    primaryContainer = RotDunkelPrimaryContainer,
    onPrimaryContainer = RotDunkelOnPrimaryContainer,
    secondary = RotDunkelSecondary,
    onSecondary = RotDunkelOnSecondary,
    secondaryContainer = RotDunkelSecondaryContainer,
    onSecondaryContainer = RotDunkelOnSecondaryContainer,
    tertiary = RotDunkelTertiary,
    onTertiary = RotDunkelOnTertiary,
    tertiaryContainer = RotDunkelTertiaryContainer,
    onTertiaryContainer = RotDunkelOnTertiaryContainer,
    error = RotDunkelError,
    onError = RotDunkelOnError,
    errorContainer = RotDunkelErrorContainer,
    onErrorContainer = RotDunkelOnErrorContainer,
    background = RotDunkelBackground,
    onBackground = RotDunkelOnBackground,
    surface = RotDunkelSurface,
    onSurface = RotDunkelOnSurface,
    surfaceVariant = RotDunkelSurfaceVariant,
    onSurfaceVariant = RotDunkelOnSurfaceVariant,
    outline = RotDunkelOutline,
    inverseOnSurface = RotDunkelInverseOnSurface,
    inverseSurface = RotDunkelInverseSurface,
    inversePrimary = RotDunkelInversePrimary,
    surfaceTint = RotDunkelSurfaceTint,
    outlineVariant = RotDunkelOutlineVariant,
    scrim = RotDunkelScrim
)

@Composable
fun RotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> RotDunkelColorScheme
        else -> RotHellColorScheme
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
